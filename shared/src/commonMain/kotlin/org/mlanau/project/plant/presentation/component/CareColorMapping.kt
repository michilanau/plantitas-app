package org.mlanau.project.plant.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.shared.ui.theme.LocalCareColors

@Composable
fun careColor(type: CareType): Color {
    val careColors = LocalCareColors.current
    return when (type) {
        CareType.WATER -> careColors.water
        CareType.FERTILIZE -> careColors.fertilize
        CareType.REPOT -> careColors.repot
    }
}

@Composable
fun careContainerColor(type: CareType): Color {
    val careColors = LocalCareColors.current
    return when (type) {
        CareType.WATER -> careColors.waterContainer
        CareType.FERTILIZE -> careColors.fertilizeContainer
        CareType.REPOT -> careColors.repotContainer
    }
}

@Composable
fun overdueColor(): Color = LocalCareColors.current.overdue

@Composable
fun overdueContainerColor(): Color = LocalCareColors.current.overdueContainer

@Composable
fun doneColor(): Color = LocalCareColors.current.done

@Composable
fun onCareAccentColor(): Color = LocalCareColors.current.onAccent
