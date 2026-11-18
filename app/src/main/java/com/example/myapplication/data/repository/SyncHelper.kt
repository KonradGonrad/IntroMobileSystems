package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus

object SyncHelper {
    /** Real server state wins after confirmation/rejection; only unsent edits protect local data. */
    fun mergeCloudItems(server: List<Item>, local: List<Item>, pendingIds: Set<String>,
                        failedDraftIds: Set<String>, deletingIds: Set<String>): List<Item> {
        val byId=local.associateBy { it.id }
        val serverIds=server.map { it.id }.toSet()
        val merged=server.map { if(it.id in pendingIds) byId[it.id] ?: it else it } +
            local.filter { (it.id in pendingIds || it.id in failedDraftIds) && it.id !in serverIds }
        return merged.filter { it.id !in deletingIds }
    }

    // Kept for the historical M2 mock merge tests; not used by M3.


    /**
     * Decides which downloaded items should be written to the database.
     *  - new item from the server -> add it
     *  - item changed on this phone and not sent yet (pendingSync) -> keep our version
     *  - otherwise -> take the server version but keep "liked" and our reservation,
     *    because the mock server doesn't know about them
     */
    fun mergeServerItems(serverItems: List<Item>, localItems: List<Item>): List<Item> {
        val localById = localItems.associateBy { it.id }
        val result = mutableListOf<Item>()

        for (serverItem in serverItems) {
            val local = localById[serverItem.id]
            if (local == null) {
                result.add(serverItem)
            } else if (!local.pendingSync) {
                result.add(
                    serverItem.copy(
                        isLiked = local.isLiked,
                        reservedByMe = local.reservedByMe,
                        status = if (local.reservedByMe) ListingStatus.RESERVED else serverItem.status
                    )
                )
            }
        }
        return result
    }
}
