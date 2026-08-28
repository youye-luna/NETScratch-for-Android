package youyeluna.lanipscanner.ui

import android.content.Context
import android.content.SharedPreferences

/** 数据保存方式 */
enum class SaveMode {
    BY_TIME,    // 按时间保存
    BY_COUNT    // 按数量保存
}

/** 按时间保存的范围选项 */
enum class TimeRange(val days: Int, val labelKey: String) {
    DAYS_14(14, "timeRange14Days"),
    HALF_MONTH(15, "timeRangeHalfMonth"),
    ONE_MONTH(30, "timeRangeOneMonth"),
    ONE_YEAR(365, "timeRangeOneYear"),
    NEVER_CLEAR(0, "timeRangeNeverClear"),  // 0 表示永不清除
    CUSTOM(0, "timeRangeCustom")  // 自定义天数
}

/** 按数量保存的范围选项 */
enum class CountRange(val count: Int, val labelKey: String) {
    COUNT_30(30, "countRange30"),
    COUNT_60(60, "countRange60"),
    COUNT_90(90, "countRange90"),
    COUNT_100(100, "countRange100")
}

/** 历史记录保存设置 */
data class HistorySaveSettings(
    val saveMode: SaveMode = SaveMode.BY_COUNT,
    val timeRange: TimeRange = TimeRange.ONE_YEAR,
    val customDays: Int = 30,
    val countRange: CountRange = CountRange.COUNT_100,
    val scanThreads: Int = 30
)

/** 历史记录保存设置管理器 */
class HistorySettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("history_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVE_MODE = "save_mode"
        private const val KEY_TIME_RANGE = "time_range"
        private const val KEY_CUSTOM_DAYS = "custom_days"
        private const val KEY_COUNT_RANGE = "count_range"
        private const val KEY_SCAN_THREADS = "scan_threads"
    }

    fun getSettings(): HistorySaveSettings {
        val saveMode = try {
            SaveMode.valueOf(prefs.getString(KEY_SAVE_MODE, SaveMode.BY_COUNT.name) ?: SaveMode.BY_COUNT.name)
        } catch (e: Exception) {
            SaveMode.BY_COUNT
        }

        val timeRange = try {
            TimeRange.valueOf(prefs.getString(KEY_TIME_RANGE, TimeRange.ONE_YEAR.name) ?: TimeRange.ONE_YEAR.name)
        } catch (e: Exception) {
            TimeRange.ONE_YEAR
        }

        val customDays = prefs.getInt(KEY_CUSTOM_DAYS, 30)

        val countRange = try {
            CountRange.valueOf(prefs.getString(KEY_COUNT_RANGE, CountRange.COUNT_100.name) ?: CountRange.COUNT_100.name)
        } catch (e: Exception) {
            CountRange.COUNT_100
        }

        val scanThreads = prefs.getInt(KEY_SCAN_THREADS, 30).coerceIn(1, 100)

        return HistorySaveSettings(
            saveMode = saveMode,
            timeRange = timeRange,
            customDays = customDays,
            countRange = countRange,
            scanThreads = scanThreads
        )
    }

    fun saveSettings(settings: HistorySaveSettings) {
        prefs.edit().apply {
            putString(KEY_SAVE_MODE, settings.saveMode.name)
            putString(KEY_TIME_RANGE, settings.timeRange.name)
            putInt(KEY_CUSTOM_DAYS, settings.customDays)
            putString(KEY_COUNT_RANGE, settings.countRange.name)
            putInt(KEY_SCAN_THREADS, settings.scanThreads)
            apply()
        }
    }

    fun saveScanThreads(threads: Int) {
        prefs.edit().putInt(KEY_SCAN_THREADS, threads.coerceIn(1, 100)).apply()
    }

    fun getScanThreads(): Int {
        return prefs.getInt(KEY_SCAN_THREADS, 30).coerceIn(1, 100)
    }
}