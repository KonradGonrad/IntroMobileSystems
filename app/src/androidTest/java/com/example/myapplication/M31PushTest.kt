package com.example.myapplication

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.notifications.ActivityInbox
import com.example.myapplication.data.notifications.AlertType
import com.example.myapplication.data.notifications.showPush
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class M31PushTest {
    @Test fun systemAlertsRespectAccountCategoryAndReplay() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(NotificationManager::class.java)
        val account = UUID.randomUUID().toString()
        val inbox = ActivityInbox(context, account)
        val event = UUID.randomUUID().toString()
        fun matching() = manager.activeNotifications.filter { it.tag == event }
        val payload = mapOf("recipient" to account, "kind" to "message", "event_id" to event)
        try {
            showPush(context, null, payload)
            showPush(context, "another-account", payload)
            assertTrue(matching().isEmpty())
            inbox.toggle(AlertType.Message, false)
            showPush(context, account, payload)
            assertTrue(matching().isEmpty())
            assertFalse(AlertType.Message in ActivityInbox(context, account).enabled.value)
            showPush(context, account, payload + ("kind" to "like"))
            showPush(context, account, payload + ("kind" to "like"))
            assertEquals(1, matching().size)
            assertNotNull(matching().single().notification.contentIntent)
        } finally {
            manager.cancel(event, 0)
            context.getSharedPreferences("activity-$account", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
