package com.example.myapplication.ui.addItem

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.core.theme.BorderGrey
import com.example.myapplication.core.theme.CardGrey
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.TextMuted
import com.example.myapplication.data.model.Category
import com.example.myapplication.ui.components.InfoBanner
import com.example.myapplication.ui.components.ItemPhoto

/**
 * Mobile capability: take a photo of the item with the camera.
 * Fallbacks: permission denied or no camera -> message + "choose from gallery".
 * The photo is optional, so the listing can always be published without it.
 */
@Composable
fun PhotoSection(viewModel: AddItemViewModel, category: Category?) {
    val context = LocalContext.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // the Uri must survive rotation while the camera app is open
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) viewModel.onPhotoSelected(uri)
    }

    // system photo picker - needs no permission, so it works as the fallback
    val pickPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoSelected(uri)
    }

    fun openCamera() {
        val uri = viewModel.createCameraUri()
        cameraUri = uri
        try {
            takePictureLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.onCameraUnavailable() // no camera app installed
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else viewModel.onCameraPermissionDenied()
    }

    fun onTakePhotoClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        when {
            !hasCamera -> viewModel.onCameraUnavailable()
            granted -> openCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onGalleryClick() {
        viewModel.onPhotoProblemDismissed()
        pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        context.startActivity(intent)
    }

    val photoPath = viewModel.form.photoPath

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            viewModel.isProcessingPhoto -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DormGreen)
            }

            photoPath != null -> {
                ItemPhoto(
                    photoPath = photoPath,
                    category = category ?: Category.OTHER,
                    contentDescription = "Photo of your item",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onTakePhotoClick() }) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Text("Retake", modifier = Modifier.padding(start = 6.dp))
                    }
                    TextButton(onClick = viewModel::onRemovePhoto) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text("Remove photo", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            else -> AddPhotoBox(
                hasCamera = hasCamera,
                onClick = { if (hasCamera) onTakePhotoClick() else onGalleryClick() }
            )
        }

        if (photoPath == null && hasCamera && viewModel.photoProblem == null) {
            TextButton(onClick = { onGalleryClick() }) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text("Choose from gallery instead", modifier = Modifier.padding(start = 6.dp))
            }
        }

        viewModel.photoProblem?.let { problem ->
            InfoBanner(
                icon = Icons.Outlined.NoPhotography,
                text = problem.message,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
            Row {
                TextButton(onClick = { onGalleryClick() }) { Text("Choose from gallery") }
                if (problem == PhotoProblem.PERMISSION_DENIED) {
                    TextButton(onClick = { openAppSettings() }) { Text("Open settings") }
                }
            }
        }
    }
}

/** Dashed "Add photo" box from the Figma design. */
@Composable
private fun AddPhotoBox(hasCamera: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = BorderGrey,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clickable(
                onClickLabel = if (hasCamera) "take a photo" else "choose a photo",
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .drawBehind { drawCircle(CardGrey) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasCamera) Icons.Outlined.PhotoCamera else Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = DormGreen
            )
        }
        Text(
            text = if (hasCamera) "Add photo" else "Choose photo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DormGreen,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = if (hasCamera) "Show the item up close (optional)" else "No camera on this device (optional)",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}
