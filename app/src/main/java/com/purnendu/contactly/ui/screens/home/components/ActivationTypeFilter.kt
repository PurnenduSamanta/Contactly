package com.purnendu.contactly.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.utils.ActivationMode

/**
 * Filter options for the activation list.
 * ALL = All types, otherwise filters by specific ActivationMode.
 */
enum class ActivationTypeFilter(val label: String) {
    ALL("All"),
    ONE_TIME("One-time"),
    REPEAT("Repeat"),
    INSTANT("Instant"),
    NEARBY("Nearby");

    fun toActivationMode(): ActivationMode? = when (this) {
        ALL -> null
        ONE_TIME -> ActivationMode.ONE_TIME
        REPEAT -> ActivationMode.REPEAT
        INSTANT -> ActivationMode.INSTANT
        NEARBY -> ActivationMode.NEARBY
    }
}

@Composable
fun ActivationsFilterChips(
    selectedFilter: ActivationTypeFilter,
    onFilterChange: (ActivationTypeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(ActivationTypeFilter.entries) { filter ->
            val selected = selectedFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 2.dp
                )
            )
        }
    }
}
