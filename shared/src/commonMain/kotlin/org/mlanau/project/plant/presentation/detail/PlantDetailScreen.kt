package org.mlanau.project.plant.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.*
import plantitas_app.shared.generated.resources.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*
import org.mlanau.project.shared.ui.theme.PlantitasTheme
import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.domain.service.OccurrenceStatus
import org.mlanau.project.plant.presentation.component.CareOccurrenceActionDialog
import org.mlanau.project.plant.presentation.component.FullScreenImageDialog
import org.mlanau.project.plant.presentation.component.LogCareDialog
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.vector.ImageVector
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import org.mlanau.project.shared.ui.DateFormatUtils
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.localizedMessage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantDetailScreen(
    viewModel: PlantDetailViewModel,
    plantId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showOccurrenceOptions by remember { mutableStateOf<CareOccurrence?>(null) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    var showLogCareDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    uiState.error?.let { error ->
        val errorMessage = error.localizedMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.plant?.id?.value == plantId) uiState.plant?.name ?: "" else "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                actions = {
                    if (uiState.plant?.id?.value == plantId) {
                        IconButton(onClick = { onEdit(plantId) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.care_edit_rule))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading || uiState.plant?.id?.value != plantId) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.plant?.let { plant ->
                PlantDetailContent(
                    plant = plant,
                    nextOccurrence = uiState.nextOccurrence,
                    careRules = uiState.careRules,
                    history = uiState.history,
                    onShowOccurrenceOptions = { showOccurrenceOptions = it },
                    onImageClick = { showFullScreenImage = it },
                    onLogCareClick = { showLogCareDialog = true },
                    onUndoLog = { viewModel.onUndoLog(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (showFullScreenImage != null) {
        FullScreenImageDialog(
            imageUrl = showFullScreenImage!!,
            onDismissRequest = { showFullScreenImage = null }
        )
    }

    showOccurrenceOptions?.let { occurrence ->
        CareOccurrenceActionDialog(
            occurrence = occurrence,
            onDismissRequest = { showOccurrenceOptions = null },
            onMarkDone = { viewModel.onMarkDone(it) },
            onDismissOccurrence = { viewModel.onDismissOccurrence(it) }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantDetailContent(
    plant: Plant,
    nextOccurrence: CareOccurrence?,
    careRules: List<CareRule>,
    history: List<CareLog>,
    onShowOccurrenceOptions: (CareOccurrence) -> Unit,
    onImageClick: (String) -> Unit,
    onLogCareClick: () -> Unit,
    onUndoLog: (CareLog) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
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
                    Text("🌿", style = MaterialTheme.typography.displayLarge)
                }
            }
        }

        // Info Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                plant.location?.takeIf { it.isNotBlank() }?.let {
                    DetailAttribute(Icons.Default.LocationOn, stringResource(Res.string.home_plant_location), it)
                }
                plant.lightNeed?.let {
                    DetailAttribute(
                        Icons.Default.WbSunny,
                        stringResource(Res.string.home_plant_light),
                        when(it) {
                            LightNeed.LOW -> stringResource(Res.string.light_low)
                            LightNeed.MEDIUM -> stringResource(Res.string.light_medium)
                            LightNeed.HIGH -> stringResource(Res.string.light_high)
                        }
                    )
                }
                plant.potSize?.let {
                    DetailAttribute(
                        Icons.Default.Yard,
                        stringResource(Res.string.home_plant_pot),
                        when(it) {
                            PotSize.SMALL -> stringResource(Res.string.pot_small)
                            PotSize.MEDIUM -> stringResource(Res.string.pot_medium)
                            PotSize.LARGE -> stringResource(Res.string.pot_large)
                            PotSize.EXTRA_LARGE -> stringResource(Res.string.pot_extra_large)
                        }
                    )
                }
            }
        }

        // Description
        plant.description?.takeIf { it.isNotBlank() }?.let {
            Column {
                Text(
                    text = stringResource(Res.string.home_plant_description),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Configured Care Rules Section
        if (careRules.isNotEmpty()) {
            Column {
                Text(
                    text = stringResource(Res.string.care_rules_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                careRules.forEach { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (rule.details) {
                                is CareDetails.Water -> Icons.Default.WaterDrop
                                is CareDetails.Fertilize -> Icons.Default.Science
                                is CareDetails.Repot -> Icons.Default.HomeRepairService
                            }
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when (rule.details) {
                                        is CareDetails.Water -> stringResource(Res.string.care_type_water)
                                        is CareDetails.Fertilize -> stringResource(Res.string.care_type_fertilize)
                                        is CareDetails.Repot -> stringResource(Res.string.care_type_repot)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Text(
                                    text = when (val rec = rule.recurrence) {
                                        is RecurrenceRule.Once -> stringResource(Res.string.care_recurrence_once)
                                        is RecurrenceRule.Periodic -> stringResource(Res.string.care_recurrence_periodic, rec.everyDays)
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Next Care Section
        val isOverdue = nextOccurrence?.status == OccurrenceStatus.OVERDUE
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = nextOccurrence != null) { nextOccurrence?.let { onShowOccurrenceOptions(it) } },
            colors = CardDefaults.cardColors(
                containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.plant_detail_next_care),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (nextOccurrence != null) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (nextOccurrence != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (nextOccurrence.type) {
                            CareType.WATER -> Icons.Default.WaterDrop
                            CareType.FERTILIZE -> Icons.Default.Science
                            CareType.REPOT -> Icons.Default.HomeRepairService
                        }
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else careColor(nextOccurrence.type)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (nextOccurrence.type) {
                                    CareType.WATER -> stringResource(Res.string.care_type_water)
                                    CareType.FERTILIZE -> stringResource(Res.string.care_type_fertilize)
                                    CareType.REPOT -> stringResource(Res.string.care_type_repot)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            if (isOverdue) {
                                Text(
                                    text = daysWithoutCareText(nextOccurrence),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = DateFormatUtils.formatDateTime(nextOccurrence.scheduledAt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.plant_detail_no_upcoming_care),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Log care action
        OutlinedButton(
            onClick = onLogCareClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.care_log_action))
        }

        // History Section
        Column {
            Text(
                text = stringResource(Res.string.care_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(
                    text = stringResource(Res.string.care_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.forEach { log ->
                    HistoryRow(log = log, onUndo = { onUndoLog(log) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(log: CareLog, onUndo: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(careColor(log.type), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (log.type) {
                        CareType.WATER -> stringResource(Res.string.care_type_water)
                        CareType.FERTILIZE -> stringResource(Res.string.care_type_fertilize)
                        CareType.REPOT -> stringResource(Res.string.care_type_repot)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = TextDecoration.LineThrough
                )
                Text(
                    text = DateFormatUtils.formatDateTime(log.performedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                log.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = stringResource(Res.string.care_action_undo))
            }
        }
    }
}

@Composable
private fun daysWithoutCareText(occurrence: CareOccurrence): String {
    val lastCareAt = occurrence.lastCareAt ?: return stringResource(Res.string.care_never_done)
    val timeZone = TimeZone.currentSystemDefault()
    val days = lastCareAt.daysUntil(Clock.System.now(), timeZone).coerceAtLeast(0)
    val verb = when (occurrence.type) {
        CareType.WATER -> stringResource(Res.string.care_verb_water)
        CareType.FERTILIZE -> stringResource(Res.string.care_verb_fertilize)
        CareType.REPOT -> stringResource(Res.string.care_verb_repot)
    }
    return stringResource(Res.string.care_days_without_care, days, verb)
}

@Composable
private fun DetailAttribute(icon: ImageVector, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
