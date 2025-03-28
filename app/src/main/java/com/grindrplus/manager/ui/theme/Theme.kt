package com.grindrplus.manager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val GrindrYellow = Color(0xFFFFCC00)
val GrindrDarkYellow = Color(0xFFA38300)

private val LightColorScheme = lightColorScheme(
    primary = GrindrYellow,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFFFE897),
    onPrimaryContainer = Color(0xFF241A00),

    secondary = GrindrDarkYellow,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E1BB),
    onSecondaryContainer = Color(0xFF231B04),

    tertiary = Color(0xFF47664A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8ECC9),
    onTertiaryContainer = Color(0xFF03210C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFEBE1CF),
    onSurfaceVariant = Color(0xFF4C4639),
    outline = Color(0xFF7E7667),
    outlineVariant = Color(0xFFCFC6B4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    inversePrimary = GrindrDarkYellow,
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFF0E7D9),
    surfaceContainerHighest = Color(0xFFEAE1D4)
)

private val DarkColorScheme = darkColorScheme(
    primary = GrindrYellow,
    onPrimary = Color(0xFF000000),
    primaryContainer = GrindrDarkYellow,
    onPrimaryContainer = Color(0xFFFFE897),

    secondary = Color(0xFFD5C5A1),
    onSecondary = Color(0xFF50462A),
    secondaryContainer = GrindrDarkYellow,
    onSecondaryContainer = Color(0xFFF2E1BB),

    tertiary = Color(0xFFADCFAE),
    onTertiary = Color(0xFF2F4D34),
    tertiaryContainer = Color(0xFF47664A),
    onTertiaryContainer = Color(0xFFC8ECC9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1F1B13),
    onBackground = Color(0xFFF8F0E2),
    surface = Color(0xFF1F1B13),
    onSurface = Color(0xFFF8F0E2),
    surfaceVariant = Color(0xFF4C4639),
    onSurfaceVariant = Color(0xFFCFC6B4),
    outline = Color(0xFF999080),
    outlineVariant = Color(0xFF4C4639),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF8F0E2),
    inverseOnSurface = Color(0xFF343027),
    inversePrimary = GrindrYellow,
    surfaceDim = Color(0xFF141009),
    surfaceBright = Color(0xFF3C372B),
    surfaceContainerLowest = Color(0xFF0F0C06),
    surfaceContainerLow = Color(0xFF1A160F),
    surfaceContainer = Color(0xFF1F1B13),
    surfaceContainerHigh = Color(0xFF29251C),
    surfaceContainerHighest = Color(0xFF342F25)
)

@Composable
fun GrindrPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}