package com.spendwise.app.ui.botanical

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.domain.Category
import java.time.LocalDate
import java.time.YearMonth

// Glass styling for the small controls. Static gradients keep the scrolling
// header on the same rendering path across all supported Android versions.
internal fun Modifier.heroGlass(radius: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.14f)
        )))
        .border(1.dp, Brush.verticalGradient(listOf(
            Color.White.copy(alpha = 0.48f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.25f)
        )), shape)
}

@Composable
internal fun HomeHero(
    month: YearMonth,
    totalExpense: Long,
    categories: List<Category>,
    spentByCategory: Map<Long, Long>,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    onCategory: (Category) -> Unit,
    onAdd: () -> Unit
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val widthDp = LocalConfiguration.current.screenWidthDp
    val balanceSize = (widthDp * 0.12f).coerceIn(35f, 49f)
    val expenseCategories = remember(categories) { categories.filterNot { it.isIncomeAdjustment } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(LedgerAppearance.current.heroStart, LedgerAppearance.current.heroEnd)))
            .padding(start = 16.dp, end = 16.dp, top = statusTop + 6.dp, bottom = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                PeriodSelector(month, onMonthChange, onOpenPeriodPicker, onDark = true, compact = true)
            }
            Box(
                Modifier.size(44.dp).heroGlass(22.dp).botPress(onClick = onOpenSettings)
                    .semantics { contentDescription = "打开设置" },
                contentAlignment = Alignment.Center
            ) {
                BotIcon(BotIcons.Menu, size = 20.dp, tint = Color.White)
            }
        }

        Text(
            "清记  /  本月支出",
            style = body(13f, FontWeight.Medium),
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
        )
        Money(
            cents = totalExpense,
            style = moneyStyle(balanceSize, letterSpacingPx = -1.5f),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            animate = false
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "支出分类",
                style = body(13f, FontWeight.Medium),
                color = Color.White,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Box(
                Modifier.heightIn(min = 44.dp).botPress(onClick = onOpenCategories)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("管理分类", style = body(12f), color = Color.White)
            }
        }
        if (expenseCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(expenseCategories, key = { it.id }) { category ->
                    val amount = spentByCategory[category.id] ?: 0L
                    Column(
                        Modifier.width(74.dp).heroGlass(14.dp)
                            .botPress { onCategory(category) }
                            .semantics { contentDescription = "${category.name} ${formatRm(amount)}" }
                            .padding(horizontal = 5.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ArtImage(category.art.drawable, size = 22.dp, tint = Color.White)
                        Text(
                            category.name,
                            style = body(11f, FontWeight.Medium),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Money(
                            cents = amount,
                            style = body(10f, FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.9f),
                            centsScale = 1f,
                            currencyWeight = FontWeight.Medium,
                            abbreviate = true,
                            align = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                            animate = false
                        )
                    }
                }
            }
        }
        HeroAction("记支出", BotIcons.Up, Modifier.padding(top = 12.dp), onAdd)
    }
}

@Composable
fun SpendingCalendarScreen(
    month: YearMonth,
    dailyTotals: Map<Int, Long>,
    monthSpend: Long,
    bottomPadding: Dp,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onDay: (LocalDate) -> Unit
) {
    DarkPage {
        LazyColumn(contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding
        )) {
            item(key = "calendar-header") {
                ScreenHeader("消费日历", month, onMonthChange, onOpenPeriodPicker) {}
            }
            item(key = "calendar-grid") {
                Box(Modifier.padding(top = 16.dp)) {
                    SpendingHeatmap(month, dailyTotals, monthSpend, onDay)
                }
            }
        }
    }
}

@Composable
private fun HeroAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    // Single-line label that never clips: when space runs short (narrow
    // phone, large text) the icon goes first, then the label steps down in
    // size until it fits.
    val style = body(15f, FontWeight.Medium, color = Bot.Ink)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val labelWidth = remember(label, style, density) { measurer.measure(label, style).size.width }
        val showIcon = with(density) { labelWidth.toDp() } + (18 + 8 + 32 + 8).dp <= maxWidth
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Bot.Surface)
                .botPress(onClick = onClick)
                .semantics { contentDescription = label }
                .padding(horizontal = if (showIcon) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                BotIcon(icon, size = 18.dp, tint = Bot.Ink)
                Spacer(Modifier.width(8.dp))
            }
            BasicText(
                text = label,
                style = style,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 11.sp,
                    maxFontSize = style.fontSize,
                    stepSize = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun SpendingHeatmap(
    month: YearMonth,
    dailyTotals: Map<Int, Long>,
    monthSpend: Long,
    onDay: (LocalDate) -> Unit
) {
    val today = todayKl()
    val limit = SpendingPreferences.dailyLimitCents
    val redLoss = SpendingPreferences.redMeansLoss
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "每日消费",
                style = display(23f, FontWeight.Medium, letterSpacing = -0.5f),
                color = Bot.SurfaceInk,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.semantics { contentDescription = "支出由少到多" }
            ) {
                (0..4).map { SpendingHeat.color(it * 25L, 100L, redLoss) }.forEach { color ->
                    Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
        }
        SpendingSettingsControl(autoAsk = true)
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (if (limit > 0L) listOf("0%", "50%", "100%及以上") else listOf("设置上限后启用消费颜色")).forEach { mark ->
                Text(mark, style = body(10f), color = Bot.SurfaceMuted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            "日一二三四五六".forEach { letter ->
                Text(
                    letter.toString(),
                    style = body(11f, FontWeight.Medium),
                    color = LedgerAppearance.color(Color(0xFF737989)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(bottom = 6.dp)
                )
            }
        }
        // Another month's pattern washes in cell by cell; a sixth week unfolds or folds away.
        Column(
            modifier = Modifier.animateContentSize(BotMotion.Resize),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            calendarCells(month).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(62.dp)) {
                            if (date != null) {
                                val total = dailyTotals[date.dayOfMonth] ?: 0L
                                val background = if (total > 0 && limit > 0) SpendingHeat.color(total, limit, redLoss) else Bot.TileBg
                                val ink = if (total > 0 && limit > 0) SpendingHeat.ink(background) else Bot.Ink
                                val future = date > today
                                val heat = animatedColor(
                                    background,
                                    "heat"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (future) 0.4f else 1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(heat)
                                        .botPress(enabled = !future) { onDay(date) }
                                        .semantics {
                                            contentDescription = "${dateLabel(date)} 消费 ${formatRm(total)}"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(date.dayOfMonth.toString(), style = body(11f), color = ink)
                                        Spacer(Modifier.height(6.dp))
                                        BasicText(
                                            text = if (future) "—" else SpendingHeat.wholeYuan(total),
                                            style = body(14f, FontWeight.SemiBold, color = ink),
                                            maxLines = 1,
                                            autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 14.sp),
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "本月支出 ${formatRm(monthSpend)} · 日历金额取整显示 点击日期查看完整金额",
            style = body(11f, lineHeight = 1.5f),
            color = LedgerAppearance.color(Color(0xFF778090)),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
