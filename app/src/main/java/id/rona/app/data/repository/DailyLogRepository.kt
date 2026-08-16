package id.rona.app.data.repository

import androidx.room.withTransaction
import id.rona.app.data.db.RonaDatabase
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.SymptomLogEntity
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val db: RonaDatabase,
    private val dailyLogDao: DailyLogDao,
    private val symptomLogDao: SymptomLogDao,
) {

    fun observeForDate(date: LocalDate): Flow<DailyLogEntity?> =
        dailyLogDao.observeAll().map { logs ->
            logs.firstOrNull { it.dateEpochDay == date.toEpochDay() }
        }

    suspend fun getForDate(date: LocalDate): DailyLogEntity? =
        dailyLogDao.getByDate(date.toEpochDay())

    suspend fun observeSymptomsForLog(logId: Long): Flow<List<SymptomLogEntity>> =
        symptomLogDao.observeForLog(logId)

    suspend fun getSymptomsForLog(logId: Long): List<SymptomLogEntity> =
        symptomLogDao.getForLog(logId)

    /**
     * Saves a full daily log transactionally: upsert log, replace symptoms.
     */
    suspend fun save(
        date: LocalDate,
        flow: FlowLevel?,
        mood: Mood?,
        energy: Energy?,
        note: String?,
        symptoms: List<Pair<SymptomType, Severity>>,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        db.withTransaction {
            val existing = dailyLogDao.getByDate(date.toEpochDay())
            val logId = dailyLogDao.upsert(
                DailyLogEntity(
                    id = existing?.id ?: 0,
                    dateEpochDay = date.toEpochDay(),
                    flow = flow,
                    mood = mood,
                    energy = energy,
                    note = note?.takeIf { it.isNotBlank() },
                    createdAt = existing?.createdAt ?: nowMs,
                    updatedAt = nowMs,
                )
            )
            symptomLogDao.deleteForLog(logId)
            symptoms.forEach { (type, severity) ->
                symptomLogDao.upsert(
                    SymptomLogEntity(
                        dailyLogId = logId,
                        symptomType = type,
                        severity = severity,
                    )
                )
            }
        }
    }

    suspend fun delete(date: LocalDate) {
        dailyLogDao.getByDate(date.toEpochDay())?.let { log ->
            dailyLogDao.deleteById(log.id)
        }
    }
}
