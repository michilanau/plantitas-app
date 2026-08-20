package org.mlanau.project.plant.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.exceptions.EmptyPlantNameException
import org.mlanau.project.plant.domain.exceptions.FutureCareLogException
import org.mlanau.project.plant.domain.exceptions.InvalidCareRuleDateRangeException
import org.mlanau.project.plant.domain.exceptions.InvalidRecurrenceException
import org.mlanau.project.plant.domain.exceptions.PlantNotFoundException
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.care_log_future_error
import plantitas_app.shared.generated.resources.error_empty_name
import plantitas_app.shared.generated.resources.error_load_plants
import plantitas_app.shared.generated.resources.error_unknown

/**
 * A ViewModel-level error the UI can render, decoupled from Compose's [org.jetbrains.compose.resources.StringResource]
 * so `uiState` doesn't depend on a UI framework type. Each `@Composable` screen turns this into
 * localized text via [localizedMessage] at the point it's actually displayed.
 */
sealed interface UiError {
    data object EmptyPlantName : UiError
    data object PlantNotFound : UiError
    data object InvalidCareRuleDateRange : UiError
    data object InvalidRecurrence : UiError
    data object LoadPlantsFailed : UiError
    data object FutureCareLog : UiError
    data object Unknown : UiError
}

fun Throwable.toUiError(): UiError = when (this) {
    is EmptyPlantNameException -> UiError.EmptyPlantName
    is PlantNotFoundException -> UiError.PlantNotFound
    is InvalidCareRuleDateRangeException -> UiError.InvalidCareRuleDateRange
    is InvalidRecurrenceException -> UiError.InvalidRecurrence
    is FutureCareLogException -> UiError.FutureCareLog
    else -> UiError.Unknown
}

@Composable
fun UiError.localizedMessage(): String = when (this) {
    UiError.EmptyPlantName -> stringResource(Res.string.error_empty_name)
    UiError.LoadPlantsFailed -> stringResource(Res.string.error_load_plants)
    UiError.FutureCareLog -> stringResource(Res.string.care_log_future_error)
    // These validation failures don't have dedicated copy yet (the UI never surfaced them
    // distinctly before this refactor either); they fall back to the generic message rather than
    // inventing product copy as part of an architecture change.
    UiError.PlantNotFound,
    UiError.InvalidCareRuleDateRange,
    UiError.InvalidRecurrence,
    UiError.Unknown -> stringResource(Res.string.error_unknown)
}
