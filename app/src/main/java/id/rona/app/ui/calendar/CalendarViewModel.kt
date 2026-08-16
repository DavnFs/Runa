package id.rona.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
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
    val isPredicted: Boolean = false,
    val hasLog: Boolean = false,
)

data class CalendarUiState(
    val isLoading: Boolean = true,
    val yearMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val selectedDay: LocalDate? = null,
    val prediction: CyclePrediction? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val periodRecordDao: PeriodRecordDao,
    private val dailyLogDao: DailyLogDao,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        periodRecordDao.observeAll(),
        dailyLogDao.observeAll(),
        _yearMonth,
        _selectedDay,
    ) { periods, logs, yearMonth, selectedDay ->
        CalendarUiState(
            isLoading = false,
            yearMonth = yearMonth,
            days = buildDays(yearMonth, periods, logs),
            selectedDay = selectedDay,
            prediction = CycleEngine.predict(
                periods.map { LocalDate.ofEpochDay(it.startEpochDay) }
            ),
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

    fun upsertPeriod(start: LocalDate, end: LocalDate?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = periodRecordDao.getByStartDay(start.toEpochDay())
            periodRecordDao.upsert(
                PeriodRecordEntity(
                    id = existing?.id ?: 0,
                    startEpochDay = start.toEpochDay(),
                    endEpochDay = end?.toEpochDay(),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        }
    }

    fun deletePeriod(id: Long) {
        viewModelScope.launch { periodRecordDao.deleteById(id) }
    }

    private fun buildDays(
        yearMonth: YearMonth,
        periods: List<PeriodRecordEntity>,
        logs: List<DailyLogEntity>,
    ): List<CalendarDay> {
        val firstOfMonth = yearMonth.atDay(1)
        val start = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
        val totalDays = 42

        val periodDays = periods.flatMap { period ->
            val startDay = LocalDate.ofEpochDay(period.startEpochDay)
            val endDay = period.endEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
            (period.startEpochDay..endDay.toEpochDay()).map { day ->
                day to period
            }
        }.toMap()

        val prediction = CycleEngine.predict(
            periods.map { LocalDate.ofEpochDay(it.startEpochDay) }
        )
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
                isPeriodOngoing = period?.endEpochDay == null,
                isPredicted = predictedRange?.contains(date.toEpochDay()) == true && period == null,
                hasLog = date.toEpochDay() in logDays,
            )
        }
    }
}
