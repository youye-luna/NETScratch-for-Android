package youyeluna.lanipscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import youyeluna.lanipscanner.ui.theme.AppColors

/**
 * 首页：设置扫描范围
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    onOpenResults: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = AppStrings.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }

    // MAC 地址限制提示 — 始终可见，仅 Android 10+ 且无 root
    val showMacWarning = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !uiState.hasRootPermission
    }
    var macWarningDismissed by remember {
        mutableStateOf(prefs.getBoolean("mac_warning_dismissed", false))
    }

    // 首次进入弹窗
    var showFirstRunDialog by remember {
        mutableStateOf(prefs.getBoolean("first_run_dialog_shown", true))
    }

    if (showFirstRunDialog && showMacWarning) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(strings.macWarningDialogTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.macWarningDialogMsg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showFirstRunDialog = false
                    prefs.edit().putBoolean("first_run_dialog_shown", false).apply()
                }) { Text(strings.macWarningAck) }
            }
        )
    }

    val startParts = viewModel.defaultStartIp.split(".")
    val endParts = viewModel.defaultEndIp.split(".")
    val startInit = startParts.take(4) + List(maxOf(0, 4 - startParts.size)) { "" }
    val endInit = endParts.take(4) + List(maxOf(0, 4 - endParts.size)) { "" }

    var startSeg0 by remember { mutableStateOf(startInit[0]) }
    var startSeg1 by remember { mutableStateOf(startInit[1]) }
    var startSeg2 by remember { mutableStateOf(startInit[2]) }
    var startSeg3 by remember { mutableStateOf(startInit[3]) }
    var endSeg0 by remember { mutableStateOf(endInit[0]) }
    var endSeg1 by remember { mutableStateOf(endInit[1]) }
    var endSeg2 by remember { mutableStateOf(endInit[2]) }
    var endSeg3 by remember { mutableStateOf(endInit[3]) }

    val startFocus = remember { List(4) { FocusRequester() } }
    val endFocus = remember { List(4) { FocusRequester() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
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
                    strings.searchRangeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                IpInputRow(
                    label = strings.startIp,
                    seg0 = startSeg0,
                    seg1 = startSeg1,
                    seg2 = startSeg2,
                    seg3 = startSeg3,
                    focusRequesters = startFocus
                ) { index, value ->
                    when (index) {
                        0 -> startSeg0 = value
                        1 -> startSeg1 = value
                        2 -> startSeg2 = value
                        3 -> startSeg3 = value
                    }
                }
                Spacer(Modifier.height(8.dp))
                IpInputRow(
                    label = strings.endIp,
                    seg0 = endSeg0,
                    seg1 = endSeg1,
                    seg2 = endSeg2,
                    seg3 = endSeg3,
                    focusRequesters = endFocus
                ) { index, value ->
                    when (index) {
                        0 -> endSeg0 = value
                        1 -> endSeg1 = value
                        2 -> endSeg2 = value
                        3 -> endSeg3 = value
                    }
                }

                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = !uiState.isScanning,
                        onClick = {
                            viewModel.startScan(
                                "$startSeg0.$startSeg1.$startSeg2.$startSeg3",
                                "$endSeg0.$endSeg1.$endSeg2.$endSeg3"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(strings.startScan, fontWeight = FontWeight.Medium) }
                    OutlinedButton(
                        enabled = uiState.isScanning,
                        onClick = { viewModel.stopScan() },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.stopScan) }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                uiState.statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.statusText.contains("不是内网IP地址")) {
                    AppColors.current.errorColor
                } else {
                    Color.Unspecified
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                "${strings.scanned} ${uiState.scannedCount} · ${strings.online} ${uiState.onlineCount} · ${strings.dhcp} ${uiState.dhcpCount}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.current.textGray
            )
        }
        if (uiState.isScanning) {
            LinearProgressIndicator(
                progress = { if (uiState.progress > 0) uiState.progress / 100f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        if (!uiState.isScanning && uiState.results.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        strings.scanComplete,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    val noDevice = uiState.results.size - uiState.onlineCount
                    Text(
                        "${strings.totalIp} ${uiState.results.size}${strings.ipCount} · ${strings.online} ${uiState.onlineCount} · ${strings.noDevice} $noDevice · ${strings.dhcp} ${uiState.dhcpCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.current.textGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onOpenResults,
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(strings.viewResults, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }

        // MAC 地址限制提示卡片 — 始终显示，样式与扫描完成卡片一致
        if (showMacWarning && !macWarningDismissed) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        strings.macWarningTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        strings.macWarningDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.current.textGray
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            macWarningDismissed = true
                            prefs.edit().putBoolean("mac_warning_dismissed", true).apply()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.macWarningAck, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}
