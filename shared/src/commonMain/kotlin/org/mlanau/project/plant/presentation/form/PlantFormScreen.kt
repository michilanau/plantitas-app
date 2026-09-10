package org.mlanau.project.plant.presentation.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.presentation.component.CareRuleDialog
import org.mlanau.project.plant.presentation.component.CareRuleRow
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.getPotSizeString
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.component.AppButton
import org.mlanau.project.shared.ui.component.AppButtonStyle
import org.mlanau.project.shared.ui.component.AppChip
import org.mlanau.project.shared.ui.component.AppTextField
import org.mlanau.project.shared.ui.component.FieldLabel
import org.mlanau.project.shared.ui.component.RoundIconButton
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.ScreenGutter
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
        // The library's default shrinks any photo over 1 MB to fit 800 px, which the plant
        // hero and the full-screen viewer then have to stretch; 2048 px covers both.
        resizeOptions = ResizeOptions(width = 2048, height = 2048, compressionQuality = 0.9),
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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = ContentMaxWidth)) {
                ScreenHeader(
                    title = stringResource(if (plantId == null) Res.string.home_add_plant else Res.string.plant_form_edit_title),
                    onNavigateBack = { if (uiState.hasChanges) showCancelConfirmation = true else onBack() },
                    backIcon = Res.drawable.ic_close,
                    backContentDescription = stringResource(Res.string.common_back),
                    actions = {
                        AppButton(
                            text = stringResource(Res.string.common_save),
                            onClick = { viewModel.onSavePlant() },
                            modifier = Modifier.height(44.dp),
                            enabled = uiState.name.isNotBlank() && !uiState.isLoading,
                            loading = uiState.isSaving
                        )
                    }
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    return@Column
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenGutter)
                        .padding(top = 4.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    PhotoPicker(
                        imageBytes = uiState.imageBytes,
                        imageUrl = uiState.imageUrl,
                        onPick = { launcher.launch() },
                        onClear = { viewModel.onImageCleared() }
                    )

                    AppTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onNameChanged(it) },
                        label = stringResource(Res.string.home_plant_name),
                        errorText = uiState.error?.localizedMessage(),
                        enabled = !uiState.isSaving,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    AppTextField(
                        value = uiState.location,
                        onValueChange = { viewModel.onLocationChanged(it) },
                        label = stringResource(Res.string.home_plant_location),
                        enabled = !uiState.isSaving,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Column {
                        FieldLabel(stringResource(Res.string.home_plant_light))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LightNeed.entries.forEach { need ->
                                val selected = uiState.lightNeed == need
                                AppChip(
                                    label = getLightNeedString(need),
                                    selected = selected,
                                    onClick = { viewModel.onLightNeedSelected(if (selected) null else need) }
                                )
                            }
                        }
                    }

                    Column {
                        FieldLabel(stringResource(Res.string.home_plant_pot))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PotSize.entries.forEach { size ->
                                val selected = uiState.potSize == size
                                AppChip(
                                    label = getPotSizeString(size),
                                    selected = selected,
                                    onClick = { viewModel.onPotSizeSelected(if (selected) null else size) }
                                )
                            }
                        }
                    }

                    AppTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        label = stringResource(Res.string.home_plant_description),
                        enabled = !uiState.isSaving,
                        singleLine = false,
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(
                            text = stringResource(Res.string.care_rules_title),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        uiState.careRules.forEach { rule ->
                            CareRuleRow(
                                rule = rule,
                                onClick = { ruleToEdit = rule },
                                onRemove = { viewModel.removeCareRule(rule) }
                            )
                        }
                        if (uiState.careRules.size < CareType.entries.size) {
                            AppButton(
                                text = stringResource(Res.string.care_add_rule),
                                onClick = { isAddCareDialogOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                style = AppButtonStyle.Outline,
                                icon = Res.drawable.ic_plus
                            )
                        }
                    }

                    if (plantId != null) {
                        AppButton(
                            text = stringResource(Res.string.plant_form_delete_dialog_title),
                            onClick = { isDeleteDialogOpen = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            style = AppButtonStyle.Danger,
                            enabled = !uiState.isSaving
                        )
                    }
                }
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
                        Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurface)
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
                        Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurface)
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
                    val editing = ruleToEdit
                    if (editing != null) {
                        viewModel.updateCareRuleInList(editing, rule)
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
private fun PhotoPicker(
    imageBytes: ByteArray?,
    imageUrl: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val image: Any? = imageBytes ?: imageUrl

    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.matchParentSize().clip(shape).clickable(onClick = onPick),
                contentScale = ContentScale.Crop
            )
            RoundIconButton(
                icon = Res.drawable.ic_close,
                contentDescription = stringResource(Res.string.common_remove_image),
                onClick = onClear,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                size = 40.dp,
                iconSize = 18.dp,
                containerColor = colors.surface
            )
            RoundIconButton(
                icon = Res.drawable.ic_camera,
                contentDescription = stringResource(Res.string.common_add_image),
                onClick = onPick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                size = 40.dp,
                iconSize = 18.dp,
                containerColor = colors.surface
            )
        } else {
            val dashColor = colors.outline
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(colors.surfaceContainer)
                    .drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val inset = strokeWidth / 2
                        drawRoundRect(
                            color = dashColor,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            cornerRadius = CornerRadius(32.dp.toPx() - inset),
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
                            )
                        )
                    }
                    .clickable(onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = stringResource(Res.string.common_add_image),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
