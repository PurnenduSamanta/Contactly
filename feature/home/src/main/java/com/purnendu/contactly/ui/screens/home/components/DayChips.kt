package com.purnendu.contactly.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.common.DayUtils
import com.purnendu.contactly.common.AppThemeMode

/**
 * Display-only day chips component showing selected days
 * Uses the shared DayChip component without onClick interaction
 * 
 * @param selectedDays Bitmask of selected days (127 = all days)
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayChips(
    selectedDays: Int,
    modifier: Modifier = Modifier
) {
    val daysList = DayUtils.extractDaysFromBitmask(selectedDays)
    val dayNames = DayUtils.getShortDayNames()
    
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 7
    ) {
        dayNames.forEachIndexed { index, dayName ->
            val isSelected = daysList.contains(index)
            
            DayChip(
                label = dayName,
                isSelected = isSelected,
                onClick = null,  // Display-only, no interaction
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayChipsPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        DayChips(
            selectedDays = 42, // Monday, Wednesday, Friday
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DayChipsAllDaysPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        DayChips(
            selectedDays = 127, // All days
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DayChipsWeekdaysPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.DARK) {
        DayChips(
            selectedDays = 62, // Mon-Fri (weekdays)
            modifier = Modifier.padding(16.dp)
        )
    }
}
