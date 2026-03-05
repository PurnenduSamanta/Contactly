package com.purnendu.contactly.ui.screens.home.components.contactSelectionBottomSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.R
import com.purnendu.contactly.domain.model.Contact
import com.purnendu.contactly.ui.screens.home.components.ErrorMessageCard
import com.purnendu.contactly.ui.theme.ContactlyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectionBottomSheet(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onSyncClick: () -> Unit,
    error:String?,
    onErrorCardDismiss: () -> Unit,
    contacts: List<Contact>,
    onDismissContactSelection: () -> Unit,
    onContactClick: (Contact) -> Unit
) {

    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = contacts.filter {
        it.name?.contains(searchQuery, ignoreCase = true) == true ||
                it.phone.contains(searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = {onDismissContactSelection() },
        shape = RoundedCornerShape(15.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
        contentWindowInsets = {WindowInsets.navigationBars}
    )
    {
        if(isLoading)
        {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), contentAlignment = Alignment.Center)
            {
                CircularProgressIndicator()
            }
        }
        else
        {
            Column(modifier = Modifier.fillMaxHeight(0.9f)) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSyncClick, enabled = !isLoading) { Icon(Icons.Default.Refresh, contentDescription = "Sync Contacts") }
                   }

                if(error!=null)
                {
                    ErrorMessageCard(
                        message = error,
                        onDismiss = { onErrorCardDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(filteredContacts) { contact ->
                        ContactItem(contact = contact, onClick = { onContactClick(contact) })
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ContactSelectionBottomSheetPreview()
{
    ContactlyTheme {
        ContactSelectionBottomSheet(
            isLoading = false,
            onSyncClick = {},
            error = null,
            onErrorCardDismiss = {},
            onDismissContactSelection = {},
            contacts = List(20) { index ->
                Contact(
                    name = "Contact $index",
                    phone = "1234567890",
                    image = R.drawable.avatar_liam)
            }
        ) { }

    }
}
