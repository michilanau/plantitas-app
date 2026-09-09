package org.mlanau.project.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalCareColors = staticCompositionLocalOf { PlantitasLightCareColors }

private val PlantitasShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PlantitasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PlantitasDarkColors else PlantitasLightColors
    val careColors = if (darkTheme) PlantitasDarkCareColors else PlantitasLightCareColors

    ApplySystemBarAppearance(darkTheme)

    CompositionLocalProvider(LocalCareColors provides careColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = PlantitasShapes,
            typography = plantitasTypography(),
            content = content
        )
    }
}
