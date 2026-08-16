package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.rona.app.domain.model.Confidence

@Entity(
    tableName = "cycle_predictions",
    indices = [
        Index("generatedAt"),
        Index("predictedStartEpochDay"),
    ],
)
data class CyclePredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Long,
    val predictedStartEpochDay: Long,
    val rangeLowEpochDay: Long,
    val rangeHighEpochDay: Long,
    val medianCycleLengthDays: Int,
    val meanCycleLengthDays: Double,
    val madDays: Double,
    val cycleCountUsed: Int,
    val confidence: Confidence,
    val engineVersion: Int,
)
