package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_metadata")
data class ExportMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exportedAt: Long,
    val fileName: String? = null,
    val schemaVersion: Int,
    val appVersion: String,
    val periodCount: Int,
    val dailyLogCount: Int,
    val ciphertextSha256: String,
)
