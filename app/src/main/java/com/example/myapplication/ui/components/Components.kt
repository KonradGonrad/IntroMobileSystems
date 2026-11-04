package com.example.myapplication.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.core.theme.CardGrey
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.DormGreenLight
import com.example.myapplication.core.theme.TextMuted
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.data.model.ListingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Small grey pill shown in the top right corner when there is no internet. */
@Composable
fun OfflineChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = CardGrey
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/** Light green info box used for offline / sync messages. */
@Composable
fun InfoBanner(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    containerColor: Color = DormGreenLight,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                if (title != null) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
            if (action != null) action()
        }
    }
}

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.CLOTHES -> Icons.Outlined.Checkroom
    Category.ELECTRONICS -> Icons.Outlined.Devices
    Category.BOOKS -> Icons.AutoMirrored.Outlined.MenuBook
    Category.KITCHEN -> Icons.Outlined.Kitchen
    Category.FURNITURE -> Icons.Outlined.Chair
    Category.OTHER -> Icons.Outlined.Category
}

/**
 * Photo of the item. Items without a photo (or if the file is missing)
 * get a grey box with the category icon.
 */
@Composable
fun ItemPhoto(
    photoPath: String?,
    category: Category,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    // load the file in the background so scrolling stays smooth
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoPath) {
        value = photoPath?.let { path ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
    }

    Box(
        modifier = modifier.background(CardGrey),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = contentDescription,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ConditionLabel(condition: Condition, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = DormGreen,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = condition.label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun StatusChip(status: ListingStatus, modifier: Modifier = Modifier) {
    val (background, textColor) = when (status) {
        ListingStatus.ACTIVE -> DormGreenLight to DormGreen
        ListingStatus.RESERVED -> Color(0xFFFFF1D6) to Color(0xFF7A5200)
        ListingStatus.EXCHANGED -> CardGrey to TextMuted
    }
    SmallChip(text = status.label, background = background, textColor = textColor, modifier = modifier)
}

@Composable
fun SmallChip(text: String, background: Color, textColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}
