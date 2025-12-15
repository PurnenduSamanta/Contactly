package com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.ui.screens.schedule.components.DayChip
import com.purnendu.contactly.ui.theme.ChipBorder
import com.purnendu.contactly.ui.theme.ChipSelectedBorder
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.DayUtils

/**
 * iOS-style day picker with circular chips for each day of the week
 * @param selectedDays Set of selected day indices (0=Sunday, 1=Monday, ..., 6=Saturday)
 * @param onDaysChanged Callback when days selection changes
 * @param singleSelection If true, only one day can be selected at a time
 */
@Composable
fun DayPickerWheel(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    singleSelection: Boolean = false
) {
    val dayLabels = DayUtils.getShortDayNames()
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Days row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEachIndexed { index, dayLabel ->
                val isSelected = selectedDays.contains(index)
                DayChip(
                    label = dayLabel,
                    isSelected = isSelected,
                    onClick = {
                        val newSelection = if (singleSelection) {
                            // Single selection mode: replace with new day
                            setOf(index)
                        } else {
                            // Multiple selection mode: toggle
                            if (isSelected) {
                                selectedDays - index
                            } else {
                                selectedDays + index
                            }
                        }
                        onDaysChanged(newSelection)
                    },
                    modifier = Modifier.weight(1f)
                )
                
                if (index < dayLabels.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
        
        // Quick select button (Only show for multiple selection mode)
        if (!singleSelection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val allSelected = selectedDays.size == 7
                Text(
                    text = if (allSelected) "Deselect All" else "Every Day",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            onDaysChanged(
                                if (allSelected) emptySet() else setOf(0, 1, 2, 3, 4, 5, 6)
                            )
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayPickerWheelPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            DayPickerWheel(
                selectedDays = setOf(0, 2, 4, 6), // Sun, Tue, Thu, Sat
                onDaysChanged = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayPickerWheelAllSelectedPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.DARK) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            DayPickerWheel(
                selectedDays = setOf(0, 1, 2, 3, 4, 5, 6), // All days
                onDaysChanged = {}
            )
        }
    }
}
