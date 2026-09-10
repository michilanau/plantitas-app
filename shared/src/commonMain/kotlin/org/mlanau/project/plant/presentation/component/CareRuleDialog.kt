package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.notification.presentation.NotificationPermissionStatus
import org.mlanau.project.notification.presentation.rememberNotificationPermissions
import org.mlanau.project.plant.application.runCatchingDomainErrors
import org.mlanau.project.plant.domain.exception.InvalidRecurrenceException
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppBottomSheet
import org.mlanau.project.shared.ui.component.AppButton
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.AppChip
import org.mlanau.project.shared.ui.component.AppPickerField
import org.mlanau.project.shared.ui.component.AppSwitch
import org.mlanau.project.shared.ui.component.AppTextField
import org.mlanau.project.shared.ui.component.FieldLabel
import org.mlanau.project.shared.ui.component.RoundIconButton
import plantitas_app.shared.generated.resources.*

/**
 * Creates or edits a plant's rule for one kind of care.
 *
 * [takenTypes] are the types this plant already has a rule for. A plant has at most one rule per
 * type, so those are simply not offered, and editing an existing rule shows its type as fixed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CareRuleDialog(
    plantId: PlantId?,
    takenTypes: Set<CareType>,
    initialRule: CareRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (CareRule) -> Unit
) {
    val notificationPermissions = rememberNotificationPermissions()
    val dateFormatter = LocalDateFormatter.current

    val selectableTypes = remember(takenTypes, initialRule) {
        CareType.entries.filter { it !in takenTypes || it == initialRule?.type }
    }
    if (selectableTypes.isEmpty()) {
        AllTypesTakenDialog(onDismiss)
        return
    }

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val initialLocalDateTime = remember(initialRule) {
        initialRule?.startDate?.toLocalDateTime(timeZone) ?: Clock.System.now().toLocalDateTime(timeZone)
    }

    var type by remember { mutableStateOf(initialRule?.type ?: selectableTypes.first()) }
    var everyDays by remember { mutableStateOf(initialRule?.everyDays ?: 7) }
    var startDate by remember(initialLocalDateTime) { mutableStateOf(initialLocalDateTime.date) }
    var notificationTime by remember(initialLocalDateTime) {
        mutableStateOf(initialRule?.notificationTime ?: LocalTime(initialLocalDateTime.hour, initialLocalDateTime.minute))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var amountMl by remember { mutableStateOf((initialRule?.details as? CareDetails.Water)?.amountMl?.toString() ?: "") }
    var fertilizerName by remember { mutableStateOf((initialRule?.details as? CareDetails.Fertilize)?.fertilizerName ?: "") }
    var newPotSize by remember { mutableStateOf((initialRule?.details as? CareDetails.Repot)?.newPotSize ?: PotSize.MEDIUM) }
    var notificationsEnabled by remember { mutableStateOf(initialRule?.notificationsEnabled ?: true) }

    val strConfirm = stringResource(Res.string.common_confirm)
    val strCancel = stringResource(Res.string.common_cancel)
    val strFertilizerDefault = stringResource(Res.string.care_fertilizer_default_name)

    val ruleResult = runCatchingDomainErrors {
        val startInstant = LocalDateTime(startDate, notificationTime).toInstant(timeZone)
        val details: CareDetails = when (type) {
            CareType.WATER -> CareDetails.Water.create(amountMl = amountMl.toIntOrNull())
            CareType.FERTILIZE -> CareDetails.Fertilize.create(fertilizerName = fertilizerName.ifBlank { strFertilizerDefault })
            CareType.REPOT -> CareDetails.Repot.create(newPotSize = newPotSize)
        }

        initialRule?.withSchedule(everyDays, startInstant, notificationTime, notificationsEnabled, details)
            ?: CareRule.create(
                plantId = plantId,
                everyDays = everyDays,
                startDate = startInstant,
                notificationTime = notificationTime,
                notificationsEnabled = notificationsEnabled,
                details = details
            )
    }
    val ruleError = ruleResult.exceptionOrNull()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                    }
                    showDatePicker = false
                }) { Text(strConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strCancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notificationTime.hour,
            initialMinute = notificationTime.minute,
            is24Hour = dateFormatter.uses24HourClock
        )
        var timeDisplayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    notificationTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(strConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(strCancel) }
            },
            title = { TimePickerDialogDefaults.Title(displayMode = timeDisplayMode) },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        timeDisplayMode = if (timeDisplayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = timeDisplayMode
                )
            }
        ) {
            if (timeDisplayMode == TimePickerDisplayMode.Picker) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }
        }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(if (initialRule == null) Res.string.care_add_rule else Res.string.care_edit_rule)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column {
                FieldLabel(stringResource(Res.string.care_type_label))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectableTypes.forEach { selectable ->
                        AppChip(
                            label = getCareTypeString(selectable),
                            selected = type == selectable,
                            onClick = { type = selectable },
                            enabled = initialRule == null || selectable == type,
                            icon = selectable.icon
                        )
                    }
                }
            }

            Column {
                FieldLabel(stringResource(Res.string.care_every_days_label))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoundIconButton(
                        icon = Res.drawable.ic_minus,
                        contentDescription = null,
                        onClick = { everyDays = (everyDays - 1).coerceAtLeast(1) },
                        enabled = everyDays > 1
                    )
                    Text(
                        text = everyDays.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 64.dp)
                    )
                    RoundIconButton(
                        icon = Res.drawable.ic_plus,
                        contentDescription = null,
                        onClick = { everyDays += 1 }
                    )
                    Text(
                        text = stringResource(Res.string.care_days_unit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (ruleError is InvalidRecurrenceException) {
                    Text(
                        text = stringResource(Res.string.care_every_days_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            AppPickerField(
                label = stringResource(Res.string.care_start_date_label),
                value = dateFormatter.formatDate(startDate),
                icon = Res.drawable.ic_calendar,
                onClick = { showDatePicker = true }
            )

            AppPickerField(
                label = stringResource(Res.string.care_time_label),
                value = dateFormatter.formatTime(notificationTime),
                icon = Res.drawable.ic_clock,
                onClick = { showTimePicker = true }
            )

            AppCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.care_notifications_enabled),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        AppSwitch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                if (enabled && notificationPermissions.status != NotificationPermissionStatus.GRANTED) {
                                    notificationPermissions.request()
                                }
                            }
                        )
                    }
                    if (notificationsEnabled && notificationPermissions.status == NotificationPermissionStatus.DENIED) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(Res.string.care_notifications_permission_denied),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { notificationPermissions.openAppNotificationSettings() }) {
                                Text(
                                    text = stringResource(Res.string.care_notifications_open_settings),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            when (type) {
                CareType.WATER -> AppTextField(
                    value = amountMl,
                    onValueChange = { amountMl = it.filter { c -> c.isDigit() } },
                    label = stringResource(Res.string.care_water_amount_label),
                    errorText = if (ruleError is NonPositiveAmountException) {
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

            AppButton(
                text = stringResource(if (initialRule == null) Res.string.home_button_add else Res.string.common_save),
                onClick = { ruleResult.getOrNull()?.let(onConfirm) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                enabled = ruleResult.isSuccess
            )
        }
    }
}

@Composable
private fun AllTypesTakenDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.care_add_rule)) },
        text = { Text(stringResource(Res.string.care_rule_type_taken)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_confirm)) }
        }
    )
}
