package com.venom.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.venom.launcher.data.LauncherSettings

// ---------------------------------------------------------------- palette ----

val VenomBlack = Color(0xFF05070A)
val VenomSurface = Color(0xFF0E1216)
val VenomSurfaceHigh = Color(0xFF151B20)
val VenomSurfaceHighest = Color(0xFF1D242A)
val VenomHairline = Color(0x1FFFFFFF)
val VenomText = Color(0xFFF2F5F4)
val VenomTextDim = Color(0xFF9AA7A3)

/** Glass used by the dock, sheets and dialogs. */
val VenomGlass = Color(0x1410FF40)
val VenomGlassBorder = Color(0x24FFFFFF)

// ------------------------------------------------------------ typography ----

val VenomTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

val VenomShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

// ----------------------------------------------------------------- theme ----

@Composable
fun VenomTheme(
    settings: LauncherSettings = LauncherSettings(),
    content: @Composable () -> Unit,
) {
    val accent = Color(settings.accent.color)
    val context = LocalContext.current

    val scheme = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { dynamicDarkColorScheme(context) }.getOrDefault(venomScheme(accent))
    } else {
        venomScheme(accent)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = VenomTypography,
        shapes = VenomShapes,
        content = content,
    )
}

private fun venomScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF06080A),
    primaryContainer = accent.copy(alpha = 0.16f),
    onPrimaryContainer = accent,
    secondary = accent.copy(alpha = 0.78f),
    onSecondary = Color(0xFF06080A),
    background = VenomBlack,
    onBackground = VenomText,
    surface = VenomSurface,
    onSurface = VenomText,
    surfaceVariant = VenomSurfaceHigh,
    onSurfaceVariant = VenomTextDim,
    surfaceContainerHigh = VenomSurfaceHigh,
    surfaceContainerHighest = VenomSurfaceHighest,
    outline = Color.White.copy(alpha = 0.14f),
    outlineVariant = Color.White.copy(alpha = 0.08f),
)
