package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.SymptomLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomLogDao {

    @Query("SELECT * FROM symptom_logs WHERE dailyLogId = :dailyLogId ORDER BY symptomType ASC")
    fun observeForLog(dailyLogId: Long): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs")
    fun observeAll(): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE dailyLogId = :dailyLogId")
    suspend fun getForLog(dailyLogId: Long): List<SymptomLogEntity>

    @Query("SELECT symptomType, COUNT(*) AS frequency FROM symptom_logs GROUP BY symptomType ORDER BY frequency DESC")
    suspend fun getFrequencyByType(): List<SymptomFrequency>

    @Upsert
    suspend fun upsert(symptom: SymptomLogEntity): Long

    @Query("DELETE FROM symptom_logs WHERE dailyLogId = :dailyLogId AND symptomType = :symptomType")
    suspend fun deleteForLogAndType(dailyLogId: Long, symptomType: String)

    @Query("DELETE FROM symptom_logs WHERE dailyLogId = :dailyLogId")
    suspend fun deleteForLog(dailyLogId: Long)

    @Query("DELETE FROM symptom_logs")
    suspend fun deleteAll()

    data class SymptomFrequency(
        val symptomType: String,
        val frequency: Int,
    )
}
