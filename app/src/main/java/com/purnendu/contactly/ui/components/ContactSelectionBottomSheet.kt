package com.purnendu.contactly.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectionBottomSheet(modifier: Modifier = Modifier) {

    ModalBottomSheet(
        onDismissRequest = {  },
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.White,
        dragHandle = {

        },
        modifier = modifier
    ) { }





















}