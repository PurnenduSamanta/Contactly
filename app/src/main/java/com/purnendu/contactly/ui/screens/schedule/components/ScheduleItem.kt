package com.purnendu.contactly.ui.screens.schedule.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.purnendu.contactly.utils.expressiveElevation
import com.purnendu.contactly.utils.rememberExpressiveAnimation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun ScheduleItem(
    modifier: Modifier = Modifier,
    schedule: Schedule,
    avatarUri: String? = null,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
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
            shape = RoundedCornerShape(20.dp), // Enhanced rounded corners for Material 3 Expressive
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp // Slightly increased elevation for depth
            ),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Avatar with expressive rounded corners
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp)) // Enhanced rounded corners
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        when {
                            schedule.avatarResId != null -> {
                                Image(
                                    painter = painterResource(id = schedule.avatarResId!!),
                                    contentDescription = "Avatar for ${schedule.name}",
                                    modifier = Modifier.size(width = 119.dp, height = 58.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            avatarUri != null && avatarUri.isNotBlank() -> {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "Avatar for ${schedule.name}",
                                    modifier = Modifier.size(width = 119.dp, height = 58.dp),
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons with expressive styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                interactionSource = interactionSource
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp), // Enhanced rounded corners for buttons
                interactionSource = interactionSource
            ) {
                Text(
                    stringResource(id = R.string.action_edit),
                    style = MaterialTheme.typography.labelLarge, // Using expressive typography
                    color = MaterialTheme.colorScheme.onPrimary,
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
