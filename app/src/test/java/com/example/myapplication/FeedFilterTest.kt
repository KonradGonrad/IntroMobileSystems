package com.example.myapplication

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import com.example.myapplication.ui.feed.FeedFilter
import com.example.myapplication.ui.feed.FeedTab
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedFilterTest {

    private fun item(
        id: String,
        title: String,
        category: Category = Category.OTHER,
        isMine: Boolean = false,
        isLiked: Boolean = false,
        status: ListingStatus = ListingStatus.ACTIVE
    ) = Item(
        id = id,
        title = title,
        description = "test item",
        category = category,
        condition = Condition.GOOD,
        location = "Dorm 5",
        ownerName = "Test",
        isMine = isMine,
        isLiked = isLiked,
        status = status
    )

    private val items = listOf(
        item("1", "Desk lamp", Category.ELECTRONICS),
        item("2", "Hoodie", Category.CLOTHES, isLiked = true),
        item("3", "My chair", Category.FURNITURE, isMine = true),
        item("4", "Old jacket", Category.CLOTHES, status = ListingStatus.EXCHANGED)
    )

    @Test
    fun browseTab_hidesMyItemsAndExchangedItems() {
        val result = FeedFilter.filter(items, FeedTab.BROWSE, category = null, query = "")

        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun searchIgnoresCaseAndSpaces_andWorksWithCategory() {
        val result = FeedFilter.filter(items, FeedTab.BROWSE, Category.CLOTHES, query = "  HOOD ")

        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun noMatch_returnsEmptyList() {
        val result = FeedFilter.filter(items, FeedTab.BROWSE, category = null, query = "piano")

        assertEquals(0, result.size)
    }

    @Test
    fun myListingsAndLikedTabs_showTheRightItems() {
        assertEquals(listOf("3"), FeedFilter.filter(items, FeedTab.MY_LISTINGS, null, "").map { it.id })
        assertEquals(listOf("2"), FeedFilter.filter(items, FeedTab.LIKED, null, "").map { it.id })
    }
}
