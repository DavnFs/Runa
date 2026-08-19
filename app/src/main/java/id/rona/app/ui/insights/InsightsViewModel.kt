package id.rona.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.engine.PeriodSpan
import id.rona.app.domain.engine.StatisticsEngine
import id.rona.app.domain.engine.SymptomFrequency
import id.rona.app.domain.model.SymptomType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.model.PeriodRecord

data class InsightsUiState(
    val isLoading: Boolean = true,
    val avgCycleLength: Double? = null,
    val medianCycleLength: Int? = null,
    val madCycleDays: Double? = null,
    val avgPeriodDuration: Double? = null,
    val medianPeriodDuration: Int? = null,
    val mostFrequentSymptoms: List<SymptomFrequency> = emptyList(),
    val phaseDistribution: Map<CyclePhase, Int> = emptyMap(),
    val totalLogs: Int = 0,
    val totalPeriods: Int = 0,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val periodRecordRepository: PeriodRecordRepository,
    private val dailyLogDao: DailyLogDao,
    private val symptomLogDao: SymptomLogDao,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        periodRecordRepository.observeAllPeriods(),
        dailyLogDao.observeAll(),
        symptomLogDao.observeAll(),
    ) { periods, logs, symptoms ->
        val starts = periods.map { it.startDate }.distinct().sorted()
        val cycleLengths = CycleEngine.recentValidCycleLengths(starts)
        val median = cycleLengths.takeIf { it.isNotEmpty() }?.let { CycleEngine.median(it) }
        val mad = median?.let { CycleEngine.medianAbsoluteDeviation(cycleLengths, it) }

        val durations = periods.map { period ->
            PeriodSpan(period.startDate.toEpochDay(), period.endDate?.toEpochDay())
        }

        val symptomCounts = symptoms
            .groupingBy { it.symptomType }
            .eachCount()

        val logDates = logs.map { LocalDate.ofEpochDay(it.dateEpochDay) }
        val phaseDistribution = median?.let { m ->
            val cycleDays = logDates.mapNotNull { logDate ->
                CycleEngine.cycleDayFor(logDate, starts)?.takeIf { it <= m }
            }
            if (cycleDays.isNotEmpty()) {
                StatisticsEngine.phaseDistribution(cycleDays, m)
            } else {
                emptyMap()
            }
        } ?: emptyMap()

        InsightsUiState(
            isLoading = false,
            avgCycleLength = cycleLengths.takeIf { it.isNotEmpty() }?.average(),
            medianCycleLength = median,
            madCycleDays = mad,
            avgPeriodDuration = StatisticsEngine.averagePeriodDurationDays(durations),
            medianPeriodDuration = StatisticsEngine.medianPeriodDurationDays(durations),
            mostFrequentSymptoms = StatisticsEngine.mostFrequentSymptoms(symptomCounts),
            phaseDistribution = phaseDistribution,
            totalLogs = logs.size,
            totalPeriods = periods.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(),
    )
}
