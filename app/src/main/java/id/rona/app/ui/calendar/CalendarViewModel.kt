package id.rona.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.domain.model.PeriodRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isPeriodActual: Boolean = false,
    val isPeriodOngoing: Boolean = false,
    val periodId: Long? = null,
    val isPredicted: Boolean = false,
    val hasLog: Boolean = false,
)

data class CalendarUiState(
    val isLoading: Boolean = true,
    val yearMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val selectedDay: LocalDate? = null,
    val selectedPeriodId: Long? = null,
    val prediction: CyclePrediction? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val periodRecordRepository: PeriodRecordRepository,
    private val dailyLogDao: DailyLogDao,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        periodRecordRepository.observeAllPeriods(),
        dailyLogDao.observeAll(),
        _yearMonth,
        _selectedDay,
    ) { periods, logs, yearMonth, selectedDay ->
        val days = buildDays(yearMonth, periods, logs)
        val selectedCell = days.firstOrNull { it.date == selectedDay }
        CalendarUiState(
            isLoading = false,
            yearMonth = yearMonth,
            days = days,
            selectedDay = selectedDay,
            selectedPeriodId = selectedCell?.periodId,
            prediction = CycleEngine.predict(periods.map { it.startDate }),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _yearMonth.value = _yearMonth.value.plusMonths(1)
    }

    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
    }

    fun dismissDayDetail() {
        _selectedDay.value = null
    }

    private fun buildDays(
        yearMonth: YearMonth,
        periods: List<PeriodRecord>,
        logs: List<DailyLogEntity>,
    ): List<CalendarDay> {
        val firstOfMonth = yearMonth.atDay(1)
        val start = monthStartDay(yearMonth)
        val totalDays = 42

        val periodDays = periods.flatMap { period ->
            val startDay = period.startDate
            val endDay = period.endDate ?: LocalDate.now()
            (startDay.toEpochDay()..endDay.toEpochDay()).map { day ->
                day to period
            }
        }.toMap()

        val prediction = CycleEngine.predict(periods.map { it.startDate })
        val predictedRange = prediction?.let {
            it.rangeLow.toEpochDay()..it.rangeHigh.toEpochDay()
        }

        val logDays = logs.map { it.dateEpochDay }.toSet()

        return (0 until totalDays).map { index ->
            val date = start.plusDays(index.toLong())
            val period = periodDays[date.toEpochDay()]
            CalendarDay(
                date = date,
                inCurrentMonth = YearMonth.from(date) == yearMonth,
                isPeriodActual = period != null,
                isPeriodOngoing = period?.endDate == null,
                periodId = period?.id,
                isPredicted = predictedRange?.contains(date.toEpochDay()) == true && period == null,
                hasLog = date.toEpochDay() in logDays,
            )
        }
    }
}
