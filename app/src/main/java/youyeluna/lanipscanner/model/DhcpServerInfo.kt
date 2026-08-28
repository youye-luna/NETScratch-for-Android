package youyeluna.lanipscanner.model

/**
 * 扫描结果信息（对应原 WinForms 版 DhcpServerInfo）
 */
data class DhcpServerInfo(
    val ip: String,
    var macAddress: String = "-",
    var hostName: String = "-",
    var pingMs: Long = -1,
    var isActive: Boolean = false,
    var isDhcpServer: Boolean = false
) {
    /** 前三段，用于按网段分组 */
    val subnet: String get() = ip.substringBeforeLast('.')
}
