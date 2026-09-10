package org.mlanau.project.plant.presentation.component

import org.jetbrains.compose.resources.DrawableResource
import org.mlanau.project.plant.domain.model.CareType
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.ic_care_fertilize
import plantitas_app.shared.generated.resources.ic_care_repot
import plantitas_app.shared.generated.resources.ic_care_water

/** The single source of the icon for each kind of care. */
val CareType.icon: DrawableResource
    get() = when (this) {
        CareType.WATER -> Res.drawable.ic_care_water
        CareType.FERTILIZE -> Res.drawable.ic_care_fertilize
        CareType.REPOT -> Res.drawable.ic_care_repot
    }
