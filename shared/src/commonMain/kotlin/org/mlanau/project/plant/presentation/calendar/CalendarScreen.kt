package org.mlanau.project.plant.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlin.time.Clock
import kotlinx.datetime.*
import org.mlanau.project.plant.application.CalendarEntry
import org.mlanau.project.plant.domain.model.*
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.*

import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.domain.service.OccurrenceStatus
import org.mlanau.project.plant.presentation.component.CareOccurrenceActionDialog
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.component.overdueColor
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.DateFormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToPlantDetail: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewMonth = uiState.viewMonth
    val viewYear = uiState.viewYear

    var showOccurrenceOptions by remember { mutableStateOf<CareOccurrence?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${getMonthName(viewMonth)} $viewYear")
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.onPreviousMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(Res.string.calendar_prev_month))
                        }
                        IconButton(onClick = { viewModel.resetToToday() }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource(Res.string.calendar_today))
                        }
                        IconButton(onClick = { viewModel.onNextMonth() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(Res.string.calendar_next_month))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            CalendarGrid(
                viewMonth = viewMonth,
                viewYear = viewYear,
                selectedDate = uiState.selectedDate,
                entries = uiState.entries,
                onDateSelected = { viewModel.onDateSelected(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val timeZone = TimeZone.currentSystemDefault()
            val selectedDateEntries = uiState.entries.filter {
                it.at.toLocalDateTime(timeZone).date == uiState.selectedDate
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedDateEntries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.calendar_no_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDateEntries) { entry ->
                        CalendarEntryRow(
                            entry = entry,
                            onShowOccurrenceOptions = { showOccurrenceOptions = it },
                            onUndoLog = { viewModel.onUndoLog(it) },
                            onClick = { onNavigateToPlantDetail(entry.plantId.value) }
                        )
                    }
                }
            }
        }
    }

    showOccurrenceOptions?.let { occurrence ->
        CareOccurrenceActionDialog(
            occurrence = occurrence,
            onDismissRequest = { showOccurrenceOptions = null },
            onMarkDone = { viewModel.onMarkDone(it) },
            onDismissOccurrence = { viewModel.onDismissOccurrence(it) },
            onViewPlant = { onNavigateToPlantDetail(it) }
        )
    }
}

@Composable
private fun CalendarGrid(
    viewMonth: Month,
    viewYear: Int,
    selectedDate: LocalDate,
    entries: List<CalendarEntry>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = LocalDate(viewYear, viewMonth, 1)
        .plus(1, DateTimeUnit.MONTH)
        .minus(1, DateTimeUnit.DAY).day
    val firstDayOfMonth = LocalDate(viewYear, viewMonth, 1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.ordinal) % 7

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("L", "M", "X", "J", "V", "S", "D")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = daysInMonth + dayOfWeekOffset
        val rows = (totalCells + 6) / 7

        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { columnIndex ->
                    val dayIndex = rowIndex * 7 + columnIndex - dayOfWeekOffset + 1
                    if (dayIndex in 1..daysInMonth) {
                        val date = LocalDate(viewYear, viewMonth, dayIndex)
                        val isSelected = date == selectedDate
                        val timeZone = TimeZone.currentSystemDefault()
                        val today = Clock.System.now().toLocalDateTime(timeZone).date
                        val isToday = date == today
                        val dayEntries = entries.filter { it.at.toLocalDateTime(timeZone).date == date }
                        val hasEntries = dayEntries.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else if (isToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                           else if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasEntries) {
                                    val hasOverdue = dayEntries.any {
                                        it is CalendarEntry.Scheduled && it.occurrence.status == OccurrenceStatus.OVERDUE
                                    }
                                    val entryColors = dayEntries
                                        .map { entryColorFor(it) }
                                        .distinct()
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        entryColors.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .let {
                                                        if (hasOverdue) it.border(1.dp, overdueColor(), CircleShape) else it
                                                    }
                                                    .background(color, shape = CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun entryColorFor(entry: CalendarEntry): Color = when (entry) {
    is CalendarEntry.Scheduled -> careColor(entry.occurrence.type)
    is CalendarEntry.Logged -> careColor(entry.log.type)
}

@Composable
private fun CalendarEntryRow(
    entry: CalendarEntry,
    onShowOccurrenceOptions: (CareOccurrence) -> Unit,
    onUndoLog: (CareLog) -> Unit,
    onClick: () -> Unit
) {
    when (entry) {
        is CalendarEntry.Scheduled -> OccurrenceRow(entry, onShowOccurrenceOptions, onClick)
        is CalendarEntry.Logged -> LoggedRow(entry, onUndoLog, onClick)
    }
}

@Composable
private fun OccurrenceRow(
    entry: CalendarEntry.Scheduled,
    onShowOptions: (CareOccurrence) -> Unit,
    onClick: () -> Unit
) {
    val occurrence = entry.occurrence
    val isOverdue = occurrence.status == OccurrenceStatus.OVERDUE
    val resolvedPlantName = entry.plantName ?: stringResource(Res.string.calendar_unknown_plant)
    val typeColor = careColor(occurrence.type)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(typeColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${careTypeLabel(occurrence.type)} - $resolvedPlantName",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isOverdue) {
                    Text(
                        text = daysWithoutCareText(occurrence),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    val timeZone = TimeZone.currentSystemDefault()
                    Text(
                        text = DateFormatUtils.formatTime(occurrence.scheduledAt, timeZone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isOverdue) {
                Icon(Icons.Default.Warning, contentDescription = stringResource(Res.string.care_overdue_label), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { onShowOptions(occurrence) }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LoggedRow(
    entry: CalendarEntry.Logged,
    onUndoLog: (CareLog) -> Unit,
    onClick: () -> Unit
) {
    val log = entry.log
    val resolvedPlantName = entry.plantName ?: stringResource(Res.string.calendar_unknown_plant)
    val typeColor = careColor(log.type)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(typeColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${careTypeLabel(log.type)} - $resolvedPlantName",
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = TextDecoration.LineThrough
                )
                val timeZone = TimeZone.currentSystemDefault()
                Text(
                    text = DateFormatUtils.formatTime(log.performedAt, timeZone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
            IconButton(onClick = { onUndoLog(log) }) {
                Icon(Icons.Default.Undo, contentDescription = stringResource(Res.string.care_action_undo))
            }
        }
    }
}

@Composable
private fun careTypeLabel(type: CareType): String = when (type) {
    CareType.WATER -> stringResource(Res.string.care_type_water)
    CareType.FERTILIZE -> stringResource(Res.string.care_type_fertilize)
    CareType.REPOT -> stringResource(Res.string.care_type_repot)
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
private fun getMonthName(month: Month): String = when (month) {
    Month.JANUARY -> stringResource(Res.string.month_january)
    Month.FEBRUARY -> stringResource(Res.string.month_february)
    Month.MARCH -> stringResource(Res.string.month_march)
    Month.APRIL -> stringResource(Res.string.month_april)
    Month.MAY -> stringResource(Res.string.month_may)
    Month.JUNE -> stringResource(Res.string.month_june)
    Month.JULY -> stringResource(Res.string.month_july)
    Month.AUGUST -> stringResource(Res.string.month_august)
    Month.SEPTEMBER -> stringResource(Res.string.month_september)
    Month.OCTOBER -> stringResource(Res.string.month_october)
    Month.NOVEMBER -> stringResource(Res.string.month_november)
    Month.DECEMBER -> stringResource(Res.string.month_december)
}
