package id.rona.app.data.repository

import com.google.common.truth.Truth.assertThat
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.CyclePredictionEntity
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.SymptomLogEntity
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.OverlapResolutionStrategy
import id.rona.app.domain.model.PeriodValidationResult
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PeriodRecordRepositoryTest {

    private lateinit var fakePeriodDao: InMemoryPeriodDao
    private lateinit var fakePredictionDao: InMemoryPredictionDao
    private lateinit var fakeDailyLogDao: InMemoryDailyLogDao
    private lateinit var fakeSymptomLogDao: InMemorySymptomLogDao
    private lateinit var repository: PeriodRecordRepository

    @Before
    fun setUp() {
        fakePeriodDao = InMemoryPeriodDao()
        fakePredictionDao = InMemoryPredictionDao()
        fakeDailyLogDao = InMemoryDailyLogDao()
        fakeSymptomLogDao = InMemorySymptomLogDao()
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
        fakePeriodDao.upsert(
            PeriodRecordEntity(
                id = 1L,
                startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                endEpochDay = LocalDate.of(2026, 8, 4).toEpochDay(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        val logIds = seedDailyLogsFor(LocalDate.of(2026, 8, 1)..LocalDate.of(2026, 8, 7))

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

        assertThat(fakeDailyLogDao.getAll()).hasSize(7)
        assertThat(fakeDailyLogDao.getAll().map { it.dateEpochDay })
            .containsExactlyElementsIn(
                buildList {
                    var day = LocalDate.of(2026, 8, 1)
                    while (day <= LocalDate.of(2026, 8, 7)) {
                        add(day.toEpochDay())
                        day = day.plusDays(1)
                    }
                }
            )
        assertThat(fakeDailyLogDao.getAll().map { it.note })
            .contains("test note only — no real health data")
        assertThat(fakeSymptomLogDao.getAll()).hasSize(2)
        assertThat(fakeSymptomLogDao.getForLog(logIds.first)).hasSize(2)
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

        val logIds = seedDailyLogsFor(LocalDate.of(2026, 8, 1)..LocalDate.of(2026, 8, 5))

        val result = repository.deletePeriod(1L)
        assertThat(result.isSuccess).isTrue()
        assertThat(repository.getAllPeriods()).isEmpty()

        assertThat(fakeDailyLogDao.getAll()).hasSize(5)
        assertThat(fakeDailyLogDao.getById(logIds.first)).isNotNull()
        assertThat(fakeSymptomLogDao.getForLog(logIds.first)).hasSize(2)
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

    private class InMemoryDailyLogDao : DailyLogDao {
        private val logs = mutableMapOf<Long, DailyLogEntity>()
        private val state = MutableStateFlow<List<DailyLogEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<DailyLogEntity>> = state
        override suspend fun getAll(): List<DailyLogEntity> =
            logs.values.sortedBy { it.dateEpochDay }
        override suspend fun getById(id: Long): DailyLogEntity? = logs[id]
        override suspend fun getByDate(epochDay: Long): DailyLogEntity? =
            logs.values.firstOrNull { it.dateEpochDay == epochDay }
        override fun observeBetween(fromDay: Long, toDay: Long): Flow<List<DailyLogEntity>> = state
        override suspend fun count(): Int = logs.size
        override suspend fun upsert(log: DailyLogEntity): Long {
            val id = if (log.id != 0L) log.id else nextId++
            val saved = log.copy(id = id)
            logs[id] = saved
            state.value = logs.values.sortedBy { it.dateEpochDay }
            return id
        }
        override suspend fun deleteById(id: Long) {
            logs.remove(id)
            state.value = logs.values.sortedBy { it.dateEpochDay }
        }
        override suspend fun deleteAll() {
            logs.clear()
            state.value = emptyList()
        }
    }

    private class InMemorySymptomLogDao : SymptomLogDao {
        private val symptoms = mutableMapOf<Long, SymptomLogEntity>()
        private val state = MutableStateFlow<List<SymptomLogEntity>>(emptyList())
        private var nextId = 1L

        override fun observeForLog(dailyLogId: Long): Flow<List<SymptomLogEntity>> = state
        override fun observeAll(): Flow<List<SymptomLogEntity>> = state
        override suspend fun getForLog(dailyLogId: Long): List<SymptomLogEntity> =
            symptoms.values.filter { it.dailyLogId == dailyLogId }
        override suspend fun getFrequencyByType(): List<SymptomLogDao.SymptomFrequency> = emptyList()
        override suspend fun upsert(symptom: SymptomLogEntity): Long {
            val id = if (symptom.id != 0L) symptom.id else nextId++
            val saved = symptom.copy(id = id)
            symptoms[id] = saved
            state.value = symptoms.values.toList()
            return id
        }
        override suspend fun deleteForLogAndType(dailyLogId: Long, symptomType: String) {
            symptoms.values.removeIf { it.dailyLogId == dailyLogId && it.symptomType.name == symptomType }
        }
        override suspend fun deleteForLog(dailyLogId: Long) {
            symptoms.values.removeIf { it.dailyLogId == dailyLogId }
        }
        override suspend fun deleteAll() {
            symptoms.clear()
            state.value = emptyList()
        }
    }

    /**
     * Seeds one synthetic daily log per date with a neutral fixture note and
     * two synthetic symptoms on the first log. No real user data is ever used.
     */
    private suspend fun seedDailyLogsFor(dates: ClosedRange<LocalDate>): List<Long> {
        val ids = mutableListOf<Long>()
        var day = dates.start
        while (day <= dates.endInclusive) {
            val id = fakeDailyLogDao.upsert(
                DailyLogEntity(
                    dateEpochDay = day.toEpochDay(),
                    flow = FlowLevel.MEDIUM,
                    mood = Mood.NEUTRAL,
                    energy = Energy.NEUTRAL,
                    note = "test note only — no real health data",
                    createdAt = 1L,
                    updatedAt = 1L,
                )
            )
            ids += id
            day = day.plusDays(1)
        }
        fakeSymptomLogDao.upsert(
            SymptomLogEntity(dailyLogId = ids.first(), symptomType = SymptomType.KRAM, severity = Severity.MILD)
        )
        fakeSymptomLogDao.upsert(
            SymptomLogEntity(dailyLogId = ids.first(), symptomType = SymptomType.HEADACHE, severity = Severity.MODERATE)
        )
        return ids
    }
}
