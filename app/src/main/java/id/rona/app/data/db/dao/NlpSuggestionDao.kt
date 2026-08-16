package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.NlpSuggestionEntity
import id.rona.app.domain.model.NlpStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface NlpSuggestionDao {

    @Query("SELECT * FROM nlp_suggestions WHERE dailyLogId = :dailyLogId")
    fun observeForLog(dailyLogId: Long): Flow<List<NlpSuggestionEntity>>

    @Query("SELECT * FROM nlp_suggestions WHERE dailyLogId = :dailyLogId")
    suspend fun getForLog(dailyLogId: Long): List<NlpSuggestionEntity>

    @Query("SELECT * FROM nlp_suggestions WHERE status = :status")
    suspend fun getByStatus(status: NlpStatus): List<NlpSuggestionEntity>

    @Upsert
    suspend fun upsert(suggestion: NlpSuggestionEntity): Long

    @Query("UPDATE nlp_suggestions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: NlpStatus)

    @Query("DELETE FROM nlp_suggestions WHERE dailyLogId = :dailyLogId")
    suspend fun deleteForLog(dailyLogId: Long)

    @Query("DELETE FROM nlp_suggestions")
    suspend fun deleteAll()
}
