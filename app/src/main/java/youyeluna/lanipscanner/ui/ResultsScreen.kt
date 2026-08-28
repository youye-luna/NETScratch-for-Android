package youyeluna.lanipscanner.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import youyeluna.lanipscanner.model.DhcpServerInfo
import youyeluna.lanipscanner.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 扫描结果页：右上角切换 列表视图 / 分布图视图
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultsScreen(
    viewModel: ScanViewModel,
    onNavigateToPing: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = AppStrings.current
    var isGridView by remember { mutableStateOf(true) }
    var detailDevice by remember { mutableStateOf<DhcpServerInfo?>(null) }
    var isPriorityMode by remember { mutableStateOf(true) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csv = buildCsv(uiState.results)
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Cannot open output stream")
        }.onSuccess {
            Toast.makeText(context, strings.exportSuccess, Toast.LENGTH_SHORT).show()
        }.onFailure { e ->
            Toast.makeText(context, strings.exportFailed(e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    if (uiState.results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (uiState.isScanning) strings.scanning else "${strings.noResults}\n${strings.noResultsHint}",
                color = AppColors.current.textGray,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${strings.totalIp} ${uiState.results.size}${strings.ipCount} · ${strings.online} ${uiState.onlineCount} · ${strings.dhcp} ${uiState.dhcpCount}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (!isGridView) {
                FilterChip(
                    selected = isPriorityMode,
                    onClick = { isPriorityMode = !isPriorityMode },
                    label = {
                        Text(
                            if (isPriorityMode) strings.priorityDisplay else strings.normalDisplay,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            IconButton(onClick = { isGridView = !isGridView }) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.Star,
                    contentDescription = if (isGridView) strings.switchToList else strings.switchToGrid,
                    tint = ColorLinkBlue
                )
            }
            TextButton(
                onClick = {
                    val name = "DHCP_scan_" +
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) +
                            ".csv"
                    exportLauncher.launch(name)
                }
            ) { Text(strings.export) }
        }

        if (isGridView) {
            IpGridView(uiState.results) { detailDevice = it }
        } else {
            ListContent(uiState, detailDevice, isPriorityMode) { detailDevice = it }
        }
    }

    detailDevice?.let { device ->
        DeviceDetailDialog(
            info = device,
            onDismiss = { detailDevice = null },
            onNavigateToPing = onNavigateToPing
        )
    }
}

// ===== 分布图视图 =====

@Composable
fun IpGridView(
    results: List<DhcpServerInfo>,
    onDeviceSelected: (DhcpServerInfo) -> Unit
) {
    val strings = AppStrings.current
    val subnets = results.groupBy { it.subnet }.keys.toList()
    var selectedSubnet by remember { mutableStateOf(subnets.first()) }
    val currentSubnet = if (subnets.contains(selectedSubnet)) selectedSubnet else subnets.first()
    var emptyIp by remember { mutableStateOf<String?>(null) }

    val deviceMap = remember(currentSubnet, results) {
        buildMap {
            results.filter { it.subnet == currentSubnet }.forEach { device ->
                val last = device.ip.substringAfterLast('.').toIntOrNull() ?: return@forEach
                if (last in 1..255) put(last - 1, device)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (subnets.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subnets) { subnet ->
                    FilterChip(
                        selected = subnet == currentSubnet,
                        onClick = { selectedSubnet = subnet },
                        label = { Text("${strings.subnet} $subnet") }
                    )
                }
            }
        } else {
            Text(
                "${strings.subnet} $currentSubnet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ColorLinkBlue,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed((LocalConfiguration.current.screenWidthDp / 48).coerceIn(8, 20)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            items(255) { index ->
                IpGridCell(
                    index = index,
                    info = deviceMap[index],
                    onClick = {
                        deviceMap[index]?.let { onDeviceSelected(it) }
                            ?: run { emptyIp = "${index + 1}" }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(AppColors.current.gridEmpty, strings.unscanned)
            LegendItem(ColorOnline, strings.noDevice)
            LegendItem(ColorOnlineBlue, strings.gridOnline)
            LegendItem(ColorDhcp, strings.dhcp)
        }
    }

    emptyIp?.let { ip ->
        AlertDialog(
            onDismissRequest = { emptyIp = null },
            title = { Text("$currentSubnet.$ip", fontWeight = FontWeight.Bold) },
            text = { Text(strings.ipNoResponse) },
            confirmButton = { TextButton(onClick = { emptyIp = null }) { Text(strings.close) } }
        )
    }
}

/** 分布图视图（静态版）：不依赖 Lazy 网格，供整页滚动场景使用 */
@Composable
fun IpGridStatic(
    results: List<DhcpServerInfo>,
    onDeviceSelected: (DhcpServerInfo) -> Unit
) {
    val strings = AppStrings.current
    val subnets = results.groupBy { it.subnet }.keys.toList()
    var selectedSubnet by remember { mutableStateOf(subnets.first()) }
    val currentSubnet = if (subnets.contains(selectedSubnet)) selectedSubnet else subnets.first()
    var emptyIp by remember { mutableStateOf<String?>(null) }

    val deviceMap = remember(currentSubnet, results) {
        buildMap {
            results.filter { it.subnet == currentSubnet }.forEach { device ->
                val last = device.ip.substringAfterLast('.').toIntOrNull() ?: return@forEach
                if (last in 1..255) put(last - 1, device)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (subnets.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subnets) { subnet ->
                    FilterChip(
                        selected = subnet == currentSubnet,
                        onClick = { selectedSubnet = subnet },
                        label = { Text("${strings.subnet} $subnet") }
                    )
                }
            }
        } else {
            Text(
                "${strings.subnet} $currentSubnet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ColorLinkBlue,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // 静态网格：255 个格子按行渲染
        val columns = (LocalConfiguration.current.screenWidthDp / 48).coerceIn(8, 20)
        (0 until 255).chunked(columns).forEach { rowIndices ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rowIndices.forEach { index ->
                    IpGridCell(
                        index = index,
                        info = deviceMap[index],
                        onClick = {
                            deviceMap[index]?.let { onDeviceSelected(it) }
                                ?: run { emptyIp = "${index + 1}" }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 最后一行补足空白
                repeat(columns - rowIndices.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(AppColors.current.gridEmpty, strings.unscanned)
            LegendItem(ColorOnline, strings.noDevice)
            LegendItem(ColorOnlineBlue, strings.gridOnline)
            LegendItem(ColorDhcp, strings.dhcp)
        }
    }

    emptyIp?.let { ip ->
        AlertDialog(
            onDismissRequest = { emptyIp = null },
            title = { Text("$currentSubnet.$ip", fontWeight = FontWeight.Bold) },
            text = { Text(strings.ipNoResponse) },
            confirmButton = { TextButton(onClick = { emptyIp = null }) { Text(strings.close) } }
        )
    }
}

/** 单个网格单元 */
@Composable
private fun IpGridCell(
    index: Int,
    info: DhcpServerInfo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when {
        info?.isDhcpServer == true -> ColorDhcp to Color.White
        info?.isActive == true -> ColorOnlineBlue to Color.White
        info != null -> ColorOnline to Color.White
        else -> AppColors.current.gridEmpty to AppColors.current.gridEmptyText
    }
    val columns = (LocalConfiguration.current.screenWidthDp / 48).coerceIn(8, 20)
    val cellSize = LocalConfiguration.current.screenWidthDp / columns
    val fontSize = (cellSize * 0.35f).coerceIn(8f, 14f).sp
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            (index + 1).toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}

/** 图例项 */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = AppColors.current.textGray)
    }
}

// ===== 列表视图 =====

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ListContent(
    uiState: ScanUiState,
    detailDevice: DhcpServerInfo?,
    isPriorityMode: Boolean,
    onDeviceSelected: (DhcpServerInfo) -> Unit
) {
    val groups = uiState.results.groupBy { it.subnet }
    val subnets = groups.keys.toList()
    val strings = AppStrings.current
    var selectedSubnet by remember { mutableStateOf(subnets.firstOrNull()) }
    val currentSubnet = if (subnets.contains(selectedSubnet)) selectedSubnet else subnets.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        if (subnets.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subnets) { subnet ->
                    val deviceCount = groups[subnet]?.size ?: 0
                    FilterChip(
                        selected = subnet == currentSubnet,
                        onClick = { selectedSubnet = subnet },
                        label = { Text("${strings.subnet} $subnet ($deviceCount)") }
                    )
                }
            }
        }

        val filteredGroups = if (currentSubnet != null) {
            groups.filter { it.key == currentSubnet }
        } else {
            groups
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            filteredGroups.forEach { (subnet, devices) ->
                stickyHeader(key = "subnet_$subnet") {
                    SubnetHeader(
                        subnet = subnet,
                        online = devices.count { it.isActive },
                        dhcp = devices.count { it.isDhcpServer }
                    )
                }
                val sortedDevices = if (isPriorityMode) {
                    devices.sortedWith(compareByDescending<DhcpServerInfo> { it.isDhcpServer }
                        .thenByDescending { it.isActive }
                        .thenBy { it.ip })
                } else {
                    devices.sortedBy { it.ip }
                }
                items(sortedDevices, key = { it.ip }) { device ->
                    ResultRow(device) { onDeviceSelected(device) }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** 列表视图（静态版）：不依赖 Lazy 列表，供整页滚动场景使用 */
@Composable
internal fun ListContentStatic(
    uiState: ScanUiState,
    isPriorityMode: Boolean,
    onDeviceSelected: (DhcpServerInfo) -> Unit
) {
    val groups = uiState.results.groupBy { it.subnet }
    val subnets = groups.keys.toList()
    val strings = AppStrings.current
    var selectedSubnet by remember { mutableStateOf(subnets.firstOrNull()) }
    val currentSubnet = if (subnets.contains(selectedSubnet)) selectedSubnet else subnets.firstOrNull()

    Column(modifier = Modifier.fillMaxWidth()) {
        if (subnets.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subnets) { subnet ->
                    val deviceCount = groups[subnet]?.size ?: 0
                    FilterChip(
                        selected = subnet == currentSubnet,
                        onClick = { selectedSubnet = subnet },
                        label = { Text("${strings.subnet} $subnet ($deviceCount)") }
                    )
                }
            }
        }

        val filteredGroups = if (currentSubnet != null) {
            groups.filter { it.key == currentSubnet }
        } else {
            groups
        }

        filteredGroups.forEach { (subnet, devices) ->
            SubnetHeader(
                subnet = subnet,
                online = devices.count { it.isActive },
                dhcp = devices.count { it.isDhcpServer }
            )
            val sortedDevices = if (isPriorityMode) {
                devices.sortedWith(compareByDescending<DhcpServerInfo> { it.isDhcpServer }
                    .thenByDescending { it.isActive }
                    .thenBy { it.ip })
            } else {
                devices.sortedBy { it.ip }
            }
            sortedDevices.forEach { device ->
                ResultRow(device) { onDeviceSelected(device) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
