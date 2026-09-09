package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.application.runCatchingDomainErrors
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppBottomSheet
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
    val strCancel = stringResource(Res.string.common_cancel)
    val strFertilizerDefault = stringResource(Res.string.care_fertilizer_default_name)
    val strAmountError = stringResource(Res.string.care_water_amount_error)

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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (preselectedType == null) {
                Text(stringResource(Res.string.care_type_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareType.entries.forEach { entry ->
                        FilterChip(
                            selected = type == entry,
                            onClick = { type = entry },
                            label = { Text(getCareTypeString(entry)) }
                        )
                    }
                }
            }

            ClickableField(
                value = dateFormatter.formatDate(selectedDate),
                label = stringResource(Res.string.care_log_date_label),
                icon = Icons.Default.DateRange,
                onClick = { showDatePicker = true }
            )

            when (type) {
                CareType.WATER -> OutlinedTextField(
                    value = amountMl,
                    onValueChange = { amountMl = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(Res.string.care_water_amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    isError = detailsError is NonPositiveAmountException,
                    supportingText = if (detailsError is NonPositiveAmountException) {
                        { Text(strAmountError) }
                    } else null
                )
                CareType.FERTILIZE -> OutlinedTextField(
                    value = fertilizerName,
                    onValueChange = { fertilizerName = it },
                    label = { Text(stringResource(Res.string.care_fertilizer_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                CareType.REPOT -> {
                    Text(stringResource(Res.string.care_repot_pot_size_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PotSize.entries.forEach { size ->
                            FilterChip(
                                selected = newPotSize == size,
                                onClick = { newPotSize = size },
                                label = { Text(getPotSizeString(size)) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(Res.string.care_log_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(strCancel) }
                Button(
                    onClick = {
                        val details = detailsResult.getOrNull() ?: return@Button
                        val performedAt = if (selectedDate == today) {
                            clock.now()
                        } else {
                            selectedDate.atTime(12, 0).toInstant(timeZone)
                        }
                        onConfirm(details, performedAt, note.ifBlank { null })
                    },
                    enabled = detailsResult.isSuccess,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strConfirm)
                }
            }
        }
    }
}
