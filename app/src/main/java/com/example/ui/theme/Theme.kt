package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Navy950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    secondary = Sky500,
    onSecondary = Navy950,
    secondaryContainer = Navy700,
    onSecondaryContainer = PureWhite,
    tertiary = Amber500,
    onTertiary = Navy950,
    background = Navy950,
    onBackground = PureWhite,
    surface = Navy900,
    onSurface = PureWhite,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate400,
    outline = Navy700,
    error = Rose500,
    onError = PureWhite,
    errorContainer = Rose900,
    onErrorContainer = Rose100
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = PureWhite,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Navy800,
    onSecondary = PureWhite,
    secondaryContainer = Slate100,
    onSecondaryContainer = Navy900,
    tertiary = Amber500,
    onTertiary = PureWhite,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    error = Rose500,
    onError = PureWhite,
    errorContainer = Rose100,
    onErrorContainer = Rose900
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StockShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
