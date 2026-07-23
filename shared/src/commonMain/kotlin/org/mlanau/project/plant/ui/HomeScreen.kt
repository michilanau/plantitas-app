package org.mlanau.project.plant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.application.HomeUiState
import org.mlanau.project.plant.application.HomeViewModel
import org.mlanau.project.plant.domain.model.Plant
import plantitas_app.shared.generated.resources.*

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onSavePlant = { id, name, description -> viewModel.onSavePlant(id, name, description) },
        onDeletePlant = { id -> viewModel.onDeletePlant(id) },
        onClearError = { viewModel.clearError() },
        onResetSaveState = { viewModel.resetSaveState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onSavePlant: (Int?, String, String?) -> Unit,
    onDeletePlant: (Int) -> Unit,
    onClearError: () -> Unit,
    onResetSaveState: () -> Unit
) {
    var selectedPlant by remember { mutableStateOf<Plant?>(null) }
    var plantToDelete by remember { mutableStateOf<Plant?>(null) }
    var isDialogOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.error?.let { errorRes ->
        val errorMessage = stringResource(errorRes)
        LaunchedEffect(errorRes) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_title)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                selectedPlant = null
                isDialogOpen = true 
            }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.plants.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.home_welcome))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.plants) { plant ->
                    PlantItem(
                        plant = plant,
                        onClick = {
                            selectedPlant = plant
                            isDialogOpen = true
                        },
                        onDelete = { plantToDelete = plant }
                    )
                }
            }
        }

        if (isDialogOpen) {
            LaunchedEffect(uiState.isSaveSuccess) {
                if (uiState.isSaveSuccess) {
                    isDialogOpen = false
                    onResetSaveState()
                }
            }

            PlantDialog(
                initialPlant = selectedPlant,
                error = uiState.saveError?.let { stringResource(it) },
                isSaving = uiState.isSaving,
                onDismiss = { 
                    isDialogOpen = false
                    onResetSaveState()
                },
                onConfirm = { name, desc ->
                    onSavePlant(selectedPlant?.id, name, desc)
                }
            )
        }

        plantToDelete?.let { plant ->
            AlertDialog(
                onDismissRequest = { plantToDelete = null },
                title = { Text("Eliminar planta") },
                text = { Text("¿Estás seguro de que quieres eliminar '${plant.name}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            plant.id?.let { onDeletePlant(it) }
                            plantToDelete = null
                        }
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { plantToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun PlantDialog(
    initialPlant: Plant? = null,
    error: String? = null,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialPlant?.name ?: "") }
    var description by remember { mutableStateOf(initialPlant?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (initialPlant == null) stringResource(Res.string.home_add_plant) 
                else "Editar Planta"
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.home_plant_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    enabled = !isSaving
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.home_plant_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(if (initialPlant == null) stringResource(Res.string.home_button_add) else "Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(Res.string.home_button_cancel))
            }
        }
    )
}

@Composable
fun PlantItem(
    plant: Plant,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleMedium
                )
                plant.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Text("🗑️") // Emoji como fallback si no hay iconos configurados
            }
        }
    }
}
