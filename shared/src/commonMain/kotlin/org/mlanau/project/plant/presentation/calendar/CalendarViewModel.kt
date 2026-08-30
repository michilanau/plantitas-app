package org.mlanau.project.plant.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.mlanau.project.plant.application.CompleteCareTask
import org.mlanau.project.plant.application.DeleteCareTask
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.GetCalendarTasks
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.presentation.CareToast
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.toUiError
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

data class CalendarTaskItem(val task: CareTask, val plantName: String?)

data class CalendarUiState(
    val selectedDate: LocalDate,
    val viewMonth: Month,
    val viewYear: Int,
    val entries: List<CalendarTaskItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)

class CalendarViewModel(
    private val getCalendarTasks: GetCalendarTasks,
    private val findAllPlants: FindAllPlants,
    private val completeCareTask: CompleteCareTask,
    private val deleteCareTask: DeleteCareTask,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) : ViewModel() {

    private fun today(): LocalDateTime = clock.now().toLocalDateTime(timeZoneProvider())

    private val _uiState = MutableStateFlow(today().let {
        CalendarUiState(selectedDate = it.date, viewMonth = it.month, viewYear = it.year)
    })
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _toasts = Channel<CareToast>(Channel.BUFFERED)
    val toasts = _toasts.receiveAsFlow()

    private var loadEntriesJob: Job? = null

    init {
        loadEntriesForView()
    }

    private fun loadEntriesForView() {
        // Cancel the previous collector before starting a new one, otherwise switching months
        // repeatedly leaves every earlier collect() running forever, each still overwriting
        // uiState.entries with its own (increasingly stale) month range.
        loadEntriesJob?.cancel()
        loadEntriesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val fromLocalDate = LocalDate(_uiState.value.viewYear, _uiState.value.viewMonth, 1).minus(7, DateTimeUnit.DAY)
            val toLocalDate = LocalDate(_uiState.value.viewYear, _uiState.value.viewMonth, 1).plus(1, DateTimeUnit.MONTH).plus(7, DateTimeUnit.DAY)

            val timeZone = timeZoneProvider()
            val fromInstant = fromLocalDate.atStartOfDayIn(timeZone)
            val toInstant = toLocalDate.atTime(LocalTime(23, 59, 59)).toInstant(timeZone)

            combine(getCalendarTasks(fromInstant, toInstant), findAllPlants()) { tasks, plants ->
                val plantNameById = plants.associate { it.id to it.name }
                tasks.map { CalendarTaskItem(it, plantNameById[it.plantId]) }
            }.collect { entries ->
                _uiState.update { it.copy(entries = entries, isLoading = false) }
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
        loadEntriesForView()
    }

    fun onNextMonth() {
        _uiState.update {
            val currentView = LocalDate(it.viewYear, it.viewMonth, 1).plus(1, DateTimeUnit.MONTH)
            it.copy(viewMonth = currentView.month, viewYear = currentView.year)
        }
        loadEntriesForView()
    }

    fun resetToToday() {
        val now = today()
        _uiState.update {
            it.copy(
                selectedDate = now.date,
                viewMonth = now.month,
                viewYear = now.year
            )
        }
        loadEntriesForView()
    }

    private fun <T> Result<T>.publishErrorIfAny() {
        onFailure { exception -> _uiState.update { it.copy(error = exception.toUiError()) } }
    }

    fun onMarkDone(pending: CareTask.Pending) {
        viewModelScope.launch {
            completeCareTask(pending)
                .onSuccess { _toasts.send(CareToast.Logged(it)) }
                .publishErrorIfAny()
        }
    }

    fun onUndoTask(done: CareTask.Done) {
        viewModelScope.launch {
            done.id?.let { id ->
                deleteCareTask(id)
                    .onSuccess { _toasts.send(CareToast.Undone) }
                    .publishErrorIfAny()
            }
        }
    }

    /** UNDO of the snackbar shown after [onMarkDone]. */
    fun onUndoLoggedCare(taskId: CareTaskId) {
        viewModelScope.launch {
            deleteCareTask(taskId).publishErrorIfAny()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
