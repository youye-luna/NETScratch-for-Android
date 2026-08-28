package youyeluna.lanipscanner.scanner

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 网络工具类（对应原 WinForms 版 DhcpScanner 中的本地网络信息方法）
 */
object NetworkUtils {

    /**
     * 获取本机 IPv4 地址，失败返回 null
     */
    fun getLocalIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                val addresses = intf.inetAddresses ?: continue
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        // 跳过链路本地地址（APIPA）
                        if (!host.startsWith("169.254.")) {
                            return host
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取所有本地 IPv4 子网（前三段），失败时回退到 192.168.1
     */
    fun getLocalSubnets(): List<String> {
        val subnets = LinkedHashSet<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return listOf("192.168.1")
            for (intf in interfaces) {
                val addresses = intf.inetAddresses ?: continue
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("169.254.")) {
                            val parts = host.split(".")
                            if (parts.size == 4) {
                                subnets.add("${parts[0]}.${parts[1]}.${parts[2]}")
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore
        }
        if (subnets.isEmpty()) subnets.add("192.168.1")
        return subnets.toList()
    }

    /**
     * 获取默认扫描范围：本机 IP 所在网段 .1 ~ .255
     */
    fun getDefaultRange(): Pair<String, String> {
        val localIp = getLocalIpv4() ?: "192.168.1.1"
        val parts = localIp.split(".")
        val subnet = if (parts.size == 4) parts.take(3).joinToString(".") else "192.168.1"
        // 确保返回4段格式的IP地址
        val subnetParts = subnet.split(".")
        val normalizedSubnet = when {
            subnetParts.size == 3 -> subnet
            subnetParts.size == 2 -> "${subnetParts[0]}.1.${subnetParts[1]}"
            subnetParts.size == 1 -> "${subnetParts[0]}.1.1"
            else -> "192.168.1"
        }
        return "$normalizedSubnet.1" to "$normalizedSubnet.255"
    }

    /**
     * 检查IP地址是否为内网IP地址
     * 内网IP地址范围：
     * - 10.0.0.0 到 10.255.255.255
     * - 172.16.0.0 到 172.31.255.255
     * - 192.168.0.0 到 192.168.255.255
     */
    fun isPrivateIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false
        
        return when (first) {
            10 -> true
            172 -> second in 16..31
            192 -> second == 168
            else -> false
        }
    }

    /**
     * 验证IP范围是否都在内网范围内
     * @return 如果都在内网范围内返回null，否则返回错误信息
     */
    fun validatePrivateIpRange(startIp: String, endIp: String): String? {
        if (!isPrivateIp(startIp)) {
            return "起始IP地址 $startIp 不是内网IP地址"
        }
        if (!isPrivateIp(endIp)) {
            return "结束IP地址 $endIp 不是内网IP地址"
        }
        return null
    }
}
