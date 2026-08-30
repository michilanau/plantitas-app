package org.mlanau.project.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class CareColors(
    val water: Color,
    val fertilize: Color,
    val repot: Color,
    val overdue: Color,
    val done: Color
)

val LightCareColors = CareColors(
    water = Color(0xFF1976D2),
    fertilize = Color(0xFF388E3C),
    repot = Color(0xFF795548),
    overdue = Color(0xFFB00020),
    done = Color(0xFF4CAF50)
)

val DarkCareColors = CareColors(
    water = Color(0xFF64B5F6),
    fertilize = Color(0xFF81C784),
    repot = Color(0xFFD7CCC8),
    overdue = Color(0xFFEF9A9A),
    done = Color(0xFFA5D6A7)
)

val LocalCareColors = staticCompositionLocalOf { LightCareColors }

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF003300),
    secondary = Color(0xFF558B2F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDC8),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFF795548),
    onTertiary = Color.White,
    error = Color(0xFFB00020),
    background = Color(0xFFF1F8E9),
    surface = Color.White,
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFAED581),
    onSecondary = Color(0xFF1B5E20),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9)
)

private val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun PlantitasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val careColors = if (darkTheme) DarkCareColors else LightCareColors

    CompositionLocalProvider(
        LocalCareColors provides careColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes,
            content = content
        )
    }
}
