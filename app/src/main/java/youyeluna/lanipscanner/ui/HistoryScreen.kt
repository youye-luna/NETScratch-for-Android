package youyeluna.lanipscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import youyeluna.lanipscanner.model.DhcpServerInfo
import youyeluna.lanipscanner.model.ScanHistory
import youyeluna.lanipscanner.model.SpeedTestHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    onHistoryClick: (Long) -> Unit
) {
    val historyList by historyViewModel.allHistory.collectAsState()
    val speedHistory by historyViewModel.allSpeedTestHistory.collectAsState()
    val strings = AppStrings.current
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showClearSpeedDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0=扫描历史 1=测速历史

    Column(modifier = Modifier.fillMaxSize()) {
        // 切换按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                Button(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.scanHistory)
                }
            } else {
                OutlinedButton(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.scanHistory)
                }
            }
            if (selectedTab == 1) {
                Button(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.speedTestHistory)
                }
            } else {
                OutlinedButton(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.speedTestHistory)
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // 扫描历史
                if (historyList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.noHistory,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.noHistoryHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(strings.deleteAllHistory)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        items(historyList) { history ->
                            HistoryCard(
                                history = history,
                                onClick = { onHistoryClick(history.id) },
                                onDelete = { historyViewModel.deleteHistory(history.id) }
                            )
                        }
                    }
                }
            }
            else -> {
                // 测速历史
                if (speedHistory.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.noSpeedTestHistory,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClearSpeedDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(strings.clear)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        items(speedHistory) { record ->
                            SpeedTestHistoryCard(
                                record = record,
                                onDelete = { historyViewModel.deleteSpeedTestHistory(record.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(strings.deleteConfirm) },
            text = { Text(strings.deleteAllHistory) },
            confirmButton = {
                TextButton(onClick = {
                    historyViewModel.deleteAllHistory()
                    showDeleteAllDialog = false
                }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showClearSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showClearSpeedDialog = false },
            title = { Text(strings.deleteConfirm) },
            text = { Text(strings.speedTestHistory) },
            confirmButton = {
                TextButton(onClick = {
                    historyViewModel.deleteAllSpeedTestHistory()
                    showClearSpeedDialog = false
                }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSpeedDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

/** 测速历史单条记录卡片 */
@Composable
private fun SpeedTestHistoryCard(
    record: SpeedTestHistory,
    onDelete: () -> Unit
) {
    val strings = AppStrings.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "↓ %.2f".format(record.downloadSpeedMbps),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (record.uploadSucceeded) "↑ %.2f".format(record.uploadSpeedMbps) else "↑ --",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (record.uploadSucceeded) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mbps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dateFormat.format(Date(record.testTime))} · %.1fs".format(record.durationMs / 1000.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = strings.deleteHistory,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteConfirm) },
            text = { Text(strings.speedTestHistory) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun HistoryCard(
    history: ScanHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = AppStrings.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${history.startIp} ~ ${history.endIp}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.deleteHistory,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${strings.scanTime}: ${dateFormat.format(Date(history.scanTime))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.totalDevices}: ${history.totalDevices}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${strings.onlineDevices}: ${history.onlineDevices}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${strings.dhcpServers}: ${history.dhcpServers}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.devices,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteConfirm) },
            text = { Text(strings.deleteHistory) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun HistoryDetailScreen(
    historyId: Long,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val items by historyViewModel.getHistoryItems(historyId).collectAsState()
    val strings = AppStrings.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    var isGridView by remember { mutableStateOf(false) }
    var detailDevice by remember { mutableStateOf<DhcpServerInfo?>(null) }
    var history by remember { mutableStateOf<ScanHistory?>(null) }

    LaunchedEffect(historyId) {
        history = historyViewModel.getHistoryById(historyId)
    }

    // 转换为 DhcpServerInfo 供分布图使用
    val devices = remember(items) {
        items.map {
            DhcpServerInfo(
                ip = it.ip,
                macAddress = it.macAddress,
                hostName = it.hostName,
                pingMs = it.pingMs,
                isActive = it.isActive,
                isDhcpServer = it.isDhcpServer
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        history?.let { h ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.historyDetail,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${strings.scanTime}: ${dateFormat.format(Date(h.scanTime))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "IP: ${h.startIp} ~ ${h.endIp}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${h.totalDevices}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.totalDevices,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${h.onlineDevices}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.onlineDevices,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${h.dhcpServers}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = strings.dhcpServers,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.devices,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (items.isNotEmpty()) {
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.Star,
                        contentDescription = if (isGridView) strings.switchToList else strings.switchToGrid,
                        tint = ColorLinkBlue
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noResults,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isGridView) {
            IpGridView(devices) { detailDevice = it }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                val onlineItems = items.filter { it.isActive }
                val offlineItems = items.filter { !it.isActive }

                if (onlineItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "${strings.online}: ${onlineItems.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(onlineItems) { item ->
                        HistoryDeviceCard(item)
                    }
                }

                if (offlineItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "${strings.statusOffline}: ${offlineItems.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(offlineItems) { item ->
                        HistoryDeviceCard(item)
                    }
                }
            }
        }
    }

    detailDevice?.let { device ->
        DeviceDetailDialog(
            info = device,
            onDismiss = { detailDevice = null }
        )
    }
}

@Composable
private fun HistoryDeviceCard(item: youyeluna.lanipscanner.model.ScanHistoryItem) {
    val strings = AppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.ip,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (item.hostName != "-") {
                    Text(
                        text = item.hostName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.macAddress != "-") {
                    Text(
                        text = item.macAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item.isActive) {
                    Text(
                        text = strings.statusOnline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.pingMs > 0) {
                        Text(
                            text = "${item.pingMs}ms",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Text(
                        text = strings.statusOffline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.isDhcpServer) {
                    Text(
                        text = strings.dhcpServer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}