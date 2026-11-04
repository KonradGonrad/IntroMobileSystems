package com.example.myapplication.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Small values that don't need a database table (SharedPreferences).
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("dormswap_prefs", Context.MODE_PRIVATE)

    private val _lastSyncTime = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 })
    /** When listings were last downloaded from the server (null = never). */
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime

    private val _simulateServerError = MutableStateFlow(prefs.getBoolean(KEY_SIMULATE_ERROR, false))
    /** DEMO switch: makes every server call fail, to show the error state. */
    val simulateServerError: StateFlow<Boolean> = _simulateServerError

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
        _lastSyncTime.value = time
    }

    fun setSimulateServerError(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATE_ERROR, enabled).apply()
        _simulateServerError.value = enabled
    }

    private companion object {
        const val KEY_LAST_SYNC = "last_sync_time"
        const val KEY_SIMULATE_ERROR = "simulate_server_error"
    }
}
