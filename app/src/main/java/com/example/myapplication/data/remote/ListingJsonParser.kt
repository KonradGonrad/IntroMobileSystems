package com.example.myapplication.data.remote

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import org.json.JSONObject

/**
 * Turns the JSON from the mock API into Item objects.
 * Unknown category / condition values don't crash the app - we use a default.
 */
object ListingJsonParser {

    fun parse(json: String): List<Item> {
        val array = JSONObject(json).getJSONArray("listings")
        val result = mutableListOf<Item>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.optString("id")
            val title = obj.optString("title")
            if (id.isBlank() || title.isBlank()) continue // skip broken entries

            result.add(
                Item(
                    id = "server-$id",
                    title = title,
                    description = obj.optString("description"),
                    category = enumOrDefault(obj.optString("category"), Category.OTHER),
                    condition = enumOrDefault(obj.optString("condition"), Condition.GOOD),
                    location = obj.optString("location", "Dorm 5"),
                    ownerName = obj.optString("ownerName", "Student"),
                    status = enumOrDefault(obj.optString("status"), ListingStatus.ACTIVE),
                    createdAt = obj.optLong("createdAt", 0L)
                )
            )
        }
        return result
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value.uppercase() } ?: default
}
