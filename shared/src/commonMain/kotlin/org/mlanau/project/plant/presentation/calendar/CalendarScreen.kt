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
import org.mlanau.project.plant.domain.model.*
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.*

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
                it.task.at.toLocalDateTime(timeZone).date == uiState.selectedDate
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
                            onMarkDone = { viewModel.onMarkDone(it) },
                            onUndoTask = { viewModel.onUndoTask(it) },
                            onClick = { onNavigateToPlantDetail(entry.task.plantId.value) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    viewMonth: Month,
    viewYear: Int,
    selectedDate: LocalDate,
    entries: List<CalendarTaskItem>,
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
                        val dayEntries = entries.filter { it.task.at.toLocalDateTime(timeZone).date == date }
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
                                        val task = it.task
                                        task is CareTask.Pending && task.status == PendingStatus.OVERDUE
                                    }
                                    val entryColors = dayEntries
                                        .map { careColor(it.task.type) }
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
private fun CalendarEntryRow(
    entry: CalendarTaskItem,
    onMarkDone: (CareTask.Pending) -> Unit,
    onUndoTask: (CareTask.Done) -> Unit,
    onClick: () -> Unit
) {
    when (val task = entry.task) {
        is CareTask.Pending -> PendingRow(task, entry.plantName, onMarkDone, onClick)
        is CareTask.Done -> DoneRow(task, entry.plantName, onUndoTask, onClick)
    }
}

@Composable
private fun PendingRow(
    task: CareTask.Pending,
    plantName: String?,
    onMarkDone: (CareTask.Pending) -> Unit,
    onClick: () -> Unit
) {
    val isOverdue = task.isOverdue
    val timeZone = TimeZone.currentSystemDefault()
    // Today's tasks can be ticked off as well as overdue ones; a task the user hasn't reached yet
    // simply offers no button, since completing it would be an ad-hoc care on another day.
    val isCompletable = task.isCompletableOn(Clock.System.now(), timeZone)
    val resolvedPlantName = plantName ?: stringResource(Res.string.calendar_unknown_plant)
    val typeColor = careColor(task.type)

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
                    text = "${careTypeLabel(task.type)} - $resolvedPlantName",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isOverdue) {
                    Text(
                        text = overdueText(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = DateFormatUtils.formatTime(task.dueAt, timeZone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isOverdue) {
                Icon(Icons.Default.Warning, contentDescription = stringResource(Res.string.care_overdue_label), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isCompletable) {
                IconButton(onClick = { onMarkDone(task) }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(Res.string.care_action_done),
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoneRow(
    task: CareTask.Done,
    plantName: String?,
    onUndoTask: (CareTask.Done) -> Unit,
    onClick: () -> Unit
) {
    val resolvedPlantName = plantName ?: stringResource(Res.string.calendar_unknown_plant)
    val typeColor = careColor(task.type)

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
                    text = "${careTypeLabel(task.type)} - $resolvedPlantName",
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = TextDecoration.LineThrough
                )
                val timeZone = TimeZone.currentSystemDefault()
                Text(
                    text = DateFormatUtils.formatTime(task.performedAt, timeZone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
            IconButton(onClick = { onUndoTask(task) }) {
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

/**
 * How far behind an overdue task is, counted from the day it was first owed rather than from the
 * last care — the same measure the reminder notification uses, so both say the same number.
 * [CareTask.Pending.missedCount] is appended when the task stands for more than one missed
 * occurrence, since a single entry is all the user ever sees however long they forgot the plant.
 */
@Composable
private fun overdueText(task: CareTask.Pending): String {
    val timeZone = TimeZone.currentSystemDefault()
    val daysLate = task.dueAt.daysUntil(Clock.System.now(), timeZone).coerceAtLeast(0)
    val lateness = if (daysLate == 0) {
        stringResource(Res.string.care_overdue_today)
    } else {
        stringResource(Res.string.care_overdue_days, daysLate)
    }
    return if (task.missedCount > 1) {
        "$lateness  ${stringResource(Res.string.care_overdue_missed_count, task.missedCount)}"
    } else {
        lateness
    }
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
