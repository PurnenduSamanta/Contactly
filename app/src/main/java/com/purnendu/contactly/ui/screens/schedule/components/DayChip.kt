package com.purnendu.contactly.ui.screens.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.ui.theme.ChipBorder
import com.purnendu.contactly.ui.theme.ChipSelectedBorder
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.utils.AppThemeMode

/**
 * Reusable day chip component with selection animation
 * Used in both DayPickerWheel (interactive) and DayChips (display-only)
 * 
 * @param label Day abbreviation (e.g., "S", "M", "T")
 * @param isSelected Whether this day is selected
 * @param onClick Callback when chip is clicked (null for non-interactive)
 * @param modifier Modifier for customization
 */
@Composable
fun DayChip(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            ChipSelectedBorder
        } else {
            ChipBorder
        },
        label = "borderColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "textColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DayChipSelectedPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        DayChip(
            label = "M",
            isSelected = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DayChipUnselectedPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        DayChip(
            label = "T",
            isSelected = false,
            onClick = {}
        )
    }
}
