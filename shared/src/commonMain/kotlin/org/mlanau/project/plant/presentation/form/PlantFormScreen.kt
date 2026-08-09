package org.mlanau.project.plant.presentation.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.font.FontWeight
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.WaterCareRule
import org.mlanau.project.plant.domain.model.FertilizeCareRule
import org.mlanau.project.plant.domain.model.RepotCareRule
import org.mlanau.project.plant.domain.model.RecurrenceRule
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantFormScreen(
    viewModel: PlantFormViewModel,
    initialPlant: Plant? = null,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(initialPlant?.name ?: "") }
    var description by remember { mutableStateOf(initialPlant?.description ?: "") }
    var location by remember { mutableStateOf(initialPlant?.location ?: "") }
    var selectedLightNeed by remember { mutableStateOf<LightNeed?>(initialPlant?.lightNeed) }
    var selectedPotSize by remember { mutableStateOf<PotSize?>(initialPlant?.potSize) }

    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    var isAddCareDialogOpen by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<CareRule?>(null) }

    LaunchedEffect(initialPlant) {
        initialPlant?.id?.let { viewModel.loadCareRules(it) }
    }

    LaunchedEffect(uiState.isSaveSuccess, uiState.isDeleteSuccess) {
        if (uiState.isSaveSuccess || uiState.isDeleteSuccess) {
            viewModel.resetState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (initialPlant == null) stringResource(Res.string.home_add_plant)
                        else stringResource(Res.string.plant_form_edit_title)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                actions = {
                    if (initialPlant != null) {
                        IconButton(onClick = { isDeleteDialogOpen = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.common_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name Field (Required)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("${stringResource(Res.string.home_plant_name)} *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.error != null,
                    enabled = !uiState.isSaving,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                if (uiState.error != null) {
                    Text(
                        text = stringResource(uiState.error!!),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("${stringResource(Res.string.home_plant_description)} ${stringResource(Res.string.common_optional)}") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            // Location Field
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("${stringResource(Res.string.home_plant_location)} ${stringResource(Res.string.common_optional)}") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            // Light Need Selector
            Text(
                text = stringResource(Res.string.home_plant_light),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LightNeed.entries.forEach { need ->
                    val isSelected = selectedLightNeed == need
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLightNeed = need },
                        label = { Text(getLightNeedString(need)) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pot Size Selector
            Text(
                text = stringResource(Res.string.home_plant_pot),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PotSize.entries.forEach { size ->
                    val isSelected = selectedPotSize == size
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPotSize = size },
                        label = { Text(getPotSizeString(size)) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            // Care Rules Section
            Text(
                text = stringResource(Res.string.care_rules_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            uiState.careRules.forEach { rule ->
                CareRuleItem(
                    rule = rule,
                    onClick = { ruleToEdit = rule },
                    onRemove = { viewModel.removeCareRule(rule) }
                )
            }

            OutlinedButton(
                onClick = { isAddCareDialogOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.care_add_rule))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.onSavePlant(
                        id = initialPlant?.id,
                        name = name,
                        description = description,
                        location = location,
                        lightNeed = selectedLightNeed,
                        potSize = selectedPotSize,
                        createdAt = initialPlant?.createdAt
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isSaving && name.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (initialPlant == null) stringResource(Res.string.home_button_add) 
                        else stringResource(Res.string.plant_form_save_changes),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        if (isDeleteDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDeleteDialogOpen = false },
                title = { Text(stringResource(Res.string.plant_form_delete_dialog_title)) },
                text = { Text(stringResource(Res.string.plant_form_delete_dialog_message, initialPlant?.name ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            initialPlant?.id?.let { viewModel.onDeletePlant(it) }
                            isDeleteDialogOpen = false
                        }
                    ) {
                        Text(stringResource(Res.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDeleteDialogOpen = false }) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                }
            )
        }

        if (isAddCareDialogOpen || ruleToEdit != null) {
            CareRuleDialog(
                plantId = initialPlant?.id ?: 0,
                initialRule = ruleToEdit,
                onDismiss = { 
                    isAddCareDialogOpen = false
                    ruleToEdit = null
                },
                onConfirm = { rule ->
                    if (ruleToEdit != null) {
                        viewModel.updateCareRuleInList(ruleToEdit!!, rule)
                    } else {
                        viewModel.addCareRule(rule)
                    }
                    isAddCareDialogOpen = false
                    ruleToEdit = null
                }
            )
        }
    }
}

@Composable
private fun getLightNeedString(need: LightNeed): String = when (need) {
    LightNeed.LOW -> stringResource(Res.string.light_low)
    LightNeed.MEDIUM -> stringResource(Res.string.light_medium)
    LightNeed.HIGH -> stringResource(Res.string.light_high)
}

@Composable
private fun getPotSizeString(size: PotSize): String = when (size) {
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
                imageVector = when (rule) {
                    is WaterCareRule -> Icons.Default.WaterDrop
                    is FertilizeCareRule -> Icons.Default.Science
                    is RepotCareRule -> Icons.Default.Yard
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (rule) {
                        is WaterCareRule -> stringResource(Res.string.care_type_water)
                        is FertilizeCareRule -> stringResource(Res.string.care_type_fertilize)
                        is RepotCareRule -> stringResource(Res.string.care_type_repot)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareRuleDialog(
    plantId: Int,
    initialRule: CareRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (CareRule) -> Unit
) {
    var type by remember { mutableStateOf(
        when (initialRule) {
            is WaterCareRule -> "WATER"
            is FertilizeCareRule -> "FERTILIZE"
            is RepotCareRule -> "REPOT"
            null -> "WATER"
        }
    ) }
    var recurrenceType by remember { mutableStateOf(
        if (initialRule?.recurrence is RecurrenceRule.Once) "ONCE" else "PERIODIC"
    ) }
    var everyDays by remember { mutableStateOf(
        (initialRule?.recurrence as? RecurrenceRule.Periodic)?.everyDays?.toString() ?: "7"
    ) }
    
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val now = Clock.System.now().toLocalDateTime(timeZone)
    
    val initialLocalDateTime = remember(initialRule) {
        initialRule?.startDate?.toLocalDateTime(timeZone) ?: now
    }
    
    var hour by remember(initialLocalDateTime) { mutableStateOf(
        initialLocalDateTime.hour.toString().padStart(2, '0')
    ) }
    var minute by remember(initialLocalDateTime) { mutableStateOf(
        initialLocalDateTime.minute.toString().padStart(2, '0')
    ) }

    var startDate by remember(initialLocalDateTime) { mutableStateOf(initialLocalDateTime.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Specific fields
    var amountMl by remember { mutableStateOf(
        (initialRule as? WaterCareRule)?.amountMl?.toString() ?: ""
    ) }
    var fertilizerName by remember { mutableStateOf(
        (initialRule as? FertilizeCareRule)?.fertilizerName ?: ""
    ) }
    var newPotSize by remember { mutableStateOf(
        (initialRule as? RepotCareRule)?.newPotSize ?: PotSize.MEDIUM
    ) }

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
                }) { Text("Confirmar") }
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
                }) { Text("Confirmar") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) stringResource(Res.string.care_add_rule) else "Editar cuidado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selector
                Text("Tipo de cuidado", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "WATER", onClick = { type = "WATER" }, label = { Text(stringResource(Res.string.care_type_water)) })
                    FilterChip(selected = type == "FERTILIZE", onClick = { type = "FERTILIZE" }, label = { Text(stringResource(Res.string.care_type_fertilize)) })
                    FilterChip(selected = type == "REPOT", onClick = { type = "REPOT" }, label = { Text(stringResource(Res.string.care_type_repot)) })
                }

                // Field Selectors
                ClickableField(
                    value = startDate.toString(),
                    label = if (recurrenceType == "ONCE") "Día de la tarea" else "Día de comienzo",
                    icon = Icons.Default.DateRange,
                    onClick = { showDatePicker = true }
                )

                ClickableField(
                    value = "$hour:$minute",
                    label = "Hora del recordatorio",
                    icon = Icons.Default.Schedule,
                    onClick = { showTimePicker = true }
                )

                // Recurrence Selector
                Text("Recurrencia", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = recurrenceType == "ONCE", onClick = { recurrenceType = "ONCE" }, label = { Text(stringResource(Res.string.care_recurrence_once)) })
                    FilterChip(selected = recurrenceType == "PERIODIC", onClick = { recurrenceType = "PERIODIC" }, label = { Text("Periódico") })
                }

                if (recurrenceType == "PERIODIC") {
                    OutlinedTextField(
                        value = everyDays,
                        onValueChange = { everyDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Cada cuántos días") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Specific fields based on type
                when (type) {
                    "WATER" -> {
                        OutlinedTextField(
                            value = amountMl,
                            onValueChange = { amountMl = it.filter { c -> c.isDigit() } },
                            label = { Text("Cantidad (ml)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                    "FERTILIZE" -> {
                        OutlinedTextField(
                            value = fertilizerName,
                            onValueChange = { fertilizerName = it },
                            label = { Text("Nombre del fertilizante") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                    "REPOT" -> {
                        Text("Nuevo tamaño de maceta", style = MaterialTheme.typography.labelLarge)
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
                    val recurrence = if (recurrenceType == "ONCE") RecurrenceRule.Once else RecurrenceRule.Periodic(everyDays.toIntOrNull() ?: 7)
                    
                    val selectedHour = hour.toIntOrNull()?.coerceIn(0, 23) ?: 10
                    val selectedMinute = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    
                    val startInstant = LocalDateTime(startDate, LocalTime(selectedHour, selectedMinute)).toInstant(timeZone)

                    val rule = when (type) {
                        "WATER" -> WaterCareRule(
                            id = initialRule?.id,
                            plantId = plantId,
                            recurrence = recurrence,
                            startDate = startInstant,
                            amountMl = amountMl.toIntOrNull(),
                            active = initialRule?.active ?: true
                        )
                        "FERTILIZE" -> FertilizeCareRule(
                            id = initialRule?.id,
                            plantId = plantId,
                            recurrence = recurrence,
                            startDate = startInstant,
                            fertilizerName = fertilizerName.ifBlank { "Abono" },
                            active = initialRule?.active ?: true
                        )
                        "REPOT" -> RepotCareRule(
                            id = initialRule?.id,
                            plantId = plantId,
                            recurrence = recurrence,
                            startDate = startInstant,
                            newPotSize = newPotSize,
                            active = initialRule?.active ?: true
                        )
                        else -> throw IllegalStateException()
                    }
                    onConfirm(rule)
                }
            ) {
                Text(stringResource(Res.string.home_button_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

@Composable
fun ClickableField(
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
