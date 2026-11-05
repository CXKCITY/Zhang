package com.spendwise.app.ui.botanical

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.ui.DateRange
import com.spendwise.app.ui.EntryWindow
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    month: YearMonth,
    items: List<LedgerItem>,
    categories: List<Category>,
    accounts: List<Account>,
    query: ActivityQuery,
    range: DateRange?,
    bottomPadding: Dp,
    canFlipMonth: Boolean,
    enteringDirection: Int,
    onFlipMonth: (Int) -> Unit,
    onQueryChange: (ActivityQuery) -> Unit,
    onClearRange: () -> Unit,
    onClearAll: () -> Unit,
    hero: @Composable () -> Unit,
    onHeroCoveredChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenItem: (LedgerItem) -> Unit,
    onManage: suspend (Set<Long>, Long?) -> String?
) {
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val names = remember(accounts) { accountNames(accounts) }
    val shown = remember(items, query, range, categoriesById) {
        items.filter { item ->
            item.kind == EntryKind.Expense &&
                (range == null || item.date in range.from..range.to) &&
                matches(item, query, categoriesById)
        }
    }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    LaunchedEffect(month, query, range) { selectedIds = emptySet() }
    LaunchedEffect(shown) { selectedIds = selectedIds.intersect(shown.map { (it as LedgerItem.Entry).expense.id }.toSet()) }
    androidx.activity.compose.BackHandler(enabled = selectedIds.isNotEmpty()) { selectedIds = emptySet() }
    val days = remember(shown) { shown.groupBy { it.date }.toList() }
    val filterCount = listOf(query.accountId != null, query.categoryId != null, range != null).count { it }
    val narrowed = query.search.isNotBlank() || filterCount > 0
    val focus = LocalFocusManager.current
    val footerIndex = 3 + (if (filterCount > 0) 1 else 0) +
        (if (shown.isEmpty()) 1 else days.size * 2 + 1)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (enteringDirection > 0) footerIndex else 0
    )
    val offset = remember { Animatable(if (enteringDirection == 0) 0f else 1f) }
    LaunchedEffect(offset) {
        if (offset.value != 0f) offset.animateTo(0f, BotMotion.smooth(0.24f))
    }
    val canGoNewer = month < YearMonth.from(todayKl())
    val gesturesEnabled = canFlipMonth && selectedIds.isEmpty()
    val latestFlip by rememberUpdatedState(onFlipMonth)
    val statusHeight = pageTopPadding() - 12.dp
    val statusHeightPx = with(LocalDensity.current) { statusHeight.roundToPx() }
    LaunchedEffect(listState, statusHeightPx) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val heroItem = layout.visibleItemsInfo.firstOrNull { it.key == "header" }
            if (heroItem == null) listState.firstVisibleItemIndex > 0
            else heroItem.offset + heroItem.size <= statusHeightPx
        }.collect { onHeroCoveredChange(it) }
    }
    val inset = Modifier.padding(horizontal = 16.dp)

    DarkPage(Modifier.graphicsLayer {
        translationY = (if (enteringDirection < 0) 34.dp else (-34).dp).toPx() * offset.value
        alpha = 1f - offset.value * 0.25f
    }) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().pointerInput(listState, gesturesEnabled, canGoNewer) {
                if (!gesturesEnabled) return@pointerInput
                val threshold = 72.dp.toPx()
                // Observe the same drag as the list without intercepting taps or horizontal cards.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var horizontal = 0f
                    var vertical = 0f
                    var edgeDistance = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (event.changes.any { it.id != down.id && it.pressed }) break
                        if (!change.pressed) {
                            if (!change.isConsumed && abs(vertical) > abs(horizontal) * 1.25f) {
                                when {
                                    edgeDistance <= -threshold && !listState.canScrollForward -> latestFlip(-1)
                                    edgeDistance >= threshold && canGoNewer && !listState.canScrollBackward -> latestFlip(1)
                                }
                            }
                            break
                        }
                        val delta = change.position - change.previousPosition
                        horizontal += delta.x
                        vertical += delta.y
                        edgeDistance = when {
                            delta.y < 0f && !listState.canScrollForward -> minOf(0f, edgeDistance) + delta.y
                            delta.y > 0f && canGoNewer && !listState.canScrollBackward -> maxOf(0f, edgeDistance) + delta.y
                            delta.y != 0f -> 0f
                            else -> edgeDistance
                        }
                    }
                }
            },
            contentPadding = PaddingValues(bottom = bottomPadding + if (selectedIds.isNotEmpty()) 150.dp else 0.dp)
        ) {
            item(key = "header") { hero() }
            item(key = "details-heading") {
                Text(
                    "明细",
                    style = display(24f),
                    color = Bot.Ink,
                    modifier = inset.padding(top = 16.dp).semantics { heading() }
                )
            }
            item(key = "tools") {
                Row(
                    modifier = inset.then(itemMotion()).fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchField(
                        value = query.search,
                        onValueChange = { onQueryChange(query.copy(search = it)) },
                        onDone = { focus.clearFocus() },
                        modifier = Modifier.weight(1f)
                    )
                    FilterButton(count = filterCount, onClick = onOpenFilters)
                }
            }
            if (filterCount > 0) {
                item(key = "chips") {
                    FlowRow(
                        modifier = inset.then(itemMotion()).fillMaxWidth().padding(top = 12.dp).animateContentSize(BotMotion.Resize),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        query.accountId?.let { id ->
                            FilterChip(names[id] ?: "账户") { onQueryChange(query.copy(accountId = null)) }
                        }
                        query.categoryId?.let { id ->
                            FilterChip(categoriesById[id]?.name ?: "分类") { onQueryChange(query.copy(categoryId = null)) }
                        }
                        range?.let { r ->
                            FilterChip("${shortDateLabel(r.from)} – ${shortDateLabel(r.to)}", onRemove = onClearRange)
                        }
                        if (filterCount > 1) {
                            Box(
                                Modifier
                                    .heightIn(min = 44.dp)
                                    .botPress {
                                        onQueryChange(query.copy(accountId = null, categoryId = null))
                                        onClearRange()
                                    }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "全部清除",
                                    style = body(13f, FontWeight.Medium).copy(textDecoration = TextDecoration.Underline),
                                    color = Bot.PageMuted
                                )
                            }
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        modifier = inset.then(itemMotion()),
                        title = if (query.search.isNotBlank()) "没有找到与“${query.search.trim()}”匹配的记录" else "这个时段还没有记录",
                        body = if (narrowed) "清除筛选即可查看当月全部记录" else "记下第一笔 或切换其他月份",
                        action = if (narrowed) "清除筛选" else "记一笔",
                        onAction = if (narrowed) onClearAll else onAdd
                    )
                }
            } else {
                days.forEach { (date, dayItems) ->
                    item(key = "day-$date") {
                        Row(
                            modifier = inset.then(itemMotion()).fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                dayHeading(date),
                                style = body(13f),
                                color = Bot.PageMuted,
                                modifier = Modifier.weight(1f).semantics { heading() }
                            )
                            Money(
                                cents = totalsOf(dayItems).net,
                                style = body(12f, FontWeight.Medium),
                                color = Bot.PageMuted,
                                centsScale = 1f,
                                currencyWeight = FontWeight.Medium
                            )
                        }
                    }
                    item(key = "list-$date") {
                        Column(
                            inset.then(itemMotion())
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25.dp))
                                .background(Bot.Surface)
                                .animateContentSize(BotMotion.Resize)
                        ) {
                            dayItems.forEachIndexed { index, item ->
                                val text = rowText(item, categoriesById, names)
                                TransactionRow(
                                    title = text.title,
                                    meta = text.meta,
                                    cents = item.cents,
                                    kind = item.kind,
                                    art = text.art,
                                    onClick = {
                                        val id = (item as LedgerItem.Entry).expense.id
                                        if (selectedIds.isEmpty()) onOpenItem(item)
                                        else selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                    },
                                    onLongClick = { focus.clearFocus(); selectedIds = selectedIds + (item as LedgerItem.Entry).expense.id },
                                    selected = (item as LedgerItem.Entry).expense.id in selectedIds,
                                    selectionMode = selectedIds.isNotEmpty(),
                                    showDivider = index < dayItems.lastIndex
                                )
                            }
                        }
                    }
                }
                item(key = "count") {
                    Note(
                        "${shown.size} 笔 · 长按记录可多选",
                        modifier = inset.then(itemMotion()).padding(top = 25.dp)
                    )
                }
            }
            item(key = "month-end") {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .botPress(enabled = gesturesEnabled) { onFlipMonth(-1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("继续上划查看${month.minusMonths(1).monthValue}月", style = body(12f), color = Bot.PageMuted)
                }
            }
        }
        BulkRecordActions(selectedIds, shown.map { (it as LedgerItem.Entry).expense }, categories,
            onSelectionChange = { selectedIds = it }, onManage = onManage,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(start=12.dp, end=12.dp, bottom=bottomPadding + 8.dp))
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Bot.ChipBg)
            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(999.dp))
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BotIcon(BotIcons.Search, size = 18.dp, tint = Bot.Ink)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("搜索消费内容或分类", style = body(16f), color = Bot.PageMuted, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = body(16f, color = Bot.Ink),
                cursorBrush = SolidColor(Bot.ActionSolid),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "搜索消费内容或分类" }
            )
        }
        AnimatedVisibility(
            visible = value.isNotEmpty(),
            enter = fadeIn(BotMotion.smooth(0.2f)) + scaleIn(BotMotion.bouncy(0.32f), initialScale = 0.5f),
            exit = fadeOut(BotMotion.smooth(0.16f)) + scaleOut(BotMotion.smooth(0.2f), targetScale = 0.5f)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .botPress { onValueChange("") }
                    .semantics { contentDescription = "清除搜索" },
                contentAlignment = Alignment.Center
            ) {
                BotIcon(BotIcons.Close, size = 16.dp, tint = Bot.Ink)
            }
        }
    }
}

@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    val active = count > 0
    val shownCount = rememberRetained(count.takeIf { it > 0 }) ?: 0
    Box(Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(animatedColor(if (active) Bot.ActionSolid else Bot.ChipBg, "filterFill"))
                .border(1.dp, animatedColor(if (active) Bot.ActionSolid else Bot.Rule, "filterEdge"), CircleShape)
                .botPress(onClick = onClick)
                .semantics { contentDescription = if (active) "筛选 已启用 $count 项" else "筛选" },
            contentAlignment = Alignment.Center
        ) {
            BotIcon(BotIcons.Filter, size = 20.dp, tint = animatedColor(if (active) Color.White else Bot.Ink, "filterInk"))
        }
        AnimatedVisibility(
            visible = active,
            enter = fadeIn(BotMotion.smooth(0.2f)) + scaleIn(BotMotion.bouncy(0.34f), initialScale = 0.3f),
            exit = fadeOut(BotMotion.smooth(0.16f)) + scaleOut(BotMotion.smooth(0.22f), targetScale = 0.3f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B1428))
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Bot.Orchid),
                contentAlignment = Alignment.Center
            ) {
                Text(shownCount.toString(), style = body(11f, FontWeight.Bold), color = Bot.Navy)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Bot.Rule)
            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(99.dp))
            .botPress(onClick = onRemove)
            .semantics { contentDescription = "移除筛选 $label" }
            .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            label,
            style = body(13f, FontWeight.Medium, lineHeight = 1.3f),
            color = Bot.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp)
        )
        BotIcon(BotIcons.Close, size = 14.dp, tint = Bot.Ink.copy(alpha = 0.75f))
    }
}

/** Six months as quiet bars: this month carries the accent, last month (the comparison) sits half-lit. */
@Composable
fun MonthlyComparisonPanel(
    comparison: MonthComparison,
    month: YearMonth,
    income: Boolean,
    filtered: Boolean,
    modifier: Modifier = Modifier
) {
    val percent = comparison.percent
    val change = when {
        percent == null -> if (comparison.current > 0) "上个月暂无记录" else "这两个月暂无记录"
        percent == 0 -> "与上个月持平"
        else -> "比上个月${if (percent > 0) "增加" else "减少"} ${abs(percent)}%"
    }
    val toneColor = when {
        percent == null || percent == 0 -> Bot.SurfaceInk
        (if (income) percent > 0 else percent < 0) -> Bot.TrendGood
        else -> Bot.TrendCaution
    }
    val trend = if (income) Bot.IncomeBar else Bot.ExpenseBar
    val maximum = maxOf(comparison.series.maxOrNull() ?: 0L, 1L)
    Panel(modifier) {
        SectionHead(if (income) "收入趋势" else "支出趋势", modifier = Modifier.padding(bottom = 10.dp)) {
            Text("最近六个月", style = body(13f, FontWeight.Medium, lineHeight = 1.4f), color = Bot.ChartLabel)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (percent != null && percent != 0) {
                BotIcon(if (percent > 0) BotIcons.Up else BotIcons.Down, size = 14.dp, tint = toneColor)
            }
            AnimatedContent(
                targetState = change,
                transitionSpec = {
                    fadeIn(BotMotion.smooth(0.28f))
                        .togetherWith(fadeOut(BotMotion.smooth(0.18f)))
                        .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                },
                label = "trendChange"
            ) { line ->
                Text(line, style = body(16f, FontWeight.SemiBold, lineHeight = 1.35f), color = animatedColor(toneColor, "trendTone"))
            }
        }
        Text(
            "${monthLabel(comparison.previousMonth)} · ${formatRm(comparison.previous)}",
            style = body(13f, lineHeight = 1.5f),
            color = Bot.SurfaceMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (comparison.series.any { it > 0 }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(112.dp)
                    .semantics {
                        contentDescription = "截至 ${monthLabel(month)} 的六个月统计 " +
                            comparison.series.joinToString(", ") { formatRm(it) }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comparison.series.forEachIndexed { index, value ->
                    val m = month.minusMonths((5 - index).toLong())
                    val current = index == 5
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            val fraction = (value.toFloat() / maximum).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .widthIn(max = 28.dp)
                                    .fillMaxWidth()
                                    .growingBar(fraction, index, minHeight = 3.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(
                                        if (value == 0L) Color(0xFFDFE3EB)
                                        else trend.copy(alpha = if (current) 1f else if (index == 4) 0.5f else 0.22f)
                                    )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            monthShort(m.monthValue),
                            style = body(12f, if (current) FontWeight.Bold else FontWeight.Medium),
                            color = if (current) Bot.Navy else Bot.ChartLabel
                        )
                    }
                }
            }
        }
        if (filtered) {
            Text(
                "已应用当前筛选",
                style = body(12f, lineHeight = 1.5f),
                color = Bot.SurfaceMuted,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

/**
 * Account, category and date filters (`FilterSheet`). Edits are drafts until
 * "Show N entries"; the count is live, including for a custom range that
 * isn't loaded yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    visible: Boolean,
    month: YearMonth,
    query: ActivityQuery,
    range: DateRange?,
    accounts: List<Account>,
    archivedAccounts: List<Account>,
    categories: List<Category>,
    monthItems: List<LedgerItem>,
    loadEntries: suspend (LocalDate, LocalDate) -> EntryWindow,
    onApply: (accountId: Long?, categoryId: Long?, range: DateRange?) -> Unit,
    onDismiss: () -> Unit
) {
    var accountId by remember(visible) { mutableStateOf(query.accountId) }
    var categoryId by remember(visible) { mutableStateOf(query.categoryId) }
    var useRange by remember(visible) { mutableStateOf(range != null) }
    var from by remember(visible) { mutableStateOf(range?.from ?: month.atDay(1)) }
    var to by remember(visible) { mutableStateOf(range?.to ?: minOf(month.atEndOfMonth(), todayKl())) }
    var picking by remember(visible) { mutableStateOf<String?>(null) }
    var rangeItems by remember(visible) { mutableStateOf<List<LedgerItem>?>(null) }
    val incomeIds = remember(categories) { incomeCategoryIds(categories) }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val invalid = useRange && from > to

    LaunchedEffect(visible, useRange, from, to) {
        if (!visible || !useRange || from > to) return@LaunchedEffect
        delay(150)
        val window = loadEntries(from, to)
        rangeItems = ledgerItems(window.expenses, window.transfers, incomeIds)
    }
    val draft = query.copy(accountId = accountId, categoryId = categoryId)
    val count = when {
        invalid -> 0
        useRange -> rangeItems?.count { matches(it, draft, categoriesById) }
        else -> monthItems.count { matches(it, draft, categoriesById) }
    }
    val active = listOf(accountId != null, categoryId != null, useRange).count { it }

    Box(Modifier.fillMaxSize()) {
        BotSheet(
            visible = visible,
            onDismiss = onDismiss,
            title = "筛选",
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BotButton(
                        "重置",
                        onClick = {
                            accountId = null
                            categoryId = null
                            useRange = false
                        },
                        type = ButtonType.Secondary,
                        enabled = active > 0,
                        dmSans = true
                    )
                    BotButton(
                        when {
                            invalid -> "检查日期"
                            count == null -> "统计中"
                            count == 1 -> "查看 1 笔记录"
                            else -> "查看 $count 笔记录"
                        },
                        onClick = { onApply(accountId, categoryId, if (useRange) DateRange(from, to) else null) },
                        enabled = !invalid,
                        dmSans = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        ) {
            FilterGroup("分类") {
                FilterOption("全部分类", categoryId == null) { categoryId = null }
                categories.forEach { category ->
                    FilterOption(category.name, categoryId == category.id) { categoryId = category.id }
                }
            }
            FilterGroup("日期") {
                FilterOption(monthLabel(month), !useRange) { useRange = false }
                FilterOption("自定义时间", useRange) { useRange = true }
            }
            Reveal(visible = useRange) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                    DateField("从", from, Modifier.weight(1f)) { picking = "from" }
                    DateField("To", to, Modifier.weight(1f)) { picking = "to" }
                }
            }
            AnimatedError(
                if (invalid) "结束日期不能早于开始日期" else null,
                Modifier.padding(top = 16.dp)
            )
        }
        CalendarPicker(
            visible = visible && picking != null,
            mode = CalendarMode.Date,
            initial = if (picking == "to") to else from,
            onCancel = { picking = null },
            onConfirm = { date ->
                if (picking == "to") to = date else from = date
                picking = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, first: Boolean = false, content: @Composable () -> Unit) {
    Text(
        title,
        style = body(14f, FontWeight.Medium),
        color = Bot.SurfaceMuted,
        modifier = Modifier
            .padding(top = if (first) 0.dp else 20.dp, bottom = 10.dp)
            .semantics { heading() }
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(animatedColor(if (selected) Bot.ActionSolid else Color(0xFFF0F2F6), "optionFill"))
            .botPress(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = body(14f, FontWeight.Medium, lineHeight = 1.3f),
            color = animatedColor(if (selected) Color.White else Bot.SecondaryInk, "optionInk"),
            textAlign = TextAlign.Center
        )
    }
}

/** A date shown as a field; tapping opens the illustrated calendar. */
@Composable
fun DateField(label: String, date: LocalDate, modifier: Modifier = Modifier, isError: Boolean = false, onClick: () -> Unit) {
    Column(modifier) {
        FieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 49.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Bot.FieldBg)
                .border(1.dp, if (isError) Color(0xFFD9788C) else Bot.FieldBorder, RoundedCornerShape(17.dp))
                .botPress(scale = 0.98f, onClick = onClick)
                .semantics { contentDescription = "$label ${dateLabel(date)} 修改日期" }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dateLabel(date), style = body(14f), color = Bot.FieldInk, modifier = Modifier.weight(1f), maxLines = 1)
            BotIcon(BotIcons.Calendar, size = 18.dp, tint = Bot.FieldInk)
        }
    }
}

