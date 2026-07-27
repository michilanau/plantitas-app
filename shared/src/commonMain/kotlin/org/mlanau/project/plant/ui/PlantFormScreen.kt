package org.mlanau.project.plant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PotSize
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantFormScreen(
    viewModel: HomeViewModel,
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

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            viewModel.resetSaveState()
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
            // ... resto del contenido ...
            // Name Field (Required)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("${stringResource(Res.string.home_plant_name)} *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.saveError != null,
                    enabled = !uiState.isSaving,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                if (uiState.saveError != null) {
                    Text(
                        text = stringResource(uiState.saveError!!),
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.onSavePlant(
                        id = initialPlant?.id,
                        name = name,
                        description = description,
                        location = location,
                        lightNeed = selectedLightNeed,
                        potSize = selectedPotSize
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
                            onBack()
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
