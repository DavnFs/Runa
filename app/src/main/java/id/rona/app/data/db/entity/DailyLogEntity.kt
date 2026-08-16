package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood

@Entity(
    tableName = "daily_logs",
    indices = [Index(value = ["dateEpochDay"], unique = true)],
)
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long, // SENSITIVE — one log per day
    val flow: FlowLevel? = null, // SENSITIVE
    val mood: Mood? = null, // SENSITIVE
    val energy: Energy? = null, // SENSITIVE
    val note: String? = null, // SENSITIVE — free-form private note
    val createdAt: Long,
    val updatedAt: Long,
)
