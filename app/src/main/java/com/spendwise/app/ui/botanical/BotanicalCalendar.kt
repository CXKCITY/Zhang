package com.spendwise.app.ui.botanical

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarMode { Date, Month }

private val Glass get() = LedgerAppearance.current.heroStart.copy(alpha = 0.6f)
private val GlassEdge = Color(0x47FFFFFF)
private val CardShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
private val CardColor get() = LedgerAppearance.current.heroStart

/**
 * The first date of each of [month]'s calendar cells, Sunday first; null for
 * the blank lead-in and tail. Always whole weeks, so six-week months (31 days
 * starting on a Friday or Saturday) get their sixth row.
 */
fun calendarCells(month: YearMonth): List<LocalDate?> {
    val offset = month.atDay(1).dayOfWeek.value % 7
    val days = month.lengthOfMonth()
    val count = (offset + days + 6) / 7 * 7
    return List(count) { index ->
        val day = index - offset + 1
        if (day in 1..days) month.atDay(day) else null
    }
}

/**
 * Moves the staged date to [target]'s month, keeping its day where the month
 * allows (Jan 31 → Feb 28) and never past [maxDate].
 */
fun clampToMonth(selected: LocalDate, target: YearMonth, maxDate: LocalDate?): LocalDate {
    val maxMonth = maxDate?.let(YearMonth::from)
    val month = if (maxMonth != null && target > maxMonth) maxMonth else target
    var day = minOf(selected.dayOfMonth, month.lengthOfMonth())
    if (maxDate != null && month == maxMonth) day = minOf(day, maxDate.dayOfMonth)
    return month.atDay(day)
}

/** Month selection uses the grid; individual dates use the three wheels. */
@Composable
fun CalendarPicker(
    visible: Boolean,
    mode: CalendarMode,
    initial: LocalDate,
    onCancel: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    maxDate: LocalDate? = todayKl()
) {
    if (mode == CalendarMode.Date) {
        DateWheelPicker(visible, initial, maxDate, onCancel, onConfirm)
        return
    }
    BottomCardHost(visible = visible, onDismiss = onCancel, scrim = Color(0x33000000), fill = CardColor) { cardModifier ->
        val bounded = if (maxDate != null) minOf(initial, maxDate) else initial
        var visibleMonth by remember(initial) { mutableStateOf(YearMonth.from(bounded)) }
        val maxMonth = maxDate?.let(YearMonth::from)
        val lastYear = maxMonth?.year ?: maxOf(todayKl().year + 10, visibleMonth.year)
        val yearRange = minOf(lastYear - 110, bounded.year)..lastYear
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier.then(cardModifier)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.94f)
                .shadow(24.dp, CardShape, clip = false)
                .clip(CardShape)
                .background(CardColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp + navBottom)
                .semantics { paneTitle = "选择月份" }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GlassCircle(BotIcons.Close, "取消", size = 48, onClick = onCancel)
                GlassCircle(BotIcons.Check, "确定", size = 48) { onConfirm(visibleMonth.atDay(1)) }
            }
            Spacer(Modifier.height(24.dp))
            MonthView(
                visibleMonth = visibleMonth,
                maxMonth = maxMonth,
                yearRange = yearRange,
                onPick = { visibleMonth = it },
                onYear = { year ->
                    val target = YearMonth.of(year, visibleMonth.monthValue)
                    visibleMonth = if (maxMonth != null) minOf(target, maxMonth) else target
                }
            )
        }
    }
}

@Composable
private fun MonthView(
    visibleMonth: YearMonth,
    maxMonth: YearMonth?,
    yearRange: IntRange,
    onPick: (YearMonth) -> Unit,
    onYear: (Int) -> Unit
) {
    var yearsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("选择月份", style = body(18f, FontWeight.SemiBold, lineHeight = 1.3f), color = Color.White)
            Box {
                GlassPill(onClick = { yearsOpen = true }, contentDescription = "${visibleMonth.year}年 选择年份") {
                    Text(visibleMonth.year.toString(), style = body(18f, FontWeight.Medium), color = Color.White)
                    Spacer(Modifier.size(10.dp))
                    BotIcon(BotIcons.ChevronDown, size = 18.dp, tint = Color.White)
                }
                DropdownMenu(
                    expanded = yearsOpen,
                    onDismissRequest = { yearsOpen = false },
                    containerColor = Bot.Surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    yearRange.reversed().forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    year.toString(),
                                    style = body(15f, if (year == visibleMonth.year) FontWeight.SemiBold else FontWeight.Normal),
                                    color = Bot.FieldInk
                                )
                            },
                            onClick = {
                                onYear(year)
                                yearsOpen = false
                            }
                        )
                    }
                }
            }
        }
        (1..12).chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { monthNumber ->
                    val month = YearMonth.of(visibleMonth.year, monthNumber)
                    val isSelected = monthNumber == visibleMonth.monthValue
                    val enabled = maxMonth == null || month <= maxMonth
                    val shape = RoundedCornerShape(100.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .alpha(if (enabled) 1f else 0.4f)
                            .clip(shape)
                            .background(animatedColor(if (isSelected) Bot.ActionSolid else Glass, "monthFill"))
                            .border(if (isSelected) 2.dp else 1.dp, animatedColor(if (isSelected) Color.White else GlassEdge, "monthEdge"), shape)
                            .botPress(enabled = enabled) { onPick(month) }
                            .semantics {
                                contentDescription = monthLabel(month)
                                selected = isSelected
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(monthShort(monthNumber), style = body(17f, FontWeight.Medium, lineHeight = 1.3f), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCircle(
    icon: ImageVector,
    label: String,
    size: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Glass)
            .border(1.dp, GlassEdge, CircleShape)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = 22.dp, tint = Color.White)
    }
}

@Composable
private fun GlassPill(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(Glass)
            .border(1.dp, GlassEdge, shape)
            .botPress(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
