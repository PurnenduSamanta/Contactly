package com.purnendu.contactly.ui.screens.setting.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A beautiful artistic footer with an animated heart and floating particles.
 * 
 * "Designed & developed from a true story ❤️"
 */
@Composable
fun ArtisticFooter(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "footer_animation")
    
    // Heart pulse animation (breathing effect)
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )
    
    // Glow pulse animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    
    // Shimmer animation for text
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // Rotation for sparkles
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )
    
    // Floating particles state
    val particles = remember { mutableStateListOf<Particle>() }
    
    // Generate particles periodically
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < 12) {
                particles.add(Particle.create())
            }
            delay(400)
        }
    }
    
    // Animate particles
    LaunchedEffect(Unit) {
        while (true) {
            particles.forEachIndexed { index, particle ->
                particles[index] = particle.update()
            }
            particles.removeAll { it.alpha <= 0f }
            delay(16) // ~60fps
        }
    }
    
    // Colors
    val heartColor = Color(0xFFE91E63) // Pink
    val heartColorLight = Color(0xFFFF5C8D)
    val heartColorDark = Color(0xFFC2185B)
    val glowColor = Color(0xFFFF80AB)
    val sparkleColor = Color(0xFFFFD700) // Gold
    
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Heart with particles
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw floating particles
                particles.forEach { particle ->
                    drawParticle(particle, centerX, centerY)
                }
                
                // Draw outer glow rings
                for (i in 3 downTo 1) {
                    drawCircle(
                        color = glowColor.copy(alpha = glowAlpha * 0.15f / i),
                        radius = 45f * heartScale + (i * 12f),
                        center = Offset(centerX, centerY)
                    )
                }
                
                // Draw sparkles around the heart
                drawSparkles(
                    centerX = centerX,
                    centerY = centerY,
                    rotation = sparkleRotation,
                    color = sparkleColor,
                    heartScale = heartScale
                )
                
                // Draw the heart
                drawHeart(
                    centerX = centerX,
                    centerY = centerY,
                    scale = heartScale,
                    baseColor = heartColor,
                    lightColor = heartColorLight,
                    darkColor = heartColorDark
                )
                
                // Draw heart shine (highlight)
                drawHeartShine(
                    centerX = centerX,
                    centerY = centerY,
                    scale = heartScale
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main text with shimmer effect
        Box {
            // Create shimmer gradient
            val shimmerColors = if (isDarkTheme) {
                listOf(
                    Color(0xFFAAAAAA),
                    Color(0xFFFFFFFF),
                    Color(0xFFE91E63),
                    Color(0xFFFFFFFF),
                    Color(0xFFAAAAAA)
                )
            } else {
                listOf(
                    Color(0xFF666666),
                    Color(0xFF333333),
                    Color(0xFFE91E63),
                    Color(0xFF333333),
                    Color(0xFF666666)
                )
            }
            
            val shimmerBrush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(shimmerOffset * 600f - 200f, 0f),
                end = Offset(shimmerOffset * 600f + 200f, 0f)
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
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Secondary text
        Text(
            text = "from a true story",
            style = TextStyle(
                color = if (isDarkTheme) Color(0xFFBBBBBB) else Color(0xFF777777),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Particle data class for floating animation
 */
private data class Particle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val alpha: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val type: ParticleType
) {
    fun update(): Particle = copy(
        x = x + velocityX,
        y = y + velocityY,
        alpha = (alpha - 0.008f).coerceAtLeast(0f),
        rotation = rotation + rotationSpeed
    )
    
    companion object {
        fun create(): Particle {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 0.8f + 0.3f
            return Particle(
                x = 0f,
                y = 0f,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed - 0.5f, // Slight upward bias
                size = Random.nextFloat() * 4f + 2f,
                alpha = 1f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 4f - 2f,
                type = ParticleType.entries[Random.nextInt(ParticleType.entries.size)]
            )
        }
    }
}

private enum class ParticleType {
    CIRCLE, STAR, HEART_MINI
}

/**
 * Draw a single particle
 */
private fun DrawScope.drawParticle(
    particle: Particle,
    centerX: Float,
    centerY: Float
) {
    val px = centerX + particle.x * 50f
    val py = centerY + particle.y * 50f
    
    val particleColors = listOf(
        Color(0xFFFF80AB),
        Color(0xFFFFD700),
        Color(0xFFE91E63),
        Color(0xFFFF5C8D)
    )
    val color = particleColors[particle.type.ordinal % particleColors.size]
        .copy(alpha = particle.alpha * 0.8f)
    
    when (particle.type) {
        ParticleType.CIRCLE -> {
            drawCircle(
                color = color,
                radius = particle.size,
                center = Offset(px, py)
            )
        }
        ParticleType.STAR -> {
            rotate(particle.rotation, Offset(px, py)) {
                drawStar(px, py, particle.size, color)
            }
        }
        ParticleType.HEART_MINI -> {
            rotate(particle.rotation, Offset(px, py)) {
                drawMiniHeart(px, py, particle.size * 0.8f, color)
            }
        }
    }
}

/**
 * Draw sparkles around the heart
 */
private fun DrawScope.drawSparkles(
    centerX: Float,
    centerY: Float,
    rotation: Float,
    color: Color,
    heartScale: Float
) {
    val sparkleCount = 6
    val radius = 50f * heartScale
    
    for (i in 0 until sparkleCount) {
        val angle = (rotation + i * (360f / sparkleCount)) * PI.toFloat() / 180f
        val sparkleX = centerX + cos(angle) * radius
        val sparkleY = centerY + sin(angle) * radius
        
        val sparkleAlpha = (0.4f + 0.4f * sin(rotation * PI.toFloat() / 180f + i)).coerceIn(0.2f, 0.8f)
        
        rotate(rotation + i * 60f, Offset(sparkleX, sparkleY)) {
            drawStar(sparkleX, sparkleY, 4f, color.copy(alpha = sparkleAlpha))
        }
    }
}

/**
 * Draw a 4-pointed star
 */
private fun DrawScope.drawStar(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx + size * 0.3f, cy)
        lineTo(cx, cy + size)
        lineTo(cx - size * 0.3f, cy)
        close()
        
        moveTo(cx - size, cy)
        lineTo(cx, cy + size * 0.3f)
        lineTo(cx + size, cy)
        lineTo(cx, cy - size * 0.3f)
        close()
    }
    drawPath(path, color)
}

/**
 * Draw a mini heart particle
 */
private fun DrawScope.drawMiniHeart(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        val width = size * 2
        val height = size * 2
        
        moveTo(cx, cy + height * 0.35f)
        cubicTo(
            cx - width * 0.5f, cy,
            cx - width * 0.5f, cy - height * 0.35f,
            cx, cy - height * 0.15f
        )
        cubicTo(
            cx + width * 0.5f, cy - height * 0.35f,
            cx + width * 0.5f, cy,
            cx, cy + height * 0.35f
        )
    }
    drawPath(path, color, style = Fill)
}

/**
 * Draw the main heart shape with gradient
 */
private fun DrawScope.drawHeart(
    centerX: Float,
    centerY: Float,
    scale: Float,
    baseColor: Color,
    lightColor: Color,
    darkColor: Color
) {
    val heartSize = 28f * scale
    
    val path = Path().apply {
        val width = heartSize * 2
        val height = heartSize * 2.2f
        val topY = centerY - height * 0.3f
        val bottomY = centerY + height * 0.5f
        
        // Start at bottom point
        moveTo(centerX, bottomY)
        
        // Left curve
        cubicTo(
            centerX - width * 0.55f, centerY + height * 0.1f,
            centerX - width * 0.55f, topY,
            centerX, topY + height * 0.2f
        )
        
        // Right curve
        cubicTo(
            centerX + width * 0.55f, topY,
            centerX + width * 0.55f, centerY + height * 0.1f,
            centerX, bottomY
        )
    }
    
    // Draw heart shadow
    val shadowPath = Path().apply {
        addPath(path, Offset(2f, 3f))
    }
    drawPath(
        path = shadowPath,
        color = Color.Black.copy(alpha = 0.2f),
        style = Fill
    )
    
    // Draw heart gradient fill
    val heartGradient = Brush.verticalGradient(
        colors = listOf(lightColor, baseColor, darkColor),
        startY = centerY - heartSize,
        endY = centerY + heartSize
    )
    drawPath(path, heartGradient, style = Fill)
    
    // Draw heart outline
    drawPath(
        path = path,
        color = darkColor,
        style = Stroke(width = 1.5f, cap = StrokeCap.Round)
    )
}

/**
 * Draw shine/highlight on the heart
 */
private fun DrawScope.drawHeartShine(
    centerX: Float,
    centerY: Float,
    scale: Float
) {
    val shineSize = 8f * scale
    val shineX = centerX - 10f * scale
    val shineY = centerY - 12f * scale
    
    // Main shine
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.7f),
                Color.White.copy(alpha = 0f)
            ),
            center = Offset(shineX, shineY),
            radius = shineSize
        ),
        radius = shineSize,
        center = Offset(shineX, shineY)
    )
    
    // Small secondary shine
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0f)
            ),
            center = Offset(shineX + 8f * scale, shineY + 6f * scale),
            radius = shineSize * 0.4f
        ),
        radius = shineSize * 0.4f,
        center = Offset(shineX + 8f * scale, shineY + 6f * scale)
    )
}

/**
 * Extension to get luminance of a color
 */
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
