package youyeluna.lanipscanner

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import youyeluna.lanipscanner.data.AppDatabase
import youyeluna.lanipscanner.data.ScanHistoryRepository
import youyeluna.lanipscanner.data.SpeedTestHistoryRepository
import youyeluna.lanipscanner.ui.HistorySettingsManager

class App : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ScanHistoryRepository(database.scanHistoryDao()) }
    val speedTestHistoryRepository by lazy { SpeedTestHistoryRepository(database.speedTestHistoryDao()) }

    override fun onCreate() {
        super.onCreate()
        // 启动时校验时间并清理历史记录
        applicationScope.launch {
            try {
                val settingsManager = HistorySettingsManager(this@App)
                val settings = settingsManager.getSettings()
                repository.applyRetentionPolicy(settings)
            } catch (_: Exception) {
                // 清理失败不影响启动
            }
        }
    }
}