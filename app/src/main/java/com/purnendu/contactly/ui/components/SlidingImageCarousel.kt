package com.purnendu.contactly.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.purnendu.contactly.R
import kotlinx.coroutines.delay

/**
 * Image type indicator for the carousel
 */
enum class ImageType {
    ORIGINAL,
    TEMPORARY
}

/**
 * Auto-sliding image carousel that displays original and temporary contact images.
 * 
 * Features:
 * - Auto-slides between images every 3 seconds
 * - Color-coded borders: Green for Original, Blue/Purple for Temporary
 * - Graceful fallback if temporary image URI is invalid
 * - Indicator dots showing current image type
 * 
 * @param originalImageUri URI of the original contact image (from contacts)
 * @param temporaryImageUri Optional URI of the temporary image (from gallery)
 * @param modifier Modifier for the carousel container
 * @param imageSize Size of the image
 * @param autoSlideIntervalMs Auto-slide interval in milliseconds (default: 3000ms)
 */
@Composable
fun SlidingImageCarousel(
    originalImageUri: String?,
    temporaryImageUri: String?,
    modifier: Modifier = Modifier,
    imageSize: Modifier = Modifier.size(androidx.compose.ui.unit.Dp(80f)),
    autoSlideIntervalMs: Long = 3000L
) {
    // If no temporary image, just show original without carousel
    if (temporaryImageUri.isNullOrBlank()) {
        SingleImage(
            imageUri = originalImageUri,
            imageType = ImageType.ORIGINAL,
            modifier = modifier.then(imageSize)
        )
        return
    }
    
    // Track current image index
    var currentIndex by remember { mutableIntStateOf(0) }
    val images = remember(originalImageUri, temporaryImageUri) {
        listOf(
            Pair(originalImageUri, ImageType.ORIGINAL),
            Pair(temporaryImageUri, ImageType.TEMPORARY)
        )
    }
    
    // Auto-slide effect
    LaunchedEffect(images) {
        while (true) {
            delay(autoSlideIntervalMs)
            currentIndex = (currentIndex + 1) % images.size
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val (imageUri, imageType) = images[currentIndex]
        
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "ImageCarouselAnimation"
        ) { index ->
            val (uri, type) = images[index]
            SingleImage(
                imageUri = uri,
                imageType = type,
                modifier = imageSize
            )
        }
        
        // Indicator dots at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = androidx.compose.ui.unit.Dp(4f))
        ) {
            images.forEachIndexed { index, (_, type) ->
                IndicatorDot(
                    isActive = index == currentIndex,
                    imageType = type
                )
                if (index < images.size - 1) {
                    Spacer(modifier = Modifier.width(androidx.compose.ui.unit.Dp(4f)))
                }
            }
        }
    }
}

/**
 * Single image with colored border based on image type
 */
@Composable
private fun SingleImage(
    imageUri: String?,
    imageType: ImageType,
    modifier: Modifier = Modifier
) {
    val borderColor = when (imageType) {
        ImageType.ORIGINAL -> Color(0xFF4CAF50) // Green for original
        ImageType.TEMPORARY -> Color(0xFF7C4DFF) // Purple for temporary
    }
    
    val borderGradient = when (imageType) {
        ImageType.ORIGINAL -> Brush.linearGradient(
            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
        )
        ImageType.TEMPORARY -> Brush.linearGradient(
            colors = listOf(Color(0xFF7C4DFF), Color(0xFFB388FF))
        )
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = androidx.compose.ui.unit.Dp(3f),
                brush = borderGradient,
                shape = CircleShape
            )
            .padding(androidx.compose.ui.unit.Dp(3f))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            !imageUri.isNullOrBlank() -> {
                AsyncImage(
                    model = imageUri,
                    contentDescription = if (imageType == ImageType.ORIGINAL) "Original contact image" else "Temporary contact image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.avatar_placeholder),
                    placeholder = painterResource(id = R.drawable.avatar_placeholder)
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Avatar placeholder",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Small indicator dot showing current image type
 */
@Composable
private fun IndicatorDot(
    isActive: Boolean,
    imageType: ImageType
) {
    val activeColor = when (imageType) {
        ImageType.ORIGINAL -> Color(0xFF4CAF50)
        ImageType.TEMPORARY -> Color(0xFF7C4DFF)
    }
    
    val color = if (isActive) activeColor else activeColor.copy(alpha = 0.3f)
    
    Box(
        modifier = Modifier
            .size(androidx.compose.ui.unit.Dp(6f))
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Rectangle version of the sliding carousel for list view (wider aspect ratio)
 */
@Composable
fun SlidingImageCarouselRect(
    originalImageUri: String?,
    temporaryImageUri: String?,
    modifier: Modifier = Modifier,
    autoSlideIntervalMs: Long = 3000L
) {
    // If no temporary image, just show original without carousel
    if (temporaryImageUri.isNullOrBlank()) {
        SingleImageRect(
            imageUri = originalImageUri,
            imageType = ImageType.ORIGINAL,
            modifier = modifier
        )
        return
    }
    
    // Track current image index
    var currentIndex by remember { mutableIntStateOf(0) }
    val images = remember(originalImageUri, temporaryImageUri) {
        listOf(
            Pair(originalImageUri, ImageType.ORIGINAL),
            Pair(temporaryImageUri, ImageType.TEMPORARY)
        )
    }
    
    // Auto-slide effect
    LaunchedEffect(images) {
        while (true) {
            delay(autoSlideIntervalMs)
            currentIndex = (currentIndex + 1) % images.size
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "ImageCarouselRectAnimation"
        ) { index ->
            val (uri, type) = images[index]
            SingleImageRect(
                imageUri = uri,
                imageType = type,
                modifier = Modifier.matchParentSize()
            )
        }
        
        // Indicator dots at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = androidx.compose.ui.unit.Dp(4f))
        ) {
            images.forEachIndexed { index, (_, type) ->
                IndicatorDot(
                    isActive = index == currentIndex,
                    imageType = type
                )
                if (index < images.size - 1) {
                    Spacer(modifier = Modifier.width(androidx.compose.ui.unit.Dp(4f)))
                }
            }
        }
    }
}

/**
 * Single rectangular image with colored border based on image type
 */
@Composable
private fun SingleImageRect(
    imageUri: String?,
    imageType: ImageType,
    modifier: Modifier = Modifier
) {
    val borderGradient = when (imageType) {
        ImageType.ORIGINAL -> Brush.linearGradient(
            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
        )
        ImageType.TEMPORARY -> Brush.linearGradient(
            colors = listOf(Color(0xFF7C4DFF), Color(0xFFB388FF))
        )
    }
    
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(androidx.compose.ui.unit.Dp(15f))
    
    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = androidx.compose.ui.unit.Dp(3f),
                brush = borderGradient,
                shape = shape
            )
            .padding(androidx.compose.ui.unit.Dp(3f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            !imageUri.isNullOrBlank() -> {
                AsyncImage(
                    model = imageUri,
                    contentDescription = if (imageType == ImageType.ORIGINAL) "Original contact image" else "Temporary contact image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.avatar_placeholder),
                    placeholder = painterResource(id = R.drawable.avatar_placeholder)
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Avatar placeholder",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
