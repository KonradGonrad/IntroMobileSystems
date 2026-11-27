package com.example.myapplication.data.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

enum class AlertType(val key: String, val label: String, val alert: String) {
    Reservation("reserve","Reservations","New reservation on your listing"),
    Message("message","Messages","New message"),
    Like("like","Likes","Someone liked your listing")
}

data class ActivityNotice(val id: String, val kind: AlertType, val summary: String)

class ActivityInbox(context: Context, userId: String) {
    private val prefs = context.getSharedPreferences("activity-$userId", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(AlertType.entries.filter { prefs.getBoolean(it.key, true) }.toSet())
    val enabled = _enabled.asStateFlow()
    private val _notices = MutableStateFlow(decode(prefs.getString("inbox","[]") ?: "[]"))
    val notices = _notices.asStateFlow()
    private val _readIds = MutableStateFlow(prefs.getStringSet("read_ids", emptySet()).orEmpty().toSet())
    val readIds = _readIds.asStateFlow()

    fun markRead(displayed: List<ActivityNotice>) {
        val ids = displayed.mapTo(_readIds.value.toMutableSet()) { it.id }
        ids.retainAll(notices.value.map { it.id }.toSet())
        if (ids != _readIds.value) {
            prefs.edit().putStringSet("read_ids", ids).apply()
            _readIds.value = ids
        }
    }

    fun toggle(kind: AlertType, allow: Boolean) {
        prefs.edit().putBoolean(kind.key,allow).apply()
        _enabled.value = if (allow) _enabled.value + kind else _enabled.value - kind
    }

    fun update(json: String) {
        val rows = decode(json)
        if (_notices.value != rows) {
            prefs.edit().putString("inbox",json).apply()
            _notices.value = rows
        }
    }

    private fun decode(json: String): List<ActivityNotice> {
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                val kind = AlertType.entries.find { it.key == row.getString("kind") } ?: continue
                add(ActivityNotice(row.getString("id"),kind,row.getString("summary")))
            }
        }
    }
}
