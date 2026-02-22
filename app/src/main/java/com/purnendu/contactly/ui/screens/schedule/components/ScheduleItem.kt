package com.purnendu.contactly.ui.screens.schedule.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.components.SlidingImageCarousel
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.utils.ViewMode
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.ScheduleType
import com.purnendu.contactly.utils.expressiveScale
import com.purnendu.contactly.utils.rememberExpressiveAnimation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleItem(
    modifier: Modifier = Modifier,
    schedule: Schedule,
    index: Int = 0,
    viewMode: ViewMode = ViewMode.LIST,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit,
    onInstantToggle: ((schedule: Schedule) -> Unit)? = null,
) {
    if (viewMode == ViewMode.LIST)
    {
        ListScheduleItem(
            modifier = modifier,
            schedule = schedule,
            index = index,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onContactDetailsClick = onContactDetailsClick,
            onInstantToggle = onInstantToggle
        )
    }
    else {
        GridScheduleItem(
            modifier = modifier,
            schedule = schedule,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onContactDetailsClick = onContactDetailsClick,
            onInstantToggle = onInstantToggle
        )
    }
}

@Composable
private fun ListScheduleItem(
    modifier: Modifier,
    schedule: Schedule,
    index: Int,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit,
    onInstantToggle: ((schedule: Schedule) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val expressiveScale = rememberExpressiveAnimation(
        targetValue = if (isPressed) 0.97f else 1f
    )
    val isEven = index % 2 == 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .expressiveScale(expressiveScale.value),
        contentAlignment = if (isEven) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        val pointRatio = 0.18f
        val iconSizeDp = 36.dp
        val density = LocalDensity.current
        var cardSizePx by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { cardSizePx = it }
                    .border(width = 1.dp, shape = HexagonShape(), color = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = HexagonShape(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular avatar at top - centered
                    SlidingImageCarousel(
                        originalImageUri = schedule.originalImageUri,
                        temporaryImageUri = schedule.temporaryImageUri,
                        modifier = Modifier,
                        imageSize = Modifier.size(80.dp),
                        autoSlideIntervalMs = 3000L
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Temporary name - prominent, centered
                    Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Original name
                    Text(
                        text = stringResource(id = R.string.original_name, schedule.originalName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time/Active/Instant status
                    if (schedule.scheduleType == ScheduleType.INSTANT) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (schedule.isCurrentlyActive) "⚡ Applied" else "⚡ Instant",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = schedule.isCurrentlyActive,
                                onCheckedChange = { onInstantToggle?.invoke(schedule) }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    } else if (schedule.startAtMillis != null && schedule.startAtMillis > 0 && schedule.endAtMillis != null && schedule.endAtMillis > 0) {
                        if (schedule.isCurrentlyActive) {
                            // Show "Active" when schedule is currently running
                            Text(
                                text = "● Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            val startTime = formatter.format(Date(schedule.startAtMillis))
                            val endTime = formatter.format(Date(schedule.endAtMillis))

                            val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                            val startDate = dateFormatter.format(Date(schedule.startAtMillis))
                            val endDate = dateFormatter.format(Date(schedule.endAtMillis))
                            if (startDate != null && endDate != null) {
                                if (startDate == endDate) {
                                    Text(
                                        text = "$startDate\n$startTime - $endTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Day chips - only for non-INSTANT schedules
                    if (schedule.scheduleType != ScheduleType.INSTANT) {
                        DayChips(
                            selectedDays = schedule.selectedDays ?: 0,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { onDeleteClick(schedule) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onEditClick(schedule) },
                            enabled = !schedule.isCurrentlyActive,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = if (schedule.isCurrentlyActive)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Shimmer overlay on top of Card
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .shimmerBorder(shape = HexagonShape(), borderWidth = 2.5.dp)
            )

            // Contact details icon - positioned outside Card so hexagon shape doesn't clip it.
            // Top-right diagonal edge: from (width*(1-pointRatio), 0) to (width, height/2)
            // Midpoint: x = width*(1 - pointRatio/2), y = height/4
            if (cardSizePx != IntSize.Zero) {
                val midXDp = with(density) { (cardSizePx.width * (1f - pointRatio / 2f)).toDp() }
                val midYDp = with(density) { (cardSizePx.height / 4f).toDp() }

                IconButton(
                    onClick = { onContactDetailsClick(schedule) },
                    modifier = Modifier
                        .offset(x = midXDp - iconSizeDp / 2, y = midYDp - iconSizeDp / 2)
                        .size(iconSizeDp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "View Contact Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridScheduleItem(
    modifier: Modifier,
    schedule: Schedule,
    onEditClick: (schedule: Schedule) -> Unit,
    onDeleteClick: (schedule: Schedule) -> Unit,
    onContactDetailsClick: (schedule: Schedule) -> Unit,
    onInstantToggle: ((schedule: Schedule) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val expressiveScale = rememberExpressiveAnimation(
        targetValue = if (isPressed) 0.97f else 1f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .expressiveScale(expressiveScale.value)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(15.dp)
                ),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar - Use carousel for both original and temp images
                SlidingImageCarousel(
                    originalImageUri = schedule.originalImageUri,
                    temporaryImageUri = schedule.temporaryImageUri,
                    modifier = Modifier,
                    imageSize = Modifier.size(80.dp),
                    autoSlideIntervalMs = 3000L
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Name
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Original name
                Text(
                    text = stringResource(id = R.string.original_name, schedule.originalName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time/Active/Instant status
                if (schedule.scheduleType == ScheduleType.INSTANT) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (schedule.isCurrentlyActive) "⚡ Applied" else "⚡ Instant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = schedule.isCurrentlyActive,
                            onCheckedChange = { onInstantToggle?.invoke(schedule) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                } else if (schedule.startAtMillis != null && schedule.startAtMillis > 0 && schedule.endAtMillis != null && schedule.endAtMillis > 0) {
                    if (schedule.isCurrentlyActive) {
                        // Show "Active" when schedule is currently running
                        Text(
                            text = "● Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val startTime = formatter.format(Date(schedule.startAtMillis))
                        val endTime = formatter.format(Date(schedule.endAtMillis))

                        val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val startDate = dateFormatter.format(Date(schedule.startAtMillis))
                        val endDate = dateFormatter.format(Date(schedule.endAtMillis))
                        if (startDate != null && endDate != null) {
                            if (startDate == endDate) {
                                Text(
                                    text = "$startDate\n$startTime - $endTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Day chips - only for non-INSTANT schedules
                if (schedule.scheduleType != ScheduleType.INSTANT) {
                    DayChips(
                        selectedDays = schedule.selectedDays ?: 0,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onDeleteClick(schedule) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEditClick(schedule) },
                        enabled = !schedule.isCurrentlyActive,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (schedule.isCurrentlyActive)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Contact details icon button
            IconButton(
                onClick = { onContactDetailsClick(schedule) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "View Contact Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            }
        }

        // Shimmer overlay on top of Card
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .shimmerBorder(shape = RoundedCornerShape(15.dp), borderWidth = 2.5.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleItemPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        Column {
            Text("List View:")
            ScheduleItem(
                schedule = Schedule(
                    id = "1",
                    name = "Ethan Carter",
                    originalName = "Ethan",
                    avatarResId = null,
                    contactId = 0L,
                    selectedDays = 127,
                    startAtMillis = 0L,
                    endAtMillis = 0L,
                    scheduleType = ScheduleType.ONE_TIME,
                    temporaryImageUri = null,
                    originalImageUri = null
                ),
                index = 0,
                viewMode = ViewMode.LIST,
                onEditClick = {},
                onDeleteClick = {},
                onContactDetailsClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Grid View:")
            ScheduleItem(
                schedule = Schedule(
                    id = "1",
                    name = "Ethan Carter",
                    originalName = "Ethan",
                    avatarResId = null,
                    contactId = 0L,
                    selectedDays = 127,
                    startAtMillis = 0L,
                    endAtMillis = 0L,
                    scheduleType = ScheduleType.ONE_TIME,
                    temporaryImageUri = null,
                    originalImageUri = null
                ),
                index = 0,
                viewMode = ViewMode.GRID,
                onEditClick = {},
                onDeleteClick = {},
                onContactDetailsClick = {}
            )
        }
    }
}

/**
 * A reusable shimmer border modifier that draws an animated shimmer light
 * sweeping along the border of any [Shape]. Adapts colors for dark/light theme.
 */
@Composable
fun Modifier.shimmerBorder(
    shape: Shape,
    borderWidth: Dp = 1.5.dp,
    durationMillis: Int = 8000
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "shimmerBorder")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    // Warm gold for light mode, cool silver-white for dark mode
    val shimmerHighlight = if (isDark) {
        Color(0xFFB0C4DE) // Light steel blue
    } else {
        Color(0xFF6366F1) // Indigo/violet - visible on light backgrounds
    }

    return this.drawWithContent {
        drawContent()

        val shimmerWidth = size.width * 0.5f
        val totalTravel = size.width + size.height + shimmerWidth
        val progress = shimmerProgress * totalTravel

        val peakAlpha = if (isDark) 0.45f else 0.75f

        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                shimmerHighlight.copy(alpha = peakAlpha * 0.2f),
                shimmerHighlight.copy(alpha = peakAlpha),
                shimmerHighlight.copy(alpha = peakAlpha * 0.2f),
                Color.Transparent
            ),
            start = Offset(progress - shimmerWidth, progress - shimmerWidth),
            end = Offset(progress, progress)
        )

        val outline = shape.createOutline(size, layoutDirection, this)
        when (outline) {
            is Outline.Generic -> {
                drawPath(
                    path = outline.path,
                    brush = brush,
                    style = Stroke(width = borderWidth.toPx())
                )
            }
            is Outline.Rounded -> {
                drawRoundRect(
                    brush = brush,
                    cornerRadius = CornerRadius(
                        outline.roundRect.topLeftCornerRadius.x,
                        outline.roundRect.topLeftCornerRadius.y
                    ),
                    style = Stroke(width = borderWidth.toPx())
                )
            }
            is Outline.Rectangle -> {
                drawRect(
                    brush = brush,
                    style = Stroke(width = borderWidth.toPx())
                )
            }
        }
    }
}

class HexagonShape(private val pointRatio: Float = 0.18f, private val cornerRadiusRatio: Float = 0.12f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            Path().apply {
                val width = size.width
                val height = size.height
                val pointWidth = width * pointRatio
                val cornerRadius = height * cornerRadiusRatio
                
                val vertices = listOf(
                    Offset(pointWidth, 0f),
                    Offset(width - pointWidth, 0f),
                    Offset(width, height / 2f),
                    Offset(width - pointWidth, height),
                    Offset(pointWidth, height),
                    Offset(0f, height / 2f)
                )

                for (i in vertices.indices) {
                    val prev = vertices[(i - 1 + vertices.size) % vertices.size]
                    val curr = vertices[i]
                    val next = vertices[(i + 1) % vertices.size]

                    val dx1 = prev.x - curr.x
                    val dy1 = prev.y - curr.y
                    val len1 = kotlin.math.hypot(dx1, dy1)
                    val r1 = cornerRadius.coerceAtMost(len1 / 2f)
                    val pInX = curr.x + dx1 * (r1 / len1)
                    val pInY = curr.y + dy1 * (r1 / len1)

                    val dx2 = next.x - curr.x
                    val dy2 = next.y - curr.y
                    val len2 = kotlin.math.hypot(dx2, dy2)
                    val r2 = cornerRadius.coerceAtMost(len2 / 2f)
                    val pOutX = curr.x + dx2 * (r2 / len2)
                    val pOutY = curr.y + dy2 * (r2 / len2)

                    if (i == 0) {
                        moveTo(pInX, pInY)
                    } else {
                        lineTo(pInX, pInY)
                    }
                    if (r1 > 0f || r2 > 0f) {
                        quadraticTo(curr.x, curr.y, pOutX, pOutY)
                    }
                }
                close()
            }
        )
    }
}
