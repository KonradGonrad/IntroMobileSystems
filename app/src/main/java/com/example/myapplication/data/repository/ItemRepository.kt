package com.example.myapplication.data.repository

import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.data.local.AppPreferences
import com.example.myapplication.data.local.ItemDao
import com.example.myapplication.data.local.SampleData
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import com.example.myapplication.data.remote.FakeUploadService
import com.example.myapplication.data.remote.ListingsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.util.UUID

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Loading : SyncStatus
    data class Success(val newItems: Int) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/**
 * Single place the ViewModels get data from.
 * Room is the source of truth - the UI always reads from the database,
 * the network is only used to update the database (offline first).
 */
class ItemRepository(
    private val dao: ItemDao,
    private val api: ListingsApi,
    private val uploadService: FakeUploadService,
    private val networkMonitor: NetworkMonitor,
    private val prefs: AppPreferences,
    private val appScope: CoroutineScope
) {

    val items: Flow<List<Item>> = dao.observeAll()
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val lastSyncTime: StateFlow<Long?> = prefs.lastSyncTime
    val simulateServerError: StateFlow<Boolean> = prefs.simulateServerError

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private var isSyncing = false

    fun item(id: String): Flow<Item?> = dao.observeById(id)

    suspend fun addSampleDataIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(SampleData.items())
        }
    }

    fun setSimulateServerError(enabled: Boolean) = prefs.setSimulateServerError(enabled)

    /** Starts a sync that keeps running even if the user leaves the screen. */
    fun requestSync() {
        appScope.launch { sync() }
    }

    /**
     * 1. sends changes made on this phone (simulated upload)
     * 2. downloads listings from the mock API and saves them in Room
     */
    suspend fun sync() {
        if (isSyncing) return
        if (!isOnline.value) {
            _syncStatus.value = SyncStatus.Error("You're offline")
            return
        }

        isSyncing = true
        _syncStatus.value = SyncStatus.Loading
        try {
            for (item in dao.getPending()) {
                uploadService.send(item)
                // only mark as synced if the user didn't change it again in the meantime
                if (dao.getById(item.id) == item) {
                    dao.upsert(item.copy(pendingSync = false))
                }
            }

            val serverItems = api.fetchListings()
            val localItems = dao.getAll()
            val newCount = serverItems.count { server -> localItems.none { it.id == server.id } }
            dao.upsertAll(SyncHelper.mergeServerItems(serverItems, localItems))

            prefs.setLastSyncTime(System.currentTimeMillis())
            _syncStatus.value = SyncStatus.Success(newCount)
        } catch (e: IOException) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Network error")
        } catch (e: JSONException) {
            _syncStatus.value = SyncStatus.Error("Server sent wrong data")
        } finally {
            isSyncing = false
        }
    }

    /** Saves a new listing on the phone. Returns the id of the new item. */
    suspend fun addListing(
        title: String,
        description: String,
        category: Category,
        condition: Condition,
        photoPath: String?
    ): String {
        val item = Item(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            category = category,
            condition = condition,
            location = "Dorm 5 · Łódź",
            ownerName = "You",
            photoPath = photoPath,
            isMine = true,
            pendingSync = true
        )
        dao.upsert(item)
        if (isOnline.value) requestSync()
        return item.id
    }

    suspend fun toggleLike(id: String) {
        val item = dao.getById(id) ?: return
        dao.upsert(item.copy(isLiked = !item.isLiked))
    }

    suspend fun reserve(id: String) {
        val item = dao.getById(id) ?: return
        if (item.status != ListingStatus.ACTIVE) return
        dao.upsert(item.copy(status = ListingStatus.RESERVED, reservedByMe = true, pendingSync = true))
        if (isOnline.value) requestSync()
    }

    suspend fun cancelReservation(id: String) {
        val item = dao.getById(id) ?: return
        if (!item.reservedByMe) return
        dao.upsert(item.copy(status = ListingStatus.ACTIVE, reservedByMe = false, pendingSync = true))
        if (isOnline.value) requestSync()
    }
}
