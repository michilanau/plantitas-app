package org.mlanau.project.plant.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.shared.ui.theme.LocalCareColors

/** Maps a care type to the color used to represent it across the calendar and detail screens.
 * Kept in presentation (not the theme) since it depends on the domain model. */
@Composable
fun careColor(type: CareType): Color {
    val careColors = LocalCareColors.current
    return when (type) {
        CareType.WATER -> careColors.water
        CareType.FERTILIZE -> careColors.fertilize
        CareType.REPOT -> careColors.repot
    }
}

/** The color for an overdue occurrence, regardless of its care type — see [CareColors.overdue]. */
@Composable
fun overdueColor(): Color = LocalCareColors.current.overdue
