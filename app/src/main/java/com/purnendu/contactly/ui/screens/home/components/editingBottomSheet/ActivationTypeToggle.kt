package com.purnendu.contactly.ui.screens.home.components.editingBottomSheet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.LocationOn
import com.purnendu.contactly.utils.ActivationMode
import com.purnendu.contactly.utils.expressiveScale

@Composable
fun ActivationTypeToggle(
    selectedType: ActivationMode,
    onTypeChange: (ActivationMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LazyRow  (
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        item{
            Spacer(modifier = Modifier.width(8.dp))

            Row {
                ActivationTypeChip(
                    text = "One-time",
                    icon = Icons.Outlined.CalendarMonth,
                    selected = selectedType == ActivationMode.ONE_TIME,
                    onClick = { if (enabled) onTypeChange(ActivationMode.ONE_TIME) },
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

        }

        item{
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                ActivationTypeChip(
                    text = "Repeat",
                    icon = Icons.Outlined.Repeat,
                    selected = selectedType == ActivationMode.REPEAT,
                    onClick = { if (enabled) onTypeChange(ActivationMode.REPEAT) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

        }

        item {
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                ActivationTypeChip(
                    text = "Instant",
                    icon = Icons.Outlined.Bolt,
                    selected = selectedType == ActivationMode.INSTANT,
                    onClick = { if (enabled) onTypeChange(ActivationMode.INSTANT) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                ActivationTypeChip(
                    text = "Nearby",
                    icon = Icons.Outlined.LocationOn,
                    selected = selectedType == ActivationMode.NEARBY,
                    onClick = { if (enabled) onTypeChange(ActivationMode.NEARBY) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun ActivationTypeChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "activation-type-chip-scale"
    )

    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .expressiveScale(scale)
            .clip(RoundedCornerShape(20.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
