package com.purnendu.contactly.ui.components.contactSelectionBottomSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.theme.SheetBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectionBottomSheet(
    modifier: Modifier = Modifier,
    contacts: List<Contact>,
    onContactClick: () -> Unit
) {

    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = contacts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = { },
        shape = RoundedCornerShape(15.dp),
        containerColor = SheetBackground,
        modifier = modifier
    )
    {
            Column(modifier = Modifier) {

                Spacer(modifier = Modifier.height(20.dp))

                SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(filteredContacts) { contact ->
                        ContactItem(contact = contact, onClick = {  })
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
            contacts = List(20) { index ->
                Contact(
                    name = "Contact $index",
                    phone = "1234567890",
                    image = R.drawable.avatar_liam)
            }
        ) { }

    }
}
