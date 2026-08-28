package youyeluna.lanipscanner.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import youyeluna.lanipscanner.model.DhcpServerInfo
import youyeluna.lanipscanner.ui.theme.AppColors

// ===== 全局配色（与原版 WinForms 主题一致） =====
internal val ColorDhcp = Color(0xFFFF8A80)      // DHCP 服务器
internal val ColorOnline = Color(0xFF4CAF50)    // 在线
internal val ColorOnlineBlue = Color(0xFF2196F3) // 在线(分布图)
internal val ColorOffline = Color(0xFF9E9E9E)   // 无设备
internal val ColorLinkBlue = Color(0xFF0078D7)  // 品牌蓝

/** 一行 IP 输入（4 个数字段 + 点分隔） */
@Composable
internal fun IpInputRow(
    label: String,
    seg0: String,
    seg1: String,
    seg2: String,
    seg3: String,
    focusRequesters: List<FocusRequester>,
    onSegmentChange: (index: Int, value: String) -> Unit
) {
    val segments = listOf(seg0, seg1, seg2, seg3)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(56.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    Text(
                        ".",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.current.ipDotColor
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .focusRequester(focusRequesters[index]),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = segment,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(3)
                            // 限制最大值为255
                            val numValue = digits.toIntOrNull() ?: 0
                            val clampedDigits = if (numValue > 255) "255" else digits
                            onSegmentChange(index, clampedDigits)
                            if (clampedDigits.length == 3 && index < 3) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/** 网段分组标题 */
@Composable
internal fun SubnetHeader(subnet: String, online: Int, dhcp: Int) {
    val strings = AppStrings.current
    Surface(color = AppColors.current.subnetBg) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${strings.subnet} $subnet",
                fontWeight = FontWeight.Bold,
                color = ColorLinkBlue
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${strings.online} $online · ${strings.dhcp} $dhcp",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.current.textGray
            )
        }
    }
}

/** 单条设备结果 */
@Composable
internal fun ResultRow(info: DhcpServerInfo, onClick: () -> Unit) {
    val strings = AppStrings.current
    val statusColor = when {
        info.isDhcpServer -> ColorDhcp
        info.isActive -> ColorOnline
        else -> ColorOffline
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (info.isActive) AppColors.current.cardBg else AppColors.current.cardBgInactive
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    info.ip,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (info.isDhcpServer) ColorLinkBlue else Color.Unspecified
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${info.macAddress}  ${info.hostName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.current.textGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (info.isActive) {
                    Text(
                        "${info.pingMs} ms",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = ColorOnline
                    )
                }
                if (info.isDhcpServer) {
                    Text(
                        strings.dhcpServer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
                Text(
                    if (info.isActive) strings.statusOnline else strings.statusOffline,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (info.isActive) ColorOnline else ColorOffline
                )
            }
        }
    }
}

/** 设备详情对话框 */
@Composable
internal fun DeviceDetailDialog(
    info: DhcpServerInfo,
    onDismiss: () -> Unit,
    onNavigateToPing: ((ip: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = AppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(info.ip, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DetailRow(strings.ipAddress, info.ip)
                DetailRow(strings.macAddress, info.macAddress)
                DetailRow(strings.hostName, info.hostName)
                DetailRow(strings.latency, if (info.isActive) "${info.pingMs} ms" else "-")
                DetailRow(strings.dhcpServer, if (info.isDhcpServer) strings.yes else strings.no)
                DetailRow(strings.status, if (info.isActive) strings.statusOnline else strings.statusOffline)
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        onDismiss()
                        onNavigateToPing?.invoke(info.ip)
                    }
                ) { Text(strings.ping) }
                if (info.isDhcpServer) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${info.ip}"))
                                )
                            }
                        }
                    ) { Text(strings.visitBackend) }
                }
                TextButton(onClick = onDismiss) { Text(strings.close) }
            }
        }
    )
}

/** 详情行（标签 + 值） */
@Composable
internal fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            label,
            modifier = Modifier.width(90.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.current.textGray
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** 生成 CSV 内容（按网段分组，与原版导出格式一致） */
internal fun buildCsv(results: List<DhcpServerInfo>): String {
    val strings = stringsFor(LanguageManager.currentLanguage)
    val sb = StringBuilder()
    sb.appendLine(strings.csvHeader)
    results.groupBy { it.subnet }.forEach { (subnet, devices) ->
        devices.forEach { r ->
            val row = listOf(
                subnet,
                r.ip,
                r.macAddress,
                r.hostName,
                if (r.isActive) r.pingMs.toString() else "-",
                if (r.isDhcpServer) strings.yes else strings.no,
                if (r.isActive) strings.statusOnline else strings.statusOffline
            )
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
    }
    return sb.toString()
}

internal fun escapeCsv(field: String): String {
    return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
        "\"${field.replace("\"", "\"\"")}\""
    } else {
        field
    }
}
