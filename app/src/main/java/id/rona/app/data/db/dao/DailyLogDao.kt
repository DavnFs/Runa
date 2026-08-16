package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY dateEpochDay ASC")
    suspend fun getAll(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE id = :id")
    suspend fun getById(id: Long): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE dateEpochDay = :epochDay")
    suspend fun getByDate(epochDay: Long): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE dateEpochDay BETWEEN :fromDay AND :toDay ORDER BY dateEpochDay ASC")
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT COUNT(*) FROM daily_logs")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(log: DailyLogEntity): Long

    @Query("DELETE FROM daily_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAll()
}
