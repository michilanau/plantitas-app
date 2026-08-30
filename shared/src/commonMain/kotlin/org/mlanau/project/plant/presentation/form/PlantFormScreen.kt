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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.presentation.component.CareRuleDialog
import org.mlanau.project.plant.presentation.component.CareRuleItem
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.getPotSizeString
import plantitas_app.shared.generated.resources.*
import coil3.compose.AsyncImage
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.SelectionMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantFormScreen(
    viewModel: PlantFormViewModel,
    plantId: Int? = null,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current

    var showCancelConfirmation by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val launcher = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let { viewModel.onImagePicked(it) }
        }
    )

    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    var isAddCareDialogOpen by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<CareRule?>(null) }

    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    LaunchedEffect(uiState.isSaveSuccess, uiState.isDeleteSuccess) {
        when {
            uiState.isDeleteSuccess -> {
                viewModel.resetState()
                onDeleted()
            }
            uiState.isSaveSuccess -> {
                viewModel.resetState()
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (plantId == null) stringResource(Res.string.home_add_plant)
                        else stringResource(Res.string.plant_form_edit_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasChanges) showCancelConfirmation = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                actions = {
                    if (plantId != null) {
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { viewModel.onSavePlant() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (plantId == null) stringResource(Res.string.home_button_add)
                            else stringResource(Res.string.plant_form_save_changes),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clickable { launcher.launch() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (uiState.imageBytes != null) {
                        AsyncImage(
                            model = uiState.imageBytes,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (uiState.imageUrl != null) {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(Res.string.common_add_image),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (uiState.imageBytes != null || uiState.imageUrl != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("${stringResource(Res.string.home_plant_name)} *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.error != null,
                    enabled = !uiState.isSaving,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!.localizedMessage(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("${stringResource(Res.string.home_plant_description)} ${stringResource(Res.string.common_optional)}") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            OutlinedTextField(
                value = uiState.location,
                onValueChange = { viewModel.onLocationChanged(it) },
                label = { Text("${stringResource(Res.string.home_plant_location)} ${stringResource(Res.string.common_optional)}") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

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
                    val isSelected = uiState.lightNeed == need
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onLightNeedSelected(need) },
                        label = { Text(getLightNeedString(need)) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                    val isSelected = uiState.potSize == size
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onPotSizeSelected(size) },
                        label = { Text(getPotSizeString(size)) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

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
                    onTogglePaused = { viewModel.toggleCareRulePaused(rule) },
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
        }

        if (showCancelConfirmation) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmation = false },
                title = { Text(stringResource(Res.string.plant_form_cancel_dialog_title)) },
                text = { Text(stringResource(Res.string.plant_form_cancel_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(Res.string.common_discard), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmation = false }) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                }
            )
        }

        if (isDeleteDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDeleteDialogOpen = false },
                title = { Text(stringResource(Res.string.plant_form_delete_dialog_title)) },
                text = { Text(stringResource(Res.string.plant_form_delete_dialog_message, uiState.plant?.name ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onDeletePlant()
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
                plantId = uiState.plant?.id,
                takenTypes = uiState.careRules.map { it.type }.toSet(),
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
