package org.mlanau.project.plant.presentation.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.care_action_fertilize
import plantitas_app.shared.generated.resources.care_action_repot
import plantitas_app.shared.generated.resources.care_action_water
import plantitas_app.shared.generated.resources.care_type_fertilize
import plantitas_app.shared.generated.resources.care_type_repot
import plantitas_app.shared.generated.resources.care_type_water
import plantitas_app.shared.generated.resources.light_high
import plantitas_app.shared.generated.resources.light_low
import plantitas_app.shared.generated.resources.light_medium
import plantitas_app.shared.generated.resources.pot_extra_large
import plantitas_app.shared.generated.resources.pot_large
import plantitas_app.shared.generated.resources.pot_medium
import plantitas_app.shared.generated.resources.pot_small

@Composable
fun getLightNeedString(need: LightNeed): String = when (need) {
    LightNeed.LOW -> stringResource(Res.string.light_low)
    LightNeed.MEDIUM -> stringResource(Res.string.light_medium)
    LightNeed.HIGH -> stringResource(Res.string.light_high)
}

@Composable
fun getPotSizeString(size: PotSize): String = when (size) {
    PotSize.SMALL -> stringResource(Res.string.pot_small)
    PotSize.MEDIUM -> stringResource(Res.string.pot_medium)
    PotSize.LARGE -> stringResource(Res.string.pot_large)
    PotSize.EXTRA_LARGE -> stringResource(Res.string.pot_extra_large)
}

/** The noun for a kind of care: "Riego". */
@Composable
fun getCareTypeString(type: CareType): String = when (type) {
    CareType.WATER -> stringResource(Res.string.care_type_water)
    CareType.FERTILIZE -> stringResource(Res.string.care_type_fertilize)
    CareType.REPOT -> stringResource(Res.string.care_type_repot)
}

/** The imperative for a kind of care, as it leads a plant's next-care pill: "Regar". */
@Composable
fun getCareActionString(type: CareType): String = when (type) {
    CareType.WATER -> stringResource(Res.string.care_action_water)
    CareType.FERTILIZE -> stringResource(Res.string.care_action_fertilize)
    CareType.REPOT -> stringResource(Res.string.care_action_repot)
}
