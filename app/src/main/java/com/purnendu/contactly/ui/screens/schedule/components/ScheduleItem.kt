package com.purnendu.contactly.ui.screens.schedule.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.theme.ContactlyTheme
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.purnendu.contactly.utils.expressiveScale
import com.purnendu.contactly.utils.rememberExpressiveAnimation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun ScheduleItem(
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .expressiveScale(expressiveScale.value)
    ) {
        Card(
            modifier = Modifier.border(width = 1.dp, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(15.dp), // Enhanced rounded corners for Material 3 Expressive
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                            style = MaterialTheme.typography.titleLarge, // Using expressive typography
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.original_name, schedule.originalName),
                            style = MaterialTheme.typography.bodyMedium, // Using expressive typography
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Scheduled time display
                        if (schedule.startAtMillis > 0 && schedule.endAtMillis > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                            val startTime = formatter.format(java.util.Date(schedule.startAtMillis))
                            val endTime = formatter.format(java.util.Date(schedule.endAtMillis))
                            
                            Text(
                                text = "$startTime - $endTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Avatar with expressive rounded corners
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(15.dp)) // Enhanced rounded corners
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        when {
                            schedule.avatarResId != null -> {
                                Image(
                                    painter = painterResource(id = schedule.avatarResId),
                                    contentDescription = "Avatar for ${schedule.name}",
                                    modifier = Modifier.width(110.dp).aspectRatio(ratio = 1.6f,true),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            avatarUri != null && avatarUri.isNotBlank() -> {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "Avatar for ${schedule.name}",
                                    modifier = Modifier.width(110.dp).aspectRatio(ratio = 1.6f,true),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Image(
                                    modifier = Modifier.size(width = 119.dp, height = 58.dp),
                                    painter = painterResource(id = R.drawable.avatar_placeholder),
                                    contentDescription = "Avatar placeholder",
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                
                // Selected days display
                Spacer(modifier = Modifier.height(12.dp))
                
                DayChips(
                    selectedDays = schedule.selectedDays,
                    modifier = Modifier.padding(start = 0.dp)
                )
            }
                
                // Contact details icon button (upper right corner)
                androidx.compose.material3.IconButton(
                    onClick = { onContactDetailsClick(schedule) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(36.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "View Contact Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons with expressive styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Button(
                onClick = { onDeleteClick(schedule) },
                modifier = Modifier
                    .height(40.dp)
                    .expressiveScale(if (isPressed) 0.95f else 1f), // Expressive press animation
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp), // Enhanced rounded corners for buttons
            ) {
                Text(
                    stringResource(id = R.string.action_delete),
                    style = MaterialTheme.typography.labelLarge, // Using expressive typography
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { onEditClick(schedule) },
                modifier = Modifier
                    .height(40.dp)
                    .expressiveScale(if (isPressed) 0.95f else 1f), // Expressive press animation
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp), // Enhanced rounded corners for buttons
            ) {
                Text(
                    stringResource(id = R.string.action_edit),
                    style = MaterialTheme.typography.labelLarge, // Using expressive typography
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleItemPreview() {
    ContactlyTheme(appThemeMode = com.purnendu.contactly.utils.AppThemeMode.LIGHT) {
        ScheduleItem(
            schedule = Schedule(
                id = "1",
                name = "Ethan Carter",
                originalName = "Ethan"
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

