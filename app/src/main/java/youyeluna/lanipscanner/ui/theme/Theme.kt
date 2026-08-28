package youyeluna.lanipscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 品牌色（与原版 WinForms 主题一致）
private val BrandPrimary = Color(0xFF0078D7)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    secondary = Color(0xFF2196F3),
    tertiary = Color(0xFF4CAF50)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CACEE),
    secondary = Color(0xFF64B5F6),
    tertiary = Color(0xFF81C784),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF555555)
)

/** 应用自定义颜色（随深色/浅色模式变化） */
data class CustomColors(
    val subnetBg: Color,
    val textGray: Color,
    val gridEmpty: Color,
    val cardBg: Color,
    val cardBgInactive: Color,
    val settingsCardBg: Color,
    val ipDotColor: Color,
    val gridEmptyText: Color,
    val errorColor: Color
)

private val LightCustomColors = CustomColors(
    subnetBg = Color(0xFFE3F0FA),
    textGray = Color(0xFF888888),
    gridEmpty = Color(0xFFF0F0F0),
    cardBg = Color.White,
    cardBgInactive = Color(0xFFF5F5F5),
    settingsCardBg = Color(0xFFF0F4F7),
    ipDotColor = Color(0xFF505050),
    gridEmptyText = Color(0xFF333333),
    errorColor = Color(0xFFD32F2F)
)

private val DarkCustomColors = CustomColors(
    subnetBg = Color(0xFF1A2A3A),
    textGray = Color(0xFFAAAAAA),
    gridEmpty = Color(0xFF2A2A2A),
    cardBg = Color(0xFF1E1E1E),
    cardBgInactive = Color(0xFF252525),
    settingsCardBg = Color(0xFF1E2A35),
    ipDotColor = Color(0xFFBBBBBB),
    gridEmptyText = Color(0xFFCCCCCC),
    errorColor = Color(0xFFEF5350)
)

val LocalCustomColors = staticCompositionLocalOf { LightCustomColors }

/** 便捷访问当前主题自定义颜色 */
object AppColors {
    val current: CustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current
}

@Composable
fun DhcpScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors
    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
