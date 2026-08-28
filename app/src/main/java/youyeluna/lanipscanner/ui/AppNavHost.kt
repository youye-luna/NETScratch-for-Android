package youyeluna.lanipscanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import youyeluna.lanipscanner.ui.theme.AppColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/** 自绘"速度计"图标（Material Icons speed 造型），用于底栏测速入口 */
private val SpeedTestIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SpeedTest",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes(
                "m20.38,8.57 l-1.23,1.85 a8,8 0 0,1 -0.22,7.58 H5.07 A8,8 0 0,1 15.58,6.85 l1.85,-1.23 A10,10 0 0,0 3.35,19 a2,2 0 0,0 1.72,1 h13.85 a2,2 0 0,0 1.74,-1 10,10 0 0,0 -0.27,-10.44 z m-9.79,6.84 a2,2 0 0,0 2.83,0 l5.66,-8.49 l-8.49,5.66 a2,2 0 0,0 0,2.83 z"
            ),
            fill = SolidColor(Color.Black)
        )
    }.build()
}

/** 应用页面 */
enum class AppScreen(val route: String, val icon: ImageVector) {
    Home("home", Icons.Filled.Home),
    Ping("ping", Icons.Filled.PlayArrow),
    SpeedTest("speed_test", SpeedTestIcon),
    History("history", Icons.Filled.DateRange),
    Settings("settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(viewModel: ScanViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentScreen = AppScreen.entries.firstOrNull { it.route == currentRoute } ?: AppScreen.Home
    val strings = AppStrings.current

    var showAbout by remember { mutableStateOf(false) }

    // 共享 PingViewModel，详情弹窗点 Ping 后跳转并自动 ping
    val pingViewModel: PingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val historyViewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val navigateToPing: (String) -> Unit = { ip ->
        pingViewModel.requestPing(ip)
        navController.navigate(AppScreen.Ping.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val topBarTitle: String = when {
        currentRoute == "history_detail/{historyId}" -> strings.historyDetail
        currentScreen == AppScreen.Home -> strings.navHome
        currentScreen == AppScreen.History -> strings.navHistory
        currentScreen == AppScreen.Ping -> strings.navPing
        currentScreen == AppScreen.SpeedTest -> strings.speedTestTitle
        currentScreen == AppScreen.Settings -> strings.settings
        else -> strings.navHome
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                AppScreen.entries.forEach { screen ->
                    val label = when (screen) {
                        AppScreen.Home -> strings.navHome
                        AppScreen.History -> strings.navHistory
                        AppScreen.Ping -> strings.navPing
                        AppScreen.SpeedTest -> strings.navSpeedTest
                        AppScreen.Settings -> strings.settings
                    }
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppScreen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPing = navigateToPing
                )
            }
            composable(AppScreen.History.route) {
                HistoryScreen(
                    historyViewModel = historyViewModel,
                    onHistoryClick = { historyId ->
                        navController.navigate("history_detail/$historyId")
                    }
                )
            }
            composable("history_detail/{historyId}") { backStackEntry ->
                val historyId = backStackEntry.arguments?.getString("historyId")?.toLongOrNull() ?: 0L
                HistoryDetailScreen(
                    historyId = historyId,
                    historyViewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppScreen.Ping.route) {
                PingScreen(pingViewModel)
            }
            composable(AppScreen.SpeedTest.route) {
                SpeedTestScreen()
            }
            composable(AppScreen.Settings.route) {
                SettingsScreen(onAbout = { showAbout = true })
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SettingsScreen(onAbout: () -> Unit) {
    val strings = AppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { HistorySettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.getSettings()) }
    var saved by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(LanguageManager.currentLanguage) }

    // 自定义天数输入框状态
    var customDaysText by remember { mutableStateOf(settings.customDays.toString()) }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 语言设置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    strings.language,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Language.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = selectedLanguage == lang,
                                onClick = { selectedLanguage = lang }
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang }
                        )
                        Text(
                            lang.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        LanguageManager.setLanguage(selectedLanguage)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.save, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 扫描线程数设置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.scanThreads,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${settings.scanThreads}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.scanThreadsDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = settings.scanThreads.toFloat(),
                    onValueChange = { settings = settings.copy(scanThreads = it.toInt()) },
                    valueRange = 1f..100f,
                    steps = 98,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 数据保存设置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    strings.dataSaveSettings,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 数据保存方式
                Text(
                    strings.dataSaveMode,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                RadioOptionRow(
                    label = strings.saveByTime,
                    selected = settings.saveMode == SaveMode.BY_TIME,
                    onClick = { settings = settings.copy(saveMode = SaveMode.BY_TIME) }
                )
                RadioOptionRow(
                    label = strings.saveByCount,
                    selected = settings.saveMode == SaveMode.BY_COUNT,
                    onClick = { settings = settings.copy(saveMode = SaveMode.BY_COUNT) }
                )

                Spacer(Modifier.height(12.dp))

                // 保存范围标题
                Text(
                    strings.saveRange,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 根据保存方式显示不同选项
                if (settings.saveMode == SaveMode.BY_TIME) {
                    // 按时间保存选项
                    RadioOptionRow(
                        label = strings.timeRange14Days,
                        selected = settings.timeRange == TimeRange.DAYS_14,
                        onClick = { settings = settings.copy(timeRange = TimeRange.DAYS_14) }
                    )
                    RadioOptionRow(
                        label = strings.timeRangeHalfMonth,
                        selected = settings.timeRange == TimeRange.HALF_MONTH,
                        onClick = { settings = settings.copy(timeRange = TimeRange.HALF_MONTH) }
                    )
                    RadioOptionRow(
                        label = strings.timeRangeOneMonth,
                        selected = settings.timeRange == TimeRange.ONE_MONTH,
                        onClick = { settings = settings.copy(timeRange = TimeRange.ONE_MONTH) }
                    )
                    RadioOptionRow(
                        label = strings.timeRangeOneYear,
                        selected = settings.timeRange == TimeRange.ONE_YEAR,
                        onClick = { settings = settings.copy(timeRange = TimeRange.ONE_YEAR) }
                    )
                    RadioOptionRow(
                        label = strings.timeRangeNeverClear,
                        selected = settings.timeRange == TimeRange.NEVER_CLEAR,
                        onClick = { settings = settings.copy(timeRange = TimeRange.NEVER_CLEAR) }
                    )
                    RadioOptionRow(
                        label = strings.timeRangeCustom,
                        selected = settings.timeRange == TimeRange.CUSTOM,
                        onClick = { settings = settings.copy(timeRange = TimeRange.CUSTOM) }
                    )
                    // 自定义天数输入
                    if (settings.timeRange == TimeRange.CUSTOM) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(strings.timeRangeCustom + ":", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            TextField(
                                value = customDaysText,
                                onValueChange = { value ->
                                    customDaysText = value.filter { it.isDigit() }.take(4)
                                    customDaysText.toIntOrNull()?.let { days ->
                                        if (days in 1..9999) {
                                            settings = settings.copy(customDays = days)
                                        }
                                    }
                                },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(strings.daysUnit, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    // 按数量保存选项
                    RadioOptionRow(
                        label = strings.countRange30,
                        selected = settings.countRange == CountRange.COUNT_30,
                        onClick = { settings = settings.copy(countRange = CountRange.COUNT_30) }
                    )
                    RadioOptionRow(
                        label = strings.countRange60,
                        selected = settings.countRange == CountRange.COUNT_60,
                        onClick = { settings = settings.copy(countRange = CountRange.COUNT_60) }
                    )
                    RadioOptionRow(
                        label = strings.countRange90,
                        selected = settings.countRange == CountRange.COUNT_90,
                        onClick = { settings = settings.copy(countRange = CountRange.COUNT_90) }
                    )
                    RadioOptionRow(
                        label = strings.countRange100,
                        selected = settings.countRange == CountRange.COUNT_100,
                        onClick = { settings = settings.copy(countRange = CountRange.COUNT_100) }
                    )
                }
            }
        }

        // 保存设置按钮
        Button(
            onClick = {
                settingsManager.saveSettings(settings)
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(strings.saveSettings, fontWeight = FontWeight.Medium)
        }
        if (saved) {
            Text(
                text = strings.save,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(8.dp))

        // 关于卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.current.settingsCardBg),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    strings.about,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TextButton(onClick = onAbout) {
                    Text(strings.aboutTitle, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val strings = AppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.aboutTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    strings.aboutTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${strings.version}: $versionName",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${strings.developer}: Wu Kaixuan",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    strings.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    strings.openSourceLicenses,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.current.textGray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.licenseJSpeedTest,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.current.textGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                
                val githubUrl = "https://github.com/yourusername/lanipscanner"
                val interactionSource = remember { MutableInteractionSource() }
                
                Text(
                    strings.githubLink,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    githubUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                            context.startActivity(intent)
                        }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    strings.copyright + " 2026 Wu Kaixuan",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.current.textGray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

/** 单选项组件：与语言选择行相同的布局模式，保证各设备渲染一致 */
@Composable
private fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
