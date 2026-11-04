package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ArcadeCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9CF0FF),
    secondary = ArcadeCoral,
    onSecondary = Color(0xFF5A0019),
    secondaryContainer = Color(0xFF800026),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = ArcadeYellow,
    onTertiary = Color(0xFF433400),
    background = ArcadeBackgroundDark,
    onBackground = ArcadeOnDark,
    surface = ArcadeSurfaceDark,
    onSurface = ArcadeOnDark,
    surfaceVariant = ArcadeSurfaceVariantDark,
    onSurfaceVariant = ArcadeOnSurfaceVariant,
    outline = ArcadeCardBorder
)

private val LightColorScheme = darkColorScheme(
    // We prefer a dark/arcade aesthetic for social drawing games to make canvas colors pop!
    primary = ArcadeCyan,
    onPrimary = Color(0xFF00363D),
    secondary = ArcadeCoral,
    onSecondary = Color(0xFF5A0019),
    tertiary = ArcadeYellow,
    background = ArcadeBackgroundDark,
    surface = ArcadeSurfaceDark,
    onBackground = ArcadeOnDark,
    onSurface = ArcadeOnDark
)

@Composable
fun HreTheiRawhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
