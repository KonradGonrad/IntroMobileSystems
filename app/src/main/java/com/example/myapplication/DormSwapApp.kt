package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class - creates the database and repository once
 * so every screen uses the same instances (simple manual dependency injection).
 */
class DormSwapApp : Application() {

    val appScope = CoroutineScope(SupervisorJob())

    lateinit var repository: ItemRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        repository = ItemRepository(database.itemDao())

        appScope.launch { repository.addSampleDataIfEmpty() }
    }
}
