package com.example.myapplication.data.repository

import com.example.myapplication.data.local.ItemDao
import com.example.myapplication.data.local.SampleData
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Single place the ViewModels get data from.
 * Room is the source of truth - the UI always reads from the database.
 */
class ItemRepository(private val dao: ItemDao) {

    val items: Flow<List<Item>> = dao.observeAll()

    fun item(id: String): Flow<Item?> = dao.observeById(id)

    suspend fun addSampleDataIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(SampleData.items())
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
    }

    suspend fun cancelReservation(id: String) {
        val item = dao.getById(id) ?: return
        if (!item.reservedByMe) return
        dao.upsert(item.copy(status = ListingStatus.ACTIVE, reservedByMe = false, pendingSync = true))
    }
}
