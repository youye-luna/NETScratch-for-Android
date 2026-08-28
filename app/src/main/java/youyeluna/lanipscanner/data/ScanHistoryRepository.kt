package youyeluna.lanipscanner.data

import kotlinx.coroutines.flow.Flow
import youyeluna.lanipscanner.model.DhcpServerInfo
import youyeluna.lanipscanner.model.ScanHistory
import youyeluna.lanipscanner.model.ScanHistoryItem

class ScanHistoryRepository(private val dao: ScanHistoryDao) {
    val allHistory: Flow<List<ScanHistory>> = dao.getAllHistory()

    fun getHistoryItems(historyId: Long): Flow<List<ScanHistoryItem>> {
        return dao.getHistoryItems(historyId)
    }

    suspend fun saveScanResult(
        startIp: String,
        endIp: String,
        results: List<DhcpServerInfo>
    ) {
        val onlineDevices = results.count { it.isActive }
        val dhcpServers = results.count { it.isDhcpServer }

        val history = ScanHistory(
            startIp = startIp,
            endIp = endIp,
            totalDevices = results.size,
            onlineDevices = onlineDevices,
            dhcpServers = dhcpServers
        )

        val items = results.map { info ->
            ScanHistoryItem(
                historyId = 0,
                ip = info.ip,
                macAddress = info.macAddress,
                hostName = info.hostName,
                pingMs = info.pingMs,
                isActive = info.isActive,
                isDhcpServer = info.isDhcpServer
            )
        }

        dao.insertScanWithItems(history, items)
    }

    suspend fun getHistoryById(historyId: Long): ScanHistory? {
        return dao.getHistoryById(historyId)
    }

    suspend fun deleteHistory(historyId: Long) {
        dao.deleteHistory(historyId)
    }

    suspend fun deleteAllHistory() {
        dao.deleteAllHistory()
    }

    fun getHistoryCount(): Flow<Int> {
        return dao.getHistoryCount()
    }

    suspend fun cleanByTime(days: Int) {
        if (days <= 0) return // 永不清除
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        dao.deleteHistoryOlderThan(cutoff)
    }

    suspend fun cleanByCount(maxCount: Int) {
        val totalCount = dao.getHistoryCountValue()
        if (totalCount <= maxCount) return
        val toDelete = totalCount - maxCount
        for (i in 0 until toDelete) {
            val oldestId = dao.getHistoryIdAtOffset(totalCount - 1 - i)
            if (oldestId != null) {
                dao.deleteHistory(oldestId)
            }
        }
    }

    suspend fun applyRetentionPolicy(settings: youyeluna.lanipscanner.ui.HistorySaveSettings) {
        when (settings.saveMode) {
            youyeluna.lanipscanner.ui.SaveMode.BY_TIME -> {
                val days = when (settings.timeRange) {
                    youyeluna.lanipscanner.ui.TimeRange.CUSTOM -> settings.customDays
                    youyeluna.lanipscanner.ui.TimeRange.NEVER_CLEAR -> 0
                    else -> settings.timeRange.days
                }
                cleanByTime(days)
            }
            youyeluna.lanipscanner.ui.SaveMode.BY_COUNT -> {
                cleanByCount(settings.countRange.count)
            }
        }
    }
}