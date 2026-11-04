package com.example.myapplication.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "08:32" for today, "03.11 08:32" for older dates. */
fun formatSyncTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    val day = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val sameDay = day.format(Date(millis)) == day.format(Date(now))
    val pattern = if (sameDay) "HH:mm" else "dd.MM HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
