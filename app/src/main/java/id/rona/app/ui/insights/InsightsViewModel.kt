package id.rona.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.insights.ProgressiveInsights
import id.rona.app.domain.insights.ProgressiveInsightsGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val progressiveInsights: ProgressiveInsights = ProgressiveInsightsGenerator.generate(emptyList(), null, 0),
    val cycleDay: Int? = null,
    val medianCycleLength: Int? = null,
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
        val symptomCounts = symptoms.groupingBy { it.symptomType }.eachCount()
        val cycleDay = CycleEngine.cycleDayFor(LocalDate.now(), starts)
        val medianCycleLength = CycleEngine.recentValidCycleLengths(starts)
            .takeIf { it.isNotEmpty() }
            ?.let(CycleEngine::median)
        val progressive = ProgressiveInsightsGenerator.generate(
            periodStarts = starts,
            cycleDay = cycleDay,
            dailyLogCount = logs.size,
            symptomCounts = symptomCounts,
        )

        InsightsUiState(
            isLoading = false,
            progressiveInsights = progressive,
            cycleDay = cycleDay,
            medianCycleLength = medianCycleLength,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(),
    )
}
