package com.purnendu.contactly.ui.screens.setting.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.utils.expressiveScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember


@Composable
fun SettingsRow(
    name: String,
    value: String? = null,
    onClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isClickable = onClick != null
    val scale by animateFloatAsState(
        targetValue = if (isClickable) 0.98f else 1f,
        label = "row-scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .expressiveScale(if (isClickable) scale else 1f) // Add expressive press animation
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickable
            ) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 16.dp), // Slightly increased padding for better touch area
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge, // Using expressive typography
            color = MaterialTheme.colorScheme.onSurface
        )

        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium, // Using expressive typography
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}