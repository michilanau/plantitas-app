package org.mlanau.project.plant.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.presentation.CareToast
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.component.CareRuleDialog
import org.mlanau.project.plant.presentation.component.CareRuleItem
import org.mlanau.project.plant.presentation.component.FullScreenImageDialog
import org.mlanau.project.plant.presentation.component.LogCareDialog
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.component.getCareTypeString
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.getPotSizeString
import org.mlanau.project.plant.presentation.component.icon
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import plantitas_app.shared.generated.resources.*

@Composable
fun PlantDetailScreen(
    viewModel: PlantDetailViewModel,
    plantId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    var showLogCareDialog by remember { mutableStateOf(false) }
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

    val careLoggedMessage = stringResource(Res.string.care_log_saved)
    val careUndoneMessage = stringResource(Res.string.care_log_undone)
    val undoActionLabel = stringResource(Res.string.care_action_undo)
    LaunchedEffect(Unit) {
        viewModel.toasts.collect { toast ->
            when (toast) {
                is CareToast.Logged -> {
                    val result = snackbarHostState.showSnackbar(
                        message = careLoggedMessage,
                        actionLabel = undoActionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onUndoLoggedCare(toast.taskId)
                    }
                }
                CareToast.Undone -> snackbarHostState.showSnackbar(careUndoneMessage)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.isLoading || uiState.plant?.id?.value != plantId) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                uiState.plant?.let { plant ->
                    PlantDetailContent(
                        plant = plant,
                        nextPending = uiState.nextPending,
                        careRules = uiState.careRules,
                        history = uiState.history,
                        onBack = onBack,
                        onEdit = { onEdit(plantId) },
                        onMarkDone = { viewModel.onMarkDone(it) },
                        onImageClick = { showFullScreenImage = it },
                        onLogCareClick = { showLogCareDialog = true },
                        onUndoTask = { viewModel.onUndoTask(it) },
                        onEditRule = { ruleToEdit = it }
                    )
                }
            }
        }
    }

    if (showFullScreenImage != null) {
        FullScreenImageDialog(
            imageUrl = showFullScreenImage!!,
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

    ruleToEdit?.let { rule ->
        CareRuleDialog(
            plantId = uiState.plant?.id,
            takenTypes = uiState.careRules.map { it.type }.toSet(),
            initialRule = rule,
            onDismiss = { ruleToEdit = null },
            onConfirm = {
                viewModel.onSaveRule(it)
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
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMarkDone: (CareTask.Pending) -> Unit,
    onImageClick: (String) -> Unit,
    onLogCareClick: () -> Unit,
    onUndoTask: (CareTask.Done) -> Unit,
    onEditRule: (CareRule) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        ScreenHeader(
            title = plant.name,
            onNavigateBack = onBack,
            backContentDescription = stringResource(Res.string.common_back),
            actions = {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.plant_form_edit_title))
                }
            }
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(enabled = plant.imageUrl != null) {
                        plant.imageUrl?.let { onImageClick(it) }
                    },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (plant.imageUrl != null) {
                    AsyncImage(
                        model = plant.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_leaf),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            val attributes = buildList {
                plant.location?.takeIf { it.isNotBlank() }?.let {
                    add(Triple(Icons.Default.LocationOn, stringResource(Res.string.home_plant_location), it))
                }
                plant.lightNeed?.let {
                    add(Triple(Icons.Default.WbSunny, stringResource(Res.string.home_plant_light), getLightNeedString(it)))
                }
                plant.potSize?.let {
                    add(Triple(Icons.Default.Yard, stringResource(Res.string.home_plant_pot), getPotSizeString(it)))
                }
            }
            if (attributes.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    attributes.forEach { (icon, label, value) ->
                        AttributePill(icon, label, value, modifier = Modifier.weight(1f))
                    }
                }
            }

            plant.description?.takeIf { it.isNotBlank() }?.let {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(Res.string.home_plant_description))
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (careRules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(Res.string.care_rules_title))
                    careRules.forEach { rule ->
                        CareRuleItem(rule = rule, onClick = { onEditRule(rule) })
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(Res.string.plant_detail_next_care))
                NextCareCard(nextPending = nextPending, onMarkDone = onMarkDone)
            }

            FilledTonalButton(
                onClick = onLogCareClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.care_log_action))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(Res.string.care_history_title))
                if (history.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.care_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    history.forEach { task -> HistoryRow(task = task, onUndo = { onUndoTask(task) }) }
                }
            }
        }
    }
}

@Composable
private fun AttributePill(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun NextCareCard(nextPending: CareTask.Pending?, onMarkDone: (CareTask.Pending) -> Unit) {
    val dateFormatter = LocalDateFormatter.current
    val isOverdue = nextPending?.isOverdue == true
    val isCompletable = nextPending?.isCompletableOn(Clock.System.now(), TimeZone.currentSystemDefault()) == true

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        border = if (isOverdue) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (nextPending == null) {
                Text(
                    text = stringResource(Res.string.plant_detail_no_upcoming_care),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = nextPending.type.icon,
                    contentDescription = null,
                    tint = if (isOverdue) MaterialTheme.colorScheme.error else careColor(nextPending.type)
                )
                Column {
                    Text(
                        text = getCareTypeString(nextPending.type),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isOverdue) daysWithoutCareText(nextPending) else dateFormatter.formatDateTime(nextPending.dueAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isOverdue) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onMarkDone(nextPending) },
                enabled = isCompletable,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.care_action_done))
            }
            if (!isCompletable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.plant_detail_available_on, dateFormatter.formatDate(nextPending.dueAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(task: CareTask.Done, onUndo: () -> Unit) {
    val dateFormatter = LocalDateFormatter.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(careColor(task.type)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCareTypeString(task.type),
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormatter.formatDateTime(task.performedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                task.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(Res.string.care_action_undo))
            }
        }
    }
}

/**
 * How far behind an overdue task is, counted from the day it was first owed — the same measure the
 * calendar and the reminder notification use, so all three say the same number.
 */
@Composable
private fun daysWithoutCareText(pending: CareTask.Pending): String {
    val timeZone = TimeZone.currentSystemDefault()
    val daysLate = pending.dueAt.daysUntil(Clock.System.now(), timeZone).coerceAtLeast(0)
    val lateness = if (daysLate == 0) {
        stringResource(Res.string.care_overdue_today)
    } else {
        pluralStringResource(Res.plurals.care_overdue_days, daysLate, daysLate)
    }
    return if (pending.missedCount > 1) {
        "$lateness  ${stringResource(Res.string.care_overdue_missed_count, pending.missedCount)}"
    } else {
        lateness
    }
}
