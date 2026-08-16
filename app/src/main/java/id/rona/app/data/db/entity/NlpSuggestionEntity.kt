package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import id.rona.app.domain.model.NlpStatus

@Entity(
    tableName = "nlp_suggestions",
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
        Index("status"),
    ],
)
data class NlpSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyLogId: Long,
    val source: String, // RULES | ONNX
    val modelVersion: String? = null,
    val payloadJson: String, // suggested labels + confidence; NEVER raw note text
    val status: NlpStatus,
    val createdAt: Long,
)
