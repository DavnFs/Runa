package id.rona.app.data.repository

import id.rona.app.data.db.TransactionRunner
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.CyclePredictionEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.model.OverlapResolutionStrategy
import id.rona.app.domain.model.PeriodRecord
import id.rona.app.domain.model.PeriodValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodRecordRepository @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val periodRecordDao: PeriodRecordDao,
    private val cyclePredictionDao: CyclePredictionDao,
) {

    fun observeAllPeriods(): Flow<List<PeriodRecord>> =
        periodRecordDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllPeriods(): List<PeriodRecord> =
        periodRecordDao.getAll().map { it.toDomain() }

    suspend fun getPeriodById(id: Long): PeriodRecord? =
        periodRecordDao.getById(id)?.toDomain()

    suspend fun getLatestPeriod(): PeriodRecord? =
        periodRecordDao.getAll().maxByOrNull { it.startEpochDay }?.toDomain()

    suspend fun getPeriodActiveOn(date: LocalDate): PeriodRecord? =
        periodRecordDao.getActiveOn(date.toEpochDay())?.toDomain()

    /**
     * Pure validation logic checking for date ordering, overlap conflicts, and duration warnings.
     */
    suspend fun validatePeriod(
        start: LocalDate,
        end: LocalDate?,
        currentRecordId: Long = 0L,
    ): PeriodValidationResult {
        if (end != null && end < start) {
            return PeriodValidationResult.InvalidDateOrder()
        }

        val allRecords = getAllPeriods().filter { it.id != currentRecordId }
        val conflicts = findOverlappingRecords(start, end, allRecords)

        if (conflicts.isNotEmpty()) {
            return PeriodValidationResult.OverlapConflict(
                message = "Rentang tanggal ini beririsan dengan ${conflicts.size} catatan periode yang sudah ada.",
                conflictingRecords = conflicts,
            )
        }

        if (end != null) {
            val duration = (end.toEpochDay() - start.toEpochDay()).toInt() + 1
            if (duration > 15) {
                return PeriodValidationResult.UnusualDurationWarning(durationDays = duration)
            }
        }

        return PeriodValidationResult.Valid
    }

    /**
     * Transactional save method for inserting or updating a period record,
     * resolving any conflicts as specified, and immediately invalidating & recalculating predictions.
     */
    suspend fun savePeriod(
        start: LocalDate,
        end: LocalDate?,
        currentRecordId: Long = 0L,
        resolutionStrategy: OverlapResolutionStrategy? = null,
    ): Result<PeriodRecord> = runCatching {
        if (end != null && end < start) {
            throw IllegalArgumentException("Tanggal selesai tidak boleh sebelum tanggal mulai.")
        }

        transactionRunner {
            val now = System.currentTimeMillis()
            val allRecords = periodRecordDao.getAll().map { it.toDomain() }
                .filter { it.id != currentRecordId }
            val conflicts = findOverlappingRecords(start, end, allRecords)

            var effectiveStart = start
            var effectiveEnd = end

            if (conflicts.isNotEmpty()) {
                when (resolutionStrategy) {
                    OverlapResolutionStrategy.MERGE -> {
                        val allStarts = conflicts.map { it.startDate } + start
                        effectiveStart = allStarts.minOrNull() ?: start

                        val hasOngoing = end == null || conflicts.any { it.endDate == null }
                        effectiveEnd = if (hasOngoing) {
                            null
                        } else {
                            val allEnds = conflicts.mapNotNull { it.endDate } + listOfNotNull(end)
                            allEnds.maxOrNull()
                        }

                        // Delete conflicting records to merge into one
                        conflicts.forEach { periodRecordDao.deleteById(it.id) }
                    }
                    OverlapResolutionStrategy.REPLACE -> {
                        // Delete conflicting records and replace with new
                        conflicts.forEach { periodRecordDao.deleteById(it.id) }
                    }
                    null -> {
                        throw IllegalStateException("Terdapat konflik catatan beririsan. Pilih tindakan penyelesaian.")
                    }
                }
            }

            val existingEntity = if (currentRecordId != 0L) periodRecordDao.getById(currentRecordId) else null
            val entityToSave = PeriodRecordEntity(
                id = currentRecordId,
                startEpochDay = effectiveStart.toEpochDay(),
                endEpochDay = effectiveEnd?.toEpochDay(),
                createdAt = existingEntity?.createdAt ?: now,
                updatedAt = now,
            )

            val savedId = periodRecordDao.upsert(entityToSave)
            val finalId = if (currentRecordId != 0L) currentRecordId else savedId

            // Invalidate and recompute prediction cache in same transaction
            recalculatePredictionCacheInternal()

            PeriodRecord(
                id = finalId,
                startDate = effectiveStart,
                endDate = effectiveEnd,
                createdAt = entityToSave.createdAt,
                updatedAt = now,
            )
        }
    }

    /**
     * Transactional delete method for a period record.
     */
    suspend fun deletePeriod(id: Long): Result<Unit> = runCatching {
        transactionRunner {
            periodRecordDao.deleteById(id)
            recalculatePredictionCacheInternal()
        }
    }

    /**
     * Invalidate prediction cache and recompute deterministic prediction from scratch.
     */
    suspend fun recalculatePredictionCache() {
        transactionRunner {
            recalculatePredictionCacheInternal()
        }
    }

    private suspend fun recalculatePredictionCacheInternal() {
        cyclePredictionDao.deleteAll()

        val allStarts = periodRecordDao.getAll()
            .map { LocalDate.ofEpochDay(it.startEpochDay) }
            .distinct()
            .sorted()

        val prediction = CycleEngine.predict(allStarts)
        if (prediction != null) {
            val now = System.currentTimeMillis()
            cyclePredictionDao.upsert(
                CyclePredictionEntity(
                    generatedAt = now,
                    predictedStartEpochDay = prediction.predictedStart.toEpochDay(),
                    rangeLowEpochDay = prediction.rangeLow.toEpochDay(),
                    rangeHighEpochDay = prediction.rangeHigh.toEpochDay(),
                    medianCycleLengthDays = prediction.medianCycleLengthDays,
                    meanCycleLengthDays = prediction.meanCycleLengthDays,
                    madDays = prediction.madDays,
                    cycleCountUsed = prediction.cycleCountUsed,
                    confidence = prediction.confidence,
                    engineVersion = CycleEngine.ENGINE_VERSION,
                )
            )
        }
    }

    private fun findOverlappingRecords(
        start: LocalDate,
        end: LocalDate?,
        records: List<PeriodRecord>,
    ): List<PeriodRecord> {
        return records.filter { rec ->
            isOverlapping(start, end, rec.startDate, rec.endDate)
        }
    }

    private fun isOverlapping(
        start1: LocalDate,
        end1: LocalDate?,
        start2: LocalDate,
        end2: LocalDate?,
    ): Boolean {
        // Case 1: Both have defined end dates
        if (end1 != null && end2 != null) {
            return !(end1 < start2 || start1 > end2)
        }
        // Case 2: Range 1 is ongoing (end1 == null), Range 2 has end date
        if (end1 == null && end2 != null) {
            return start1 <= end2
        }
        // Case 3: Range 1 has end date, Range 2 is ongoing (end2 == null)
        if (end1 != null && end2 == null) {
            return start2 <= end1
        }
        // Case 4: Both are ongoing
        return true
    }

    private fun PeriodRecordEntity.toDomain() = PeriodRecord(
        id = id,
        startDate = LocalDate.ofEpochDay(startEpochDay),
        endDate = endEpochDay?.let { LocalDate.ofEpochDay(it) },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
