package org.mlanau.project.plant.presentation.calendar

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import kotlin.time.Clock
import kotlinx.datetime.*
import org.mlanau.project.plant.application.CareEventWithPlantName
import org.mlanau.project.plant.domain.model.*
import org.jetbrains.compose.resources.stringResource
import plantitas_app.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewMonth = uiState.viewMonth
    val viewYear = uiState.viewYear

    Scaffold(
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
                events = uiState.events.map { it.event },
                onDateSelected = { viewModel.onDateSelected(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val timeZone = TimeZone.currentSystemDefault()
            val selectedDateEvents = uiState.events.filter {
                it.event.scheduledAt.toLocalDateTime(timeZone).date == uiState.selectedDate
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedDateEvents.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.calendar_no_tasks))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDateEvents) { eventWithPlant ->
                        EventItem(
                            eventWithPlant = eventWithPlant,
                            onToggleStatus = { viewModel.toggleEventStatus(eventWithPlant.event) }
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
    events: List<CareEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = getDaysInMonth(viewMonth, viewYear)
    val firstDayOfMonth = LocalDate(viewYear, viewMonth, 1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.ordinal) % 7

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("L", "M", "X", "J", "V", "S", "D")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
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
                        val hasEvents = events.any { it.scheduledAt.toLocalDateTime(timeZone).date == date }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else if (isToday) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasEvents) {
                                    val eventColors = events.filter { it.scheduledAt.toLocalDateTime(timeZone).date == date }
                                        .map { getEventColor(it) }.distinct()
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        eventColors.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
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

private fun getDaysInMonth(month: Month, year: Int): Int {
    return when (month) {
        Month.FEBRUARY -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
}

private fun getEventColor(event: CareEvent): Color = when (event) {
    is WaterCareEvent -> Color(0xFF2196F3)
    is FertilizeCareEvent -> Color(0xFF4CAF50)
    is RepotCareEvent -> Color(0xFF795548)
}

@Composable
private fun EventItem(
    eventWithPlant: CareEventWithPlantName,
    onToggleStatus: () -> Unit
) {
    val event = eventWithPlant.event
    val resolvedPlantName = eventWithPlant.plantName ?: stringResource(Res.string.calendar_unknown_plant)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggleStatus() },
        colors = CardDefaults.cardColors(
            containerColor = if (event.status == CareEventStatus.DONE)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(getEventColor(event), shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val timeZone = TimeZone.currentSystemDefault()
                val localDateTime = event.scheduledAt.toLocalDateTime(timeZone)
                Text(
                    text = "${when (event) {
                        is WaterCareEvent -> stringResource(Res.string.care_type_water)
                        is FertilizeCareEvent -> stringResource(Res.string.care_type_fertilize)
                        is RepotCareEvent -> stringResource(Res.string.care_type_repot)
                    }} - $resolvedPlantName",
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (event.status == CareEventStatus.DONE)
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                        else null
                )
                Text(
                    text = "${localDateTime.time.hour}:${localDateTime.time.minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(
                checked = event.status == CareEventStatus.DONE,
                onCheckedChange = { onToggleStatus() }
            )
        }
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
