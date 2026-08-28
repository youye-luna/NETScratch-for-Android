package youyeluna.lanipscanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import youyeluna.lanipscanner.scanner.DhcpScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 单次 ping 结果 */
data class PingItem(
    val index: Int,
    val ip: String,
    val rttMs: Long?,
    val ttl: Int,
    val bytes: Int,
    val time: String,
    val timestamp: Long = 0 // 采集时刻，用于图表 X 轴（经过秒数）
)

/** Ping 页面状态 */
data class PingUiState(
    val targetIp: String = "",
    val isPinging: Boolean = false,
    val items: List<PingItem> = emptyList(),
    val sent: Int = 0,
    val received: Int = 0,
    val minRtt: Long = 0,
    val maxRtt: Long = 0,
    val totalRtt: Long = 0,
    val startTime: Long = 0, // 本次测试开始时刻，图表 X 轴零点
    val intervalMs: Long = 1000 // 数据刷新间隔
) {
    val lostPercent: Int get() = if (sent > 0) ((sent - received) * 100) / sent else 0
    val avgRtt: Long get() = if (received > 0) totalRtt / received else 0
}

class PingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PingUiState())
    val uiState: StateFlow<PingUiState> = _uiState.asStateFlow()

    /** 外部请求 ping 的目标 IP，PingScreen 观察到后自动开始 */
    private val _pendingIp = MutableStateFlow<String?>(null)
    val pendingIp: StateFlow<String?> = _pendingIp.asStateFlow()

    private var job: Job? = null

    /** 外部（详情弹窗）请求跳转 ping 页面并自动 ping */
    fun requestPing(ip: String) {
        stop()
        clear()
        _pendingIp.value = ip
    }

    /** PingScreen 消费后调用，避免重复触发 */
    fun consumePendingIp() {
        _pendingIp.value = null
    }

    /** 连续 ping 单个 IP，直到停止 */
    fun start(ip: String) {
        if (_uiState.value.isPinging) { stop() }
        job?.cancel()
        val keepInterval = _uiState.value.intervalMs
        _uiState.value = PingUiState(
            isPinging = true,
            targetIp = ip,
            startTime = System.currentTimeMillis(),
            intervalMs = keepInterval
        )
        job = viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            var index = 1
            while (isActive) {
                val result = DhcpScanner.pingWithDetail(ip)
                _uiState.update { s ->
                    val rtt = result?.rttMs
                    val newReceived = s.received + if (result != null) 1 else 0
                    val newMin = if (rtt != null && (s.received == 0 || rtt < s.minRtt)) rtt else s.minRtt
                    val newMax = if (rtt != null && rtt > s.maxRtt) rtt else s.maxRtt
                    s.copy(
                        items = (s.items + PingItem(
                            index = index,
                            ip = ip,
                            rttMs = rtt,
                            ttl = result?.ttl ?: 0,
                            bytes = result?.bytes ?: 32,
                            time = timeFormat.format(Date()),
                            timestamp = System.currentTimeMillis()
                        )).takeLast(200),
                        sent = s.sent + 1,
                        received = newReceived,
                        minRtt = newMin,
                        maxRtt = newMax,
                        totalRtt = s.totalRtt + (rtt ?: 0)
                    )
                }
                index++
                delay(_uiState.value.intervalMs)
            }
        }
    }

    /** 设置数据刷新间隔（毫秒），立即生效 */
    fun setInterval(ms: Long) {
        _uiState.update { it.copy(intervalMs = ms) }
    }

    fun stop() {
        job?.cancel()
        _uiState.update { it.copy(isPinging = false) }
    }

    fun clear() {
        job?.cancel()
        _uiState.value = PingUiState()
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}
