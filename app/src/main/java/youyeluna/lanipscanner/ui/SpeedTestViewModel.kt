package youyeluna.lanipscanner.ui

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import youyeluna.lanipscanner.App
import youyeluna.lanipscanner.model.SpeedTestHistory
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

/** 测速阶段 */
enum class TestPhase { IDLE, DOWNLOADING, UPLOADING }

/** 测速页 UI 状态 */
data class SpeedTestUiState(
    val isTesting: Boolean = false,
    val phase: TestPhase = TestPhase.IDLE,
    /** 当前阶段进度百分比 */
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val uploadedBytes: Long = 0L,
    /** 当前阶段实时速度（Mbps） */
    val currentSpeedMbps: Double = 0.0,
    /** 下载最终速度 */
    val downloadSpeedMbps: Double? = null,
    /** 上传最终速度 */
    val uploadSpeedMbps: Double? = null,
    /** 本轮测速总耗时（毫秒，从点击开始算起） */
    val durationMs: Long = 0L,
    /** 当前阶段起始时钟（SystemClock.elapsedRealtime，用于阶段计时） */
    val phaseStartElapsedMs: Long = 0L,
    /** 原始错误码，UI 层负责转换为本地化文案 */
    val errorCode: String? = null,
    /** 上传阶段失败（下载结果仍有效） */
    val uploadError: Boolean = false
)

/**
 * 测速页 ViewModel：下载 + 上传 HTTP 计时测速
 *
 * 实现说明（参考 JSpeedTest 方案，fr.bmartel:jspeedtest，MIT License）：
 * - HTTPS 流式下载/上传计时，基于标准 HttpURLConnection（JSpeedTest 手写 TLS socket
 *   在部分现代 CDN 环境拿不到数据，故改用标准协议栈）
 * - 单次测速内置最大时长保护（各 15 秒），弱网环境也不会长时间占用
 * - 下载完成后自动接续上传测试；下载阶段失败自动回退备用测速源重试一次
 * - 下载流式读取、上传循环写随机缓冲，全程不落盘、不整块缓存内存，测速结束即释放
 * - [onCleared] 时取消任务并断开连接，防止页面销毁后后台残留
 */
class SpeedTestViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SpeedTestVM"

        /** 下载标称大小（字节），服务端未返回长度时用于估算百分比 */
        private const val EXPECTED_DOWNLOAD_BYTES = 100L * 1000 * 1000

        /** 上传标称大小（字节），上传无 Content-Length，按此估算百分比 */
        private const val EXPECTED_UPLOAD_BYTES = 50L * 1000 * 1000

        /** 进度回调间隔（毫秒） */
        private const val REPORT_INTERVAL_MS = 300

        /** 单阶段最大测速时长（毫秒） */
        private const val MAX_PHASE_DURATION_MS = 15_000

        /** 连接超时（毫秒） */
        private const val CONNECT_TIMEOUT_MS = 8_000

        /** 下载读超时（毫秒） */
        private const val DOWNLOAD_READ_TIMEOUT_MS = 10_000

        /** 主下载测速源（HTTPS 大文件，国内 CDN 长期稳定，避免 targetSdk 明文流量限制） */
        private const val PRIMARY_URL = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

        /** 备用下载测速源（Cloudflare HTTPS 下行端点） */
        private const val FALLBACK_URL = "https://speed.cloudflare.com/__down?bytes=100000000"

        /** 上传测速端点（Cloudflare 接收任意 POST 上行并丢弃） */
        private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"

        /** 上传随机缓冲大小（字节）：预生成循环写入，避免压缩干扰与重复分配 */
        private const val UPLOAD_BUFFER_SIZE = 512 * 1024

        /** 错误码：网络类异常（UI 层展示统一的网络异常提示） */
        const val ERROR_NETWORK = "NETWORK"

        /** 测速历史最大保留条数 */
        private const val MAX_HISTORY_COUNT = 100
    }

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    private val repository = (application as App).speedTestHistoryRepository

    private var job: Job? = null
    private var connection: HttpURLConnection? = null
    private var manualStop = false

    /** 当前尝试的下载测速源：0=主源，1=备用源 */
    private var urlIndex = 0

    /** 本轮测速起始时间（总耗时口径） */
    private var sessionStartMs = 0L

    /** 开始测速：先下载，完成后自动接上传 */
    fun startTest() {
        if (_uiState.value.isTesting) return
        urlIndex = 0
        manualStop = false
        sessionStartMs = SystemClock.elapsedRealtime()
        _uiState.update {
            it.copy(
                isTesting = true,
                phase = TestPhase.DOWNLOADING,
                phaseStartElapsedMs = sessionStartMs,
                progressPercent = 0,
                downloadedBytes = 0,
                uploadedBytes = 0,
                currentSpeedMbps = 0.0,
                downloadSpeedMbps = null,
                uploadSpeedMbps = null,
                durationMs = 0,
                errorCode = null,
                uploadError = false
            )
        }
        launchDownload()
    }

    /** 停止测速（保留已完成阶段的结果） */
    fun stopTest() {
        if (!_uiState.value.isTesting) return
        manualStop = true
        job?.cancel()
        connection?.disconnect()
    }

    /** 清空结果 */
    fun clear() {
        if (_uiState.value.isTesting) return
        _uiState.value = SpeedTestUiState()
    }

    // ---------- 下载 ----------

    /** 启动一次下载测速（网络必须在 IO 线程执行，否则 DNS/建连触发 NetworkOnMainThreadException） */
    private fun launchDownload() {
        val url = if (urlIndex == 0) PRIMARY_URL else FALLBACK_URL
        job = viewModelScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = newConnection(url).apply {
                    requestMethod = "GET"
                    readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                }
                connection = conn
                conn.connect()

                val contentLength = conn.contentLengthLong
                var total = 0L
                var lastReportAt = 0L
                val buf = ByteArray(64 * 1024)

                conn.inputStream.use { input ->
                    while (isActive) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        val now = SystemClock.elapsedRealtime()
                        if (now - sessionStartOfPhase() >= MAX_PHASE_DURATION_MS) break
                        if (now - lastReportAt >= REPORT_INTERVAL_MS) {
                            lastReportAt = now
                            reportPhaseProgress(
                                percent = estimatePercent(total, contentLength, EXPECTED_DOWNLOAD_BYTES, includeDone = false),
                                bytes = total,
                                phaseElapsed = now - sessionStartOfPhase()
                            )
                        }
                    }
                }

                releaseConnection()
                if (manualStop) {
                    finishTesting()
                    return@launch
                }
                if (total <= 0L) {
                    // 服务端无数据返回，视为失败走回退或报错
                    handleDownloadFailure()
                    return@launch
                }
                val phaseElapsed = phaseElapsedNow()
                val mbps = calcMbps(total, phaseElapsed)
                _uiState.update {
                    it.copy(
                        progressPercent = 100,
                        downloadedBytes = total,
                        downloadSpeedMbps = mbps,
                        currentSpeedMbps = 0.0,
                        phase = TestPhase.UPLOADING,
                        phaseStartElapsedMs = SystemClock.elapsedRealtime()
                    )
                }
                launchUpload()
            } catch (e: Exception) {
                Log.e(TAG, "download failed on #$urlIndex: $e")
                releaseConnection()
                if (manualStop) finishTesting() else handleDownloadFailure()
            }
        }
    }

    /** 下载失败：主源失败自动回退备用源重试一次，仍失败则整体报错 */
    private fun handleDownloadFailure() {
        if (urlIndex == 0) {
            urlIndex = 1
            launchDownload()
        } else {
            _uiState.update {
                it.copy(isTesting = false, phase = TestPhase.IDLE, errorCode = ERROR_NETWORK)
            }
        }
    }

    // ---------- 上传 ----------

    /** 启动上传测速：POST 随机数据循环写入，上传失败不影响已得到的下载结果 */
    private fun launchUpload() {
        job = viewModelScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = newConnection(UPLOAD_URL).apply {
                    requestMethod = "POST"
                    doOutput = true
                    readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                    setRequestProperty("Content-Type", "application/octet-stream")
                    // 数据量取决于测速时长，用 chunked 流式上传
                    setChunkedStreamingMode(64 * 1024)
                }
                connection = conn
                conn.connect()

                // 预生成随机缓冲：压缩对真实上行速率影响极小，但更接近真实流量特征
                val random = Random()
                val payload = ByteArray(UPLOAD_BUFFER_SIZE).also { random.nextBytes(it) }

                var total = 0L
                var lastReportAt = 0L
                conn.outputStream.use { output ->
                    while (isActive) {
                        val now = SystemClock.elapsedRealtime()
                        if (now - sessionStartOfPhase() >= MAX_PHASE_DURATION_MS) break
                        output.write(payload)
                        total += payload.size
                        if (now - lastReportAt >= REPORT_INTERVAL_MS) {
                            lastReportAt = now
                            reportPhaseProgress(
                                percent = estimatePercent(total, -1, EXPECTED_UPLOAD_BYTES, includeDone = false),
                                bytes = total,
                                phaseElapsed = now - sessionStartOfPhase()
                            )
                        }
                    }
                    output.flush()
                }

                // 数据发完即记录结束时间，等待服务端响应的阻塞时间不计入速率
                val uploadEndMs = SystemClock.elapsedRealtime()
                val responseCode = conn.responseCode
                releaseConnection()
                if (manualStop) {
                    finishTesting()
                    return@launch
                }
                val phaseElapsed = (uploadEndMs - sessionStartOfPhase()).coerceAtLeast(1)
                val mbps = calcMbps(total, phaseElapsed)
                if (total <= 0L || responseCode !in 200..299) {
                    _uiState.update {
                        it.copy(isTesting = false, phase = TestPhase.IDLE, uploadError = true)
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        phase = TestPhase.IDLE,
                        progressPercent = 100,
                        uploadedBytes = total,
                        uploadSpeedMbps = mbps
                    )
                }
                saveHistoryRecord(uploadSucceeded = true)
            } catch (e: Exception) {
                Log.e(TAG, "upload failed: $e")
                releaseConnection()
                if (manualStop) {
                    finishTesting()
                } else {
                    // 上传失败不影响下载结果，仅标记上传失败
                    _uiState.update {
                        it.copy(isTesting = false, phase = TestPhase.IDLE, uploadError = true)
                    }
                    saveHistoryRecord(uploadSucceeded = false)
                }
            }
        }
    }

    // ---------- 工具 ----------

    private fun newConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Connection", "close")
        }

    /** 当前阶段起始时间 */
    private fun sessionStartOfPhase(): Long = _uiState.value.phaseStartElapsedMs

    private fun phaseElapsedNow(): Long =
        (SystemClock.elapsedRealtime() - sessionStartOfPhase()).coerceAtLeast(1)

    /** 回报当前阶段进度（总耗时也一并刷新） */
    private fun reportPhaseProgress(percent: Int, bytes: Long, phaseElapsed: Long) {
        _uiState.update {
            it.copy(
                progressPercent = percent,
                downloadedBytes = if (it.phase == TestPhase.DOWNLOADING) bytes else it.downloadedBytes,
                uploadedBytes = if (it.phase == TestPhase.UPLOADING) bytes else it.uploadedBytes,
                durationMs = SystemClock.elapsedRealtime() - sessionStartMs
            )
        }
    }

    private fun finishTesting() {
        _uiState.update {
            it.copy(
                isTesting = false,
                phase = TestPhase.IDLE,
                durationMs = SystemClock.elapsedRealtime() - sessionStartMs
            )
        }
        releaseConnection()
        saveHistoryRecord(uploadSucceeded = false)
    }

    // ---------- 历史 ----------

    /** 测试结束后写入历史（下载有结果才记录，超量自动清理最旧的） */
    private fun saveHistoryRecord(uploadSucceeded: Boolean) {
        val s = _uiState.value
        val download = s.downloadSpeedMbps ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.save(
                    SpeedTestHistory(
                        testTime = System.currentTimeMillis(),
                        downloadSpeedMbps = download,
                        uploadSpeedMbps = s.uploadSpeedMbps ?: 0.0,
                        uploadSucceeded = uploadSucceeded,
                        downloadedBytes = s.downloadedBytes,
                        uploadedBytes = s.uploadedBytes,
                        durationMs = s.durationMs,
                        downloadUrl = if (urlIndex == 0) PRIMARY_URL else FALLBACK_URL
                    )
                )
                repository.cleanByCount(MAX_HISTORY_COUNT)
            } catch (e: Exception) {
                Log.e(TAG, "save history failed: $e")
            }
        }
    }

    /** 删除单条测速历史 */
    fun deleteHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteById(id)
            } catch (_: Exception) {
            }
        }
    }

    /** 清空测速历史 */
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAll()
            } catch (_: Exception) {
            }
        }
    }

    private fun releaseConnection() {
        connection?.disconnect()
        connection = null
    }

    /** 平均速率：字节 × 8 bit / 毫秒 → Mbps */
    private fun calcMbps(bytes: Long, elapsedMs: Long): Double =
        bytes * 8.0 / elapsedMs / 1000.0

    /** 进度百分比：优先用服务端 Content-Length（-1 表示未知），缺失时按标称大小估算 */
    private fun estimatePercent(bytes: Long, contentLength: Long, nominal: Long, includeDone: Boolean): Int {
        val denom = if (contentLength > 0) contentLength else nominal
        val pct = (bytes * 100 / denom).toInt()
        return if (includeDone) pct.coerceIn(0, 100) else pct.coerceIn(0, 99)
    }

    override fun onCleared() {
        manualStop = true
        job?.cancel()
        releaseConnection()
    }
}
