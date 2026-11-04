package com.example.myapplication.ui.addItem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AddItemForm(
    val title: String = "",
    val description: String = "",
    val category: Category? = null,
    val condition: Condition? = null,
    val photoPath: String? = null
)

class AddItemViewModel(private val repository: ItemRepository) : ViewModel() {

    val isOnline: StateFlow<Boolean> = repository.isOnline

    var form by mutableStateOf(AddItemForm())
        private set

    var errors by mutableStateOf<Map<FormField, String>>(emptyMap())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var isSaved by mutableStateOf(false)
        private set

    fun onTitleChange(value: String) {
        form = form.copy(title = value)
        errors = errors - FormField.TITLE
    }

    fun onDescriptionChange(value: String) {
        form = form.copy(description = value)
        errors = errors - FormField.DESCRIPTION
    }

    fun onCategoryChange(value: Category) {
        form = form.copy(category = value)
        errors = errors - FormField.CATEGORY
    }

    fun onConditionChange(value: Condition) {
        form = form.copy(condition = value)
        errors = errors - FormField.CONDITION
    }

    fun onPublishClick() {
        if (isSaving) return

        errors = ListingValidator.validate(form.title, form.description, form.category, form.condition)
        if (errors.isNotEmpty()) return

        isSaving = true
        viewModelScope.launch {
            repository.addListing(
                title = form.title,
                description = form.description,
                category = form.category!!,
                condition = form.condition!!,
                photoPath = form.photoPath
            )
            isSaving = false
            isSaved = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DormSwapApp
                AddItemViewModel(app.repository)
            }
        }
    }
}
