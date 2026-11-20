package com.example.myapplication.data.repository

import androidx.room.withTransaction
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.data.auth.AuthUser
import com.example.myapplication.data.council.*
import com.example.myapplication.data.local.*
import com.example.myapplication.data.model.*
import com.example.myapplication.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Loading : SyncStatus
    data class Success(val newItems: Int) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/** Room is the UI's source of truth. A durable outbox retries actual Supabase writes. */
class ItemRepository(
    private val db: UserDatabase, private val api: CloudApi, private val user: AuthUser,
    networkMonitor: NetworkMonitor, private val prefs: AppPreferences, private val appScope: CoroutineScope
) {
    private val dao=db.items()
    private val cloud=db.cloud()
    private val mutex=Mutex()
    val items=dao.observeAll()
    val isOnline=networkMonitor.isOnline
    val lastSyncTime=prefs.lastSyncTime
    val simulateServerError=prefs.simulateServerError
    val conversations=cloud.conversations()
    val failedActions=cloud.observeErrors()
    private val _syncStatus=MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    fun item(id: String)=dao.observeById(id)
    fun messages(id: String)=cloud.messages(id)
    fun setSimulateServerError(enabled: Boolean) { prefs.setSimulateServerError(enabled); requestSync() }
    fun requestSync() { appScope.launch { sync() } }

    suspend fun isStudentCouncil() = api.isStudentCouncil()
    suspend fun previewFair(rows: List<FairRow>) = api.previewFair(rows)

    suspend fun importFair(preview: FairPreview, overwrite: Boolean) {
        mutex.withLock {
            val changing = preview.matches.filter { overwrite && it.status == FairStatus.CHANGED }.mapNotNull { it.id }.toSet()
            check(cloud.pending().none { it.targetId in changing && it.kind in listOf("save", "delete") }) {
                "Sync or resolve your pending edits in Account, then review this file again."
            }
            try {
                api.importFair(preview, overwrite)
            } finally {
                // A lost response or closed screen can still follow a successful commit.
                requestSync()
            }
        }
    }

    suspend fun sync() = mutex.withLock {
        if (!isOnline.value) return@withLock
        _syncStatus.value=SyncStatus.Loading
        try {
            if (simulateServerError.value) throw IOException("Demo: server returned HTTP 503")
            for (action in cloud.pending().filter { it.error==null }) {
                try {
                    api.upload(action)
                    db.withTransaction {
                        if(action.kind=="message") cloud.confirmMessage(action.targetId)
                        cloud.removeAction(action.id)
                    }
                } catch (e: ApiException) {
                    if(e.status in listOf(400,403,404,409,422)) cloud.saveAction(action.copy(error=e.message ?: "Action rejected"))
                    else throw e
                }
            }
            val server=api.listings()
            val allPending=cloud.pending()
            val pending=allPending.filter { it.error==null }
            db.withTransaction {
                val protected=pending.filter { it.kind!="message" }.map { it.targetId }.toSet()
                val failedDrafts=allPending.filter { it.kind=="save" && it.error!=null }.map { it.targetId }.toSet()
                val deleting=pending.filter { it.kind=="delete" }.map { it.targetId }.toSet()
                val merged=SyncHelper.mergeCloudItems(server,dao.getAll(),protected,failedDrafts,deleting)
                dao.clear(); dao.upsertAll(merged)
            }
            val (conversations,messages)=api.chats()
            db.withTransaction { cloud.saveConversations(conversations); cloud.saveMessages(messages) }
            prefs.setLastSyncTime(System.currentTimeMillis())
            val failures=cloud.pending().count { it.error!=null }
            _syncStatus.value=if(failures==0) SyncStatus.Success(server.size) else SyncStatus.Error("$failures changes were rejected. Open Account to review them.")
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { _syncStatus.value=SyncStatus.Error(e.message ?: "Cannot sync. Try again.") }
    }
    private suspend fun queue(kind: String,id: String,payload: JSONObject=JSONObject()) {
        cloud.saveAction(PendingAction(UUID.randomUUID().toString(),kind,id,payload.toString()))
    }
    suspend fun addListing(title: String,description: String,category: Category,condition: Condition,photoPath: String?): String {
        val id=UUID.randomUUID().toString()
        saveListing(Item(id,title.trim(),description.trim(),category,condition,"Dorm 5 · Łódź",user.name,photoPath,isMine=true,pendingSync=true))
        return id
    }
    private suspend fun saveListing(item: Item, editing: Boolean = false) {
        mutex.withLock { db.withTransaction {
            if(editing) {
                val current=dao.getById(item.id) ?: throw IOException("Listing not found")
                if(!current.isMine || current.status!=ListingStatus.ACTIVE) throw IOException("This listing is reserved or unavailable. Refresh before editing.")
            }
            dao.upsert(item.copy(pendingSync=true))
            queue("save",item.id,JSONObject().put("title",item.title).put("description",item.description)
                .put("category",item.category.name).put("condition",item.condition.name).put("local_photo",item.photoPath ?: ""))
        } }
        requestSync()
    }
    suspend fun editListing(id: String,title: String,description: String,category: Category,condition: Condition,photoPath: String?) {
        val item=dao.getById(id) ?: throw IOException("Listing not found")
        if(!item.isMine || item.status!=ListingStatus.ACTIVE) throw IOException("Only an available listing you own can be edited.")
        saveListing(item.copy(title=title.trim(),description=description.trim(),category=category,condition=condition,photoPath=photoPath),editing=true)
    }
    private suspend fun change(id: String,kind: String,transform: (Item)->Item?) {
        mutex.withLock { db.withTransaction {
            val old=dao.getById(id) ?: return@withTransaction
            val item=transform(old) ?: return@withTransaction
            if(kind=="delete") dao.delete(id) else dao.upsert(item.copy(pendingSync=true))
            queue(kind,id,JSONObject().put("liked",item.isLiked))
        } }
        requestSync()
    }
    suspend fun toggleLike(id: String)=change(id,"like") { it.copy(isLiked=!it.isLiked) }
    suspend fun reserve(id: String)=change(id,"reserve") { if(it.isMine || it.status!=ListingStatus.ACTIVE) null else it.copy(status=ListingStatus.RESERVED,reservedByMe=true) }
    suspend fun cancelReservation(id: String)=change(id,"cancel") { if(!it.reservedByMe) null else it.copy(status=ListingStatus.ACTIVE,reservedByMe=false) }
    suspend fun deleteListing(id: String)=change(id,"delete") { if(it.isMine) it else null }
    suspend fun startChat(id: String): String = mutex.withLock {
        if(!isOnline.value) throw IOException("Connect to start a new conversation. Existing chats work offline.")
        val result=api.startChat(id)
        val (cs,ms)=api.chats(); cloud.saveConversations(cs); cloud.saveMessages(ms)
        result
    }
    suspend fun sendMessage(conversationId: String,body: String) {
        require(body.trim().length in 1..2000) { "Message must have 1–2000 characters." }
        mutex.withLock { db.withTransaction {
            val id=UUID.randomUUID().toString()
            cloud.saveMessages(listOf(ChatMessage(id,conversationId,user.id,body.trim(),System.currentTimeMillis(),true)))
            queue("message",id,JSONObject().put("conversation_id",conversationId).put("body",body.trim()))
        } }
        requestSync()
    }
    suspend fun dismissFailure(id: String) {
        mutex.withLock { db.withTransaction {
            cloud.pending().find { it.id==id }?.let {
                if(it.kind=="message") cloud.removeMessage(it.targetId)
                if(it.kind=="save") dao.delete(it.targetId)
            }
            cloud.removeAction(id)
        } }
        requestSync()
    }
    suspend fun retryFailure(id: String) {
        mutex.withLock { cloud.pending().find { it.id==id }?.let { cloud.saveAction(it.copy(error=null)) } }
        requestSync()
    }
}
