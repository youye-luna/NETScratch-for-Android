package youyeluna.lanipscanner.scanner

import youyeluna.lanipscanner.model.DhcpServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/** Ping 详细结果（含 RTT、TTL、字节数） */
data class PingResult(
    val rttMs: Long,
    val ttl: Int,
    val bytes: Int,
    val success: Boolean
)

/**
 * 局域网扫描器（对应原 WinForms 版 DhcpScanner）
 *
 * Android 平台差异说明：
 *  - 普通应用无法直接发原始 ICMP，改用系统自带 ping 命令（/system/bin/ping）探测在线。
 *  - MAC 地址通过读取 /proc/net/arp（ping 之后内核会填充 ARP 缓存）获取。
 *  - 端口探测使用 TCP connect + 超时。
 */
object DhcpScanner {

    /** 并行度，与原版 MaxDegreeOfParallelism=30 保持一致 */
    private const val MAX_PARALLELISM = 30
    private const val PING_TIMEOUT_SECONDS = 1
    private const val PORT_TIMEOUT_MS = 300
    private const val MAX_SUBNETS = 100

    /**
     * 扫描 IP 范围（含起始与结束）。
     *
     * @param startIp 起始 IP，如 192.168.1.1
     * @param endIp 结束 IP，如 192.168.1.254
     * @param onProgress 进度回调（completed/total），可能从后台线程调用，回调内请用线程安全的方式更新 UI 状态
     * @return 按 IP 升序排列的扫描结果
     */
    suspend fun scanIpRange(
        startIp: String,
        endIp: String,
        onProgress: (completed: Int, total: Int) -> Unit
    ): List<DhcpServerInfo> = withContext(Dispatchers.IO) {
        val start = ipToLong(startIp)
        val end = ipToLong(endIp)
        require(start <= end) { "起始IP不能大于结束IP" }

        // 构建有效 IP 列表（跳过最后一段为 0 的 IP，与原版一致）
        val ipList = mutableListOf<Long>()
        var n = start
        while (n <= end) {
            if ((n and 0xFFL) != 0L) ipList.add(n)
            n++
        }

        // 统计网段数量（前三段相同为一个网段），超过上限直接抛出
        val subnetCount = ipList.map { it shr 8 }.distinct().size
        require(subnetCount <= MAX_SUBNETS) { "TOO_MANY_SUBNETS:$subnetCount" }

        val total = ipList.size
        val completed = AtomicInteger(0)
        val results = ConcurrentLinkedQueue<DhcpServerInfo>()
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLELISM)

        coroutineScope {
            val jobs = ipList.map { ipNum ->
                async(limitedDispatcher) {
                    val info = scanSingleIp(longToIp(ipNum))
                    results.add(info)
                    val c = completed.incrementAndGet()
                    onProgress(c, total)
                }
            }
            jobs.forEach { it.join() }
        }

        results.sortedBy { ipToLong(it.ip) }
    }

    /** 检测是否有root权限 */
    fun hasRootPermission(): Boolean {
        return try {
            val process = ProcessBuilder("su", "-c", "echo root")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            output.contains("root")
        } catch (_: Exception) {
            false
        }
    }

    /** 扫描单个 IP（内部用于后台线程） */
    private fun scanSingleIp(ip: String): DhcpServerInfo {
        val info = DhcpServerInfo(ip = ip)
        val rtt = doPing(ip)
        if (rtt != null) {
            info.isActive = true
            info.pingMs = rtt
            // 尝试获取MAC地址（Android 10+非root设备可能无法获取）
            info.macAddress = getMacAddress(ip)
            info.hostName = getHostName(ip)
            info.isDhcpServer = isLikelyRouterOrDhcp(ip)
        }
        return info
    }

    /** 对外暴露的 Ping（用于详情对话框），返回 RTT 毫秒数，无响应返回 null */
    suspend fun ping(ip: String): Long? = withContext(Dispatchers.IO) { doPing(ip) }

    /** 对外暴露的 Ping（含 TTL 和字节数），无响应返回 null */
    suspend fun pingWithDetail(ip: String): PingResult? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(
                "/system/bin/ping", "-c", "1", "-W", PING_TIMEOUT_SECONDS.toString(), ip
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            if (process.exitValue() == 0) {
                val rtt = Regex("""time[=<]\s*([0-9.]+)""").find(output)
                    ?.groupValues?.get(1)?.toFloatOrNull()?.toLong() ?: 0L
                val ttl = Regex("""ttl[=:]\s*(\d+)""", RegexOption.IGNORE_CASE).find(output)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val bytes = Regex("""(\d+)\s+bytes\s+from""").find(output)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 32
                PingResult(rttMs = rtt, ttl = ttl, bytes = bytes, success = true)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 调用系统 ping 命令探测主机。
     * @return RTT 毫秒数；主机不可达或命令执行失败返回 null
     */
    private fun doPing(ip: String): Long? {
        return try {
            val process = ProcessBuilder(
                "/system/bin/ping", "-c", "1", "-W", PING_TIMEOUT_SECONDS.toString(), ip
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            if (process.exitValue() == 0) {
                // 解析 "time=1.23 ms" 或 "time<1 ms"（toybox 输出格式）
                Regex("""time[=<]\s*([0-9.]+)""").find(output)
                    ?.groupValues?.get(1)?.toFloatOrNull()?.toLong() ?: 0L
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 判断是否可能是路由器或 DHCP 服务器（检查常见端口，与原版一致） */
    private fun isLikelyRouterOrDhcp(ip: String): Boolean {
        return listOf(80, 443, 8080, 67, 53).any { port -> checkPort(ip, port, PORT_TIMEOUT_MS) }
    }

    /** 检查 TCP 端口是否开放 */
    private fun checkPort(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                socket.isConnected
            }
        } catch (_: Exception) {
            false
        }
    }

    /** 获取MAC地址，尝试多种方式 */
    private fun getMacAddress(ip: String): String {
        // 方式1: 直接读取 /proc/net/arp（最快）
        val mac1 = getMacFromArpFile()
        val mac1Result = findMacInArpContent(mac1, ip)
        if (mac1Result != "-") return mac1Result
        
        // 方式2: 尝试 ip neigh show
        val macFromIpNeigh = getMacFromIpNeigh(ip)
        if (macFromIpNeigh != "-") return macFromIpNeigh
        
        // 方式3: 通过shell读取arp
        val mac2 = getMacFromShellArp(ip)
        if (mac2 != "-") return mac2
        
        // 方式4: 尝试 arp -a 命令
        return getMacFromArpAll(ip)
    }

    /** 直接读取 /proc/net/arp 文件内容 */
    private fun getMacFromArpFile(): String {
        return try {
            File("/proc/net/arp").readText()
        } catch (_: Exception) {
            ""
        }
    }

    /** 从ARP内容中查找指定IP的MAC地址 */
    private fun findMacInArpContent(content: String, ip: String): String {
        if (content.isBlank()) return "-"
        for (line in content.lines()) {
            if (line.startsWith("IP") || line.isBlank()) continue
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 4 && parts[0] == ip) {
                val mac = parts[3].uppercase()
                if (mac.isNotBlank() && mac != "00:00:00:00:00:00" && mac.contains(":")) {
                    return mac
                }
            }
        }
        return "-"
    }

    /** 通过 ip neigh 命令获取 MAC 地址 */
    private fun getMacFromIpNeigh(ip: String): String {
        return try {
            // 尝试多种命令格式
            val commands = listOf(
                listOf("ip", "neigh", "show", ip),
                listOf("ip", "neigh", "get", ip),
                listOf("ip", "-4", "neigh", "show", ip)
            )
            for (cmd in commands) {
                val result = execCommand(cmd)
                val mac = extractMacFromOutput(result)
                if (mac != "-") return mac
            }
            "-"
        } catch (_: Exception) {
            "-"
        }
    }

    /** 通过shell执行arp命令 */
    private fun getMacFromShellArp(ip: String): String {
        return try {
            val result = execCommand(listOf("sh", "-c", "arp -n $ip"))
            extractMacFromOutput(result)
        } catch (_: Exception) {
            "-"
        }
    }

    /** 通过 arp -a 命令获取 */
    private fun getMacFromArpAll(ip: String): String {
        return try {
            val result = execCommand(listOf("arp", "-a"))
            // 查找包含目标IP的行
            for (line in result.lines()) {
                if (line.contains(ip)) {
                    val mac = extractMacFromOutput(line)
                    if (mac != "-") return mac
                }
            }
            "-"
        } catch (_: Exception) {
            "-"
        }
    }

    /** 执行命令并返回输出 */
    private fun execCommand(command: List<String>): String {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
        return output
    }

    /** 从输出中提取MAC地址 */
    private fun extractMacFromOutput(output: String): String {
        // 匹配标准MAC地址格式 XX:XX:XX:XX:XX:XX
        val macRegex = Regex("""([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2})""")
        val match = macRegex.find(output)
        return if (match != null) {
            val mac = match.groupValues[1].uppercase()
            if (mac != "00:00:00:00:00:00") mac else "-"
        } else {
            "-"
        }
    }

    /** 从 /proc/net/arp 读取 MAC 地址（保留用于兼容） */
    private fun getMacFromArpCache(ip: String): String {
        val content = getMacFromArpFile()
        return findMacInArpContent(content, ip)
    }

    /** 反向 DNS 解析主机名 */
    private fun getHostName(ip: String): String {
        return try {
            val name = InetAddress.getByName(ip).canonicalHostName
            if (name == ip) "-" else name
        } catch (_: Exception) {
            "-"
        }
    }

    /** 将 IP 地址转为 long 数值 */
    fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        require(parts.size == 4) { "IP格式不正确: $ip" }
        return (parts[0].toLong() shl 24) or
                (parts[1].toLong() shl 16) or
                (parts[2].toLong() shl 8) or
                parts[3].toLong()
    }

    /** 将 long 数值转为 IP 地址字符串 */
    fun longToIp(value: Long): String =
        "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
}
