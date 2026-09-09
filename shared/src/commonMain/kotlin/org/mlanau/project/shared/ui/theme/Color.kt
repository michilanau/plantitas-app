package org.mlanau.project.shared.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Orgánico editorial": warm paper, an olive primary and a clay/terracotta secondary. The dark
 * scheme is the same idea in reverse — a warm charcoal rather than a neutral black — so switching
 * theme keeps the app feeling like the same place.
 */

internal val PlantitasLightColors = lightColorScheme(
    primary = Color(0xFF47583A),
    onPrimary = Color(0xFFFAF6EE),
    primaryContainer = Color(0xFFE4EAD3),
    onPrimaryContainer = Color(0xFF2E3A24),
    secondary = Color(0xFF6E7D53),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9EDD9),
    onSecondaryContainer = Color(0xFF3A4429),
    tertiary = Color(0xFFA9703F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEFE0D2),
    onTertiaryContainer = Color(0xFF5E3B1E),
    error = Color(0xFFBF5B3E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF5DFD5),
    onErrorContainer = Color(0xFF7A2E19),
    background = Color(0xFFFAF6EE),
    onBackground = Color(0xFF2B2621),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF2B2621),
    surfaceVariant = Color(0xFFECE4D3),
    onSurfaceVariant = Color(0xFF6E6559),
    outline = Color(0xFFC9BEA4),
    outlineVariant = Color(0xFFE3D9C3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF7EF),
    surfaceContainer = Color(0xFFF4EEE1),
    surfaceContainerHigh = Color(0xFFEFE8D8),
    surfaceContainerHighest = Color(0xFFE9E1CD),
    inverseSurface = Color(0xFF34302A),
    inverseOnSurface = Color(0xFFF3EEE2),
    inversePrimary = Color(0xFFB9C79E),
    scrim = Color(0xFF000000)
)

internal val PlantitasDarkColors = darkColorScheme(
    primary = Color(0xFFAEC08E),
    onPrimary = Color(0xFF2A3520),
    primaryContainer = Color(0xFF3B4A2C),
    onPrimaryContainer = Color(0xFFD6E3BC),
    secondary = Color(0xFFC3CDA3),
    onSecondary = Color(0xFF313B22),
    secondaryContainer = Color(0xFF454F33),
    onSecondaryContainer = Color(0xFFDFE7C4),
    tertiary = Color(0xFFD9A574),
    onTertiary = Color(0xFF47280F),
    tertiaryContainer = Color(0xFF6A4423),
    onTertiaryContainer = Color(0xFFF3DCC5),
    error = Color(0xFFE8967B),
    onError = Color(0xFF5A2413),
    errorContainer = Color(0xFF7A3623),
    onErrorContainer = Color(0xFFF6D9CD),
    background = Color(0xFF1A1712),
    onBackground = Color(0xFFECE4D6),
    surface = Color(0xFF211D17),
    onSurface = Color(0xFFECE4D6),
    surfaceVariant = Color(0xFF3A342A),
    onSurfaceVariant = Color(0xFFC5BBA6),
    outline = Color(0xFF8C826C),
    outlineVariant = Color(0xFF46402F),
    surfaceContainerLowest = Color(0xFF12100C),
    surfaceContainerLow = Color(0xFF1C1913),
    surfaceContainer = Color(0xFF201C16),
    surfaceContainerHigh = Color(0xFF2A251E),
    surfaceContainerHighest = Color(0xFF352F26),
    inverseSurface = Color(0xFFECE4D6),
    inverseOnSurface = Color(0xFF322E27),
    inversePrimary = Color(0xFF47583A),
    scrim = Color(0xFF000000)
)

internal val PlantitasLightCareColors = CareColors(
    water = Color(0xFF3E7C93),
    fertilize = Color(0xFF6B8E4E),
    repot = Color(0xFFA9703F),
    overdue = Color(0xFFBF5B3E),
    done = Color(0xFF7B9E6B)
)

internal val PlantitasDarkCareColors = CareColors(
    water = Color(0xFF7FB4C6),
    fertilize = Color(0xFFA6C486),
    repot = Color(0xFFD9A574),
    overdue = Color(0xFFE8967B),
    done = Color(0xFFA7C495)
)
