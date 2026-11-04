package com.example.myapplication

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import com.example.myapplication.data.repository.SyncHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncHelperTest {

    private fun serverItem(id: String, title: String = "Kettle") = Item(
        id = id,
        title = title,
        description = "from server",
        category = Category.KITCHEN,
        condition = Condition.GOOD,
        location = "Dorm 5",
        ownerName = "Anna"
    )

    @Test
    fun newServerItem_isAdded() {
        val result = SyncHelper.mergeServerItems(listOf(serverItem("server-1")), localItems = emptyList())

        assertEquals(listOf("server-1"), result.map { it.id })
    }

    @Test
    fun localChangeNotSentYet_isNotOverwritten() {
        val local = serverItem("server-1").copy(
            status = ListingStatus.RESERVED,
            reservedByMe = true,
            pendingSync = true
        )

        val result = SyncHelper.mergeServerItems(listOf(serverItem("server-1")), listOf(local))

        assertTrue(result.isEmpty())
    }

    @Test
    fun syncedItem_takesServerData_butKeepsLikeAndOurReservation() {
        val local = serverItem("server-1", title = "Old title").copy(
            isLiked = true,
            reservedByMe = true,
            status = ListingStatus.RESERVED
        )

        val result = SyncHelper.mergeServerItems(listOf(serverItem("server-1", title = "New title")), listOf(local))

        val merged = result.single()
        assertEquals("New title", merged.title)
        assertTrue(merged.isLiked)
        assertEquals(ListingStatus.RESERVED, merged.status)
    }
}
