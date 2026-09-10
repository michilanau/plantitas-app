package org.mlanau.project.shared.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Botánico pop": warm paper, near-black ink and a lime accent, with saturated colour reserved for
 * what each care means. `primary` is the ink (main actions, selected chips), `primaryContainer` the
 * lime (the accent action and the active tab), `background` the paper and `surface` the cards.
 */

internal val PlantitasLightColors = lightColorScheme(
    primary = Color(0xFF221E19),
    onPrimary = Color(0xFFFCF3E6),
    primaryContainer = Color(0xFFC9E84B),
    onPrimaryContainer = Color(0xFF221E19),
    secondary = Color(0xFF17603C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1F0DC),
    onSecondaryContainer = Color(0xFF0F3F27),
    tertiary = Color(0xFFE4693B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBE4D8),
    onTertiaryContainer = Color(0xFF6B2A10),
    error = Color(0xFFC7304C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE0E5),
    onErrorContainer = Color(0xFF7A1A2D),
    background = Color(0xFFFCF3E6),
    onBackground = Color(0xFF221E19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF221E19),
    surfaceVariant = Color(0xFFF4E6D1),
    onSurfaceVariant = Color(0xFF7A6D5E),
    surfaceTint = Color(0xFFFFFFFF),
    outline = Color(0xFFE8D9C0),
    outlineVariant = Color(0xFFEFE3CF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF0E1),
    surfaceContainer = Color(0xFFF4E6D1),
    surfaceContainerHigh = Color(0xFFF4E6D1),
    surfaceContainerHighest = Color(0xFFEDDDC4),
    inverseSurface = Color(0xFF221E19),
    inverseOnSurface = Color(0xFFFCF3E6),
    inversePrimary = Color(0xFFC9E84B),
    scrim = Color(0xFF000000)
)

internal val PlantitasDarkColors = darkColorScheme(
    primary = Color(0xFFF6EEE1),
    onPrimary = Color(0xFF16140F),
    primaryContainer = Color(0xFFC9E84B),
    onPrimaryContainer = Color(0xFF221E19),
    secondary = Color(0xFF8FD6A5),
    onSecondary = Color(0xFF0B2A18),
    secondaryContainer = Color(0xFF1F3A29),
    onSecondaryContainer = Color(0xFFBFE8CB),
    tertiary = Color(0xFFF0916A),
    onTertiary = Color(0xFF3E1A08),
    tertiaryContainer = Color(0xFF3E2618),
    onTertiaryContainer = Color(0xFFFAD3C2),
    error = Color(0xFFF5849A),
    onError = Color(0xFF3E0A16),
    errorContainer = Color(0xFF40202A),
    onErrorContainer = Color(0xFFFBD5DC),
    background = Color(0xFF16140F),
    onBackground = Color(0xFFF6EEE1),
    surface = Color(0xFF262218),
    onSurface = Color(0xFFF6EEE1),
    surfaceVariant = Color(0xFF221F18),
    onSurfaceVariant = Color(0xFFA99C89),
    surfaceTint = Color(0xFF262218),
    outline = Color(0xFF3B342A),
    outlineVariant = Color(0xFF2E2921),
    surfaceContainerLowest = Color(0xFF12100C),
    surfaceContainerLow = Color(0xFF1B1914),
    surfaceContainer = Color(0xFF221F18),
    surfaceContainerHigh = Color(0xFF2B271F),
    surfaceContainerHighest = Color(0xFF332E25),
    inverseSurface = Color(0xFFF6EEE1),
    inverseOnSurface = Color(0xFF16140F),
    inversePrimary = Color(0xFF17603C),
    scrim = Color(0xFF000000)
)

internal val PlantitasLightCareColors = CareColors(
    water = Color(0xFF2F5FE0),
    waterContainer = Color(0xFFE3EAFF),
    fertilize = Color(0xFF5A8C1B),
    fertilizeContainer = Color(0xFFEDF6D6),
    repot = Color(0xFFCE5420),
    repotContainer = Color(0xFFFBE4D8),
    overdue = Color(0xFFC7304C),
    overdueContainer = Color(0xFFFBE0E5),
    done = Color(0xFF17603C),
    onAccent = Color(0xFFFFFFFF)
)

internal val PlantitasDarkCareColors = CareColors(
    water = Color(0xFF9FB4FF),
    waterContainer = Color(0xFF23304E),
    fertilize = Color(0xFFB4D65F),
    fertilizeContainer = Color(0xFF2C3818),
    repot = Color(0xFFF0916A),
    repotContainer = Color(0xFF3E2618),
    overdue = Color(0xFFF5849A),
    overdueContainer = Color(0xFF40202A),
    done = Color(0xFF8FD6A5),
    onAccent = Color(0xFF16140F)
)

/** The grounds a plant without a photo gets, picked by its id. The same in both themes. */
val PlantTints = listOf(
    Color(0xFFC9E84B),
    Color(0xFFF6C453),
    Color(0xFF7FD1AE),
    Color(0xFFF19A7E),
    Color(0xFFA8C7FF)
)

val PlantTintContent = Color(0xFF221E19)
