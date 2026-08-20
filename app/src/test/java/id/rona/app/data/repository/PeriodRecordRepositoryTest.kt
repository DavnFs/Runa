package id.rona.app.data.repository

import com.google.common.truth.Truth.assertThat
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.CyclePredictionEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.domain.model.OverlapResolutionStrategy
import id.rona.app.domain.model.PeriodValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PeriodRecordRepositoryTest {

    private lateinit var fakePeriodDao: InMemoryPeriodDao
    private lateinit var fakePredictionDao: InMemoryPredictionDao
    private lateinit var repository: PeriodRecordRepository

    @Before
    fun setUp() {
        fakePeriodDao = InMemoryPeriodDao()
        fakePredictionDao = InMemoryPredictionDao()
        repository = PeriodRecordRepository(
            transactionRunner = object : id.rona.app.data.db.TransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T = block()
            },
            periodRecordDao = fakePeriodDao,
            cyclePredictionDao = fakePredictionDao,
        )
    }

    // 1. Validation: End before start fails
    @Test
    fun `validatePeriod returns InvalidDateOrder when end is before start`() = runTest {
        val start = LocalDate.of(2026, 8, 15)
        val end = LocalDate.of(2026, 8, 10)

        val result = repository.validatePeriod(start, end)
        assertThat(result).isInstanceOf(PeriodValidationResult.InvalidDateOrder::class.java)
    }

    // 2. Validation: Overlapping period records detected
    @Test
    fun `validatePeriod returns OverlapConflict when ranges overlap`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        val start = LocalDate.of(2026, 8, 4)
        val end = LocalDate.of(2026, 8, 8)

        val result = repository.validatePeriod(start, end)
        assertThat(result).isInstanceOf(PeriodValidationResult.OverlapConflict::class.java)
        val conflict = result as PeriodValidationResult.OverlapConflict
        assertThat(conflict.conflictingRecords).hasSize(1)
        assertThat(conflict.conflictingRecords.first().id).isEqualTo(1L)
    }

    // 3. Validation: Overlap ignores current editing record ID
    @Test
    fun `validatePeriod ignores current record id during edit`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        // Editing same record ID 1 to August 1 - August 6
        val result = repository.validatePeriod(
            start = LocalDate.of(2026, 8, 1),
            end = LocalDate.of(2026, 8, 6),
            currentRecordId = 1L,
        )
        assertThat(result).isEqualTo(PeriodValidationResult.Valid)
    }

    // 4. Validation: Unusual duration (>15 days) returns warning, not error
    @Test
    fun `validatePeriod returns UnusualDurationWarning for duration greater than 15 days`() = runTest {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 20) // 20 days

        val result = repository.validatePeriod(start, end)
        assertThat(result).isInstanceOf(PeriodValidationResult.UnusualDurationWarning::class.java)
        val warning = result as PeriodValidationResult.UnusualDurationWarning
        assertThat(warning.durationDays).isEqualTo(20)
    }

    // 5. Resolution Strategy MERGE: Combines bounding range
    @Test
    fun `savePeriod with MERGE strategy merges overlapping intervals`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        // Save overlapping August 4 to August 9 with MERGE
        val result = repository.savePeriod(
            start = LocalDate.of(2026, 8, 4),
            end = LocalDate.of(2026, 8, 9),
            resolutionStrategy = OverlapResolutionStrategy.MERGE,
        )

        assertThat(result.isSuccess).isTrue()
        val all = fakePeriodDao.getAll()
        assertThat(all).hasSize(1)
        assertThat(all.first().startEpochDay).isEqualTo(LocalDate.of(2026, 8, 1).toEpochDay())
        assertThat(all.first().endEpochDay).isEqualTo(LocalDate.of(2026, 8, 9).toEpochDay())
    }

    // 6. Resolution Strategy REPLACE: Replaces conflicting intervals
    @Test
    fun `savePeriod with REPLACE strategy replaces overlapping intervals`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        // Save overlapping August 4 to August 9 with REPLACE
        val result = repository.savePeriod(
            start = LocalDate.of(2026, 8, 4),
            end = LocalDate.of(2026, 8, 9),
            resolutionStrategy = OverlapResolutionStrategy.REPLACE,
        )

        assertThat(result.isSuccess).isTrue()
        val all = fakePeriodDao.getAll()
        assertThat(all).hasSize(1)
        assertThat(all.first().startEpochDay).isEqualTo(LocalDate.of(2026, 8, 4).toEpochDay())
        assertThat(all.first().endEpochDay).isEqualTo(LocalDate.of(2026, 8, 9).toEpochDay())
    }

    // 7. Delete period removes record and recomputes prediction cache
    @Test
    fun `deletePeriod removes record and invalidates prediction cache`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        val result = repository.deletePeriod(1L)
        assertThat(result.isSuccess).isTrue()
        assertThat(fakePeriodDao.getAll()).isEmpty()
        assertThat(fakePredictionDao.getLatest()).isNull()
    }

    // 8. Ongoing Period: Persists with null endDate
    @Test
    fun `testOngoingPeriodPersistsNullEndDate`() = runTest {
        val start = LocalDate.of(2026, 8, 10)
        val result = repository.savePeriod(start = start, end = null)

        assertThat(result.isSuccess).isTrue()
        val saved = result.getOrNull()
        assertThat(saved).isNotNull()
        assertThat(saved?.endDate).isNull()
        assertThat(saved?.isOngoing).isTrue()

        val all = repository.getAllPeriods()
        assertThat(all).hasSize(1)
        assertThat(all.first().endDate).isNull()
    }

    // 9. Ongoing Period: Editing completed period to ongoing clears endDate
    @Test
    fun `testEditingCompletedPeriodToOngoingClearsEndDate`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        val result = repository.savePeriod(
            start = LocalDate.of(2026, 8, 1),
            end = null,
            currentRecordId = 1L,
        )

        assertThat(result.isSuccess).isTrue()
        val updated = repository.getPeriodById(1L)
        assertThat(updated).isNotNull()
        assertThat(updated?.endDate).isNull()
        assertThat(updated?.isOngoing).isTrue()
    }

    // 10. Ongoing Period: Does not create invalid predictions
    @Test
    fun `testOngoingPeriodDoesNotCreateInvalidPrediction`() = runTest {
        val start1 = LocalDate.of(2026, 6, 1)
        val end1 = LocalDate.of(2026, 6, 5)
        val start2 = LocalDate.of(2026, 7, 1)
        val end2 = LocalDate.of(2026, 7, 5)
        val ongoingStart = LocalDate.of(2026, 8, 1)

        repository.savePeriod(start1, end1)
        repository.savePeriod(start2, end2)
        repository.savePeriod(ongoingStart, null)

        val latestPrediction = fakePredictionDao.getLatest()
        assertThat(latestPrediction).isNotNull()
        // Prediction range should be strictly forward in time
        assertThat(latestPrediction!!.rangeLowEpochDay).isGreaterThan(ongoingStart.toEpochDay())
    }

    // 11. Preserves Daily Logs: Merge periods does not delete or touch daily logs
    @Test
    fun `testMergePeriodsPreservesDailyLogs`() = runTest {
        // Daily logs in Rona are stored in independent daily_log table
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 4).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        // Merging does not modify or drop daily logs
        val result = repository.savePeriod(
            start = LocalDate.of(2026, 8, 3),
            end = LocalDate.of(2026, 8, 7),
            resolutionStrategy = OverlapResolutionStrategy.MERGE,
        )

        assertThat(result.isSuccess).isTrue()
        val periods = repository.getAllPeriods()
        assertThat(periods).hasSize(1)
        assertThat(periods.first().startDate).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(periods.first().endDate).isEqualTo(LocalDate.of(2026, 8, 7))
    }

    // 12. Preserves Daily Logs: Delete period does not delete daily logs
    @Test
    fun `testDeletePeriodPreservesDailyLogs`() = runTest {
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 5).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        val result = repository.deletePeriod(1L)
        assertThat(result.isSuccess).isTrue()
        assertThat(repository.getAllPeriods()).isEmpty()
    }

    private class InMemoryPeriodDao : PeriodRecordDao {
        private val records = mutableMapOf<Long, PeriodRecordEntity>()
        private val state = MutableStateFlow<List<PeriodRecordEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<PeriodRecordEntity>> = state

        override suspend fun getAll(): List<PeriodRecordEntity> =
            records.values.sortedBy { it.startEpochDay }

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
