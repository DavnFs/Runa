package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.PeriodRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodRecordDao {

    @Query("SELECT * FROM period_records ORDER BY startEpochDay ASC")
    fun observeAll(): Flow<List<PeriodRecordEntity>>

    @Query("SELECT * FROM period_records ORDER BY startEpochDay ASC")
    suspend fun getAll(): List<PeriodRecordEntity>

    @Query("SELECT * FROM period_records WHERE id = :id")
    suspend fun getById(id: Long): PeriodRecordEntity?

    @Query("SELECT * FROM period_records WHERE startEpochDay = :startEpochDay")
    suspend fun getByStartDay(startEpochDay: Long): PeriodRecordEntity?

    @Query("SELECT * FROM period_records WHERE endEpochDay IS NULL")
    suspend fun getOngoing(): PeriodRecordEntity?

    @Query("SELECT * FROM period_records WHERE startEpochDay <= :epochDay AND (endEpochDay IS NULL OR endEpochDay >= :epochDay)")
    suspend fun getActiveOn(epochDay: Long): PeriodRecordEntity?

    @Query("SELECT COUNT(*) FROM period_records")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(record: PeriodRecordEntity): Long

    @Query("DELETE FROM period_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM period_records")
    suspend fun deleteAll()
}
