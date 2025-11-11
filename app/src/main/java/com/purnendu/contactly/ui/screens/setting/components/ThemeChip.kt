package com.purnendu.contactly.ui.screens.setting.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.ui.theme.ChipBorder
import com.purnendu.contactly.ui.theme.ChipSelectedBorder
import com.purnendu.contactly.ui.theme.ChipText

@Composable
fun ThemeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) ChipSelectedBorder else ChipBorder

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = ChipText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}