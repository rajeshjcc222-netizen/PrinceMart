package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhiteSurface,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = AmberAccent,
    onSecondary = AmberOnContainer,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = StatusBlue,
    onTertiary = PureWhiteSurface,
    tertiaryContainer = StatusBlueContainer,
    onTertiaryContainer = StatusBlue,
    background = SoftGrayBackground,
    onBackground = InkDark,
    surface = PureWhiteSurface,
    onSurface = InkDark,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineSubtle,
    error = StatusDanger,
    onError = PureWhiteSurface,
    errorContainer = StatusDangerContainer,
    onErrorContainer = StatusDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = EmeraldLightContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = Color(0xFF423200),
    onSecondaryContainer = AmberContainer,
    tertiary = Color(0xFF8EC5FF),
    onTertiary = Color(0xFF003258),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = Color(0xFF334238),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF561818),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val PrinceMartShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PrinceMartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PrinceMartShapes,
        content = content
    )
}
