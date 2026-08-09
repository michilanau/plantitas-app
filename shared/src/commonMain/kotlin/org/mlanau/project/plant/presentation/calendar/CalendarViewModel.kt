package org.mlanau.project.plant.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.mlanau.project.plant.application.CareEventWithPlantName
import org.mlanau.project.plant.application.GetCalendarEvents
import org.mlanau.project.plant.application.ToggleCareEventStatus
import org.mlanau.project.plant.domain.model.*

data class CalendarUiState(
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val viewMonth: Month = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).month,
    val viewYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val events: List<CareEventWithPlantName> = emptyList(),
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val getCalendarEvents: GetCalendarEvents,
    private val toggleCareEventStatus: ToggleCareEventStatus
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadEventsForView()
    }

    private fun loadEventsForView() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val fromLocalDate = LocalDate(_uiState.value.viewYear, _uiState.value.viewMonth, 1).minus(7, DateTimeUnit.DAY)
            val toLocalDate = LocalDate(_uiState.value.viewYear, _uiState.value.viewMonth, 1).plus(1, DateTimeUnit.MONTH).plus(7, DateTimeUnit.DAY)

            val timeZone = TimeZone.currentSystemDefault()
            val fromInstant = fromLocalDate.atStartOfDayIn(timeZone)
            val toInstant = toLocalDate.atTime(LocalTime(23, 59, 59)).toInstant(timeZone)

            getCalendarEvents(fromInstant, toInstant).collect { events ->
                _uiState.update { it.copy(events = events, isLoading = false) }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onPreviousMonth() {
        _uiState.update {
            val currentView = LocalDate(it.viewYear, it.viewMonth, 1).minus(1, DateTimeUnit.MONTH)
            it.copy(viewMonth = currentView.month, viewYear = currentView.year)
        }
        loadEventsForView()
    }

    fun onNextMonth() {
        _uiState.update {
            val currentView = LocalDate(it.viewYear, it.viewMonth, 1).plus(1, DateTimeUnit.MONTH)
            it.copy(viewMonth = currentView.month, viewYear = currentView.year)
        }
        loadEventsForView()
    }

    fun resetToToday() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        _uiState.update {
            it.copy(
                selectedDate = now.date,
                viewMonth = now.month,
                viewYear = now.year
            )
        }
        loadEventsForView()
    }

    fun toggleEventStatus(event: CareEvent) {
        viewModelScope.launch {
            toggleCareEventStatus(event)
        }
    }
}
