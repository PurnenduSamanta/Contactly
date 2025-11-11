package com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.purnendu.contactly.ui.theme.InputBorder
import com.purnendu.contactly.ui.theme.SheetBackground
import com.purnendu.contactly.ui.theme.SheetTitleColor
import com.purnendu.contactly.ui.theme.SubtitleTextColor

@Composable
 fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(

    focusedContainerColor = SheetBackground,
    unfocusedContainerColor = SheetBackground,
    focusedTextColor = SheetTitleColor,
    unfocusedTextColor = SheetTitleColor,
    disabledTextColor = SheetTitleColor,
    focusedPlaceholderColor = SubtitleTextColor,
    unfocusedPlaceholderColor = SubtitleTextColor,
    focusedBorderColor = InputBorder,
    unfocusedBorderColor = InputBorder
)