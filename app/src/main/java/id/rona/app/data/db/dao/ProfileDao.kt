package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.LocalProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM local_profile WHERE id = :id")
    fun observe(id: Int = LocalProfileEntity.SINGLETON_ID): Flow<LocalProfileEntity?>

    @Query("SELECT * FROM local_profile WHERE id = :id")
    suspend fun get(id: Int = LocalProfileEntity.SINGLETON_ID): LocalProfileEntity?

    @Upsert
    suspend fun upsert(profile: LocalProfileEntity)
}
