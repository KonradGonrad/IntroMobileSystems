package com.example.myapplication.ui.addItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.TextMuted
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.ui.components.InfoBanner
import com.example.myapplication.ui.components.OfflineChip
import com.example.myapplication.ui.components.categoryIcon

@Composable
fun AddItemScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddItemViewModel = viewModel(factory = AddItemViewModel.Factory)
) {
    val form = viewModel.form
    val errors = viewModel.errors
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    // go back to the feed after the listing is stored in the database
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onSaved()
    }

    Scaffold(
        topBar = { AddItemTopBar(isOnline, onBackClick) },
        bottomBar = {
            PublishBar(
                isSaving = viewModel.isSaving,
                errorCount = errors.size,
                onPublishClick = viewModel::onPublishClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isOnline) {
                InfoBanner(
                    icon = Icons.Outlined.CloudOff,
                    title = "No connection? Keep adding.",
                    text = "We will save the listing on this phone and publish it once the connection is back."
                )
            }

            PhotoSection(viewModel = viewModel, category = form.category)

            FormSection("Item title") {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Electric kettle") },
                    singleLine = true,
                    isError = FormField.TITLE in errors,
                    supportingText = { errors[FormField.TITLE]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FormSection("Description") {
                OutlinedTextField(
                    value = form.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Condition, size, where to pick it up…") },
                    minLines = 3,
                    isError = FormField.DESCRIPTION in errors,
                    supportingText = { errors[FormField.DESCRIPTION]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FormSection("Category") {
                CategoryChips(
                    selected = form.category,
                    error = errors[FormField.CATEGORY],
                    onSelected = viewModel::onCategoryChange
                )
            }

            FormSection("Condition") {
                ConditionChips(
                    selected = form.condition,
                    error = errors[FormField.CONDITION],
                    onSelected = viewModel::onConditionChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AddItemTopBar(isOnline: Boolean, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
        }
        Text(
            text = "Add listing",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (!isOnline) {
            OfflineChip(modifier = Modifier.padding(end = 12.dp))
        }
    }
}

@Composable
private fun FormSection(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(selected: Category?, error: String?, onSelected: (Category) -> Unit) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onSelected(category) },
                    label = { Text(category.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DormGreen,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ConditionChips(selected: Condition?, error: String?, onSelected: (Condition) -> Unit) {
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Condition.entries.forEach { condition ->
                FilterChip(
                    selected = condition == selected,
                    onClick = { onSelected(condition) },
                    label = { Text(condition.label) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DormGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PublishBar(isSaving: Boolean, errorCount: Int, onPublishClick: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (errorCount > 0) {
                // liveRegion -> TalkBack reads this out loud when it appears
                Text(
                    text = if (errorCount == 1) "1 field needs your attention" else "$errorCount fields need your attention",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Button(
                onClick = onPublishClick,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DormGreen)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                    Text("Publish", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(
                text = "Saved on this phone first · published after sync",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
