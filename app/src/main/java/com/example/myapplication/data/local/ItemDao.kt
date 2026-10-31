package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myapplication.data.model.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: String): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): Item?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<Item>

    @Query("SELECT * FROM items WHERE pendingSync = 1")
    suspend fun getPending(): List<Item>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(item: Item)

    @Upsert
    suspend fun upsertAll(items: List<Item>)
}
