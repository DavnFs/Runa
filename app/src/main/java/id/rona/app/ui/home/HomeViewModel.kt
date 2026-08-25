package id.rona.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.domain.model.PeriodRecord
import id.rona.app.util.PrivacyLogger
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
    val totalPeriods: Int = 0,
    val totalLogs: Int = 0,
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
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = refreshTick
        .flatMapLatest { tick ->
            combine(
                periodRecordRepository.observeAllPeriods(),
                dailyLogDao.observeAll(),
            ) { periods, logs ->
                periods.toHomeState(logs.size)
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

    private fun List<PeriodRecord>.toHomeState(logCount: Int): HomeUiState {
        if (isEmpty()) return HomeUiState.Empty

        val starts = map { it.startDate }
        val latest = maxByOrNull { it.startDate }
        return HomeUiState.Success(
            HomeData(
                cycleDay = CycleEngine.cycleDayFor(LocalDate.now(), starts),
                isPeriodActive = any { it.isOngoing },
                latestPeriod = latest,
                prediction = CycleEngine.predict(starts),
                totalPeriods = size,
                totalLogs = logCount,
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
