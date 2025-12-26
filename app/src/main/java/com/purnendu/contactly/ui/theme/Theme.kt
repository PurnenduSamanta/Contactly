package com.purnendu.contactly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.purnendu.contactly.utils.AppThemeMode
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColorScheme = darkColorScheme(
    primary = Crayola,
    onPrimary = Color.White,
    background = ChineseBlack,
    onBackground = AntiFlashWhite,
    surface = RowArrowColor,
    onSurface = AntiFlashWhite,
    surfaceVariant = RowArrowColor,
    onSurfaceVariant = AuroMetalSaurus,
    outline = InputBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Crayola,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = ChineseBlack,
    surface = Color.White,
    onSurface = ChineseBlack,
    surfaceVariant = SearchBarBackground,
    onSurfaceVariant = AuroMetalSaurus,
    outline = InputBorder
)

// Define Material 3 Expressive shapes
val ContactlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ContactlyTheme(
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (appThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ContactlyShapes,
        content = content
    )
}
