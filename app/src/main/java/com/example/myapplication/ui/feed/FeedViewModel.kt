package com.example.myapplication.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedUiState(
    val items: List<Item> = emptyList(),
    val tab: FeedTab = FeedTab.BROWSE,
    val category: Category? = null,
    val isLoading: Boolean = true
)

class FeedViewModel(private val repository: ItemRepository) : ViewModel() {

    // search text is kept as Compose state so the text field does not lag while typing
    var query by mutableStateOf("")
        private set

    private val tab = MutableStateFlow(FeedTab.BROWSE)
    private val category = MutableStateFlow<Category?>(null)

    val uiState: StateFlow<FeedUiState> =
        combine(repository.items, tab, category, snapshotFlow { query }) { items, tab, category, query ->
            FeedUiState(
                items = FeedFilter.filter(items, tab, category, query),
                tab = tab,
                category = category,
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    fun onTabSelected(newTab: FeedTab) {
        tab.value = newTab
    }

    fun onCategorySelected(newCategory: Category?) {
        category.value = newCategory
    }

    fun onQueryChange(newQuery: String) {
        query = newQuery
    }

    fun onToggleLike(id: String) {
        viewModelScope.launch { repository.toggleLike(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DormSwapApp
                FeedViewModel(app.repository)
            }
        }
    }
}
