package com.example.myapplication.ui.council

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.council.*
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouncilImportScreen(onBack: () -> Unit, vm: CouncilImportViewModel = viewModel(factory = CouncilImportViewModel.Factory)) {
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::download) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::open) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Council import") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back") }
        })
    }, bottomBar = {
        vm.preview?.let { preview ->
            val updates = if (vm.overwrite) preview.changed else 0
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Add ${preview.added} · Update $updates · Skip ${preview.skipped(vm.overwrite)}")
                    Button(onClick = vm::confirm, enabled = !vm.busy && preview.added + updates > 0) { Text("Confirm import") }
                }
            }
        }
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Prepare the fair listings", style = MaterialTheme.typography.titleLarge)
                Text("Open the template in Excel. Give each item a stable reference such as FAIR-2026-001. Reuse it to update that item, even if its title changes.")
            }
            item {
                Text("Fill in the title (3–60 characters), description (10–2000), category and condition.")
                Text("Category: ${Category.entries.joinToString { it.name }}")
                Text("Condition: ${Condition.entries.joinToString { it.name }}")
            }
            item {
                OutlinedButton(onClick = { save.launch(CouncilImportViewModel.TEMPLATE) }, enabled = !vm.busy) {
                    Text("Download CSV template")
                }
            }
            item {
                Text("Save as CSV UTF-8 with commas or semicolons. Choose up to 500 listings (2 MB). Connect to review and publish.")
                Button(onClick = { open.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }, enabled = !vm.busy) {
                    Text("Choose filled CSV")
                }
            }
            if (vm.busy) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            vm.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (!vm.busy && vm.canReviewAgain) item { OutlinedButton(onClick = vm::reviewAgain) { Text("Refresh preview") } }
            vm.notice?.let { item { Text(it, style = MaterialTheme.typography.titleMedium) } }

            vm.preview?.let { preview ->
                item {
                    Text("Review import", style = MaterialTheme.typography.titleLarge)
                    Text("New: ${preview.added} · Matching references with edits: ${preview.changed}")
                }
                if (preview.changed > 0) item {
                    Row(modifier = Modifier.fillMaxWidth().toggleable(value = vm.overwrite, enabled = !vm.busy,
                        role = Role.Checkbox, onValueChange = vm::setOverwriteExisting), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = vm.overwrite, onCheckedChange = null)
                        Text("Overwrite the changed listings shown below", Modifier.padding(8.dp))
                    }
                }
                items(preview.matches, key = { it.row.ref }) { match -> PreviewListing(match, vm.overwrite) }
            }
        }
    }
}

@Composable
private fun PreviewListing(match: FairMatch, overwrite: Boolean) {
    val row = match.row
    val action = when (match.status) {
        FairStatus.NEW -> "Add"
        FairStatus.CHANGED -> if (overwrite) "Update" else "Skip existing"
        FairStatus.UNCHANGED -> "Skip · identical"
        FairStatus.RESERVED -> "Skip · reserved"
        FairStatus.DELETED -> "Skip · previously deleted"
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("$action · ${row.ref}", style = MaterialTheme.typography.labelLarge)
            Text(row.title, style = MaterialTheme.typography.titleMedium)
            match.previous?.let { old ->
                if (old.title != row.title) Text("Title: ${old.title} → ${row.title}")
                if (old.description != row.description) Text("Description: ${old.description} → ${row.description}")
                if (old.category != row.category) Text("Category: ${old.category.label} → ${row.category.label}")
                if (old.condition != row.condition) Text("Condition: ${old.condition.label} → ${row.condition.label}")
            }
            if (match.status == FairStatus.NEW) {
                Text(row.description)
                Text("${row.category.label} · ${row.condition.label}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
