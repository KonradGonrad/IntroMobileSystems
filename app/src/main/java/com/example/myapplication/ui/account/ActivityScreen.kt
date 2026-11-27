package com.example.myapplication.ui.account

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.data.notifications.*
import com.example.myapplication.data.repository.*
import kotlinx.coroutines.delay

@Composable
fun SyncStrip(repo: ItemRepository, onActivity: () -> Unit) {
    val online by repo.isOnline.collectAsStateWithLifecycle()
    val status by repo.syncStatus.collectAsStateWithLifecycle()
    val notices by repo.activity.notices.collectAsStateWithLifecycle()
    val enabled by repo.activity.enabled.collectAsStateWithLifecycle()
    val readIds by repo.activity.readIds.collectAsStateWithLifecycle()
    val hasUnread = notices.any { it.kind in enabled && it.id !in readIds }
    val latest = notices.firstOrNull()
    var previous by remember(repo) { mutableStateOf(latest?.id) }
    var alert by remember(repo) { mutableStateOf<String?>(null) }
    LaunchedEffect(latest?.id) {
        val changed = latest != null && latest.id != previous
        previous = latest?.id
        alert = latest?.takeIf { changed && it.kind in enabled }?.summary
        if (alert != null) { delay(4500); alert = null }
    }
    Surface(tonalElevation=2.dp) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=12.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(alert?.takeIf { latest?.kind in enabled } ?: when {
                !online -> "Offline · Changes saved locally"
                status is SyncStatus.Loading -> "Syncing…"
                status is SyncStatus.Error -> "Sync needs attention"
                status is SyncStatus.Success -> "Up to date"
                else -> "Waiting for sync"
            },modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
            TextButton(
                onClick = onActivity,
                modifier = Modifier.semantics {
                    if (hasUnread) stateDescription = "Unread notifications"
                }
            ) {
                BadgedBox(badge = {
                    if (hasUnread) Badge(containerColor = MaterialTheme.colorScheme.error)
                }) { Text("Notifications") }
            }
        }
    }
}

@Composable
fun ActivityScreen(inbox: ActivityInbox, onBack: () -> Unit) {
    val enabled by inbox.enabled.collectAsStateWithLifecycle()
    val notices by inbox.notices.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val visibleNotices = notices.filter { it.kind in enabled }
    val lifecycleState by owner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(inbox, visibleNotices, lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) inbox.markRead(visibleNotices)
    }
    fun hasPermission() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var allowed by remember { mutableStateOf(hasPermission()) }
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ -> allowed = hasPermission() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal=16.dp)) {
        item {
            TextButton(onClick=onBack) { Text("Back") }
            Text("Notifications",style=MaterialTheme.typography.headlineSmall)
            Text("Show on this device",style=MaterialTheme.typography.titleMedium)
            AlertType.entries.forEach { type ->
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=type in enabled, role=Role.Switch,
                    onValueChange={ inbox.toggle(type,it) }),verticalAlignment=Alignment.CenterVertically) {
                    Text(type.label,Modifier.weight(1f))
                    Switch(checked=type in enabled,onCheckedChange=null)
                }
            }
            if (!PushRegistration.configured) {
                Text("System push alerts are unavailable in this build.", style=MaterialTheme.typography.bodySmall)
            } else if (Build.VERSION.SDK_INT >= 33 && !allowed) {
                TextButton(onClick={ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow Android notifications") }
                TextButton(onClick={ context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName)) }) { Text("Android notification settings") }
            }
            HorizontalDivider()
            Text("Recent activity",style=MaterialTheme.typography.titleMedium)
            if (visibleNotices.isEmpty()) Text("No recent notifications.")
        }
        items(visibleNotices, key={it.id}) { notice ->
            ListItem(headlineContent={Text(notice.summary)},supportingContent={Text(notice.kind.label)})
            HorizontalDivider()
        }
    }
}
