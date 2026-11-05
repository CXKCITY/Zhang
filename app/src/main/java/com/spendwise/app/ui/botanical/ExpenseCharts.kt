package com.spendwise.app.ui.botanical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.domain.MonthlyAggregate
import java.time.YearMonth
import kotlin.math.atan2
import kotlin.math.hypot

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
fun ExpenseBars(bars: List<ExpenseBar>, annual: Boolean) {
    var selected by remember(bars) { mutableIntStateOf(bars.indexOfLast { it.cents > 0 }.coerceAtLeast(0)) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) { progress.snapTo(0f); progress.animateTo(1f, BotMotion.smooth(0.5f)) }
    val max = (bars.maxOfOrNull { it.cents } ?: 0L).coerceAtLeast(1L)
    val accent = Bot.ActionSolid
    val rule = Bot.Rule
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (annual) "年度支出 · 12个月" else "每日支出", style = body(18f, FontWeight.SemiBold), color = Bot.Ink)
            bars.getOrNull(selected)?.let { bar -> Text("${bar.label}  ${formatRm(bar.cents)}", style = body(14f), color = Bot.Ink) }
            Text(if (bars.none { it.cents > 0 }) "当前时段暂无支出" else "点击柱子查看金额 · 最高 ${formatRm(max)}", style = body(11f), color = Bot.SurfaceMuted)
            Canvas(Modifier.fillMaxWidth().height(180.dp)
                .semantics { contentDescription = bars.joinToString("；") { "${it.label} ${formatRm(it.cents)}" } }
                .pointerInput(bars) { detectTapGestures { point -> selected = (point.x / size.width * bars.size).toInt().coerceIn(0, bars.lastIndex) } }
            ) {
                val chartHeight = size.height - 2.dp.toPx()
                for (i in 0..3) {
                    val y = chartHeight * i / 3f
                    drawLine(rule, Offset(0f,y), Offset(size.width,y), strokeWidth = 1.dp.toPx())
                }
                val slot = size.width / bars.size.coerceAtLeast(1)
                bars.forEachIndexed { i, bar ->
                    val height = (bar.cents.toDouble() / max * chartHeight * progress.value).toFloat()
                    if (height > 0f) drawRect(if (i == selected) accent else accent.copy(alpha = 0.5f), Offset(slot*i+slot*0.18f, chartHeight-height), Size(slot*0.64f,height))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                bars.forEachIndexed { i, _ ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (annual || i == 0 || (i+1)%5 == 0 || i == bars.lastIndex) {
                            Text(if (annual) "${i+1}月" else "${i+1}", fontSize = 10.sp, color = Bot.SurfaceMuted,
                                modifier = Modifier.wrapContentWidth(unbounded = true), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPie(shares: List<CategoryShare>, onSelect: (CategoryShare) -> Unit) {
    val amounts = shares.map { it.cents }
    val total = amounts.sumOf { it.toDouble() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(shares) { progress.snapTo(0f); progress.animateTo(1f, BotMotion.smooth(0.5f)) }
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth(0.8f).aspectRatio(1f)
            .semantics { contentDescription = "分类饼图 " + shares.joinToString("；") { "${it.category.name} ${formatRm(it.cents)}" } }
            .pointerInput(shares) { detectTapGestures { point ->
                val dx = point.x-size.width/2f; val dy = point.y-size.height/2f
                if (progress.value >= 0.99f && hypot(dx.toDouble(),dy.toDouble()) <= size.width/2f) {
                    val degrees = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 450.0) % 360.0
                    ExpenseChartData.sliceIndex(degrees/360.0, amounts)?.let { onSelect(shares[it]) }
                }
            } }
        ) {
            if (total > 0) {
                var start = -90f
                shares.forEach { share ->
                    val sweep = (share.cents / total * 360.0).toFloat()
                    drawArc(share.category.art.color, start, sweep * progress.value, useCenter = true)
                    start += sweep * progress.value
                }
            }
        }
    }
}
