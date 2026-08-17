package id.rona.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.util.PrivacyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    val prediction: CyclePrediction? = null,
    val totalPeriods: Int = 0,
    val totalLogs: Int = 0,
)

/**
 * Explicit dashboard states. There is NO indefinite loading: after the first
 * upstream emission the UI is always in exactly one of these states.
 * Lock/onboarding gating lives outside this model by design.
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val periodRecordDao: PeriodRecordDao,
    private val dailyLogDao: DailyLogDao,
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = refreshTick
        .flatMapLatest { tick ->
            combine(
                periodRecordDao.observeAll(),
                dailyLogDao.observeAll(),
            ) { periods, logs ->
                periods.toHomeState(logs.size)
            }
                .onStart { if (tick > 0) emit(HomeUiState.Loading) }
        }
        .catch { e ->
            if (e is CancellationException) throw e
            // Sanitized: exception class only. No data, no dates, no notes.
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

    private fun List<PeriodRecordEntity>.toHomeState(logCount: Int): HomeUiState {
        if (isEmpty()) return HomeUiState.Empty

        val starts = map { LocalDate.ofEpochDay(it.startEpochDay) }
        return HomeUiState.Success(
            HomeData(
                cycleDay = CycleEngine.cycleDayFor(LocalDate.now(), starts),
                isPeriodActive = any { it.endEpochDay == null },
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
            val now = System.currentTimeMillis()
            val existing = periodRecordDao.getByStartDay(date.toEpochDay())
            if (existing != null) return@launch
            periodRecordDao.upsert(
                PeriodRecordEntity(
                    startEpochDay = date.toEpochDay(),
                    endEpochDay = null,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    fun endPeriod(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val ongoing = periodRecordDao.getOngoing() ?: return@launch
            periodRecordDao.upsert(
                ongoing.copy(
                    endEpochDay = date.toEpochDay(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
