package com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.ui.theme.CancelButtonColor
import com.purnendu.contactly.ui.theme.InputBorder
import com.purnendu.contactly.ui.theme.SaveButtonColor
import com.purnendu.contactly.ui.theme.SheetBackground
import com.purnendu.contactly.ui.theme.SheetTitleColor
import com.purnendu.contactly.ui.theme.SubtitleTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleSheet(
    contact: Contact,
    temporaryName: String,
    startTime: String,
    endTime: String,
    onTemporaryNameChange: (String) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onCancel() },
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {

            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back),
                        tint = SheetTitleColor
                    )
                }

                AsyncImage(
                    model = contact.image,
                    contentDescription = contact.name,
                    placeholder = painterResource(R.drawable.avatar_liam) ,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(contact.name, color = SheetTitleColor, fontWeight = FontWeight.SemiBold)
                    Text(contact.phone, color = SubtitleTextColor, fontSize = 14.sp)
                }
            }

            HorizontalDivider(color = InputBorder)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(id = R.string.label_quick_edit), color = SheetTitleColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))
                }

                // Temporary Name Input
                item {
                    Text(stringResource(id = R.string.label_temp_name), color = SheetTitleColor)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = temporaryName,
                        onValueChange = onTemporaryNameChange,
                        placeholder = { Text("Enter temporary name") },
                        colors = customTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Start Time
                item {
                    LabeledTimeInput(
                        label = stringResource(id = R.string.label_start_time),
                        value = startTime,
                        onClick = onStartTimeClick
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // End Time
                item {
                    LabeledTimeInput(
                        label = stringResource(id = R.string.label_end_time),
                        value = endTime,
                        onClick = onEndTimeClick
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CancelButtonColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(id = R.string.action_cancel), color = SheetTitleColor) }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SaveButtonColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(id = R.string.action_save)) }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}


@Preview
@Composable
fun EditScheduleSheetPreview() {
    EditScheduleSheet(
        contact = Contact("Purnendu","9614472290",null),
        temporaryName = "Joy",
        startTime = "",
        endTime ="",
        onTemporaryNameChange = {},
        onStartTimeClick = {},
        onEndTimeClick = {},
        onCancel = {},
        onSave = {}
    )
}
