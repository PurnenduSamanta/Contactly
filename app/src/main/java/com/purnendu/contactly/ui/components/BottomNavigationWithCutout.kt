package com.purnendu.contactly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.ui.screens.Screen

/**
 * A custom bottom navigation bar with a semi-circle cutout in the center
 * for a floating action button.
 */
@Composable
fun BottomNavigationWithCutout(
    modifier: Modifier = Modifier,
    screens: List<Screen>,
    currentRoute: String?,
    onItemClick: (Screen) -> Unit,
    fabContent: @Composable () -> Unit,
    fabOnClick: () -> Unit,
    showFab: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    fabContainerColor: Color = MaterialTheme.colorScheme.primary,
    fabContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    fabSize: Dp = 56.dp,
    fabElevation: Dp = 6.dp,
    navBarHeight: Dp = 80.dp
) {
    // Filter to only screens that have icons (valid nav items)
    val validNavScreens = screens.filter { it.selectedIcon != null && it.notSelectedIcon != null }
    
    // Gap between FAB and cutout edge
    val fabGap = 10.dp
    // Cutout radius = FAB radius + gap
    val cutoutRadius = (fabSize / 2) + fabGap

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navBarHeight)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .drawWithCache {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val cutoutRadiusPx = cutoutRadius.toPx()

                // Path for the navigation bar shape
                val navBarPath = Path().apply {
                    if (showFab) {
                        // Start from top-left
                        moveTo(0f, 0f)
                        
                        // Line to where cutout starts
                        lineTo(centerX - cutoutRadiusPx, 0f)
                        
                        // Semi-circle cutout (arc going down)
                        arcTo(
                            rect = Rect(
                                left = centerX - cutoutRadiusPx,
                                top = -cutoutRadiusPx,
                                right = centerX + cutoutRadiusPx,
                                bottom = cutoutRadiusPx
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )
                        
                        // Line to top-right
                        lineTo(width, 0f)
                    } else {
                        // No cutout - straight line
                        moveTo(0f, 0f)
                        lineTo(width, 0f)
                    }
                    
                    // Right edge
                    lineTo(width, height)
                    // Bottom edge
                    lineTo(0f, height)
                    // Close path
                    close()
                }

                onDrawBehind {
                    // Draw the filled background
                    drawPath(
                        path = navBarPath,
                        color = containerColor
                    )
                    
                    // Draw the outline
                    drawPath(
                        path = navBarPath,
                        color = outlineColor,
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
    ) {
        // Navigation items - split left and right
        val leftItems = validNavScreens.take(validNavScreens.size / 2 + validNavScreens.size % 2)
        val rightItems = validNavScreens.drop(validNavScreens.size / 2 + validNavScreens.size % 2)
        
        // Left side items
        Row(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(navBarHeight)
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftItems.forEach { screen ->
                val isSelected = currentRoute == screen::class.qualifiedName
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    ),
                    icon = {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon!! else screen.notSelectedIcon!!,
                            contentDescription = screen.title
                        )
                    },
                    label = {
                        Text(
                            text = screen.title ?: "",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = isSelected,
                    onClick = { onItemClick(screen) }
                )
            }
        }
        
        // Right side items
        Row(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(navBarHeight)
                .align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            rightItems.forEach { screen ->
                val isSelected = currentRoute == screen::class.qualifiedName
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    ),
                    icon = {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon!! else screen.notSelectedIcon!!,
                            contentDescription = screen.title
                        )
                    },
                    label = {
                        Text(
                            text = screen.title ?: "",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = isSelected,
                    onClick = { onItemClick(screen) }
                )
            }
        }
        
        // Centered FAB - center aligned with cutout center (top edge of nav bar)
        if (showFab) {
            FloatingActionButton(
                onClick = fabOnClick,
                modifier = Modifier
                    .size(fabSize)
                    .align(Alignment.TopCenter)
                    .offset(y = -(fabSize / 2)),  // Move up so FAB center is at top edge
                shape = CircleShape,
                containerColor = fabContainerColor,
                contentColor = fabContentColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = fabElevation,
                    pressedElevation = fabElevation + 2.dp
                )
            ) {
                fabContent()
            }
        }
    }
}
