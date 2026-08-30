package org.mlanau.project.plant.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.application.runCatchingDomainErrors
import org.mlanau.project.plant.domain.exception.InvalidRecurrenceException
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.PotSize
import plantitas_app.shared.generated.resources.*

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

@Composable
fun getCareTypeString(type: CareType): String = when (type) {
    CareType.WATER -> stringResource(Res.string.care_type_water)
    CareType.FERTILIZE -> stringResource(Res.string.care_type_fertilize)
    CareType.REPOT -> stringResource(Res.string.care_type_repot)
}

@Composable
fun CareRuleItem(
    rule: CareRule,
    onClick: () -> Unit,
    onTogglePaused: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (rule.details) {
                    is CareDetails.Water -> Icons.Default.WaterDrop
                    is CareDetails.Fertilize -> Icons.Default.Science
                    is CareDetails.Repot -> Icons.Default.HomeRepairService
                },
                contentDescription = null,
                tint = if (rule.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = getCareTypeString(rule.type), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (rule.active) {
                        stringResource(Res.string.care_recurrence_periodic, rule.everyDays)
                    } else {
                        stringResource(Res.string.care_rule_paused)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.active) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
            IconButton(onClick = onTogglePaused) {
                Icon(
                    imageVector = if (rule.active) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (rule.active) Res.string.care_rule_pause else Res.string.care_rule_resume
                    ),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Creates or edits a plant's rule for one kind of care.
 *
 * [takenTypes] are the types this plant already has a rule for. A plant has at most one rule per
 * type, so those are simply not offered, and editing an existing rule shows its type as fixed:
 * switching a rule's type is deleting one rule and creating another, not an edit.
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
    val selectableTypes = remember(takenTypes, initialRule) {
        CareType.entries.filter { it !in takenTypes || it == initialRule?.type }
    }
    if (selectableTypes.isEmpty()) {
        AllTypesTakenDialog(onDismiss)
        return
    }

    var type by remember { mutableStateOf(initialRule?.type ?: selectableTypes.first()) }
    var everyDays by remember { mutableStateOf(initialRule?.everyDays?.toString() ?: "7") }

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val now = Clock.System.now().toLocalDateTime(timeZone)

    val initialLocalDateTime = remember(initialRule) {
        initialRule?.startDate?.toLocalDateTime(timeZone) ?: now
    }

    var hour by remember(initialLocalDateTime) {
        mutableStateOf(initialLocalDateTime.hour.toString().padStart(2, '0'))
    }
    var minute by remember(initialLocalDateTime) {
        mutableStateOf(initialLocalDateTime.minute.toString().padStart(2, '0'))
    }

    var startDate by remember(initialLocalDateTime) { mutableStateOf(initialLocalDateTime.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var amountMl by remember { mutableStateOf((initialRule?.details as? CareDetails.Water)?.amountMl?.toString() ?: "") }
    var fertilizerName by remember { mutableStateOf((initialRule?.details as? CareDetails.Fertilize)?.fertilizerName ?: "") }
    var newPotSize by remember { mutableStateOf((initialRule?.details as? CareDetails.Repot)?.newPotSize ?: PotSize.MEDIUM) }
    var notificationsEnabled by remember { mutableStateOf(initialRule?.notificationsEnabled ?: true) }

    val strConfirm = stringResource(Res.string.common_confirm)
    val strCancel = stringResource(Res.string.common_cancel)
    val strAddRule = stringResource(Res.string.care_add_rule)
    val strEditRule = stringResource(Res.string.care_edit_rule)
    val strTypeLabel = stringResource(Res.string.care_type_label)
    val strStartDateLabel = stringResource(Res.string.care_start_date_label)
    val strTimeLabel = stringResource(Res.string.care_time_label)
    val strEveryDays = stringResource(Res.string.care_every_days_label)
    val strAmountMl = stringResource(Res.string.care_water_amount_label)
    val strEveryDaysError = stringResource(Res.string.care_every_days_error)
    val strAmountError = stringResource(Res.string.care_water_amount_error)
    val strFertilizerName = stringResource(Res.string.care_fertilizer_name_label)
    val strFertilizerDefault = stringResource(Res.string.care_fertilizer_default_name)
    val strRepotPotSize = stringResource(Res.string.care_repot_pot_size_label)
    val strAdd = stringResource(Res.string.home_button_add)

    // Recomputed on every recomposition from the current field values (Compose already
    // recomposes this scope on their changes), so validity — and the specific field to blame —
    // stay in sync with what's on screen instead of only surfacing at confirm time.
    val ruleResult = runCatchingDomainErrors {
        val selectedHour = hour.toIntOrNull()?.coerceIn(0, 23) ?: 10
        val selectedMinute = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val notificationTime = LocalTime(selectedHour, selectedMinute)
        val startInstant = LocalDateTime(startDate, notificationTime).toInstant(timeZone)
        val days = everyDays.toIntOrNull() ?: 0

        val details: CareDetails = when (type) {
            CareType.WATER -> CareDetails.Water.create(amountMl = amountMl.toIntOrNull())
            CareType.FERTILIZE -> CareDetails.Fertilize.create(fertilizerName = fertilizerName.ifBlank { strFertilizerDefault })
            CareType.REPOT -> CareDetails.Repot.create(newPotSize = newPotSize)
        }

        // Editing preserves the rule's id and its paused state; the type can't change, so the
        // existing rule is always the one this dialog is editing.
        initialRule?.withSchedule(days, startInstant, notificationTime, notificationsEnabled, details)
            ?: CareRule.create(
                plantId = plantId,
                everyDays = days,
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
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour.toIntOrNull() ?: 10,
            initialMinute = minute.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour.toString().padStart(2, '0')
                    minute = timePickerState.minute.toString().padStart(2, '0')
                    showTimePicker = false
                }) { Text(strConfirm) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) strAddRule else strEditRule) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strTypeLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableTypes.forEach { selectable ->
                        FilterChip(
                            selected = type == selectable,
                            onClick = { type = selectable },
                            enabled = initialRule == null,
                            label = { Text(getCareTypeString(selectable)) }
                        )
                    }
                }

                ClickableField(
                    value = startDate.toString(),
                    label = strStartDateLabel,
                    icon = Icons.Default.DateRange,
                    onClick = { showDatePicker = true }
                )

                ClickableField(
                    value = "$hour:$minute",
                    label = strTimeLabel,
                    icon = Icons.Default.Schedule,
                    onClick = { showTimePicker = true }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(Res.string.care_notifications_enabled), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = everyDays,
                    onValueChange = { everyDays = it.filter { c -> c.isDigit() } },
                    label = { Text(strEveryDays) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    isError = ruleError is InvalidRecurrenceException,
                    supportingText = if (ruleError is InvalidRecurrenceException) {
                        { Text(strEveryDaysError) }
                    } else null
                )

                when (type) {
                    CareType.WATER -> {
                        OutlinedTextField(
                            value = amountMl,
                            onValueChange = { amountMl = it.filter { c -> c.isDigit() } },
                            label = { Text(strAmountMl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            isError = ruleError is NonPositiveAmountException,
                            supportingText = if (ruleError is NonPositiveAmountException) {
                                { Text(strAmountError) }
                            } else null
                        )
                    }
                    CareType.FERTILIZE -> {
                        OutlinedTextField(
                            value = fertilizerName,
                            onValueChange = { fertilizerName = it },
                            label = { Text(strFertilizerName) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                    }
                    CareType.REPOT -> {
                        Text(strRepotPotSize, style = MaterialTheme.typography.labelLarge)
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { ruleResult.getOrNull()?.let(onConfirm) },
                enabled = ruleResult.isSuccess
            ) {
                Text(strAdd)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strCancel)
            }
        }
    )
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

@Composable
internal fun ClickableField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        enabled = false,
        colors = TextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIndicatorColor = MaterialTheme.colorScheme.outline
        ),
        shape = MaterialTheme.shapes.medium,
        trailingIcon = { Icon(icon, contentDescription = null) }
    )
}
