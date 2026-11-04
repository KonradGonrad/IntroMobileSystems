package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus

object SyncHelper {

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
