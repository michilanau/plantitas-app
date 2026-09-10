package org.mlanau.project.plant.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.presentation.component.CareTypeBadge
import org.mlanau.project.plant.presentation.component.careColor
import org.mlanau.project.plant.presentation.component.dueLabel
import org.mlanau.project.plant.presentation.component.getCareTypeString
import org.mlanau.project.plant.presentation.component.overdueColor
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.LocalDateFormatter
import org.mlanau.project.shared.ui.component.AppCard
import org.mlanau.project.shared.ui.component.AppCardTone
import org.mlanau.project.shared.ui.component.AppSnackbarHost
import org.mlanau.project.shared.ui.component.RoundIconButton
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.component.SectionLabel
import org.mlanau.project.shared.ui.monthName
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.ScreenGutter
import org.mlanau.project.shared.ui.theme.TabBarClearance
import org.mlanau.project.shared.ui.weekdayShortName
import plantitas_app.shared.generated.resources.*

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToPlantDetail: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    uiState.error?.let { error ->
        val errorMessage = error.localizedMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    val timeZone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    val selectedDateEntries = uiState.entries.filter {
        it.task.at.toLocalDateTime(timeZone).date == uiState.selectedDate
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppSnackbarHost(snackbarHostState, Modifier.padding(bottom = TabBarClearance)) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = ContentMaxWidth)) {
                ScreenHeader(
                    title = monthName(uiState.viewMonth),
                    eyebrow = "${stringResource(Res.string.nav_calendar)} · ${uiState.viewYear}",
                    actions = {
                        if (uiState.viewMonth != today.month || uiState.viewYear != today.year) {
                            RoundIconButton(
                                icon = Res.drawable.ic_calendar,
                                contentDescription = stringResource(Res.string.calendar_today),
                                onClick = { viewModel.resetToToday() },
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                        }
                        RoundIconButton(
                            icon = Res.drawable.ic_back,
                            contentDescription = stringResource(Res.string.calendar_prev_month),
                            onClick = { viewModel.onPreviousMonth() },
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        RoundIconButton(
                            icon = Res.drawable.ic_chevron_right,
                            contentDescription = stringResource(Res.string.calendar_next_month),
                            onClick = { viewModel.onNextMonth() },
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                    }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = TabBarClearance),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        CalendarGrid(
                            viewMonth = uiState.viewMonth,
                            viewYear = uiState.viewYear,
                            selectedDate = uiState.selectedDate,
                            today = today,
                            entries = uiState.entries,
                            onDateSelected = { viewModel.onDateSelected(it) }
                        )
                    }
                    item {
                        SectionLabel(
                            text = stringResource(
                                Res.string.calendar_selected_date,
                                uiState.selectedDate.day,
                                monthName(uiState.selectedDate.month)
                            ),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    when {
                        uiState.isLoading -> item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        selectedDateEntries.isEmpty() -> item {
                            Text(
                                text = stringResource(Res.string.calendar_no_tasks),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> items(selectedDateEntries) { entry ->
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
    today: LocalDate,
    entries: List<CalendarTaskItem>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = LocalDate(viewYear, viewMonth, 1)
    val daysInMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    val firstDayOfWeek = LocalDateFormatter.current.firstDayOfWeek
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.ordinal - firstDayOfWeek.ordinal + 7) % 7
    val weekdays = (0..6).map { DayOfWeek.entries[(firstDayOfWeek.ordinal + it) % 7] }
    val timeZone = TimeZone.currentSystemDefault()
    val entriesByDate = remember(entries) { entries.groupBy { it.task.at.toLocalDateTime(timeZone).date } }
    val rows = (daysInMonth + dayOfWeekOffset + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            weekdays.forEach { day ->
                Text(
                    text = weekdayShortName(day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(7) { columnIndex ->
                    val dayIndex = rowIndex * 7 + columnIndex - dayOfWeekOffset + 1
                    if (dayIndex in 1..daysInMonth) {
                        val date = LocalDate(viewYear, viewMonth, dayIndex)
                        DayCell(
                            day = dayIndex,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            entries = entriesByDate[date].orEmpty(),
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(48.dp))
                    }
                }
            }
        }
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
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val hasOverdue = entries.any { (it.task as? CareTask.Pending)?.isOverdue == true }
    val dotColors = entries.map { entry ->
        if ((entry.task as? CareTask.Pending)?.isOverdue == true) overdueColor() else careColor(entry.task.type)
    }.distinct().take(3)

    Column(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (isSelected) colors.primary else Color.Transparent)
            .then(if (isToday) Modifier.border(2.dp, colors.primaryContainer, shape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday || hasOverdue) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                isSelected -> colors.onPrimary
                hasOverdue -> overdueColor()
                else -> colors.onSurface
            }
        )
        Row(
            modifier = Modifier.padding(top = 3.dp).height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            dotColors.forEach { color ->
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
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
    val plantName = entry.plantName ?: stringResource(Res.string.calendar_unknown_plant)
    when (val task = entry.task) {
        is CareTask.Pending -> PendingRow(task, plantName, onMarkDone, onClick)
        is CareTask.Done -> DoneRow(task, plantName, onUndoTask, onClick)
    }
}

@Composable
private fun PendingRow(
    task: CareTask.Pending,
    plantName: String,
    onMarkDone: (CareTask.Pending) -> Unit,
    onClick: () -> Unit
) {
    val dateFormatter = LocalDateFormatter.current
    val colors = MaterialTheme.colorScheme
    val isOverdue = task.isOverdue
    val isCompletable = task.isCompletableOn(Clock.System.now(), TimeZone.currentSystemDefault())

    AppCard(
        tone = if (isOverdue) AppCardTone.Alert else AppCardTone.Default,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CareTypeBadge(type = task.type, overdue = isOverdue)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${getCareTypeString(task.type)} · $plantName",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isOverdue) dueLabel(task) else dateFormatter.formatTime(task.dueAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) overdueColor() else colors.onSurfaceVariant,
                    fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            RoundIconButton(
                icon = Res.drawable.ic_check,
                contentDescription = stringResource(Res.string.care_action_done),
                onClick = { onMarkDone(task) },
                size = 42.dp,
                iconSize = 20.dp,
                enabled = isCompletable,
                containerColor = if (isCompletable) colors.primary else colors.surfaceContainer,
                contentColor = if (isCompletable) colors.onPrimary else colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoneRow(
    task: CareTask.Done,
    plantName: String,
    onUndoTask: (CareTask.Done) -> Unit,
    onClick: () -> Unit
) {
    val dateFormatter = LocalDateFormatter.current
    val colors = MaterialTheme.colorScheme

    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CareTypeBadge(type = task.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${getCareTypeString(task.type)} · $plantName",
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.calendar_done_at, dateFormatter.formatTime(task.performedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            RoundIconButton(
                icon = Res.drawable.ic_undo,
                contentDescription = stringResource(Res.string.care_action_undo),
                onClick = { onUndoTask(task) },
                size = 42.dp,
                iconSize = 20.dp,
                containerColor = colors.surfaceContainer,
                contentColor = colors.onSurfaceVariant
            )
        }
    }
}
