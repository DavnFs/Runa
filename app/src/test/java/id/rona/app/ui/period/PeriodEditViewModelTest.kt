package id.rona.app.ui.period

import com.google.common.truth.Truth.assertThat
import id.rona.app.data.db.TransactionRunner
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.CyclePredictionEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.ui.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePeriodDao: InMemoryPeriodDao
    private lateinit var fakePredictionDao: InMemoryPredictionDao
    private lateinit var repository: PeriodRecordRepository
    private lateinit var viewModel: PeriodEditViewModel

    @Before
    fun setUp() {
        fakePeriodDao = InMemoryPeriodDao()
        fakePredictionDao = InMemoryPredictionDao()
        repository = PeriodRecordRepository(
            transactionRunner = object : TransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T = block()
            },
            periodRecordDao = fakePeriodDao,
            cyclePredictionDao = fakePredictionDao,
        )
        viewModel = PeriodEditViewModel(repository)
    }

    @Test
    fun `testTurningOffOngoingRequiresEndDate`() = runTest {
        viewModel.initialize(periodId = null, initialDate = LocalDate.of(2026, 8, 1))

        // Toggle ongoing off and explicitly set endDate to null
        viewModel.setOngoing(false)
        viewModel.setEndDate(null)

        // Attempt save without end date
        viewModel.requestSave()

        val state = viewModel.uiState.value
        assertThat(state.isSavedSuccessfully).isFalse()
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.errorMessage).contains("Pilih tanggal selesai")
    }

    @Test
    fun `testOngoingPeriodInitializesWithNullEndDate`() = runTest {
        viewModel.initialize(periodId = null, initialDate = LocalDate.of(2026, 8, 1))

        val state = viewModel.uiState.value
        assertThat(state.isOngoing).isTrue()
        assertThat(state.endDate).isNull()
    }

    @Test
    fun `testValidSaveUpdatesState`() = runTest {
        viewModel.initialize(periodId = null, initialDate = LocalDate.of(2026, 8, 1))
        viewModel.setOngoing(false)
        viewModel.setEndDate(LocalDate.of(2026, 8, 5))

        viewModel.requestSave()

        val state = viewModel.uiState.value
        assertThat(state.isSavedSuccessfully).isTrue()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `testOngoingPeriodPersistsNullEndDate`() = runTest {
        viewModel.initialize(periodId = null, initialDate = LocalDate.of(2026, 8, 1))
        viewModel.setStartDate(LocalDate.of(2026, 8, 1))
        viewModel.setOngoing(true)

        viewModel.requestSave()

        val state = viewModel.uiState.value
        assertThat(state.isSavedSuccessfully).isTrue()
        val saved = fakePeriodDao.getAll().single()
        assertThat(saved.endEpochDay).isNull()
        assertThat(LocalDate.ofEpochDay(saved.startEpochDay)).isEqualTo(LocalDate.of(2026, 8, 1))
    }

    @Test
    fun `testEditingExistingCompletedPeriodToOngoingClearsEndDate`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        viewModel.initialize(periodId = 1L, initialDate = null)
        val loaded = viewModel.uiState.value
        assertThat(loaded.endDate).isEqualTo(LocalDate.of(2026, 8, 5))
        assertThat(loaded.isOngoing).isFalse()

        viewModel.setOngoing(true)

        val toggled = viewModel.uiState.value
        assertThat(toggled.endDate).isNull()
        assertThat(toggled.isOngoing).isTrue()

        viewModel.requestSave()

        val state = viewModel.uiState.value
        assertThat(state.isSavedSuccessfully).isTrue()
        val saved = fakePeriodDao.getAll().single()
        assertThat(saved.endEpochDay).isNull()
    }

    private class InMemoryPeriodDao : PeriodRecordDao {
        private val records = mutableMapOf<Long, PeriodRecordEntity>()
        private val state = MutableStateFlow<List<PeriodRecordEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<PeriodRecordEntity>> = state
        override suspend fun getAll(): List<PeriodRecordEntity> = records.values.sortedBy { it.startEpochDay }
        override suspend fun getById(id: Long): PeriodRecordEntity? = records[id]
        override suspend fun getByStartDay(startEpochDay: Long): PeriodRecordEntity? =
            records.values.firstOrNull { it.startEpochDay == startEpochDay }
        override suspend fun getOngoing(): PeriodRecordEntity? =
            records.values.firstOrNull { it.endEpochDay == null }
        override suspend fun getActiveOn(epochDay: Long): PeriodRecordEntity? =
            records.values.firstOrNull {
                it.startEpochDay <= epochDay && (it.endEpochDay == null || it.endEpochDay >= epochDay)
            }
        override suspend fun count(): Int = records.size
        override suspend fun upsert(record: PeriodRecordEntity): Long {
            val id = if (record.id != 0L) record.id else nextId++
            val saved = record.copy(id = id)
            records[id] = saved
            state.value = records.values.sortedBy { it.startEpochDay }
            return id
        }
        override suspend fun deleteById(id: Long) {
            records.remove(id)
            state.value = records.values.sortedBy { it.startEpochDay }
        }
        override suspend fun deleteAll() {
            records.clear()
            state.value = emptyList()
        }
    }

    private class InMemoryPredictionDao : CyclePredictionDao {
        private var latest: CyclePredictionEntity? = null
        private val state = MutableStateFlow<CyclePredictionEntity?>(null)

        override fun observeLatest(): Flow<CyclePredictionEntity?> = state
        override suspend fun getLatest(): CyclePredictionEntity? = latest
        override suspend fun upsert(prediction: CyclePredictionEntity): Long {
            latest = prediction
            state.value = prediction
            return prediction.id
        }
        override suspend fun trimTo(keep: Int) = Unit
        override suspend fun deleteAll() {
            latest = null
            state.value = null
        }
    }
}
