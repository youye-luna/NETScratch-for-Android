package youyeluna.lanipscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import youyeluna.lanipscanner.ui.theme.AppColors
import youyeluna.lanipscanner.scanner.NetworkUtils

/** 数据刷新频率选项（毫秒） */
private val IntervalOptions = listOf(500L to "0.5s", 1000L to "1s", 2000L to "2s", 5000L to "5s")

/**
 * Ping 页：连续 ping 指定 IP，展示统计与历史结果
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PingScreen(
    viewModel: PingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingIp by viewModel.pendingIp.collectAsState()
    val strings = AppStrings.current

    var isDomainMode by remember { mutableStateOf(false) }
    var domainText by remember { mutableStateOf("") }
    val defaultIp = remember { NetworkUtils.getLocalIpv4() ?: "192.168.1.1" }
    val defaultParts = remember { defaultIp.split(".") }
    var ipSeg0 by remember { mutableStateOf(defaultParts.getOrElse(0) { "" }) }
    var ipSeg1 by remember { mutableStateOf(defaultParts.getOrElse(1) { "" }) }
    var ipSeg2 by remember { mutableStateOf(defaultParts.getOrElse(2) { "" }) }
    var ipSeg3 by remember { mutableStateOf(defaultParts.getOrElse(3) { "" }) }
    val focus = remember { List(4) { FocusRequester() } }

    LaunchedEffect(pendingIp) {
        pendingIp?.let { ip ->
            isDomainMode = false
            val parts = ip.split(".")
            if (parts.size == 4) {
                ipSeg0 = parts[0]
                ipSeg1 = parts[1]
                ipSeg2 = parts[2]
                ipSeg3 = parts[3]
            }
            viewModel.consumePendingIp()
            viewModel.start(ip)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    strings.pingTest,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                // IP / 域名模式切换
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDomainMode) {
                        OutlinedButton(
                            onClick = { isDomainMode = false },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(strings.pingIp, fontWeight = FontWeight.Medium) }
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text(strings.pingDomain, fontWeight = FontWeight.Medium) }
                    } else {
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text(strings.pingIp, fontWeight = FontWeight.Medium) }
                        OutlinedButton(
                            onClick = { isDomainMode = true },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(strings.pingDomain, fontWeight = FontWeight.Medium) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (isDomainMode) {
                    OutlinedTextField(
                        value = domainText,
                        onValueChange = { domainText = it },
                        label = { Text(strings.targetDomain) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    IpInputRow(
                        label = strings.targetIp,
                        seg0 = ipSeg0,
                        seg1 = ipSeg1,
                        seg2 = ipSeg2,
                        seg3 = ipSeg3,
                        focusRequesters = focus
                    ) { index, value ->
                        when (index) {
                            0 -> ipSeg0 = value
                            1 -> ipSeg1 = value
                            2 -> ipSeg2 = value
                            3 -> ipSeg3 = value
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = !uiState.isPinging,
                        onClick = {
                            val target = if (isDomainMode) domainText.trim()
                                         else "$ipSeg0.$ipSeg1.$ipSeg2.$ipSeg3"
                            viewModel.start(target)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(strings.startPing, fontWeight = FontWeight.Medium) }
                    OutlinedButton(
                        enabled = uiState.isPinging,
                        onClick = { viewModel.stop() },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.stop) }
                    OutlinedButton(
                        enabled = !uiState.isPinging && uiState.sent > 0,
                        onClick = { viewModel.clear() },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.clear) }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatText(strings.sent, uiState.sent.toString())
            StatText(strings.received, uiState.received.toString())
            StatText(strings.packetLoss, "${uiState.lostPercent}%")
            StatText(strings.average, if (uiState.received > 0) "${uiState.avgRtt}ms" else "-")
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        // 延迟趋势图（X 轴：秒，Y 轴：ms，实时更新）
        if (uiState.isPinging || uiState.items.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    PingChartLegend()
                    Spacer(Modifier.height(4.dp))
                    PingLatencyChart(
                        items = uiState.items,
                        startTime = uiState.startTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // 数据刷新频率控制
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.refreshRate,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.current.textGray
                        )
                        Spacer(Modifier.width(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IntervalOptions.forEach { (ms, label) ->
                                val selected = uiState.intervalMs == ms
                                if (selected) {
                                    Button(
                                        onClick = { viewModel.setInterval(ms) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) { Text(label, fontSize = 12.sp) }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.setInterval(ms) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) { Text(label, fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.items.isEmpty()) {
            if (!uiState.isPinging) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "${strings.noPingResults}\n${strings.noPingResultsHint}",
                        color = AppColors.current.textGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        } else {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            // 新数据到来时自动滚动到顶部（最新数据）
            LaunchedEffect(uiState.items.size) {
                if (uiState.items.isNotEmpty()) {
                    listState.scrollToItem(uiState.items.size - 1)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(uiState.items, key = { it.index }) { item ->
                    val ip = item.ip
                    val rtt = item.rttMs
                    if (rtt != null) {
                        Text(
                            text = strings.pingReply(ip, item.bytes, rtt, item.ttl),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    } else {
                        Text(
                            text = strings.pingTimeout(ip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEF5350),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 统计项 */
@Composable
private fun StatText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.current.textGray)
    }
}
