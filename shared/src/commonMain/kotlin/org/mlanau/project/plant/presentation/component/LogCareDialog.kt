package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.application.runCatchingDomainErrors
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppBottomSheet
import org.mlanau.project.shared.ui.component.AppButton
import org.mlanau.project.shared.ui.component.AppChip
import org.mlanau.project.shared.ui.component.AppPickerField
import org.mlanau.project.shared.ui.component.AppTextField
import org.mlanau.project.shared.ui.component.FieldLabel
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogCareDialog(
    preselectedType: CareType? = null,
    onDismiss: () -> Unit,
    onConfirm: (details: CareDetails, performedAt: Instant, note: String?) -> Unit,
    clock: Clock = Clock.System
) {
    var type by remember { mutableStateOf(preselectedType ?: CareType.WATER) }

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { clock.now().toLocalDateTime(timeZone).date }
    val dateFormatter = LocalDateFormatter.current

    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }

    var amountMl by remember { mutableStateOf("") }
    var fertilizerName by remember { mutableStateOf("") }
    var newPotSize by remember { mutableStateOf(PotSize.MEDIUM) }
    var note by remember { mutableStateOf("") }

    val strConfirm = stringResource(Res.string.common_confirm)
    val strFertilizerDefault = stringResource(Res.string.care_fertilizer_default_name)

    val detailsResult = runCatchingDomainErrors {
        when (type) {
            CareType.WATER -> CareDetails.Water.create(amountMl = amountMl.toIntOrNull())
            CareType.FERTILIZE -> CareDetails.Fertilize.create(fertilizerName = fertilizerName.ifBlank { strFertilizerDefault })
            CareType.REPOT -> CareDetails.Repot.create(newPotSize = newPotSize)
        }
    }
    val detailsError = detailsResult.exceptionOrNull()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date
                    return date <= today
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                    }
                    showDatePicker = false
                }) { Text(strConfirm) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AppBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.care_log_dialog_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (preselectedType == null) {
                Column {
                    FieldLabel(stringResource(Res.string.care_type_label))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CareType.entries.forEach { entry ->
                            AppChip(
                                label = getCareTypeString(entry),
                                selected = type == entry,
                                onClick = { type = entry },
                                icon = entry.icon
                            )
                        }
                    }
                }
            }

            AppPickerField(
                label = stringResource(Res.string.care_log_date_label),
                value = dateFormatter.formatDate(selectedDate),
                icon = Res.drawable.ic_calendar,
                onClick = { showDatePicker = true }
            )

            when (type) {
                CareType.WATER -> AppTextField(
                    value = amountMl,
                    onValueChange = { amountMl = it.filter { c -> c.isDigit() } },
                    label = stringResource(Res.string.care_water_amount_label),
                    errorText = if (detailsError is NonPositiveAmountException) {
                        stringResource(Res.string.care_water_amount_error)
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                CareType.FERTILIZE -> AppTextField(
                    value = fertilizerName,
                    onValueChange = { fertilizerName = it },
                    label = stringResource(Res.string.care_fertilizer_name_label),
                    placeholder = strFertilizerDefault,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                CareType.REPOT -> Column {
                    FieldLabel(stringResource(Res.string.care_repot_pot_size_label))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PotSize.entries.forEach { size ->
                            AppChip(
                                label = getPotSizeString(size),
                                selected = newPotSize == size,
                                onClick = { newPotSize = size }
                            )
                        }
                    }
                }
            }

            AppTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(Res.string.care_log_note_label),
                singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            AppButton(
                text = strConfirm,
                onClick = {
                    val details = detailsResult.getOrNull() ?: return@AppButton
                    val performedAt = if (selectedDate == today) {
                        clock.now()
                    } else {
                        selectedDate.atTime(12, 0).toInstant(timeZone)
                    }
                    onConfirm(details, performedAt, note.ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                enabled = detailsResult.isSuccess
            )
        }
    }
}
