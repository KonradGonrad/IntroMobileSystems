package com.example.myapplication.data.remote

import com.example.myapplication.data.local.AppPreferences
import com.example.myapplication.data.model.Item
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * SIMULATED / MOCK. DormSwap has no backend in M2, so "sending" a new listing
 * or a reservation only waits a moment and then says OK.
 * When the demo switch "simulate server error" is on, it fails instead.
 */
class FakeUploadService(private val prefs: AppPreferences) {

    suspend fun send(item: Item) {
        delay(500)
        if (prefs.simulateServerError.value) {
            throw IOException("Server error 503 (simulated)")
        }
    }
}
