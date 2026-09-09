package org.mlanau.project.plant.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import org.mlanau.project.plant.domain.model.CareType

/**
 * The single source of the icon for each kind of care. Was copied verbatim into HomeScreen,
 * PlantDetailScreen and CalendarScreen.
 */
val CareType.icon: ImageVector
    get() = when (this) {
        CareType.WATER -> Icons.Default.WaterDrop
        CareType.FERTILIZE -> Icons.Default.Science
        CareType.REPOT -> Icons.Default.HomeRepairService
    }
