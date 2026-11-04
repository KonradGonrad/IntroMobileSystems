package com.example.myapplication.ui.feed

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus

enum class FeedTab(val label: String) {
    BROWSE("Browse"),
    MY_LISTINGS("My listings"),
    LIKED("Liked listings")
}

/**
 * Filtering logic of the feed. Kept outside the ViewModel so it can be unit tested.
 */
object FeedFilter {

    fun filter(items: List<Item>, tab: FeedTab, category: Category?, query: String): List<Item> {
        val search = query.trim().lowercase()

        return items
            .filter { item ->
                when (tab) {
                    // other people's things that can still be taken
                    FeedTab.BROWSE -> !item.isMine && item.status != ListingStatus.EXCHANGED
                    FeedTab.MY_LISTINGS -> item.isMine
                    FeedTab.LIKED -> item.isLiked
                }
            }
            .filter { item -> category == null || item.category == category }
            .filter { item ->
                search.isEmpty() ||
                    item.title.lowercase().contains(search) ||
                    item.description.lowercase().contains(search)
            }
    }
}
