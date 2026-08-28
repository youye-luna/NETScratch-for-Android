package youyeluna.lanipscanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startIp: String,
    val endIp: String,
    val scanTime: Long = System.currentTimeMillis(),
    val totalDevices: Int = 0,
    val onlineDevices: Int = 0,
    val dhcpServers: Int = 0
)