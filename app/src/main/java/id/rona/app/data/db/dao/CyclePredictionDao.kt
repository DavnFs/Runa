package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.CyclePredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclePredictionDao {

    @Query("SELECT * FROM cycle_predictions ORDER BY generatedAt DESC LIMIT 1")
    fun observeLatest(): Flow<CyclePredictionEntity?>

    @Query("SELECT * FROM cycle_predictions ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatest(): CyclePredictionEntity?

    @Upsert
    suspend fun upsert(prediction: CyclePredictionEntity): Long

    @Query("DELETE FROM cycle_predictions WHERE id NOT IN (SELECT id FROM cycle_predictions ORDER BY generatedAt DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM cycle_predictions")
    suspend fun deleteAll()
}
