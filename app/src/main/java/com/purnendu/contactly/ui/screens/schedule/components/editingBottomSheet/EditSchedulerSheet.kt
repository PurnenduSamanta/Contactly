package com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.purnendu.contactly.ui.screens.schedule.components.ErrorMessageCard
import com.purnendu.contactly.utils.ScheduleType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleSheet(
    contact: Contact,
    temporaryName: String,
    temporaryImageUri: String? = null,  // NEW: Optional temporary image URI
    startTime: String,
    endTime: String,
    selectedDays: Set<Int> = setOf(0, 1, 2, 3, 4, 5, 6),  // Default: all days
    scheduleType: ScheduleType = ScheduleType.ONE_TIME,
    error: String?,
    isSaving: Boolean,
    onErrorCardDismiss: () -> Unit,
    onTemporaryNameChange: (String) -> Unit,
    onTemporaryImageClick: () -> Unit,  // NEW: Callback to open image picker
    onTemporaryImageRemove: () -> Unit,  // NEW: Callback to remove temp image
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onDaysChanged: (Set<Int>) -> Unit,
    onScheduleTypeChange: (ScheduleType) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onCancel() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {

            if (error != null) {
                stickyHeader {
                    ErrorMessageCard(
                        message = error,
                        onDismiss = { onErrorCardDismiss() }
                    )
                }
            }

            // Header Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                {
                    IconButton(
                        onClick = onCancel,
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSaving) 0.5f else 1f)
                        )
                    }

                    AsyncImage(
                        model = contact.image,
                        contentDescription = contact.name,
                        placeholder = painterResource(R.drawable.avatar_liam),
                        error = painterResource(R.drawable.avatar_ethan),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            contact.name ?: contact.phone,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            contact.phone,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(id = R.string.label_quick_edit),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))
            }

            // Temporary Name Input
            item {
                Text(
                    stringResource(id = R.string.label_temp_name),
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                    singleLine = true,
                    enabled = !isSaving
                )
                Spacer(Modifier.height(16.dp))
            }

            // NEW: Temporary Image Picker Section
            item {
                Text(
                    text = "Temporary Image (Optional)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                TemporaryImagePicker(
                    imageUri = temporaryImageUri,
                    onPickImage = if (isSaving) { {} } else onTemporaryImageClick,
                    onRemoveImage = if (isSaving) { {} } else onTemporaryImageRemove,
                    enabled = !isSaving
                )

                Spacer(Modifier.height(16.dp))
            }

            // Start Time, End Time (hidden for INSTANT)
            if (scheduleType != ScheduleType.INSTANT) {
                item {
                    LabeledTimeInput(
                        label = stringResource(id = R.string.label_start_time),
                        value = startTime,
                        onClick = if (isSaving) { {} } else onStartTimeClick,
                        enabled = !isSaving
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // End Time
                item {
                    LabeledTimeInput(
                        label = stringResource(id = R.string.label_end_time),
                        value = endTime,
                        onClick = if (isSaving) { {} } else onEndTimeClick,
                        enabled = !isSaving
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            // Schedule Type Toggle (always visible)
            item {
                Text(
                    text = "Schedule Type",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                ScheduleTypeToggle(
                    selectedType = scheduleType,
                    onTypeChange = if (isSaving) { {} } else onScheduleTypeChange,
                    enabled = !isSaving
                )
                Spacer(Modifier.height(20.dp))
            }

            // Day Picker (hidden for INSTANT) + Footer Buttons (always shown)
            item {
                if (scheduleType != ScheduleType.INSTANT) {
                    Text(
                        text = if (scheduleType == ScheduleType.ONE_TIME) {
                            "Select Day"  // One day only
                        } else {
                            "Repeat On"   // Multiple days
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    DayPickerWheel(
                        selectedDays = selectedDays,
                        onDaysChanged = if (isSaving) { {} } else onDaysChanged,
                        singleSelection = scheduleType == ScheduleType.ONE_TIME,
                        enabled = !isSaving
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                )
                {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        Text(
                            stringResource(id = R.string.action_cancel),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSaving) 0.5f else 1f)
                        )
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if(isSaving)
                        {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.secondaryFixed)
                        }
                        else
                        {
                            Text(
                                stringResource(id = R.string.action_save),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Component for picking a temporary image from gallery
 */
@Composable
private fun TemporaryImagePicker(
    imageUri: String?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    enabled: Boolean = true
) {
    val hasImage = !imageUri.isNullOrBlank()

    val borderGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF7C4DFF), Color(0xFFB388FF)) // Purple gradient for temp image
    )

    val mutableInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (hasImage) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Image container with add/preview
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = if (hasImage) borderGradient else Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(3.dp)
                .clip(RoundedCornerShape(17.dp))
                .clickable(
                    onClick = onPickImage,
                    indication = null,
                    interactionSource = mutableInteractionSource
                )
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = hasImage,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(300)
                    ) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = DampingRatioMediumBouncy
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(200)
                    ) + scaleOut(targetScale = 0.8f)
                },
                label = "image_animation"
            ) { hasImg ->
                if (hasImg) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Temporary image preview",
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(17.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.avatar_placeholder)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add temporary image",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = hasImage,
                transitionSpec = {
                    fadeIn() + slideInVertically { -it / 2 } togetherWith
                            fadeOut() + slideOutVertically { it / 2 }
                },
                label = "text_animation"
            ) { hasImg ->
                Column {
                    if (hasImg) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C4DFF))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Image Selected",
                                color = Color(0xFF7C4DFF),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.2.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap to change or remove",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            letterSpacing = 0.1.sp
                        )
                    } else {
                        Text(
                            text = "Add Temporary Image",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Choose from gallery",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            letterSpacing = 0.1.sp
                        )
                    }
                }
            }
        }

        // Remove button (only show if image is selected)
        AnimatedVisibility(
            visible = hasImage,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = DampingRatioMediumBouncy
                )
            ),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(
                onClick = onRemoveImage,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove temporary image",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Preview
@Composable
fun EditScheduleSheetPreview() {
    EditScheduleSheet(
        contact = Contact("Purnendu", "9614472290", null),
        error = null,
        isSaving = false,
        onErrorCardDismiss = {},
        temporaryName = "Joy",
        temporaryImageUri = null,
        startTime = "",
        endTime = "",
        selectedDays = setOf(1, 3, 5),  // Mon, Wed, Fri
        scheduleType = ScheduleType.REPEAT,
        onTemporaryNameChange = {},
        onTemporaryImageClick = {},
        onTemporaryImageRemove = {},
        onStartTimeClick = {},
        onEndTimeClick = {},
        onDaysChanged = {},
        onScheduleTypeChange = {},
        onCancel = {},
        onSave = {}
    )
}
