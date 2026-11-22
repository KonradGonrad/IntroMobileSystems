package com.example.myapplication.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.BuildConfig
import com.example.myapplication.DormSwapApp
import com.example.myapplication.MainActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow

object PushRegistration {
    val token = MutableStateFlow<String?>(null)
    val configured = listOf(BuildConfig.FIREBASE_APP_ID, BuildConfig.FIREBASE_API_KEY,
        BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_SENDER_ID).all { it.isNotBlank() }

    fun initialize(context: Context) {
        if (!configured) return
        FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID).setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID).setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID).build())
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token.value = it }
    }
}

class DormPush : FirebaseMessagingService() {
    override fun onNewToken(token: String) { PushRegistration.token.value = token }

    override fun onMessageReceived(message: RemoteMessage) {
        showPush(this, (application as DormSwapApp).auth.user.value?.id, message.data)
    }
}

internal fun showPush(context: Context, accountId: String?, data: Map<String,String>) = with(context) {
    if (accountId == null || data["recipient"] != accountId) return@with
    val kind = AlertType.entries.find { it.key == data["kind"] } ?: return@with
    val eventId = data["event_id"] ?: return@with
    if (!getSharedPreferences("activity-$accountId",Context.MODE_PRIVATE).getBoolean(kind.key,true)) return@with
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) return@with
    val manager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
        NotificationChannel(kind.key,kind.label,NotificationManager.IMPORTANCE_DEFAULT))
    val open = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java)
        .setAction("com.example.myapplication.ACTIVITY")
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val alert = NotificationCompat.Builder(this,kind.key)
        .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("DormSwap")
        .setContentText(kind.alert).setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).build()
    manager.notify(eventId,0,alert)
}
