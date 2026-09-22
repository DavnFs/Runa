package id.rona.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.domain.engine.FertilityEstimator
import id.rona.app.domain.engine.FertilityWindow
import id.rona.app.domain.engine.phaseNameFor
import id.rona.app.domain.insights.CycleEducationProvider
import id.rona.app.domain.insights.CycleEducationTopic
import id.rona.app.domain.insights.InsightMaturityLevel
import id.rona.app.domain.insights.ProgressiveInsights
import id.rona.app.domain.insights.ProgressiveInsightsGenerator
import id.rona.app.domain.model.PeriodRecord
import id.rona.app.domain.model.SymptomType
import id.rona.app.util.PrivacyLogger
import kotlin.collections.groupingBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Data required by the Beranda dashboard once it has loaded. */
data class HomeData(
    val today: LocalDate = LocalDate.now(),
    val cycleDay: Int? = null,
    val isPeriodActive: Boolean = false,
    val latestPeriod: PeriodRecord? = null,
    val prediction: CyclePrediction? = null,
    val daysUntilNextPeriod: Int? = null,
    val fertilityWindow: FertilityWindow? = null,
    val phaseName: String? = null,
    val dailyInsight: CycleEducationTopic? = null,
    val totalPeriods: Int = 0,
    val totalLogs: Int = 0,
    val primaryInsight: ProgressiveInsights = ProgressiveInsightsGenerator.generate(emptyList(), null, 0),
)

/**
 * Explicit dashboard states.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(val homeData: HomeData) : HomeUiState

    /** No period records yet — onboarding done, tracking not started. */
    data object Empty : HomeUiState

    data class Error(
        val userMessage: String,
        val isRetryable: Boolean,
    ) : HomeUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val periodRecordRepository: PeriodRecordRepository,
    private val dailyLogDao: DailyLogDao,
    private val symptomLogDao: SymptomLogDao,
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = refreshTick
        .flatMapLatest { tick ->
            combine(
                periodRecordRepository.observeAllPeriods(),
                dailyLogDao.observeAll(),
                symptomLogDao.observeAll(),
            ) { periods, logs, symptoms ->
                val symptomCounts = symptoms.groupingBy { it.symptomType }.eachCount()
                periods.toHomeState(logs.size, symptomCounts)
            }
                .onStart { if (tick > 0) emit(HomeUiState.Loading) }
        }
        .catch { e ->
            if (e is CancellationException) throw e
            PrivacyLogger.e(TAG) { "home flow failed: ${e.javaClass.simpleName}" }
            emit(
                HomeUiState.Error(
                    userMessage = "Datamu tidak bisa dimuat sekarang.",
                    isRetryable = true,
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    private fun List<PeriodRecord>.toHomeState(
        logCount: Int,
        symptomCounts: Map<id.rona.app.domain.model.SymptomType, Int> = emptyMap(),
    ): HomeUiState {
        if (isEmpty()) return HomeUiState.Empty

        val today = LocalDate.now()
        val starts = map { it.startDate }
        val latest = maxByOrNull { it.startDate }
        val cycleDay = CycleEngine.cycleDayFor(today, starts)
        val prediction = CycleEngine.predict(starts)
        val isPeriodActive = any { it.isOngoing }
        val primaryInsight = ProgressiveInsightsGenerator.generate(
            periodStarts = starts,
            cycleDay = cycleDay,
            dailyLogCount = logCount,
            symptomCounts = symptomCounts,
            prediction = prediction,
        )
        val dailyInsight = CycleEducationProvider.phaseTopicForToday(
            cycleDay = cycleDay,
            cycleLengthDays = prediction?.medianCycleLengthDays,
            maturity = primaryInsight.maturity,
        ) ?: CycleEducationProvider.topicsForMaturity(primaryInsight.maturity)
            .firstOrNull { it.id == "edu_cycle_basics" }
            .takeIf { primaryInsight.maturity >= InsightMaturityLevel.LEVEL_1_SINGLE_START }

        return HomeUiState.Success(
            HomeData(
                cycleDay = cycleDay,
                isPeriodActive = isPeriodActive,
                latestPeriod = latest,
                prediction = prediction,
                daysUntilNextPeriod = prediction?.let {
                    (it.predictedStart.toEpochDay() - today.toEpochDay()).toInt()
                },
                fertilityWindow = prediction?.let { FertilityEstimator.estimate(it) },
                phaseName = phaseNameFor(isPeriodActive, cycleDay),
                dailyInsight = dailyInsight,
                totalPeriods = size,
                totalLogs = logCount,
                primaryInsight = primaryInsight,
            )
        )
    }

    fun retry() {
        refreshTick.value += 1
    }

    fun startPeriod(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val existing = periodRecordRepository.getAllPeriods()
            if (existing.any { it.isOngoing }) {
                PrivacyLogger.d(TAG) { "startPeriod ignored: an ongoing period already exists" }
                return@launch
            }
            periodRecordRepository.savePeriod(start = date, end = null)
        }
    }

    fun endPeriod(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val ongoing = periodRecordRepository.getAllPeriods().firstOrNull { it.isOngoing }
            if (ongoing != null) {
                periodRecordRepository.savePeriod(
                    start = ongoing.startDate,
                    end = date,
                    currentRecordId = ongoing.id,
                )
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
