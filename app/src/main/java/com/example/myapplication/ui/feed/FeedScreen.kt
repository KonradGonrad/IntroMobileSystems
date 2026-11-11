package com.example.myapplication.ui.feed

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.TextMuted
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.repository.SyncStatus
import com.example.myapplication.ui.components.InfoBanner
import com.example.myapplication.ui.components.OfflineChip
import com.example.myapplication.ui.components.formatSyncTime

@Composable
fun FeedScreen(
    onAddItemClick: () -> Unit,
    onAccountClick: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                actions = {
                    Text(
                        text = "Add things that you don't need",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DormGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = onAddItemClick, containerColor = DormGreen, contentColor = Color.White) {
                        Icon(Icons.Filled.Add, contentDescription = "Add a new listing")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeedHeader(
                    sync = sync,
                    onAccountClick = onAccountClick,
                    onRefreshClick = viewModel::onRefreshClick,
                    onSimulateServerErrorChange = viewModel::onSimulateServerErrorChange
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                TabRow(selected = state.tab, onSelected = viewModel::onTabSelected)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchField(query = viewModel.query, onQueryChange = viewModel::onQueryChange)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryChips(selected = state.category, onSelected = viewModel::onCategorySelected)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SyncBanner(sync = sync, onRetryClick = viewModel::onRefreshClick)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(tab = state.tab, count = state.items.size)
            }

            if (!state.isLoading && state.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(tab = state.tab, hasFilters = viewModel.query.isNotBlank() || state.category != null)
                }
            }

            if (state.tab == FeedTab.MY_LISTINGS) {
                items(state.items, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { item ->
                    MyListingRow(item = item, onClick = { onItemClick(item.id) })
                }
            } else {
                items(state.items, key = { it.id }) { item ->
                    ItemGridCard(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onToggleLike = { viewModel.onToggleLike(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    sync: FeedSyncState,
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSimulateServerErrorChange: (Boolean) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Eco, contentDescription = null, tint = DormGreen, modifier = Modifier.size(28.dp))
            Text(
                text = "DormSwap",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f)
                    .semantics { heading() }
            )
            when {
                !sync.isOnline -> OfflineChip()
                sync.status is SyncStatus.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp
                )
                else -> IconButton(onClick = onRefreshClick) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh listings")
                }
            }
            IconButton(onClick = onAccountClick) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "My account", tint = DormGreen)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Demo: simulate server error") },
                        leadingIcon = { Checkbox(checked = sync.simulateServerError, onCheckedChange = null) },
                        onClick = {
                            onSimulateServerErrorChange(!sync.simulateServerError)
                            menuOpen = false
                        }
                    )
                }
            }
        }
        Text("Dormitory 5 · Łódź", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

/** Shows what is going on with the network: offline, loading, error or last update time. */
@Composable
private fun SyncBanner(sync: FeedSyncState, onRetryClick: () -> Unit) {
    val lastUpdate = sync.lastSyncTime?.let { "Last updated at ${formatSyncTime(it)}." } ?: "Not updated from the server yet."
    val status = sync.status

    when {
        !sync.isOnline -> InfoBanner(
            icon = Icons.Outlined.CloudOff,
            title = "You're currently offline",
            text = "Showing listings saved on this phone. $lastUpdate"
        )

        status is SyncStatus.Loading -> Column {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DormGreen)
            Text(
                "Checking for new listings…",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        status is SyncStatus.Error -> InfoBanner(
            icon = Icons.Outlined.ErrorOutline,
            title = "Couldn't load new listings",
            text = "${status.message}. Showing saved listings. $lastUpdate",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            action = {
                TextButton(onClick = onRetryClick) { Text("Retry") }
            }
        )

        else -> Text(
            text = lastUpdate,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun TabRow(selected: FeedTab, onSelected: (FeedTab) -> Unit) {
    // scrollable row, so big system fonts don't cut the labels
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedTab.entries.forEach { tab ->
            GreenChip(label = tab.label, selected = tab == selected, onClick = { onSelected(tab) })
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Column {
        Text("Search for items", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Spacer(modifier = Modifier.size(4.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What are you looking for?") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
    }
}

@Composable
private fun CategoryChips(selected: Category?, onSelected: (Category?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GreenChip(label = "All", selected = selected == null, onClick = { onSelected(null) })
        Category.entries.forEach { category ->
            GreenChip(label = category.label, selected = selected == category, onClick = { onSelected(category) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GreenChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DormGreen,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun SectionTitle(tab: FeedTab, count: Int) {
    val title = when (tab) {
        FeedTab.BROWSE -> "Give them a second life"
        FeedTab.MY_LISTINGS -> "My listings"
        FeedTab.LIKED -> "Liked listings"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Text(
            text = if (count == 1) "1 item" else "$count items",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun EmptyState(tab: FeedTab, hasFilters: Boolean) {
    val text = when {
        hasFilters -> "No items match your search. Try another word or category."
        tab == FeedTab.MY_LISTINGS -> "You haven't listed anything yet. Tap + to add your first item."
        tab == FeedTab.LIKED -> "Nothing liked yet. Tap the heart on an item to save it here."
        else -> "No items in your dorm right now."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
        modifier = Modifier.padding(vertical = 32.dp)
    )
}
