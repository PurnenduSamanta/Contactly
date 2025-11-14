package com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.purnendu.contactly.R
import com.purnendu.contactly.ui.theme.InputBorder
import com.purnendu.contactly.ui.theme.SheetBackground
import com.purnendu.contactly.ui.theme.SheetTitleColor
import com.purnendu.contactly.ui.theme.SubtitleTextColor

@Composable
fun LabeledTimeInput(label: String, value: String, onClick: () -> Unit) {
    Text(label, color = SheetTitleColor)
    Spacer(Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(SheetBackground),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value.ifBlank { stringResource(id = R.string.label_select, label) },
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            color = if (value.isBlank()) SubtitleTextColor else SheetTitleColor
        )

        Icon(
            painter = painterResource(R.drawable.schedule_clock_icon),
            contentDescription = null,
            tint = SubtitleTextColor,
            modifier = Modifier.padding(end = 14.dp)
        )
    }
}
