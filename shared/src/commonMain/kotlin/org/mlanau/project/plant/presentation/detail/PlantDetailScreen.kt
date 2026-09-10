package org.mlanau.project.plant.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.component.CareHistoryRow
import org.mlanau.project.plant.presentation.component.CareRuleDialog
import org.mlanau.project.plant.presentation.component.CareRuleRow
import org.mlanau.project.plant.presentation.component.CareTypeBadge
import org.mlanau.project.plant.presentation.component.FullScreenImageDialog
import org.mlanau.project.plant.presentation.component.LogCareDialog
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.component.dueLabel
import org.mlanau.project.plant.presentation.component.getCareTypeString
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.getPotSizeString
import org.mlanau.project.plant.presentation.component.overdueColor
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppButton
import org.mlanau.project.shared.ui.component.AppButtonStyle
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.AppCardTone
import org.mlanau.project.shared.ui.component.AppSnackbarHost
import org.mlanau.project.shared.ui.component.PlantAvatar
import org.mlanau.project.shared.ui.component.RoundIconButton
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.ScreenGutter
import plantitas_app.shared.generated.resources.*

@Composable
fun PlantDetailScreen(
    viewModel: PlantDetailViewModel,
    plantId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onSeeAllHistory: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    var showLogCareDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<CareRule?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error == UiError.PlantNotFound) {
            viewModel.clearError()
            onBack()
        }
    }

    uiState.error?.takeIf { it != UiError.PlantNotFound }?.let { error ->
        val errorMessage = error.localizedMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.isLoading || uiState.plant?.id?.value != plantId) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                uiState.plant?.let { plant ->
                    PlantDetailContent(
                        plant = plant,
                        nextPending = uiState.nextPending,
                        careRules = uiState.careRules,
                        history = uiState.history,
                        hasMoreHistory = uiState.hasMoreHistory,
                        onBack = onBack,
                        onEdit = { onEdit(plantId) },
                        onMarkDone = { viewModel.onMarkDone(it) },
                        onImageClick = { showFullScreenImage = it },
                        onLogCareClick = { showLogCareDialog = true },
                        onUndoTask = { viewModel.onUndoTask(it) },
                        onEditRule = { ruleToEdit = it },
                        onAddRule = { showAddRuleDialog = true },
                        onSeeAllHistory = { onSeeAllHistory(plantId) }
                    )
                }
            }
        }
    }

    showFullScreenImage?.let { imageUrl ->
        FullScreenImageDialog(
            imageUrl = imageUrl,
            onDismissRequest = { showFullScreenImage = null }
        )
    }

    if (showLogCareDialog) {
        LogCareDialog(
            onDismiss = { showLogCareDialog = false },
            onConfirm = { details, performedAt, note ->
                viewModel.onLogAdHocCare(details, performedAt, note)
                showLogCareDialog = false
            }
        )
    }

    if (showAddRuleDialog || ruleToEdit != null) {
        CareRuleDialog(
            plantId = uiState.plant?.id,
            takenTypes = uiState.careRules.map { it.type }.toSet(),
            initialRule = ruleToEdit,
            onDismiss = {
                showAddRuleDialog = false
                ruleToEdit = null
            },
            onConfirm = {
                viewModel.onSaveRule(it)
                showAddRuleDialog = false
                ruleToEdit = null
            }
        )
    }
}

@Composable
private fun PlantDetailContent(
    plant: Plant,
    nextPending: CareTask.Pending?,
    careRules: List<CareRule>,
    history: List<CareTask.Done>,
    hasMoreHistory: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMarkDone: (CareTask.Pending) -> Unit,
    onImageClick: (String) -> Unit,
    onLogCareClick: () -> Unit,
    onUndoTask: (CareTask.Done) -> Unit,
    onEditRule: (CareRule) -> Unit,
    onAddRule: () -> Unit,
    onSeeAllHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = ContentMaxWidth)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp)
    ) {
        ScreenHeader(
            title = null,
            onNavigateBack = onBack,
            backContentDescription = stringResource(Res.string.common_back),
            actions = {
                RoundIconButton(
                    icon = Res.drawable.ic_pencil,
                    contentDescription = stringResource(Res.string.plant_form_edit_title),
                    onClick = onEdit,
                    iconSize = 20.dp
                )
            }
        )

        Column(modifier = Modifier.padding(horizontal = ScreenGutter)) {
            PlantAvatar(
                imageUrl = plant.imageUrl,
                seed = plant.id?.value ?: 0,
                modifier = Modifier.fillMaxWidth().height(186.dp),
                shape = MaterialTheme.shapes.extraLarge,
                iconSize = 88.dp,
                onClick = plant.imageUrl?.let { url -> { onImageClick(url) } }
            )

            Text(
                text = plant.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 18.dp)
            )
            plant.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            val attributes = buildList {
                plant.location?.takeIf { it.isNotBlank() }?.let {
                    add(Triple(Res.drawable.ic_pin, stringResource(Res.string.home_plant_location), it))
                }
                plant.lightNeed?.let {
                    add(Triple(Res.drawable.ic_sun, stringResource(Res.string.home_plant_light), getLightNeedString(it)))
                }
                plant.potSize?.let {
                    add(Triple(Res.drawable.ic_care_repot, stringResource(Res.string.home_plant_pot), getPotSizeString(it)))
                }
            }
            if (attributes.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attributes.forEach { (icon, label, value) ->
                        AttributeTile(icon, label, value, modifier = Modifier.weight(1f))
                    }
                }
            }

            DetailSection(stringResource(Res.string.plant_detail_next_care)) {
                NextCareBlock(nextPending = nextPending, onMarkDone = onMarkDone)
            }

            DetailSection(stringResource(Res.string.care_rules_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    careRules.forEach { rule ->
                        CareRuleRow(rule = rule, onClick = { onEditRule(rule) })
                    }
                    if (careRules.size < CareType.entries.size) {
                        AppButton(
                            text = stringResource(Res.string.care_add_periodic_rule),
                            onClick = onAddRule,
                            modifier = Modifier.fillMaxWidth(),
                            style = AppButtonStyle.Outline,
                            icon = Res.drawable.ic_plus
                        )
                    }
                }
            }

            AppButton(
                text = stringResource(Res.string.care_log_action),
                onClick = onLogCareClick,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                style = AppButtonStyle.Accent,
                icon = Res.drawable.ic_clock
            )

            DetailSection(stringResource(Res.string.care_history_title)) {
                if (history.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.care_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        history.forEach { task -> CareHistoryRow(task = task, onUndo = { onUndoTask(task) }) }
                        if (hasMoreHistory) {
                            AppButton(
                                text = stringResource(Res.string.care_history_see_all),
                                onClick = onSeeAllHistory,
                                modifier = Modifier.fillMaxWidth(),
                                style = AppButtonStyle.Outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    SectionLabel(text = title, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
    content()
}

@Composable
private fun AttributeTile(icon: DrawableResource, label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NextCareBlock(nextPending: CareTask.Pending?, onMarkDone: (CareTask.Pending) -> Unit) {
    val dateFormatter = LocalDateFormatter.current
    val isOverdue = nextPending?.isOverdue == true

    AppCard(
        tone = if (isOverdue) AppCardTone.Alert else AppCardTone.Default,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (nextPending == null) {
                Text(
                    text = stringResource(Res.string.plant_detail_no_upcoming_care),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val isCompletable = nextPending.isCompletableOn(Clock.System.now(), TimeZone.currentSystemDefault())
            val whenText = if (isOverdue) {
                dueLabel(nextPending)
            } else {
                "${dueLabel(nextPending)} · ${dateFormatter.formatTime(nextPending.dueAt)}"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CareTypeBadge(type = nextPending.type, size = 52.dp, overdue = isOverdue)
                Column {
                    Text(
                        text = getCareTypeString(nextPending.type),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = whenText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverdue) overdueColor() else careColor(nextPending.type)
                    )
                }
            }

            AppButton(
                text = stringResource(Res.string.care_action_done),
                onClick = { onMarkDone(nextPending) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                icon = Res.drawable.ic_check,
                enabled = isCompletable
            )
            if (!isCompletable) {
                Text(
                    text = stringResource(Res.string.plant_detail_available_on, dateFormatter.formatDate(nextPending.dueAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }
    }
}
