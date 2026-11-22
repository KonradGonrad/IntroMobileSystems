package com.example.myapplication.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.model.Item
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "outbox")
data class PendingAction(
    @PrimaryKey val id: String,
    val kind: String,
    val targetId: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val listingId: String,
    val title: String,
    val otherName: String,
    val unavailable: Boolean = false
)

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val createdAt: Long,
    val pending: Boolean = false
)

@Dao
interface CloudDao {
    @Query("SELECT * FROM outbox ORDER BY createdAt, rowid")
    suspend fun pending(): List<PendingAction>
    @Query("SELECT * FROM outbox WHERE error IS NOT NULL ORDER BY createdAt")
    fun observeErrors(): Flow<List<PendingAction>>
    @Upsert suspend fun saveAction(action: PendingAction)
    @Query("DELETE FROM outbox WHERE id = :id") suspend fun removeAction(id: String)
    @Query("SELECT * FROM conversations ORDER BY title")
    fun conversations(): Flow<List<Conversation>>
    @Upsert suspend fun saveConversations(rows: List<Conversation>)
    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY createdAt, id")
    fun messages(id: String): Flow<List<ChatMessage>>
    @Upsert suspend fun saveMessages(rows: List<ChatMessage>)
    @Query("DELETE FROM messages WHERE id = :id") suspend fun removeMessage(id: String)
    @Query("UPDATE messages SET pending = 0 WHERE id = :id") suspend fun confirmMessage(id: String)
}

internal val reservationMigration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN reserver TEXT")
        db.execSQL("ALTER TABLE items ADD COLUMN expires INTEGER")
        db.execSQL("ALTER TABLE items ADD COLUMN blockedUntil INTEGER")
    }
}

internal val councilListingsMigration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN isCouncil INTEGER NOT NULL DEFAULT 0")
    }
}

/** Separate Room files prevent one account from seeing another account's cached data. */
@Database(entities = [Item::class, PendingAction::class, Conversation::class, ChatMessage::class], version = 3, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun items(): ItemDao
    abstract fun cloud(): CloudDao
    companion object {
        fun create(context: Context, userId: String): UserDatabase {
            require(Regex("[a-fA-F0-9-]{36}").matches(userId))
            return Room.databaseBuilder(context, UserDatabase::class.java, "dormswap-$userId.db")
                .addMigrations(councilListingsMigration, reservationMigration).build()
        }
    }
}
