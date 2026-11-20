package com.example.myapplication.ui.council

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.council.*
import kotlinx.coroutines.*

class CouncilImportViewModel(private val app: DormSwapApp) : ViewModel() {
    private val repo = app.repository
    private var rows = emptyList<FairRow>()
    var preview by mutableStateOf<FairPreview?>(null)
        private set
    var overwrite by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    val canReviewAgain get() = rows.isNotEmpty() && preview == null

    fun setOverwriteExisting(value: Boolean) { if (!busy) overwrite = value }

    private fun runTask(task: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        notice = null
        viewModelScope.launch {
            try { task() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not complete this step. Try again." }
            finally { busy = false }
        }
    }

    fun download(uri: Uri) = runTask {
        withContext(Dispatchers.IO) {
            val output = app.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot save the template here.")
            output.use { dest -> app.assets.open(TEMPLATE).use { it.copyTo(dest) } }
        }
        notice = "Template saved. Fill in one row per item and save as CSV UTF-8."
    }

    fun open(uri: Uri) = runTask {
        preview = null
        overwrite = false
        rows = emptyList()
        rows = withContext(Dispatchers.IO) {
            val input = app.contentResolver.openInputStream(uri) ?: error("Cannot open this file.")
            input.use(FairCsv::read)
        }
        preview = repo.previewFair(rows)
    }

    fun reviewAgain() = runTask {
        overwrite = false
        preview = repo.previewFair(rows)
    }

    fun confirm() {
        val reviewed = preview ?: return
        val update = overwrite
        runTask {
            // A lost response can mean the server already committed. Re-preview before retrying.
            preview = null
            overwrite = false
            repo.importFair(reviewed, update)
            rows = emptyList()
            notice = "Import complete. Added ${reviewed.added}, updated ${if (update) reviewed.changed else 0}, skipped ${reviewed.skipped(update)}."
        }
    }

    companion object {
        const val TEMPLATE = "student-council-template.csv"
        val Factory = viewModelFactory { initializer { CouncilImportViewModel(this[APPLICATION_KEY] as DormSwapApp) } }
    }
}
