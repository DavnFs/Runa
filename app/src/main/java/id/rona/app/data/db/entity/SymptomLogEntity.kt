package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType

@Entity(
    tableName = "symptom_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyLogId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dailyLogId"),
        Index(value = ["dailyLogId", "symptomType"], unique = true),
    ],
)
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyLogId: Long, // SENSITIVE — FK to daily log
    val symptomType: SymptomType, // SENSITIVE
    val severity: Severity, // SENSITIVE
)
