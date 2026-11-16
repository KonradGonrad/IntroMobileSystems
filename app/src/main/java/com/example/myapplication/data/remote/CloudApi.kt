package com.example.myapplication.data.remote

import android.content.Context
import com.example.myapplication.data.auth.AuthRepository
import com.example.myapplication.data.local.*
import com.example.myapplication.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class CloudApi(private val context: Context, private val client: SupabaseClient, private val auth: AuthRepository,
               private val userId: String) {
    suspend fun rpc(name: String, body: JSONObject = JSONObject()): String {
        check(auth.user.value?.id == userId) { "Account changed. Please reopen this screen." }
        return client.json("/rest/v1/rpc/$name",body,auth.token())
    }
    suspend fun upload(action: PendingAction) {
        val payload=JSONObject(action.payload)
        if (action.kind=="save") {
            val local=payload.optString("local_photo")
            if (local.isNotBlank()) {
                val bytes=withContext(Dispatchers.IO) { File(local).readBytes() }
                val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                val path="$userId/$hash.jpg"
                try { client.request("/storage/v1/object/listing-photos/$path","POST",auth.token(),bytes,"image/jpeg") }
                catch (e: ApiException) { if (e.status != 409 && !(e.status==400 && e.message.orEmpty().contains("already exists",true))) throw e }
                payload.put("photo_path",path)
            } else payload.put("photo_path",JSONObject.NULL)
            payload.remove("local_photo")
        }
        rpc("apply_action",JSONObject().put("action_id",action.id).put("kind",action.kind)
            .put("target_id",action.targetId).put("payload",payload))
    }
    suspend fun listings(): List<Item> {
        val rows=JSONArray(rpc("feed_snapshot"))
        return (0 until rows.length()).map { i ->
            val j=rows.getJSONObject(i)
            val path=j.optString("photo_path").takeIf { it.isNotBlank() && it!="null" }
            val local=path?.let { downloadPhoto(it) }
            Item(id=j.getString("id"), title=j.getString("title"),description=j.getString("description"),
                category=Category.valueOf(j.getString("category")),condition=Condition.valueOf(j.getString("condition")),
                location="Dorm 5 · Łódź", ownerName=j.getString("owner_name"),photoPath=local,
                isMine=j.getBoolean("is_mine"),isLiked=j.getBoolean("is_liked"),
                status=if(j.getBoolean("reserved")) ListingStatus.RESERVED else ListingStatus.ACTIVE,
                reservedByMe=j.getBoolean("reserved_by_me"),createdAt=j.getLong("created_at"))
        }
    }
    private suspend fun downloadPhoto(path: String): String = withContext(Dispatchers.IO) {
        require(Regex("[a-fA-F0-9-]{36}/[a-f0-9]{64}\\.jpg").matches(path))
        val dir=File(context.filesDir,"remote-photos/$userId").apply { mkdirs() }
        val file=File(dir,path.replace('/','_'))
        if (!file.exists()) {
            val bytes=client.request("/storage/v1/object/authenticated/listing-photos/$path",token=auth.token())
            val temp=File(dir,"${file.name}.tmp")
            temp.writeBytes(bytes)
            check(temp.renameTo(file))
        }
        file.absolutePath
    }
    suspend fun chats(): Pair<List<Conversation>,List<ChatMessage>> {
        val root=JSONObject(rpc("chat_snapshot"))
        val cs=root.getJSONArray("conversations"); val ms=root.getJSONArray("messages")
        return (0 until cs.length()).map { i -> cs.getJSONObject(i).let {
            Conversation(it.getString("id"),it.getString("listing_id"),it.getString("title"),it.getString("other_name"),it.getBoolean("unavailable"))
        } } to (0 until ms.length()).map { i -> ms.getJSONObject(i).let {
            ChatMessage(it.getString("id"),it.getString("conversation_id"),it.getString("sender_id"),it.getString("body"),it.getLong("created_at"))
        } }
    }
    suspend fun startChat(listingId: String): String = rpc("start_conversation",JSONObject().put("listing_id",listingId)).trim().trim('"')
}
