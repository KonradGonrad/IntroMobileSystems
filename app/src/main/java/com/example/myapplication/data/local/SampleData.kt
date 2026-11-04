package com.example.myapplication.data.local

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus

/**
 * SYNTHETIC sample listings put into the database on the first start,
 * so the feed is not empty. Names and rooms are made up.
 */
object SampleData {

    fun items(): List<Item> {
        val now = System.currentTimeMillis()
        return listOf(
            Item(
                id = "sample-1",
                title = "Desk lamp",
                description = "Green desk lamp with an adjustable arm. Works perfectly, light bulb included. Giving it away because I'm moving out of the dorm.",
                category = Category.ELECTRONICS,
                condition = Condition.LIKE_NEW,
                location = "Dorm 5 · Floor 3 · Room 110",
                ownerName = "Maja K.",
                createdAt = now - 1_000
            ),
            Item(
                id = "sample-2",
                title = "Oversized hoodie M",
                description = "Beige hoodie, size M. Washed, no stains.",
                category = Category.CLOTHES,
                condition = Condition.VERY_GOOD,
                location = "Dorm 5 · Floor 1 · Room 014",
                ownerName = "Ola",
                isLiked = true,
                createdAt = now - 2_000
            ),
            Item(
                id = "sample-3",
                title = "Evening reading books",
                description = "Three paperback novels in English. Pick them up all together.",
                category = Category.BOOKS,
                condition = Condition.GOOD,
                location = "Dorm 5 · Floor 2 · Room 203",
                ownerName = "Michał",
                createdAt = now - 3_000
            ),
            Item(
                id = "sample-4",
                title = "Headphones",
                description = "Wired over-ear headphones, 3.5 mm jack. Cable is 1.5 m long.",
                category = Category.ELECTRONICS,
                condition = Condition.LIKE_NEW,
                location = "Dorm 5 · Floor 4 · Room 402",
                ownerName = "Kuba",
                status = ListingStatus.RESERVED,
                createdAt = now - 4_000
            ),
            Item(
                id = "sample-5",
                title = "Desk chair",
                description = "Simple wooden chair, a bit scratched but stable.",
                category = Category.FURNITURE,
                condition = Condition.VERY_GOOD,
                location = "Dorm 5 · Floor 3 · Room 110",
                ownerName = "You",
                isMine = true,
                createdAt = now - 5_000
            )
        )
    }
}
