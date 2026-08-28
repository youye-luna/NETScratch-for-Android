package youyeluna.lanipscanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import youyeluna.lanipscanner.ui.theme.AppColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max

/** 图表时间窗口（秒）：展示最近 60 秒数据，之后滑动 */
private const val CHART_WINDOW_SECONDS = 60f

/** 超时标记颜色（与结果列表超时文字一致） */
private val ColorTimeout = Color(0xFFEF5350)

/**
 * Ping 延迟实时折线图
 * X 轴：经过时间（秒），滑动窗口；Y 轴：延迟（ms），自动缩放
 * 包含网格线、坐标轴刻度标签、数据点标记与超时标记
 */
@Composable
fun PingLatencyChart(
    items: List<PingItem>,
    startTime: Long,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val textGray = AppColors.current.textGray
    val gridColor = textGray.copy(alpha = 0.18f)
    val axisColor = textGray.copy(alpha = 0.45f)
    val labelColor = textGray

    Box(modifier = modifier) {
        if (items.isEmpty()) {
            // 空状态：仅绘制坐标轴框架
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val left = 40.dp.toPx()
                val bottom = size.height - 22.dp.toPx()
                drawLine(axisColor, Offset(left, 0f), Offset(left, bottom), 1.5f)
                drawLine(axisColor, Offset(left, bottom), Offset(size.width, bottom), 1.5f)
            }
            Text(
                strings.noPingResultsHint,
                color = textGray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            ChartCanvas(
                items = items,
                startTime = startTime,
                gridColor = gridColor,
                axisColor = axisColor,
                labelColor = labelColor,
                lineColor = ColorOnline,
                pointColor = ColorOnline,
                timeoutColor = ColorTimeout,
                yAxisTitle = strings.chartLatencyAxis,
                xAxisTitle = strings.chartTimeAxis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

/** 图例说明 */
@Composable
internal fun PingChartLegend(modifier: Modifier = Modifier) {
    val strings = AppStrings.current
    val textGray = AppColors.current.textGray
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(ColorOnline, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(strings.chartLatencyAxis, fontSize = 11.sp, color = textGray)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(8.dp)
                .background(ColorTimeout, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(strings.chartTimeoutLegend, fontSize = 11.sp, color = textGray)
        Spacer(Modifier.weight(1f))
        Text(strings.chartTimeAxis, fontSize = 11.sp, color = textGray)
    }
}

@Composable
private fun ChartCanvas(
    items: List<PingItem>,
    startTime: Long,
    gridColor: Color,
    axisColor: Color,
    labelColor: Color,
    lineColor: Color,
    pointColor: Color,
    timeoutColor: Color,
    yAxisTitle: String,
    xAxisTitle: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    // 计算绘图数据（重组时自动更新，实现实时刷新）
    val plotData = remember(items, startTime) { buildPlotData(items, startTime) }

    Canvas(modifier = modifier) {
        val leftPad = 38.dp.toPx()
        val rightPad = 6.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val plotLeft = leftPad
        val plotTop = topPad
        val plotRight = size.width - rightPad
        val plotBottom = size.height - bottomPad
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        // ---- Y 轴刻度（自动缩放到"美观"最大值）----
        val yMax = niceCeil(plotData.maxRtt)
        val ySteps = 4
        for (i in 0..ySteps) {
            val value = yMax * i / ySteps
            val y = plotBottom - (i.toFloat() / ySteps) * plotH
            // 水平网格线
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), 1f)
            // Y 轴刻度标签
            val label = if (yMax >= 10f) "${value.toInt()}" else String.format("%.1f", value)
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(
                measured,
                color = labelColor,
                topLeft = Offset(plotLeft - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f)
            )
        }

        // ---- X 轴刻度（秒）----
        val xMax = max(CHART_WINDOW_SECONDS, plotData.maxElapsed)
        val xStep = pickXStep(xMax)
        var tick = ceil(plotData.leftSec / xStep) * xStep
        while (tick <= xMax + 0.01f) {
            val x = mapX(tick, plotData.leftSec, xMax, plotLeft, plotW)
            // 垂直网格线
            drawLine(gridColor, Offset(x, plotTop), Offset(x, plotBottom), 1f)
            val measured = textMeasurer.measure("${tick.toInt()}", labelStyle)
            drawText(
                measured,
                color = labelColor,
                topLeft = Offset(x - measured.size.width / 2f, plotBottom + 4.dp.toPx())
            )
            tick += xStep
        }

        // ---- 坐标轴 ----
        drawLine(axisColor, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), 1.5f)
        drawLine(axisColor, Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), 1.5f)

        // ---- 延迟折线（跳过超时点，相邻有效点相连）----
        val linePath = Path()
        var drawing = false
        plotData.points.forEach { p ->
            if (p.rtt != null) {
                val x = mapX(p.elapsedSec, plotData.leftSec, xMax, plotLeft, plotW)
                val y = plotBottom - (p.rtt / yMax) * plotH
                if (!drawing) { linePath.moveTo(x, y); drawing = true }
                else linePath.lineTo(x, y)
            } else {
                drawing = false
            }
        }
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // ---- 数据点标记与超时标记 ----
        plotData.points.forEach { p ->
            val x = mapX(p.elapsedSec, plotData.leftSec, xMax, plotLeft, plotW)
            if (p.rtt != null) {
                val y = plotBottom - (p.rtt / yMax) * plotH
                drawCircle(pointColor, radius = 2.5.dp.toPx(), center = Offset(x, y))
            } else {
                // 超时：顶部绘制红色 ×
                val y = plotTop + 5.dp.toPx()
                val r = 3.5.dp.toPx()
                drawLine(
                    timeoutColor, Offset(x - r, y - r), Offset(x + r, y + r),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                )
                drawLine(
                    timeoutColor, Offset(x - r, y + r), Offset(x + r, y - r),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                )
            }
        }

        // ---- 轴标题 ----
        val yTitle = textMeasurer.measure(yAxisTitle, labelStyle)
        drawText(yTitle, color = labelColor, topLeft = Offset(plotLeft + 4.dp.toPx(), 1.dp.toPx()))
        val xTitle = textMeasurer.measure(xAxisTitle, labelStyle)
        drawText(
            xTitle,
            color = labelColor,
            topLeft = Offset(plotRight - xTitle.size.width, plotBottom + 14.dp.toPx())
        )
    }
}

/** 单个绘图点 */
private data class ChartPoint(val elapsedSec: Float, val rtt: Float?)

/** 绘图数据（窗口裁剪后） */
private data class PlotData(
    val points: List<ChartPoint>,
    val maxRtt: Float,
    val maxElapsed: Float,
    val leftSec: Float
)

/** 根据采样数据构建绘图点：滑动窗口取最近 60 秒 */
private fun buildPlotData(items: List<PingItem>, startTime: Long): PlotData {
    if (items.isEmpty()) return PlotData(emptyList(), 100f, 0f, 0f)
    val last = items.last().timestamp
    var leftMs = maxOf(startTime, last - (CHART_WINDOW_SECONDS * 1000).toLong())
    // 保证至少有 10 秒跨度，避免初期数据挤压
    val minSpanMs = 10_000L
    if (last - leftMs < minSpanMs) leftMs = last - minSpanMs
    val windowItems = items.filter { it.timestamp >= leftMs }
    val points = windowItems.map {
        ChartPoint(
            elapsedSec = (it.timestamp - startTime) / 1000f,
            rtt = it.rttMs?.toFloat()
        )
    }
    val maxRtt = points.mapNotNull { it.rtt }.maxOrNull() ?: 100f
    val maxElapsed = points.lastOrNull()?.elapsedSec ?: 0f
    val leftSec = maxOf(0f, maxElapsed - CHART_WINDOW_SECONDS)
    return PlotData(points, maxRtt, maxElapsed, leftSec)
}

/** 将"美观值"向上取整（1/2/5 × 10^n），用于 Y 轴最大值 */
private fun niceCeil(v: Float): Float {
    if (v <= 0f) return 100f
    val exp = floor(log10(v)).toInt()
    val base = Math.pow(10.0, exp.toDouble())
    val f = v / base.toFloat()
    val nice = when {
        f <= 1f -> 1f
        f <= 2f -> 2f
        f <= 5f -> 5f
        else -> 10f
    }
    return (nice * base).toFloat()
}

/** 根据窗口跨度挑选合适的 X 刻度步长（秒） */
private fun pickXStep(spanSec: Float): Float = when {
    spanSec <= 15f -> 5f
    spanSec <= 40f -> 10f
    spanSec <= 90f -> 15f
    spanSec <= 200f -> 30f
    else -> 60f
}

private fun mapX(t: Float, t0: Float, t1: Float, plotLeft: Float, plotW: Float): Float {
    if (t1 <= t0) return plotLeft
    val ratio = ((t - t0) / (t1 - t0)).coerceIn(0f, 1f)
    return plotLeft + ratio * plotW
}
