package com.purnendu.contactly.ui.screens.setting.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Preview for the complete ArtisticFooter with all animations
 */
@Preview(
    name = "Complete Artistic Footer - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun PreviewArtisticFooterLight() {
    MaterialTheme {
        Surface {
            ArtisticFooter()
        }
    }
}

@Preview(
    name = "Complete Artistic Footer - Dark",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewArtisticFooterDark() {
    MaterialTheme {
        Surface(color = Color(0xFF1C1B1F)) {
            ArtisticFooter()
        }
    }
}

/**
 * Preview showing just the main heart without animations
 */
@Preview(
    name = "Main Heart Shape Only",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewHeartOnly() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            drawHeart(
                centerX = centerX,
                centerY = centerY,
                scale = 1f,
                baseColor = Color(0xFFE91E63),
                lightColor = Color(0xFFFF5C8D),
                darkColor = Color(0xFFC2185B)
            )
            
            drawHeartShine(
                centerX = centerX,
                centerY = centerY,
                scale = 1f
            )
        }
    }
}

/**
 * Preview showing the heart with glow rings
 */
@Preview(
    name = "Heart with Glow Rings",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewHeartWithGlow() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val heartScale = 1f
            val glowAlpha = 0.5f
            val glowColor = Color(0xFFFF80AB)
            
            // Draw glow rings
            for (i in 3 downTo 1) {
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha * 0.15f / i),
                    radius = 45f * heartScale + (i * 12f),
                    center = Offset(centerX, centerY)
                )
            }
            
            // Draw heart
            drawHeart(
                centerX = centerX,
                centerY = centerY,
                scale = heartScale,
                baseColor = Color(0xFFE91E63),
                lightColor = Color(0xFFFF5C8D),
                darkColor = Color(0xFFC2185B)
            )
            
            drawHeartShine(
                centerX = centerX,
                centerY = centerY,
                scale = heartScale
            )
        }
    }
}

/**
 * Preview showing the sparkles around the heart
 */
@Preview(
    name = "Heart with Sparkles",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewHeartWithSparkles() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val heartScale = 1f
            val sparkleColor = Color(0xFFFFD700)
            
            // Draw sparkles
            drawSparkles(
                centerX = centerX,
                centerY = centerY,
                rotation = 0f,
                color = sparkleColor,
                heartScale = heartScale
            )
            
            // Draw heart
            drawHeart(
                centerX = centerX,
                centerY = centerY,
                scale = heartScale,
                baseColor = Color(0xFFE91E63),
                lightColor = Color(0xFFFF5C8D),
                darkColor = Color(0xFFC2185B)
            )
            
            drawHeartShine(
                centerX = centerX,
                centerY = centerY,
                scale = heartScale
            )
        }
    }
}

/**
 * Preview showing all three particle types
 */
@Preview(
    name = "Particle Types",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewParticleTypes() {
    Box(
        modifier = Modifier
            .size(300.dp, 150.dp)
            .background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2
            val spacing = size.width / 4
            
            // Circle particle
            val particle1 = Particle(
                x = 0f,
                y = 0f,
                velocityX = 0f,
                velocityY = 0f,
                size = 8f,
                alpha = 1f,
                rotation = 0f,
                rotationSpeed = 0f,
                type = ParticleType.CIRCLE
            )
            drawParticle(particle1, spacing, centerY)
            
            // Star particle
            val particle2 = Particle(
                x = 0f,
                y = 0f,
                velocityX = 0f,
                velocityY = 0f,
                size = 8f,
                alpha = 1f,
                rotation = 0f,
                rotationSpeed = 0f,
                type = ParticleType.STAR
            )
            drawParticle(particle2, spacing * 2, centerY)
            
            // Mini heart particle
            val particle3 = Particle(
                x = 0f,
                y = 0f,
                velocityX = 0f,
                velocityY = 0f,
                size = 8f,
                alpha = 1f,
                rotation = 0f,
                rotationSpeed = 0f,
                type = ParticleType.HEART_MINI
            )
            drawParticle(particle3, spacing * 3, centerY)
        }
        
        // Labels
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Circle", color = Color.White, fontSize = 12.sp)
                Text("Star", color = Color.White, fontSize = 12.sp)
                Text("Mini Heart", color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.padding(16.dp))
        }
    }
}

/**
 * Preview showing the shimmer text effect
 */
@Preview(
    name = "Shimmer Text",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewShimmerText() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1B1F))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Shimmer text
            val shimmerColors = listOf(
                Color(0xFFAAAAAA),
                Color(0xFFFFFFFF),
                Color(0xFFE91E63),
                Color(0xFFFFFFFF),
                Color(0xFFAAAAAA)
            )
            
            val shimmerBrush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(0f, 0f),
                end = Offset(400f, 0f)
            )
            
            Text(
                text = "Designed & developed",
                style = TextStyle(
                    brush = shimmerBrush,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.padding(4.dp))
            
            Text(
                text = "from a true story",
                style = TextStyle(
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preview showing heart at different scales (pulse effect)
 */
@Preview(
    name = "Heart Pulse Sizes",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewHeartPulseSizes() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Small heart (90%)
        Canvas(modifier = Modifier.size(80.dp)) {
            drawHeart(
                centerX = size.width / 2,
                centerY = size.height / 2,
                scale = 0.9f,
                baseColor = Color(0xFFE91E63),
                lightColor = Color(0xFFFF5C8D),
                darkColor = Color(0xFFC2185B)
            )
        }
        
        // Normal heart (100%)
        Canvas(modifier = Modifier.size(80.dp)) {
            drawHeart(
                centerX = size.width / 2,
                centerY = size.height / 2,
                scale = 1.0f,
                baseColor = Color(0xFFE91E63),
                lightColor = Color(0xFFFF5C8D),
                darkColor = Color(0xFFC2185B)
            )
        }
        
        // Large heart (110%)
        Canvas(modifier = Modifier.size(80.dp)) {
            drawHeart(
                centerX = size.width / 2,
                centerY = size.height / 2,
                scale = 1.1f,
                baseColor = Color(0xFFE91E63),
                lightColor = Color(0xFFFF5C8D),
                darkColor = Color(0xFFC2185B)
            )
        }
    }
}

/**
 * Preview showing sparkles only in a circle
 */
@Preview(
    name = "Sparkle Ring Only",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewSparkleRing() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            drawSparkles(
                centerX = centerX,
                centerY = centerY,
                rotation = 0f,
                color = Color(0xFFFFD700),
                heartScale = 1f
            )
        }
    }
}

/**
 * Preview showing just the glow rings
 */
@Preview(
    name = "Glow Rings Only",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F
)
@Composable
fun PreviewGlowRingsOnly() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val heartScale = 1f
            val glowAlpha = 0.6f
            val glowColor = Color(0xFFFF80AB)
            
            for (i in 3 downTo 1) {
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha * 0.15f / i),
                    radius = 45f * heartScale + (i * 12f),
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}
