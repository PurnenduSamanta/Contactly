package com.purnendu.contactly.ui.screens.home.components.editingBottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.purnendu.contactly.feature.home.R

@Composable
fun LabeledTimeInput(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Text(label, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else 0.5f), RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value.ifBlank { stringResource(id = R.string.label_select, label) },
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            color = (if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface).copy(alpha = if (enabled) 1f else 0.5f)
        )

        Icon(
            painter = painterResource(R.drawable.activation_clock_icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.padding(end = 14.dp)
        )
    }
}
