package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Category(val label: String) {
    CLOTHES("Clothes"),
    ELECTRONICS("Electronics"),
    BOOKS("Books"),
    KITCHEN("Kitchen"),
    FURNITURE("Furniture"),
    OTHER("Other")
}

enum class Condition(val label: String) {
    NEW("New"),
    LIKE_NEW("Like new"),
    VERY_GOOD("Very good condition"),
    GOOD("Good condition"),
    USED("Used")
}

enum class ListingStatus(val label: String) {
    ACTIVE("Active"),
    RESERVED("Reserved"),
    EXCHANGED("Exchanged")
}

/**
 * One listing in the dorm. Stored in Room (table "items").
 *
 * pendingSync = true means the change was only saved on this phone
 * and was not sent to the server yet.
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val condition: Condition,
    val location: String,
    val ownerName: String,
    val photoPath: String? = null,
    val isMine: Boolean = false,
    val isLiked: Boolean = false,
    val status: ListingStatus = ListingStatus.ACTIVE,
    val reservedByMe: Boolean = false,
    val pendingSync: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
