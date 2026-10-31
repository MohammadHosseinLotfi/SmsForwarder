package com.example.smsforwarder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

private val KashiLightColorScheme = lightColorScheme(
    primary = Firoozeh600,
    onPrimary = Shab0,
    primaryContainer = Firoozeh50,
    onPrimaryContainer = Firoozeh700,
    secondary = Lajvard600,
    onSecondary = Shab0,
    secondaryContainer = Lajvard50,
    onSecondaryContainer = Lajvard700,
    tertiary = Zafaran600,
    onTertiary = Shab0,
    background = Shab50,
    onBackground = Shab950,
    surface = Shab0,
    onSurface = Shab950,
    surfaceVariant = Shab100,
    onSurfaceVariant = Shab600,
    outline = Shab200,
    error = Anar600,
    errorContainer = Anar50
)

private val KashiDarkColorScheme = darkColorScheme(
    primary = Firoozeh400,
    onPrimary = Shab1000,
    primaryContainer = Color(0xFF0D5551),
    onPrimaryContainer = Firoozeh300,
    secondary = Lajvard300,
    onSecondary = Shab1000,
    secondaryContainer = Shab900,
    onSecondaryContainer = Lajvard300,
    tertiary = Zafaran400,
    onTertiary = Shab1000,
    background = Shab1000,
    onBackground = Shab50,
    surface = Shab925,
    onSurface = Shab50,
    surfaceVariant = Shab900,
    onSurfaceVariant = Shab400,
    outline = Shab800,
    error = Anar400,
    errorContainer = Color(0xFF48070B)
)

@Composable
fun SmsForwarderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KashiDarkColorScheme
        else -> KashiLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        ProvideTextStyle(value = TextStyle(fontFamily = VazirmatnFontFamily)) {
            content()
        }
    }
}
