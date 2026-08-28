package youyeluna.lanipscanner.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_history_items",
    foreignKeys = [
        ForeignKey(
            entity = ScanHistory::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["historyId"])]
)
data class ScanHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val historyId: Long,
    val ip: String,
    val macAddress: String = "-",
    val hostName: String = "-",
    val pingMs: Long = -1,
    val isActive: Boolean = false,
    val isDhcpServer: Boolean = false
)