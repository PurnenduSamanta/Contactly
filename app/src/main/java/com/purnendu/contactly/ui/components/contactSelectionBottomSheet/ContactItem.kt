package com.purnendu.contactly.ui.components.contactSelectionBottomSheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.ui.theme.ContactNameColor
import com.purnendu.contactly.ui.theme.ContactNumberColor

@Composable
fun ContactItem(
    contact: Contact,
    onClick: (Contact) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(contact) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = contact.image,
            contentDescription = contact.name,
            placeholder = painterResource(R.drawable.avatar_ethan),
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = contact.name,
                color = ContactNameColor,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = contact.phone,
                color = ContactNumberColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}