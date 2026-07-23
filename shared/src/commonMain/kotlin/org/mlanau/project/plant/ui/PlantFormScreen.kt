package org.mlanau.project.plant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PotSize
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
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

    var lightMenuExpanded by remember { mutableStateOf(false) }
    var potMenuExpanded by remember { mutableStateOf(false) }
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
                        else "Editar Planta"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                actions = {
                    if (initialPlant != null) {
                        IconButton(onClick = { isDeleteDialogOpen = true }) {
                            Text("🗑️")
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
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.home_plant_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.saveError != null,
                enabled = !uiState.isSaving
            )

            if (uiState.saveError != null) {
                Text(
                    text = stringResource(uiState.saveError!!),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(Res.string.home_plant_description)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            TextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(Res.string.home_plant_location)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            // Light Need Selector
            Box {
                OutlinedTextField(
                    value = selectedLightNeed?.let { getLightNeedString(it) } ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(Res.string.home_plant_light)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { lightMenuExpanded = true }) {
                            Text("▼")
                        }
                    }
                )
                DropdownMenu(
                    expanded = lightMenuExpanded,
                    onDismissRequest = { lightMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    LightNeed.entries.forEach { need ->
                        DropdownMenuItem(
                            text = { Text(getLightNeedString(need)) },
                            onClick = {
                                selectedLightNeed = need
                                lightMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Pot Size Selector
            Box {
                OutlinedTextField(
                    value = selectedPotSize?.let { getPotSizeString(it) } ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(Res.string.home_plant_pot)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { potMenuExpanded = true }) {
                            Text("▼")
                        }
                    }
                )
                DropdownMenu(
                    expanded = potMenuExpanded,
                    onDismissRequest = { potMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    PotSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(getPotSizeString(size)) },
                            onClick = {
                                selectedPotSize = size
                                potMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && name.isNotBlank()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (initialPlant == null) stringResource(Res.string.home_button_add) else "Guardar")
                }
            }
        }

        if (isDeleteDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDeleteDialogOpen = false },
                title = { Text("Eliminar planta") },
                text = { Text("¿Estás seguro de que quieres eliminar '${initialPlant?.name}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            initialPlant?.id?.let { viewModel.onDeletePlant(it) }
                            isDeleteDialogOpen = false
                            onBack()
                        }
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDeleteDialogOpen = false }) {
                        Text("Cancelar")
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
