package org.mlanau.project.plant.presentation.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.presentation.component.CareRuleDialog
import org.mlanau.project.plant.presentation.component.CareRuleItem
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.getPotSizeString
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
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
    BackHandler(enabled = uiState.hasChanges) { showCancelConfirmation = true }

    val scope = rememberCoroutineScope()
    val launcher = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { byteArrays -> byteArrays.firstOrNull()?.let { viewModel.onImagePicked(it) } }
    )

    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    var isAddCareDialogOpen by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<CareRule?>(null) }

    LaunchedEffect(plantId) { viewModel.loadPlant(plantId) }

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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = { viewModel.onSavePlant() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
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
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ScreenHeader(
                title = if (plantId == null) stringResource(Res.string.home_add_plant)
                else stringResource(Res.string.plant_form_edit_title),
                onNavigateBack = { if (uiState.hasChanges) showCancelConfirmation = true else onBack() },
                backContentDescription = stringResource(Res.string.common_back),
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

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))
                PhotoPicker(
                    imageBytes = uiState.imageBytes,
                    imageUrl = uiState.imageUrl,
                    onPick = { launcher.launch() },
                    onClear = { viewModel.onImageCleared() }
                )

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
                    supportingText = uiState.error?.let { { Text(it.localizedMessage(), color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

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
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
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
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                FormSection(stringResource(Res.string.home_plant_light)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LightNeed.entries.forEach { need ->
                            val selected = uiState.lightNeed == need
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onLightNeedSelected(if (selected) null else need) },
                                label = { Text(getLightNeedString(need)) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                FormSection(stringResource(Res.string.home_plant_pot)) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PotSize.entries.forEach { size ->
                            val selected = uiState.potSize == size
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onPotSizeSelected(if (selected) null else size) },
                                label = { Text(getPotSizeString(size)) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                FormSection(stringResource(Res.string.care_rules_title)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.care_add_rule))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
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
                    TextButton(onClick = {
                        viewModel.onDeletePlant()
                        isDeleteDialogOpen = false
                    }) {
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

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun PhotoPicker(
    imageBytes: ByteArray?,
    imageUrl: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier.size(150.dp).clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            when {
                imageBytes != null -> AsyncImage(model = imageBytes, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                imageUrl != null -> AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.common_add_image), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        if (imageBytes != null || imageUrl != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(7.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Surface(
                onClick = onClear,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_remove_image), modifier = Modifier.padding(7.dp))
            }
        }
    }
}
