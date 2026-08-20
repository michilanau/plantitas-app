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
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.model.RecurrenceRule
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
fun CareRuleItem(
    rule: CareRule,
    onClick: () -> Unit,
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (rule.details) {
                        is CareDetails.Water -> stringResource(Res.string.care_type_water)
                        is CareDetails.Fertilize -> stringResource(Res.string.care_type_fertilize)
                        is CareDetails.Repot -> stringResource(Res.string.care_type_repot)
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when (val rec = rule.recurrence) {
                        is RecurrenceRule.Once -> stringResource(Res.string.care_recurrence_once)
                        is RecurrenceRule.Periodic -> stringResource(Res.string.care_recurrence_periodic, rec.everyDays)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CareRuleDialog(
    plantId: PlantId,
    initialRule: CareRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (CareRule) -> Unit
) {
    // Use a typed enum instead of raw String as discriminator
    var type by remember {
        mutableStateOf(
            when (initialRule?.details) {
                is CareDetails.Water -> CareType.WATER
                is CareDetails.Fertilize -> CareType.FERTILIZE
                is CareDetails.Repot -> CareType.REPOT
                null -> CareType.WATER
            }
        )
    }
    var recurrenceType by remember {
        mutableStateOf(
            if (initialRule?.recurrence is RecurrenceRule.Once) RecurrenceRule.Once::class else RecurrenceRule.Periodic::class
        )
    }
    val isOnce = recurrenceType == RecurrenceRule.Once::class

    var everyDays by remember {
        mutableStateOf(
            (initialRule?.recurrence as? RecurrenceRule.Periodic)?.everyDays?.toString() ?: "7"
        )
    }

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

    // Specific fields
    var amountMl by remember { mutableStateOf((initialRule?.details as? CareDetails.Water)?.amountMl?.toString() ?: "") }
    var fertilizerName by remember { mutableStateOf((initialRule?.details as? CareDetails.Fertilize)?.fertilizerName ?: "") }
    var newPotSize by remember { mutableStateOf((initialRule?.details as? CareDetails.Repot)?.newPotSize ?: PotSize.MEDIUM) }
    var notificationsEnabled by remember { mutableStateOf(initialRule?.notificationsEnabled ?: true) }

    // Resource strings resolved at composition time
    val strConfirm = stringResource(Res.string.common_confirm)
    val strCancel = stringResource(Res.string.common_cancel)
    val strAddRule = stringResource(Res.string.care_add_rule)
    val strEditRule = stringResource(Res.string.care_edit_rule)
    val strTypeLabel = stringResource(Res.string.care_type_label)
    val strStartDateLabel = stringResource(Res.string.care_start_date_label)
    val strTaskDateLabel = stringResource(Res.string.care_task_date_label)
    val strTimeLabel = stringResource(Res.string.care_time_label)
    val strRecurrenceLabel = stringResource(Res.string.care_recurrence_label)
    val strPeriodic = stringResource(Res.string.care_recurrence_periodic_label)
    val strEveryDays = stringResource(Res.string.care_every_days_label)
    val strAmountMl = stringResource(Res.string.care_water_amount_label)
    val strFertilizerName = stringResource(Res.string.care_fertilizer_name_label)
    val strFertilizerDefault = stringResource(Res.string.care_fertilizer_default_name)
    val strRepotPotSize = stringResource(Res.string.care_repot_pot_size_label)
    val strWater = stringResource(Res.string.care_type_water)
    val strFertilize = stringResource(Res.string.care_type_fertilize)
    val strRepot = stringResource(Res.string.care_type_repot)
    val strOnce = stringResource(Res.string.care_recurrence_once)
    val strAdd = stringResource(Res.string.home_button_add)

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
                // Type Selector
                Text(strTypeLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == CareType.WATER, onClick = { type = CareType.WATER }, label = { Text(strWater) })
                    FilterChip(selected = type == CareType.FERTILIZE, onClick = { type = CareType.FERTILIZE }, label = { Text(strFertilize) })
                    FilterChip(selected = type == CareType.REPOT, onClick = { type = CareType.REPOT }, label = { Text(strRepot) })
                }

                // Date / Time fields
                ClickableField(
                    value = startDate.toString(),
                    label = if (isOnce) strTaskDateLabel else strStartDateLabel,
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

                // Recurrence Selector
                Text(strRecurrenceLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isOnce,
                        onClick = { recurrenceType = RecurrenceRule.Once::class },
                        label = { Text(strOnce) }
                    )
                    FilterChip(
                        selected = !isOnce,
                        onClick = { recurrenceType = RecurrenceRule.Periodic::class },
                        label = { Text(strPeriodic) }
                    )
                }

                if (!isOnce) {
                    OutlinedTextField(
                        value = everyDays,
                        onValueChange = { everyDays = it.filter { c -> c.isDigit() } },
                        label = { Text(strEveryDays) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Specific fields based on type
                when (type) {
                    CareType.WATER -> {
                        OutlinedTextField(
                            value = amountMl,
                            onValueChange = { amountMl = it.filter { c -> c.isDigit() } },
                            label = { Text(strAmountMl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
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
                onClick = {
                    val recurrence = if (isOnce) RecurrenceRule.Once else RecurrenceRule.Periodic(everyDays.toIntOrNull() ?: 7)
                    val selectedHour = hour.toIntOrNull()?.coerceIn(0, 23) ?: 10
                    val selectedMinute = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val notificationTime = LocalTime(selectedHour, selectedMinute)
                    val startInstant = LocalDateTime(startDate, notificationTime).toInstant(timeZone)

                    val details: CareDetails = when (type) {
                        CareType.WATER -> CareDetails.Water(amountMl = amountMl.toIntOrNull())
                        CareType.FERTILIZE -> CareDetails.Fertilize(fertilizerName = fertilizerName.ifBlank { strFertilizerDefault })
                        CareType.REPOT -> CareDetails.Repot(newPotSize = newPotSize)
                    }
                    val rule = CareRule(
                        id = initialRule?.id,
                        plantId = plantId,
                        recurrence = recurrence,
                        startDate = startInstant,
                        // Neither is editable from this dialog, but both must survive an edit —
                        // without this, saving any change to an existing rule would silently wipe
                        // its end date and un-dismiss whatever overdue range the user had cleared.
                        endDate = initialRule?.endDate,
                        dismissedBefore = initialRule?.dismissedBefore,
                        notificationTime = notificationTime,
                        notificationsEnabled = notificationsEnabled,
                        active = initialRule?.active ?: true,
                        details = details
                    )
                    onConfirm(rule)
                }
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
