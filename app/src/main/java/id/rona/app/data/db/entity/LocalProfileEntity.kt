package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_profile")
data class LocalProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val defaultCycleLengthDays: Int? = null,
    val onboardingCompletedAt: Long? = null,
    val createdAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
