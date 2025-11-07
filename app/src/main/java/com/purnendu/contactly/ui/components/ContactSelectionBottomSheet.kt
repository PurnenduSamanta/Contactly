package com.purnendu.contactly.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectionBottomSheet(modifier: Modifier = Modifier) {

    ModalBottomSheet(
        onDismissRequest = {  },
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.White,
        dragHandle = {
            Icon(painter = painterResource(R.drawable.handler), contentDescription = "handlerIcon")
        },
        modifier = modifier
    )
    {

        Column(modifier = Modifier.fillMaxWidth())
        {

            TextField(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {Icon(Icons.Default.Search, contentDescription = "searchIcon")},
                value = "",
                onValueChange = {},
                placeholder = { Text("Search Contacts") },
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Blue.copy(0.1f), focusedContainerColor = Color.Blue.copy(0.1f), disabledContainerColor = Color.Blue.copy(0.1f),errorContainerColor = Color.Blue.copy(0.1f))

            )







        }




    }





















}