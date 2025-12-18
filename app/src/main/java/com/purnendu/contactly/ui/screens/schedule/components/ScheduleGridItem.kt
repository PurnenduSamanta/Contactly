package com.purnendu.contactly.ui.screens.schedule.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.utils.expressiveScale
import com.purnendu.contactly.utils.rememberExpressiveAnimation

/**
 * Compact schedule card optimized for grid view
 */
@Composable
fun ScheduleGridItem(
    modifier: Modifier = Modifier,
    schedule: Schedule,
    avatarUri: String? = null,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val expressiveScale = rememberExpressiveAnimation(
        targetValue = if (isPressed) 0.97f else 1f
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(15.dp)
            )
            .expressiveScale(expressiveScale.value),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        schedule.avatarResId != null -> {
                            Image(
                                painter = painterResource(id = schedule.avatarResId),
                                contentDescription = "Avatar for ${schedule.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        avatarUri != null && avatarUri.isNotBlank() -> {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Avatar for ${schedule.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                painter = painterResource(id = R.drawable.avatar_placeholder),
                                contentDescription = "Avatar placeholder",
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Original name
                Text(
                    text = stringResource(id = R.string.original_name, schedule.originalName),
                    style = MaterialTheme.typography.bodyMedium, // Using expressive typography
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time
                if (schedule.startAtMillis > 0 && schedule.endAtMillis > 0) {
                    val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    val startTime = formatter.format(java.util.Date(schedule.startAtMillis))
                    val endTime = formatter.format(java.util.Date(schedule.endAtMillis))
                    
                    Text(
                        text = "$startTime - $endTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Compact Day Chips
                DayChips(
                    selectedDays = schedule.selectedDays,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onDeleteClick(schedule) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEditClick(schedule) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Contact details icon button (upper right corner)
            IconButton(
                onClick = { onContactDetailsClick(schedule) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "View Contact Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
