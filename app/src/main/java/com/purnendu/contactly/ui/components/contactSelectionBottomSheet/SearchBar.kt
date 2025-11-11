package com.purnendu.contactly.ui.components.contactSelectionBottomSheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.ui.theme.ContactNameColor
import com.purnendu.contactly.ui.theme.SearchBarBackground
import com.purnendu.contactly.ui.theme.SearchHintColor

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search contacts", color = SearchHintColor) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = SearchHintColor
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SearchBarBackground,
            unfocusedContainerColor = SearchBarBackground,
            cursorColor = ContactNameColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}