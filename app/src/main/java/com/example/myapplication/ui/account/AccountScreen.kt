package com.example.myapplication.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.DormGreenLight
import com.example.myapplication.core.theme.TextMuted

@Composable
fun AccountScreen(onBackClick: () -> Unit, onLogout: () -> Unit, onCouncilImport: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.myapplication.DormSwapApp
    val user = app.auth.user.value
    val failures by app.repository.failedActions.collectAsStateWithLifecycle(initialValue=emptyList())
    val scope=rememberCoroutineScope()
    val repo = app.repository
    val online by repo.isOnline.collectAsStateWithLifecycle()
    var accessCheck by remember { mutableIntStateOf(0) }
    var isCouncil by remember { mutableStateOf(false) }
    var accessError by remember { mutableStateOf(false) }
    LaunchedEffect(repo, online, accessCheck) {
        accessError = false
        if (online) {
            try { isCouncil = repo.isStudentCouncil() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { isCouncil = false; accessError = true }
        }
    }
    var discardId by remember { mutableStateOf<String?>(null) }
    discardId?.let { id ->
        AlertDialog(onDismissRequest={discardId=null},title={Text("Discard rejected change?")},
            text={Text("This removes the local attempt. Any confirmed server data will be downloaded again.")},
            confirmButton={TextButton(onClick={discardId=null; scope.launch { app.repository.dismissFailure(id) }}) { Text("Discard") }},
            dismissButton={TextButton(onClick={discardId=null}) { Text("Keep") }})
    }
    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
                Text("My account", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(color = DormGreenLight, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null,
                        tint = DormGreen, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(user?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(user?.email.orEmpty(), color = TextMuted)
                    if (isCouncil) Text("Student council", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Dormitory 5 · Łódź", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            if (isCouncil) {
                Button(onClick = onCouncilImport) { Text("Manage fair listings") }
            } else if (accessError) {
                Text("Could not check access to council tools.")
                TextButton(onClick = { accessCheck++ }) { Text("Retry access check") }
            }
            failures.forEach { failure ->
                Text("${failure.kind}: ${failure.error}",color=MaterialTheme.colorScheme.error)
                TextButton(onClick={scope.launch { app.repository.retryFailure(failure.id) }}) { Text("Retry change") }
                TextButton(onClick={discardId=failure.id}) { Text("Discard change") }
            }
            Text("Unsynced changes stay on this device for this account. Sign back in to send them.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Text("Sign out", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
