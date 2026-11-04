package com.example.myapplication

import android.app.Application
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.AppPreferences
import com.example.myapplication.data.photo.PhotoStorage
import com.example.myapplication.data.remote.FakeUploadService
import com.example.myapplication.data.remote.ListingsApi
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class - creates the database, network classes and repository once
 * so every screen uses the same instances (simple manual dependency injection).
 */
class DormSwapApp : Application() {

    // lives as long as the app process, used for sync
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var repository: ItemRepository
        private set

    lateinit var photoStorage: PhotoStorage
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        val prefs = AppPreferences(this)
        val networkMonitor = NetworkMonitor(this)
        photoStorage = PhotoStorage(this)

        repository = ItemRepository(
            dao = database.itemDao(),
            api = ListingsApi(MOCK_API_URL, prefs),
            uploadService = FakeUploadService(prefs),
            networkMonitor = networkMonitor,
            prefs = prefs,
            appScope = appScope
        )

        appScope.launch {
            repository.addSampleDataIfEmpty()
            // sync on start and every time the internet comes back
            networkMonitor.isOnline.collect { online ->
                if (online) repository.sync()
            }
        }
    }

    companion object {
        // MOCK API - static JSON file with synthetic data stored in our repository
        const val MOCK_API_URL =
            "https://raw.githubusercontent.com/KonradGonrad/IntroMobileSystems/main/M2/mock-api/listings.json"
    }
}
