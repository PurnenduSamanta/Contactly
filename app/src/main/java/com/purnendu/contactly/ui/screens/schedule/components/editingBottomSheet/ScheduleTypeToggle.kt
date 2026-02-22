package com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import com.purnendu.contactly.utils.ScheduleType
import com.purnendu.contactly.utils.expressiveScale

@Composable
fun ScheduleTypeToggle(
    selectedType: ScheduleType,
    onTypeChange: (ScheduleType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScheduleTypeChip(
            text = "One-time",
            icon = Icons.Outlined.CalendarMonth,
            selected = selectedType == ScheduleType.ONE_TIME,
            onClick = { if (enabled) onTypeChange(ScheduleType.ONE_TIME) },
            modifier = Modifier.weight(1f)
        )
        
        ScheduleTypeChip(
            text = "Repeat",
            icon = Icons.Outlined.Repeat,
            selected = selectedType == ScheduleType.REPEAT,
            onClick = { if (enabled) onTypeChange(ScheduleType.REPEAT) },
            modifier = Modifier.weight(1f)
        )

        ScheduleTypeChip(
            text = "Instant",
            icon = Icons.Outlined.Bolt,
            selected = selectedType == ScheduleType.INSTANT,
            onClick = { if (enabled) onTypeChange(ScheduleType.INSTANT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScheduleTypeChip(
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
        label = "schedule-type-chip-scale"
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
