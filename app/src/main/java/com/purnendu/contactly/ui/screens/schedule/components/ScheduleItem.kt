package com.purnendu.contactly.ui.screens.schedule.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.components.SlidingImageCarousel
import com.purnendu.contactly.ui.components.SlidingImageCarouselRect
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.utils.ViewMode
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.ScheduleType
import com.purnendu.contactly.utils.expressiveScale
import com.purnendu.contactly.utils.rememberExpressiveAnimation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleItem(
    modifier: Modifier = Modifier,
    schedule: Schedule,
    viewMode: ViewMode = ViewMode.LIST,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit,
) {
    if (viewMode == ViewMode.LIST)
    {
        ListScheduleItem(
            modifier = modifier,
            schedule = schedule,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onContactDetailsClick = onContactDetailsClick
        )
    }
    else {
        GridScheduleItem(
            modifier = modifier,
            schedule = schedule,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onContactDetailsClick = onContactDetailsClick
        )
    }
}

@Composable
private fun ListScheduleItem(
    modifier: Modifier,
    schedule: Schedule,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val expressiveScale = rememberExpressiveAnimation(
        targetValue = if (isPressed) 0.97f else 1f
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .expressiveScale(expressiveScale.value)
    ) {
        Card(
            modifier = Modifier.border(width = 1.dp, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Contact info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = schedule.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.original_name, schedule.originalName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Scheduled time display or Active status
                            if (schedule.startAtMillis > 0 && schedule.endAtMillis > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                if (schedule.isCurrentlyActive) {
                                    // Show "Active" when schedule is currently running
                                    Text(
                                        text = "● Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    // Show date and time
                                    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    val startTime = timeFormatter.format(Date(schedule.startAtMillis))
                                    val endTime = timeFormatter.format(Date(schedule.endAtMillis))

                                    val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                    val startDate = dateFormatter.format(Date(schedule.startAtMillis))
                                    val endDate = dateFormatter.format(Date(schedule.endAtMillis))
                                    if (startDate != null && endDate != null) {
                                        if (startDate == endDate) {
                                            Text(
                                                text = "$startDate ($startTime - $endTime)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Avatar - Use carousel if temp image exists, otherwise original
                        SlidingImageCarouselRect(
                            originalImageUri = schedule.originalImageUri,
                            temporaryImageUri = schedule.temporaryImageUri,
                            modifier = Modifier
                                .width(110.dp)
                                .aspectRatio(ratio = 1.6f, true),
                            autoSlideIntervalMs = 3000L
                        )
                    }

                    // Selected days display
                    Spacer(modifier = Modifier.height(12.dp))

                    DayChips(
                        selectedDays = schedule.selectedDays,
                        modifier = Modifier.padding(start = 0.dp)
                    )
                }

                // Contact details icon button
                IconButton(
                    onClick = { onContactDetailsClick(schedule) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "View Contact Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onDeleteClick(schedule) },
                modifier = Modifier
                    .height(40.dp)
                    .expressiveScale(if (isPressed) 0.95f else 1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(id = R.string.action_delete),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { onEditClick(schedule) },
                enabled = !schedule.isCurrentlyActive,
                modifier = Modifier
                    .height(40.dp)
                    .expressiveScale(if (isPressed) 0.95f else 1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(id = R.string.action_edit),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GridScheduleItem(
    modifier: Modifier,
    schedule: Schedule,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit
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
                // Avatar - Use carousel for both original and temp images
                SlidingImageCarousel(
                    originalImageUri = schedule.originalImageUri,
                    temporaryImageUri = schedule.temporaryImageUri,
                    modifier = Modifier,
                    imageSize = Modifier.size(80.dp),
                    autoSlideIntervalMs = 3000L
                )

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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time or Active status
                if (schedule.startAtMillis > 0 && schedule.endAtMillis > 0) {
                    if (schedule.isCurrentlyActive) {
                        // Show "Active" when schedule is currently running
                        Text(
                            text = "● Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val startTime = formatter.format(Date(schedule.startAtMillis))
                        val endTime = formatter.format(Date(schedule.endAtMillis))

                        val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val startDate = dateFormatter.format(Date(schedule.startAtMillis))
                        val endDate = dateFormatter.format(Date(schedule.endAtMillis))
                        if (startDate != null && endDate != null) {
                            if (startDate == endDate) {
                                Text(
                                    text = "$startDate\n$startTime - $endTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
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
                        enabled = !schedule.isCurrentlyActive,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (schedule.isCurrentlyActive)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Contact details icon button
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

@Preview(showBackground = true)
@Composable
fun ScheduleItemPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        Column {
            Text("List View:")
            ScheduleItem(
                schedule = Schedule(
                    id = "1",
                    name = "Ethan Carter",
                    originalName = "Ethan",
                    avatarResId = null,
                    contactId = 0L,
                    selectedDays = 127,
                    startAtMillis = 0L,
                    endAtMillis = 0L,
                    scheduleType = ScheduleType.ONE_TIME,
                    temporaryImageUri = null,
                    originalImageUri = null
                ),
                viewMode = ViewMode.LIST,
                onEditClick = {},
                onDeleteClick = {},
                onContactDetailsClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Grid View:")
            ScheduleItem(
                schedule = Schedule(
                    id = "1",
                    name = "Ethan Carter",
                    originalName = "Ethan",
                    avatarResId = null,
                    contactId = 0L,
                    selectedDays = 127,
                    startAtMillis = 0L,
                    endAtMillis = 0L,
                    scheduleType = ScheduleType.ONE_TIME,
                    temporaryImageUri = null,
                    originalImageUri = null
                ),
                viewMode = ViewMode.GRID,
                onEditClick = {},
                onDeleteClick = {},
                onContactDetailsClick = {}
            )
        }
    }
}
