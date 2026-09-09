package org.mlanau.project.plant.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.presentation.CareToast
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.component.doneColor
import org.mlanau.project.plant.presentation.component.getCareTypeString
import org.mlanau.project.plant.presentation.component.icon
import org.mlanau.project.plant.presentation.component.overdueColor
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.monthName
import org.mlanau.project.shared.ui.weekdayShortName
import plantitas_app.shared.generated.resources.*

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
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
                ScreenHeader(
                    title = "${monthName(viewMonth)} $viewYear",
                    eyebrow = stringResource(Res.string.calendar_title),
                    actions = {
                        IconButton(onClick = { viewModel.onPreviousMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(Res.string.calendar_prev_month))
                        }
                        IconButton(onClick = { viewModel.resetToToday() }) {
                            Icon(Icons.Default.Today, contentDescription = stringResource(Res.string.calendar_today))
                        }
                        IconButton(onClick = { viewModel.onNextMonth() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(Res.string.calendar_next_month))
                        }
                    }
                )

                CalendarGrid(
                    viewMonth = viewMonth,
                    viewYear = viewYear,
                    selectedDate = uiState.selectedDate,
                    entries = uiState.entries,
                    onDateSelected = { viewModel.onDateSelected(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

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
    val firstDayOfWeek = LocalDateFormatter.current.firstDayOfWeek
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.ordinal - firstDayOfWeek.ordinal + 7) % 7
    val weekdays = (0..6).map { DayOfWeek.entries[(firstDayOfWeek.ordinal + it) % 7] }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = weekdayShortName(day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val totalCells = daysInMonth + dayOfWeekOffset
        val rows = (totalCells + 6) / 7
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date

        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { columnIndex ->
                    val dayIndex = rowIndex * 7 + columnIndex - dayOfWeekOffset + 1
                    if (dayIndex in 1..daysInMonth) {
                        val date = LocalDate(viewYear, viewMonth, dayIndex)
                        val dayEntries = entries.filter { it.task.at.toLocalDateTime(timeZone).date == date }
                        DayCell(
                            day = dayIndex,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            entries = dayEntries,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        CalendarLegend()
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    entries: List<CalendarTaskItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasOverdue = entries.any { (it.task as? CareTask.Pending)?.status == PendingStatus.OVERDUE }
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        hasOverdue -> MaterialTheme.colorScheme.error
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday || hasOverdue) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (entries.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    entries.map { careColor(it.task.type) }.distinct().forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .let { if (hasOverdue) it.border(1.dp, overdueColor(), CircleShape) else it }
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LegendItem(careColor(CareType.WATER), stringResource(Res.string.care_type_water))
        LegendItem(careColor(CareType.FERTILIZE), stringResource(Res.string.care_type_fertilize))
        LegendItem(careColor(CareType.REPOT), stringResource(Res.string.care_type_repot))
        LegendItem(
            color = MaterialTheme.colorScheme.surface,
            label = stringResource(Res.string.care_overdue_label),
            ringColor = overdueColor()
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, ringColor: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .let { if (ringColor != null) it.border(1.dp, ringColor, CircleShape) else it }
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    val dateFormatter = LocalDateFormatter.current
    val isOverdue = task.isOverdue
    val timeZone = TimeZone.currentSystemDefault()
    val isCompletable = task.isCompletableOn(Clock.System.now(), timeZone)
    val resolvedPlantName = plantName ?: stringResource(Res.string.calendar_unknown_plant)
    val typeColor = careColor(task.type)

    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        border = if (isOverdue) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(task.type.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = typeColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${getCareTypeString(task.type)} · $resolvedPlantName",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (isOverdue) overdueText(task) else dateFormatter.formatTime(task.dueAt, timeZone),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isOverdue) FontWeight.Medium else FontWeight.Normal
                )
            }
            if (isOverdue) {
                Icon(Icons.Default.Warning, contentDescription = stringResource(Res.string.care_overdue_label), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = { onMarkDone(task) }, enabled = isCompletable) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(Res.string.care_action_done),
                    tint = if (isCompletable) doneColor() else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
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
    val dateFormatter = LocalDateFormatter.current
    val resolvedPlantName = plantName ?: stringResource(Res.string.calendar_unknown_plant)
    val timeZone = TimeZone.currentSystemDefault()

    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(task.type.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = careColor(task.type))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${getCareTypeString(task.type)} · $resolvedPlantName",
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormatter.formatTime(task.performedAt, timeZone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Check, contentDescription = null, tint = doneColor(), modifier = Modifier.size(20.dp))
            IconButton(onClick = { onUndoTask(task) }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(Res.string.care_action_undo))
            }
        }
    }
}

/**
 * How far behind an overdue task is, counted from the day it was first owed rather than from the
 * last care — the same measure the reminder notification uses, so both say the same number.
 */
@Composable
private fun overdueText(task: CareTask.Pending): String {
    val timeZone = TimeZone.currentSystemDefault()
    val daysLate = task.dueAt.daysUntil(Clock.System.now(), timeZone).coerceAtLeast(0)
    val lateness = if (daysLate == 0) {
        stringResource(Res.string.care_overdue_today)
    } else {
        pluralStringResource(Res.plurals.care_overdue_days, daysLate, daysLate)
    }
    return if (task.missedCount > 1) {
        "$lateness  ${stringResource(Res.string.care_overdue_missed_count, task.missedCount)}"
    } else {
        lateness
    }
}
