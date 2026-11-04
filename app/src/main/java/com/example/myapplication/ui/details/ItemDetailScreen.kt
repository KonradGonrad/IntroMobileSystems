package com.example.myapplication.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.DormGreenLight
import com.example.myapplication.core.theme.TextMuted
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import com.example.myapplication.ui.components.ConditionLabel
import com.example.myapplication.ui.components.InfoBanner
import com.example.myapplication.ui.components.ItemPhoto
import com.example.myapplication.ui.components.OfflineChip
import com.example.myapplication.ui.components.formatSyncTime
import com.example.myapplication.ui.components.StatusChip
import com.example.myapplication.ui.feed.LikeButton

@Composable
fun ItemDetailScreen(
    onBackClick: () -> Unit,
    viewModel: ItemDetailViewModel = viewModel(factory = ItemDetailViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    when (val s = state) {
        DetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        DetailUiState.NotFound -> Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            BackButton(onBackClick)
            Text("This listing doesn't exist anymore.", style = MaterialTheme.typography.bodyLarge)
        }

        is DetailUiState.Loaded -> ItemDetailContent(
            item = s.item,
            isOnline = isOnline,
            lastSyncTime = lastSyncTime,
            onBackClick = onBackClick,
            onReserveClick = viewModel::onReserveClick,
            onCancelClick = viewModel::onCancelReservationClick,
            onToggleLike = viewModel::onToggleLike
        )
    }
}

@Composable
private fun ItemDetailContent(
    item: Item,
    isOnline: Boolean,
    lastSyncTime: Long?,
    onBackClick: () -> Unit,
    onReserveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    Scaffold(
        bottomBar = { ReserveBar(item, lastSyncTime, onReserveClick, onCancelClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box {
                ItemPhoto(
                    photoPath = item.photoPath,
                    category = item.category,
                    contentDescription = if (item.photoPath != null) "Photo of ${item.title}" else "No photo added",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                )
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                )
                if (!isOnline) {
                    OfflineChip(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = item.category.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = DormGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    LikeButton(isLiked = item.isLiked, title = item.title, onClick = onToggleLike)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(item.status)
                    ConditionLabel(item.condition)
                }
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "About this item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
                OwnerRow(item.ownerName)
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }
        }
    }
}

@Composable
private fun OwnerRow(ownerName: String) {
    val initials = ownerName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = DormGreenLight, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(initials, color = DormGreen, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(ownerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Dorm pickup · Free", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun ReserveBar(item: Item, lastSyncTime: Long?, onReserveClick: () -> Unit, onCancelClick: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                item.isMine -> InfoBanner(
                    icon = Icons.Outlined.Info,
                    text = "This is your listing. Other students can reserve it from the feed."
                )

                item.reservedByMe -> {
                    InfoBanner(
                        icon = Icons.Outlined.BookmarkAdded,
                        title = "Reserved by you",
                        text = if (item.pendingSync) {
                            "Saved on this phone. It will be sent when you're online."
                        } else {
                            "Request sent (simulated, no real server yet). The owner will confirm the reservation."
                        }
                    )
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel reservation")
                    }
                }

                item.status == ListingStatus.ACTIVE -> {
                    InfoBanner(
                        icon = Icons.Outlined.CloudOff,
                        text = "Request will be saved locally and sent once connected. The owner will confirm the reservation."
                    )
                    Button(
                        onClick = onReserveClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DormGreen)
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                        Text("Reserve", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                else -> Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (item.status == ListingStatus.RESERVED) "Already reserved" else "No longer available")
                }
            }
            Text(
                text = "Data available offline" + (lastSyncTime?.let { " · as of ${formatSyncTime(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
