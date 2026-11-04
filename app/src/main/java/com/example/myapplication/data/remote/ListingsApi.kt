package com.example.myapplication.data.remote

import com.example.myapplication.data.local.AppPreferences
import com.example.myapplication.data.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * MOCK API: downloads a static JSON file with SYNTHETIC listings
 * (M2/mock-api/listings.json in our GitHub repo). It is a real HTTPS request,
 * but there is no real DormSwap server behind it.
 */
class ListingsApi(
    private val url: String,
    private val prefs: AppPreferences
) {

    suspend fun fetchListings(): List<Item> = withContext(Dispatchers.IO) {
        if (prefs.simulateServerError.value) {
            delay(800)
            throw IOException("Server error 503 (simulated)")
        }

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            ListingJsonParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }
}
