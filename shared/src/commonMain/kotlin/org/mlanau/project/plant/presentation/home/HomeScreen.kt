package org.mlanau.project.plant.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.component.getLightNeedString
import org.mlanau.project.plant.presentation.component.getPotSizeString
import org.mlanau.project.plant.presentation.component.icon
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.component.EmptyState
import org.mlanau.project.shared.ui.component.PlantThumbnail
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.monthName
import org.mlanau.project.shared.ui.weekdayName
import plantitas_app.shared.generated.resources.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlantDetail: (Int) -> Unit,
    onNavigateToPlantForm: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onNavigateToSettings = onNavigateToSettings,
        onClearError = { viewModel.clearError() },
        onNavigateToPlantDetail = onNavigateToPlantDetail,
        onNavigateToPlantForm = onNavigateToPlantForm
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onNavigateToSettings: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToPlantDetail: (Int) -> Unit,
    onNavigateToPlantForm: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.error?.let { error ->
        val errorMessage = error.localizedMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.plants.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onNavigateToPlantForm,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.home_add_plant_description))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.plants.isEmpty() -> {
                    EmptyState(
                        title = stringResource(Res.string.home_welcome),
                        description = stringResource(Res.string.home_empty_description),
                        modifier = Modifier.align(Alignment.Center).widthIn(max = 480.dp),
                        action = {
                            Button(onClick = onNavigateToPlantForm, shape = MaterialTheme.shapes.large) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.home_add_plant))
                            }
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            ScreenHeader(
                                title = stringResource(Res.string.home_title),
                                eyebrow = todayLabel(),
                                actions = {
                                    IconButton(onClick = onNavigateToSettings) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = stringResource(Res.string.settings_title)
                                        )
                                    }
                                }
                            )
                        }
                        item {
                            WeeklySummaryCard(
                                nextCareByPlant = uiState.nextCareByPlant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(uiState.plants) { plant ->
                            PlantCard(
                                plant = plant,
                                nextCare = plant.id?.let { uiState.nextCareByPlant[it] },
                                onClick = { plant.id?.let { onNavigateToPlantDetail(it.value) } },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun todayLabel(): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${weekdayName(today.dayOfWeek)} · ${today.day} ${monthName(today.month)}"
}

@Composable
private fun WeeklySummaryCard(
    nextCareByPlant: Map<*, CareTask.Pending>,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    val tasks = nextCareByPlant.values
    val weekCount = tasks.count { today.daysUntil(it.dueAt.toLocalDateTime(timeZone).date) <= 7 }
    val todayCount = tasks.count { it.isOverdue || it.dueAt.toLocalDateTime(timeZone).date <= today }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (weekCount == 0) {
                Text(
                    text = stringResource(Res.string.home_care_summary_clear),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = weekCount.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = pluralStringResource(Res.plurals.home_care_summary_week, weekCount, weekCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (todayCount > 0) {
                        Text(
                            text = stringResource(Res.string.home_care_summary_today, todayCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantCard(
    plant: Plant,
    nextCare: CareTask.Pending?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlantThumbnail(imageUrl = plant.imageUrl)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = plant.location?.takeIf { it.isNotBlank() }
                    ?: plant.description?.takeIf { it.isNotBlank() }
                if (subtitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (plant.location?.isNotBlank() == true) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (plant.lightNeed != null || plant.potSize != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        plant.lightNeed?.let { AttributeTag(Icons.Default.WbSunny, getLightNeedString(it)) }
                        plant.potSize?.let { AttributeTag(Icons.Default.Yard, getPotSizeString(it)) }
                    }
                }
            }

            nextCare?.let { NextCareBadge(it) }
        }
    }
}

@Composable
private fun AttributeTag(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun NextCareBadge(task: CareTask.Pending) {
    val color = if (task.isOverdue) MaterialTheme.colorScheme.error else careColor(task.type)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.widthIn(max = 92.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Icon(
            imageVector = task.type.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Text(
            text = nextCareLabel(task),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 2
        )
    }
}

@Composable
private fun nextCareLabel(task: CareTask.Pending): String {
    val timeZone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    val dueDate = task.dueAt.toLocalDateTime(timeZone).date
    val days = today.daysUntil(dueDate)
    return when {
        days < 0 -> pluralStringResource(Res.plurals.care_overdue_days, -days, -days)
        days == 0 -> if (task.isOverdue) {
            stringResource(Res.string.care_overdue_today)
        } else {
            stringResource(Res.string.home_next_care_today)
        }
        days == 1 -> stringResource(Res.string.home_next_care_tomorrow)
        else -> stringResource(Res.string.home_next_care_in_days, days)
    }
}
