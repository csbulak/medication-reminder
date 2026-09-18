package com.medicationreminder.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Forest = Color(0xFF0D6E4F)
private val ForestDeep = Color(0xFF08543C)
private val Mint = Color(0xFFD8F3E7)
private val Mist = Color(0xFFF3F7F5)
private val Ink = Color(0xFF14241C)
private val Muted = Color(0xFF5A6B62)
private val Coral = Color(0xFFC45C4A)
private val Amber = Color(0xFFB8860B)

val StatusTaken = Color(0xFF1B7A4E)
val StatusSkipped = Color(0xFF8A6A1A)
val StatusMissed = Coral
val StatusPending = Forest

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDeep,
    secondary = ForestDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EEE8),
    onSecondaryContainer = Ink,
    tertiary = Amber,
    background = Mist,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EFEC),
    onSurfaceVariant = Muted,
    outline = Color(0xFFC5D2CB),
    error = Coral,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6DD4A8),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF0B4F3A),
    onPrimaryContainer = Mint,
    secondary = Color(0xFF9FD9BE),
    background = Color(0xFF0C1612),
    onBackground = Color(0xFFE6F4EC),
    surface = Color(0xFF15201B),
    onSurface = Color(0xFFE6F4EC),
    surfaceVariant = Color(0xFF24332C),
    onSurfaceVariant = Color(0xFFB7C7BE),
    outline = Color(0xFF3E5248),
    error = Color(0xFFFFB4A8)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MedicationReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
