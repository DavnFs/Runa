package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.ExportMetadataEntity

@Dao
interface ExportMetadataDao {

    @Query("SELECT * FROM export_metadata ORDER BY exportedAt DESC")
    suspend fun getAll(): List<ExportMetadataEntity>

    @Query("SELECT COUNT(*) FROM export_metadata")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(metadata: ExportMetadataEntity): Long

    @Query("DELETE FROM export_metadata")
    suspend fun deleteAll()
}
