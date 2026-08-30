package org.mlanau.project.plant.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.exception.BlankFertilizerNameException
import org.mlanau.project.plant.domain.exception.EmptyPlantNameException
import org.mlanau.project.plant.domain.exception.FutureCareTaskException
import org.mlanau.project.plant.domain.exception.InvalidRecurrenceException
import org.mlanau.project.plant.domain.exception.TaskNotDueYetException
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException
import org.mlanau.project.plant.domain.exception.PlantNotFoundException
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.care_log_future_error
import plantitas_app.shared.generated.resources.care_task_not_due
import plantitas_app.shared.generated.resources.error_empty_name
import plantitas_app.shared.generated.resources.error_load_plants
import plantitas_app.shared.generated.resources.error_unknown

sealed interface UiError {
    data object EmptyPlantName : UiError
    data object PlantNotFound : UiError
    data object TaskNotDueYet : UiError
    data object InvalidRecurrence : UiError
    data object LoadPlantsFailed : UiError
    data object FutureCareLog : UiError
    data object BlankFertilizerName : UiError
    data object NonPositiveAmount : UiError
    data object Unknown : UiError
}

fun Throwable.toUiError(): UiError = when (this) {
    is EmptyPlantNameException -> UiError.EmptyPlantName
    is PlantNotFoundException -> UiError.PlantNotFound
    is TaskNotDueYetException -> UiError.TaskNotDueYet
    is InvalidRecurrenceException -> UiError.InvalidRecurrence
    is FutureCareTaskException -> UiError.FutureCareLog
    is BlankFertilizerNameException -> UiError.BlankFertilizerName
    is NonPositiveAmountException -> UiError.NonPositiveAmount
    else -> UiError.Unknown
}

@Composable
fun UiError.localizedMessage(): String = when (this) {
    UiError.EmptyPlantName -> stringResource(Res.string.error_empty_name)
    UiError.LoadPlantsFailed -> stringResource(Res.string.error_load_plants)
    UiError.FutureCareLog -> stringResource(Res.string.care_log_future_error)
    UiError.TaskNotDueYet -> stringResource(Res.string.care_task_not_due)
    // These validation failures don't have dedicated copy yet (the UI never surfaced them
    // distinctly before this refactor either); they fall back to the generic message rather than
    // inventing product copy as part of an architecture change.
    UiError.PlantNotFound,
    UiError.InvalidRecurrence,
    UiError.BlankFertilizerName,
    UiError.NonPositiveAmount,
    UiError.Unknown -> stringResource(Res.string.error_unknown)
}
