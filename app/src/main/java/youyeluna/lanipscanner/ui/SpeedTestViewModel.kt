package youyeluna.lanipscanner.ui

import androidx.lifecycle.ViewModel
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import fr.bmartel.speedtest.SpeedTestReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

/** 测速页 UI 状态 */
data class SpeedTestUiState(
    val isTesting: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val currentSpeedMbps: Double = 0.0,
    val finalSpeedMbps: Double? = null,
    val durationMs: Long = 0L,
    /** 原始错误码，UI 层负责转换为本地化文案 */
    val errorCode: String? = null
)

/**
 * 测速页 ViewModel
 *
 * 基于 JSpeedTest（fr.bmartel:jspeedtest，MIT License）实现 HTTP 下载测速：
 * - 使用 HTTPS 固定大小测试文件（约 10MB）下载计时，兼顾测速准确性与流量/时长开销
 * - 内置最大测速时长保护（15 秒），弱网环境下也不会长时间占用
 * - 主测速源失败时自动回退备用测速源重试一次
 * - 下载全程写入内存缓冲（RAM），不产生任何磁盘临时文件，测速结束即随内存释放
 * - [onCleared] 时强制终止任务并释放 socket，防止页面销毁后后台残留
 */
class SpeedTestViewModel : ViewModel() {

    companion object {
        /** 标称测试文件大小（字节），用于服务端未返回进度时估算百分比 */
        private const val EXPECTED_FILE_BYTES = 10L * 1000 * 1000

        /** 进度回调间隔（毫秒） */
        private const val REPORT_INTERVAL_MS = 300

        /** 最大测速时长（毫秒） */
        private const val MAX_DURATION_MS = 15_000

        /** 主测速源（HTTPS 固定大小文件，避免 targetSdk 明文流量限制） */
        private const val PRIMARY_URL = "https://ipv4.ikoula.testdebit.info/10M.iso"

        /** 备用测速源（Cloudflare HTTPS 下行端点） */
        private const val FALLBACK_URL = "https://speed.cloudflare.com/__down?bytes=10000000"

        /** 视为网络类错误的错误码集合（展示统一的网络异常提示） */
        val NETWORK_ERROR_CODES = setOf(
            SpeedTestError.SOCKET_ERROR.name,
            SpeedTestError.SOCKET_TIMEOUT.name,
            SpeedTestError.CONNECTION_ERROR.name,
            SpeedTestError.MALFORMED_URI.name,
            SpeedTestError.UNSUPPORTED_PROTOCOL.name,
            SpeedTestError.INVALID_HTTP_RESPONSE.name
        )
    }

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    private var socket: SpeedTestSocket? = null
    private var manualStop = false

    /** 当前尝试的测速源：0=主源，1=备用源 */
    private var urlIndex = 0

    /** 开始测速 */
    fun startTest() {
        if (_uiState.value.isTesting) return
        urlIndex = 0
        manualStop = false
        _uiState.update {
            it.copy(
                isTesting = true,
                progressPercent = 0,
                downloadedBytes = 0,
                currentSpeedMbps = 0.0,
                finalSpeedMbps = null,
                durationMs = 0,
                errorCode = null
            )
        }
        launchDownload()
    }

    /** 停止测速 */
    fun stopTest() {
        if (!_uiState.value.isTesting) return
        manualStop = true
        socket?.forceStopTask()
    }

    /** 清空结果 */
    fun clear() {
        if (_uiState.value.isTesting) return
        _uiState.value = SpeedTestUiState()
    }

    /** 启动一次固定时长下载任务（每次使用独立 socket 实例，下载在内存缓冲中进行） */
    private fun launchDownload() {
        val url = if (urlIndex == 0) PRIMARY_URL else FALLBACK_URL
        socket = SpeedTestSocket().also { s ->
            s.addSpeedTestListener(object : ISpeedTestListener {
                override fun onCompletion(report: SpeedTestReport) {
                    if (manualStop) {
                        finishTesting()
                        return
                    }
                    val speedMbps = (report.transferRateOctet ?: BigDecimal.ZERO).toDouble() * 8 / 1_000_000.0
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            progressPercent = 100,
                            downloadedBytes = report.temporaryPacketSize,
                            currentSpeedMbps = speedMbps,
                            finalSpeedMbps = speedMbps,
                            durationMs = report.reportTime - report.startTime
                        )
                    }
                    releaseSocket()
                }

                override fun onProgress(percent: Float, report: SpeedTestReport) {
                    val speedMbps = (report.transferRateOctet ?: BigDecimal.ZERO).toDouble() * 8 / 1_000_000.0
                    // 服务端未提供百分比时，用已下载/总大小估算
                    val pct = if (percent >= 0) percent.toInt().coerceIn(0, 100) else {
                        val total = report.totalPacketSize
                        val denom = if (total > 0) total else EXPECTED_FILE_BYTES
                        ((report.temporaryPacketSize * 100) / denom).toInt().coerceIn(0, 100)
                    }
                    _uiState.update {
                        it.copy(
                            progressPercent = pct,
                            downloadedBytes = report.temporaryPacketSize,
                            currentSpeedMbps = speedMbps,
                            durationMs = report.reportTime - report.startTime
                        )
                    }
                }

                override fun onError(speedTestError: SpeedTestError, errorMessage: String) {
                    releaseSocket()
                    if (manualStop) {
                        finishTesting()
                        return
                    }
                    // 主源失败自动回退备用源重试一次
                    if (urlIndex == 0) {
                        urlIndex = 1
                        launchDownload()
                    } else {
                        _uiState.update {
                            it.copy(isTesting = false, errorCode = speedTestError.name)
                        }
                    }
                }
            })
            s.startFixedDownload(url, REPORT_INTERVAL_MS, MAX_DURATION_MS)
        }
    }

    private fun finishTesting() {
        _uiState.update { it.copy(isTesting = false) }
        releaseSocket()
    }

    private fun releaseSocket() {
        socket = null
    }

    override fun onCleared() {
        manualStop = true
        socket?.forceStopTask()
        socket = null
    }
}
