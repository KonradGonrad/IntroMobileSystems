package com.example.myapplication.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.TextMuted
import com.example.myapplication.data.model.Item
import com.example.myapplication.data.model.ListingStatus
import com.example.myapplication.ui.components.ConditionLabel
import com.example.myapplication.ui.components.ItemPhoto
import com.example.myapplication.ui.components.SmallChip
import com.example.myapplication.ui.components.StatusChip

/** Card used in the 2-column grid (Browse and Liked tabs). */
@Composable
fun ItemGridCard(item: Item, onClick: () -> Unit, onToggleLike: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box {
            ItemPhoto(
                photoPath = item.photoPath,
                category = item.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            )
            if (item.status != ListingStatus.ACTIVE) {
                StatusChip(
                    status = item.status,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
            LikeButton(
                isLiked = item.isLiked,
                title = item.title,
                onClick = onToggleLike,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            ConditionLabel(item.condition)
            Text(
                text = "${item.ownerName} · ${item.location}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Full width row used in the "My listings" tab. */
@Composable
fun MyListingRow(item: Item, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row {
            ItemPhoto(
                photoPath = item.photoPath,
                category = item.category,
                modifier = Modifier.size(104.dp)
            )
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(item.status)
                    if (item.pendingSync) {
                        SmallChip(
                            text = "Not synced yet",
                            background = Color(0xFFFFF1D6),
                            textColor = Color(0xFF7A5200),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(item.condition.label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(item.location, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
fun LikeButton(isLiked: Boolean, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // IconButton gives us the 48dp touch target
    IconButton(onClick = onClick, modifier = modifier) {
        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Remove $title from liked" else "Like $title",
                    tint = DormGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
