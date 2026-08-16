package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "period_records",
    indices = [
        Index("startEpochDay"),
        Index("endEpochDay"),
    ],
)
data class PeriodRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochDay: Long, // SENSITIVE — required
    val endEpochDay: Long? = null, // SENSITIVE — null while ongoing
    val createdAt: Long,
    val updatedAt: Long,
)
