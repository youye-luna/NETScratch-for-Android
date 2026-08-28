package youyeluna.lanipscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import youyeluna.lanipscanner.ui.theme.AppColors

/**
 * 测速页：HTTP 下载计时测速，展示进度、实时速度与最终结果
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = AppStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        // 控制卡片
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
                    strings.speedTestCard,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = !uiState.isTesting,
                        onClick = { viewModel.startTest() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(strings.startSpeedTest, fontWeight = FontWeight.Medium) }
                    OutlinedButton(
                        enabled = uiState.isTesting,
                        onClick = { viewModel.stopTest() },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.stop) }
                    OutlinedButton(
                        enabled = !uiState.isTesting &&
                                (uiState.finalSpeedMbps != null || uiState.errorCode != null || uiState.downloadedBytes > 0),
                        onClick = { viewModel.clear() },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.clear) }
                }
            }
        }

        // 测试中：进度条
        if (uiState.isTesting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { uiState.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = AppColors.current.settingsCardBg
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.speedProgressPercent(uiState.progressPercent),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.current.textGray
                )
            }
        }

        // 结果卡片
        if (uiState.isTesting || uiState.finalSpeedMbps != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        strings.speedResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.current.textGray
                    )
                    Spacer(Modifier.height(4.dp))
                    val speed = uiState.finalSpeedMbps ?: uiState.currentSpeedMbps
                    Text(
                        "%.2f Mbps".format(speed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally)
                    ) {
                        StatText(strings.speedTestDownloaded, formatBytes(uiState.downloadedBytes))
                        StatText(strings.speedTestDuration, "%.1fs".format(uiState.durationMs / 1000.0))
                    }
                    if (uiState.finalSpeedMbps != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.speedTestComplete,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnline
                        )
                    }
                }
            }
        }

        // 错误提示
        uiState.errorCode?.let { code ->
            Text(
                text = if (code in SpeedTestViewModel.NETWORK_ERROR_CODES) strings.networkError
                else strings.speedTestFailed(code),
                color = Color(0xFFEF5350),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 空状态
        if (!uiState.isTesting && uiState.finalSpeedMbps == null && uiState.errorCode == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    strings.speedTestReady,
                    color = AppColors.current.textGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 统计项（与 Ping 页样式一致） */
@Composable
private fun StatText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.current.textGray)
    }
}

/** 字节数格式化为可读文本 */
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "${bytes}B"
}

