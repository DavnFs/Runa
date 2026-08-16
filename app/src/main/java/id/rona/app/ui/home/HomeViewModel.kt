package id.rona.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val cycleDay: Int? = null,
    val isPeriodActive: Boolean = false,
    val ongoingPeriod: PeriodRecordEntity? = null,
    val prediction: CyclePrediction? = null,
    val totalPeriods: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val periodRecordDao: PeriodRecordDao,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    private val data = periodRecordDao.observeAll()
        .map { records ->
            val prediction = CycleEngine.predict(
                records.map { LocalDate.ofEpochDay(it.startEpochDay) }
            )
            HomeUiState(
                isLoading = false,
                cycleDay = CycleEngine.cycleDayFor(
                    LocalDate.now(),
                    records.map { LocalDate.ofEpochDay(it.startEpochDay) },
                ),
                isPeriodActive = records.any { it.endEpochDay == null },
                ongoingPeriod = records.lastOrNull { it.endEpochDay == null },
                prediction = prediction,
                totalPeriods = records.size,
            )
        }

    val uiState: StateFlow<HomeUiState> = combine(data, _isLoading, _error) { state, loading, error ->
        state.copy(isLoading = loading || state.isLoading, error = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

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
}
