package com.spendwise.app.ui.botanical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.domain.MonthlyAggregate
import java.time.YearMonth
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

 data class ExpenseBar(val label: String, val cents: Long)

object ExpenseChartData {
    fun month(month: YearMonth, daily: Map<Int, Long>): List<ExpenseBar> =
        (1..month.lengthOfMonth()).map { ExpenseBar("${month.monthValue}月${it}日", daily[it] ?: 0L) }
    fun year(year: Int, aggregates: List<MonthlyAggregate>): List<ExpenseBar> =
        (1..12).map { m -> ExpenseBar("${year}年${m}月", aggregates.filter { it.month == YearMonth.of(year,m) }.sumOf { it.expenseCents }) }
    fun sliceIndex(fraction: Double, amounts: List<Long>): Int? {
        val total = amounts.sumOf { it.coerceAtLeast(0).toDouble() }
        if (total <= 0 || fraction !in 0.0..1.0) return null
        var end = 0.0
        amounts.forEachIndexed { i, v -> end += v.coerceAtLeast(0) / total; if (fraction < end) return i }
        return amounts.indexOfLast { it > 0 }.takeIf { it >= 0 }
    }
}

@Composable
fun ExpenseBars(
    bars: List<ExpenseBar>,
    annual: Boolean,
    onOpenDay: ((Int) -> Unit)? = null,
    selectableCount: Int = bars.size
) {
    var selected by remember(bars) { mutableIntStateOf(-1) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        progress.snapTo(0f)
        progress.animateTo(1f, BotMotion.smooth(0.45f))
    }
    val selectedPosition by animateFloatAsState(selected.coerceAtLeast(0).toFloat(),
        BotMotion.smooth(0.22f), label = "chartSelection")
    val max = (bars.maxOfOrNull { it.cents } ?: 0L).coerceAtLeast(1L)
    val accent = Bot.ActionSolid
    val ink = Bot.Ink
    val muted = Bot.SurfaceMuted
    val rule = Bot.Rule
    val bubble = Bot.ChipBg
    val measurer = rememberTextMeasurer()
    val openDay by rememberUpdatedState(onOpenDay)
    val selectedBar = bars.getOrNull(selected)
    val lastSelectable = selectableCount.coerceIn(0, bars.size) - 1
    fun indexAt(x: Float, width: Int) =
        (x / width.coerceAtLeast(1) * bars.size).toInt().coerceIn(0, lastSelectable)

    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (annual) "年度支出" else "每日支出",
                    style = body(18f, FontWeight.SemiBold),
                    color = ink,
                    modifier = Modifier.weight(1f)
                )
                if (!annual && selectedBar != null && onOpenDay != null) {
                    TextLinkButton("${selectedBar.label}详情", onClick = { onOpenDay(selected) },
                        color = accent)
                }
            }
            Text(
                when {
                    bars.none { it.cents > 0 } -> "当前时段暂无支出"
                    annual -> "点按或拖动选择月份"
                    else -> "点按或拖动选日期 · 再点查看详情"
                },
                style = body(11f),
                color = muted
            )
            Canvas(
                Modifier.fillMaxWidth().height(200.dp)
                    .semantics {
                        contentDescription = selectedBar?.let { "${it.label} ${formatRm(it.cents)}" }
                            ?: if (annual) "年度支出柱状图" else "每日支出柱状图"
                        if (selectedBar != null && onOpenDay != null) {
                            onClick(label = "查看每日详情") { onOpenDay(selected); true }
                        }
                        customActions = listOf(
                            CustomAccessibilityAction("上一个") {
                                if (selected <= 0) false else { selected--; true }
                            },
                            CustomAccessibilityAction("下一个") {
                                if (selected >= lastSelectable) false else { selected++; true }
                            }
                        )
                    }
                    .pointerInput(bars, lastSelectable) {
                        if (lastSelectable >= 0) detectTapGestures { point ->
                            val index = indexAt(point.x, size.width)
                            if (selected == index) openDay?.invoke(index)
                            else selected = index
                        }
                    }
                    .pointerInput(bars, lastSelectable) {
                        if (lastSelectable >= 0) detectHorizontalDragGestures(
                            onDragStart = { selected = indexAt(it.x, size.width) }
                        ) { change, _ ->
                            change.consume()
                            selected = indexAt(change.position.x, size.width)
                        }
                    }
            ) {
                if (bars.isEmpty()) return@Canvas
                val baseline = size.height - 2.dp.toPx()
                val top = 38.dp.toPx()
                val plotHeight = (baseline - top).coerceAtLeast(0f)
                val slot = size.width / bars.size
                for (i in 0..3) {
                    val y = top + plotHeight * i / 3f
                    drawLine(rule.copy(alpha = if (i == 3) 0.8f else 0.42f),
                        Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                if (selectedBar != null) {
                    val x = slot * (selectedPosition + 0.5f)
                    drawLine(accent.copy(alpha = 0.34f), Offset(x, top), Offset(x, baseline),
                        strokeWidth = 1.dp.toPx())
                }
                val barWidth = (slot * if (annual) 0.62f else 0.68f).coerceAtMost(22.dp.toPx())
                bars.forEachIndexed { index, bar ->
                    if (bar.cents > 0) {
                        val height = ((bar.cents.toDouble() / max * plotHeight * progress.value).toFloat())
                            .coerceAtLeast(2.dp.toPx())
                        val y = baseline - height
                        val radius = minOf(barWidth, height) / 2f
                        val active = selected == index
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(accent.copy(alpha = if (active) 1f else 0.72f),
                                    accent.copy(alpha = if (active) 0.72f else 0.32f)),
                                startY = y, endY = baseline
                            ),
                            topLeft = Offset(slot * (index + 0.5f) - barWidth / 2, y),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(radius)
                        )
                    }
                }
                if (selectedBar != null) {
                    val text = measurer.measure(formatRm(selectedBar.cents),
                        style = TextStyle(color = ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1)
                    val padding = 7.dp.toPx()
                    val width = text.size.width + 2 * padding
                    val height = text.size.height + padding
                    val barTop = baseline - (selectedBar.cents.toDouble() / max * plotHeight * progress.value).toFloat()
                    val x = (slot * (selectedPosition + 0.5f) - width / 2)
                        .coerceIn(0f, (size.width - width).coerceAtLeast(0f))
                    val y = (barTop - height - 5.dp.toPx()).coerceAtLeast(0f)
                    drawRoundRect(bubble, Offset(x, y), Size(width, height), CornerRadius(height / 2))
                    drawText(text, topLeft = Offset(x + padding, y + padding / 2))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                bars.forEachIndexed { index, _ ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (annual || index == 0 || (index + 1) % 5 == 0 ||
                            (index == bars.lastIndex && (index + 1) % 5 > 1)) {
                            Text(if (annual) "${index + 1}月" else "${index + 1}",
                                fontSize = 10.sp, color = if (index == selected) ink else muted,
                                modifier = Modifier.wrapContentWidth(unbounded = true), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private val categoryChartPalette = listOf(
    Color(0xFF628DFC), Color(0xFF38BFA5), Color(0xFFF3A558), Color(0xFFB58AE9),
    Color(0xFFE7768D), Color(0xFF44B8DC), Color(0xFF9ABD59), Color(0xFFDA8C5D),
    Color(0xFF867DE2), Color(0xFF4C9E78), Color(0xFFE596C2), Color(0xFFC6A64F),
    Color(0xFF547CB5), Color(0xFFE37A59), Color(0xFF7CC9B7), Color(0xFFAB80A6),
)

internal fun categoryChartColors(count: Int): List<Color> {
    val colors = categoryChartPalette.take(count).toMutableList()
    val used = colors.toMutableSet()
    var step = 0
    while (colors.size < count) {
        val color = Color.hsl((23f + step * 137.508f) % 360f, .58f, if (step % 2 == 0) .50f else .68f)
        if (used.add(color)) colors.add(color)
        step++
    }
    return colors
}

@Composable
fun CategoryDonut(shares: List<CategoryShare>, colors: List<Color>, onSelect: (CategoryShare) -> Unit) {
    val amounts = remember(shares) { shares.map { it.cents } }
    val total = amounts.sumOf { it.toDouble() }
    val progress = remember { Animatable(0f) }
    val latestSelect by rememberUpdatedState(onSelect)
    val track = Bot.RuleSoft
    LaunchedEffect(shares) {
        progress.snapTo(0f)
        progress.animateTo(1f, BotMotion.smooth(0.5f))
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxWidth(.76f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()
                .semantics { contentDescription = "分类环形图 " + shares.joinToString("；") { "${it.category.name} ${formatRm(it.cents)}" } }
                .pointerInput(shares) {
                    detectTapGestures { point ->
                        val dx = point.x - size.width / 2f
                        val dy = point.y - size.height / 2f
                        val radius = hypot(dx.toDouble(), dy.toDouble())
                        if (progress.value >= .99f && radius in (size.width * .30)..(size.width * .50)) {
                            val degrees = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 450.0) % 360.0
                            ExpenseChartData.sliceIndex(degrees / 360.0, amounts)?.let { latestSelect(shares[it]) }
                        }
                    }
                }
            ) {
                val stroke = Stroke(width = size.width * .15f)
                val inset = stroke.width / 2f
                val bounds = Size(size.width - stroke.width, size.height - stroke.width)
                val origin = Offset(inset, inset)
                drawCircle(track, radius = bounds.width / 2f, style = stroke)
                var start = -90f
                shares.forEachIndexed { index, share ->
                    val sweep = (share.cents / total * 360.0).toFloat()
                    val gap = if (shares.size == 1) 0f else minOf(2.8f, sweep * .24f)
                    val visible = (360f * progress.value - (start + 90f) - gap / 2f)
                        .coerceIn(0f, sweep - gap)
                    if (visible > 0f) {
                        drawArc(colors[index], start + gap / 2f, visible, useCenter = false,
                            topLeft = origin, size = bounds, style = stroke)
                    }
                    start += sweep
                }
            }
            val leading = shares.first()
            Column(Modifier.widthIn(max = maxWidth * .55f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(leading.cents * 100.0 / total).roundToInt()}%",
                    style = display(30f, FontWeight.SemiBold), color = Bot.SurfaceInk)
                Text(leading.category.name, style = body(12f), color = Bot.SurfaceMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
