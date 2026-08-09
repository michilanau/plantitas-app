package org.mlanau.project.plant.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.mlanau.project.plant.application.GenerateCareEvents
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository

data class CareEventWithPlant(
    val event: CareEvent,
    val plantName: String
)

data class CalendarUiState(
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val viewMonth: Month = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).month,
    val viewYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val events: List<CareEventWithPlant> = emptyList(),
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val generateCareEvents: GenerateCareEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        // Initial load
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

            combine(
                careRepository.getAllCareRules(),
                careRepository.getEventsInRange(fromInstant, toInstant),
                plantRepository.findAll()
            ) { rules, persistedEvents, plants ->
                generateCareEvents.generate(
                    rules = rules,
                    persistedEvents = persistedEvents,
                    from = fromInstant,
                    until = toInstant
                ).map { event ->
                    CareEventWithPlant(
                        event = event,
                        plantName = plants.find { it.id == event.plantId }?.name ?: "Planta desconocida"
                    )
                }
            }.collect { events ->
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
            val newStatus = if (event.status == CareEventStatus.DONE) CareEventStatus.PENDING else CareEventStatus.DONE
            val completedAt = if (newStatus == CareEventStatus.DONE) Clock.System.now() else null
            
            if (event.id != null) {
                careRepository.updateEventStatus(event.id!!, newStatus, completedAt)
            } else {
                val materializedEvent = when (event) {
                    is WaterCareEvent -> event.copy(status = newStatus, completedAt = completedAt)
                    is FertilizeCareEvent -> event.copy(status = newStatus, completedAt = completedAt)
                    is RepotCareEvent -> event.copy(status = newStatus, completedAt = completedAt)
                }
                careRepository.saveCareEvent(materializedEvent)
            }
        }
    }
}
