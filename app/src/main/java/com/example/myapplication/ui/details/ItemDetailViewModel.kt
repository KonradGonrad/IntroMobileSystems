package com.example.myapplication.ui.details

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Loaded(val item: Item) : DetailUiState
}

class ItemDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ItemRepository
) : ViewModel() {

    // "itemId" comes from the navigation route item/{itemId}
    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    val uiState: StateFlow<DetailUiState> = repository.item(itemId)
        .map { item -> if (item == null) DetailUiState.NotFound else DetailUiState.Loaded(item) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    val isOnline: StateFlow<Boolean> = repository.isOnline
    val lastSyncTime: StateFlow<Long?> = repository.lastSyncTime

    var actionError by mutableStateOf<String?>(null); private set
    var chatId by mutableStateOf<String?>(null); private set
    var deleted by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    fun clearChat() { chatId=null }
    fun startChat() = action { chatId=repository.startChat(itemId) }
    fun delete() = action { repository.deleteListing(itemId); deleted=true }
    private fun action(block: suspend ()->Unit) {
        if(busy) return
        busy=true
        viewModelScope.launch {
            try { block(); actionError=null }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { actionError=e.message }
            finally { busy=false }
        }
    }

    fun onReserveClick() {
        action { repository.reserve(itemId) }
    }

    fun onCancelReservationClick() {
        viewModelScope.launch { repository.cancelReservation(itemId) }
    }

    fun onToggleLike() {
        viewModelScope.launch { repository.toggleLike(itemId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DormSwapApp
                ItemDetailViewModel(createSavedStateHandle(), app.repository)
            }
        }
    }
}
