package com.example.myapplication.ui.addItem

import android.net.Uri
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
import com.example.myapplication.data.photo.PhotoStorage
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

/** Messages shown when the camera can't be used (we then offer the gallery instead). */
enum class PhotoProblem(val message: String) {
    PERMISSION_DENIED("Camera permission was denied. You can choose a photo from the gallery instead, or allow the camera in Settings."),
    NO_CAMERA("The camera is not available on this device. You can choose a photo from the gallery instead."),
    SAVE_FAILED("Couldn't read this photo. Please try another one.")
}

class AddItemViewModel(
    private val repository: ItemRepository,
    private val photoStorage: PhotoStorage
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = repository.isOnline

    var form by mutableStateOf(AddItemForm())
        private set

    var errors by mutableStateOf<Map<FormField, String>>(emptyMap())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var isSaved by mutableStateOf(false)
        private set

    var isProcessingPhoto by mutableStateOf(false)
        private set

    var photoProblem by mutableStateOf<PhotoProblem?>(null)
        private set

    fun createCameraUri(): Uri = photoStorage.createCameraUri()

    /** Called with the picture from the camera or from the gallery. */
    fun onPhotoSelected(uri: Uri) {
        photoProblem = null
        isProcessingPhoto = true
        viewModelScope.launch {
            val path = photoStorage.savePhoto(uri)
            if (path != null) {
                form = form.copy(photoPath = path)
            } else {
                photoProblem = PhotoProblem.SAVE_FAILED
            }
            isProcessingPhoto = false
        }
    }

    fun onRemovePhoto() {
        form = form.copy(photoPath = null)
    }

    fun onCameraPermissionDenied() {
        photoProblem = PhotoProblem.PERMISSION_DENIED
    }

    fun onCameraUnavailable() {
        photoProblem = PhotoProblem.NO_CAMERA
    }

    fun onPhotoProblemDismissed() {
        photoProblem = null
    }

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
        if (isSaving || isProcessingPhoto) return

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
                AddItemViewModel(app.repository, app.photoStorage)
            }
        }
    }
}
