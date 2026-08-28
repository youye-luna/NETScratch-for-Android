package youyeluna.lanipscanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_history")
data class SpeedTestHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testTime: Long = System.currentTimeMillis(),
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val uploadSucceeded: Boolean = false,
    val downloadedBytes: Long = 0,
    val uploadedBytes: Long = 0,
    val durationMs: Long = 0,
    val downloadUrl: String = ""
)
