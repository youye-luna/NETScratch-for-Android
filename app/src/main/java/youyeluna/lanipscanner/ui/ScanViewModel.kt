package youyeluna.lanipscanner.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import youyeluna.lanipscanner.App
import youyeluna.lanipscanner.model.DhcpServerInfo
import youyeluna.lanipscanner.scanner.DhcpScanner
import youyeluna.lanipscanner.scanner.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 界面状态 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val progress: Int = 0,
    val statusText: String = "",
    val results: List<DhcpServerInfo> = emptyList(),
    val scannedCount: Int = 0,
    val onlineCount: Int = 0,
    val dhcpCount: Int = 0,
    val hasRootPermission: Boolean = false
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private val repository = (application as App).repository
    private val settingsManager = HistorySettingsManager(application)
    private var currentStartIp: String = ""
    private var currentEndIp: String = ""

    val defaultStartIp: String
    val defaultEndIp: String

    val historySettingsManager: HistorySettingsManager get() = settingsManager

    init {
        val (start, end) = NetworkUtils.getDefaultRange()
        defaultStartIp = start
        defaultEndIp = end
        // 仅 Android 10+ 需要检测 root 权限才能获取 MAC 地址
        val hasRoot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DhcpScanner.hasRootPermission()
        } else {
            true // 低版本系统不需要 root
        }
        val strings = stringsFor(LanguageManager.currentLanguage)
        _uiState.update { it.copy(statusText = strings.ready, hasRootPermission = hasRoot) }
    }

    fun startScan(startIp: String, endIp: String) {
        if (_uiState.value.isScanning) return
        
        // 验证IP地址是否为内网IP
        val validationError = NetworkUtils.validatePrivateIpRange(startIp, endIp)
        if (validationError != null) {
            val strings = stringsFor(LanguageManager.currentLanguage)
            _uiState.update { it.copy(statusText = validationError) }
            return
        }
        
        currentStartIp = startIp
        currentEndIp = endIp
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val strings = stringsFor(LanguageManager.currentLanguage)
            _uiState.update {
                it.copy(
                    isScanning = true,
                    progress = 0,
                    statusText = strings.scanningIpRange(startIp, endIp),
                    results = emptyList(),
                    scannedCount = 0,
                    onlineCount = 0,
                    dhcpCount = 0
                )
            }
            try {
                val results = DhcpScanner.scanIpRange(startIp, endIp) { completed, total ->
                    _uiState.update {
                        it.copy(
                            progress = if (total > 0) (completed * 100) / total else 0,
                            scannedCount = completed
                        )
                    }
                }
                val s = stringsFor(LanguageManager.currentLanguage)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        progress = 100,
                        results = results,
                        statusText = s.scanComplete,
                        scannedCount = results.size,
                        onlineCount = results.count { r -> r.isActive },
                        dhcpCount = results.count { r -> r.isDhcpServer }
                    )
                }
                // 保存扫描历史并执行清理策略
                try {
                    repository.saveScanResult(startIp, endIp, results)
                    val settings = settingsManager.getSettings()
                    repository.applyRetentionPolicy(settings)
                } catch (e: Exception) {
                    // 保存历史失败不影响主流程
                }
            } catch (e: CancellationException) {
                val s = stringsFor(LanguageManager.currentLanguage)
                _uiState.update { it.copy(isScanning = false, statusText = s.scanStopped) }
            } catch (e: Exception) {
                val s = stringsFor(LanguageManager.currentLanguage)
                _uiState.update { it.copy(isScanning = false, statusText = s.scanError(e.message ?: "")) }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    fun clear() {
        val strings = stringsFor(LanguageManager.currentLanguage)
        _uiState.value = ScanUiState(statusText = strings.ready)
    }
}
