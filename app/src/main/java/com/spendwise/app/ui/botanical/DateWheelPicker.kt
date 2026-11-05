package com.spendwise.app.ui.botanical

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun DateWheelPicker(
    visible: Boolean,
    initial: LocalDate,
    maxDate: LocalDate?,
    onCancel: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    // Reaching the end of a wheel must not drag the date sheet closed.
    BottomCardHost(visible, onCancel, scrim = Bot.Scrim, fill = Bot.Surface, dragEnabled = false) { cardModifier ->
        val initialDate = if (maxDate != null) minOf(initial, maxDate) else initial
        val lastYear = maxDate?.year ?: maxOf(todayKl().year + 10, initialDate.year)
        val firstYear = minOf(lastYear - 110, initialDate.year)
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val rowHeight = ((maxHeight - navBottom - 76.dp) / 3).coerceIn(32.dp, 57.dp)
        key(initialDate, maxDate) {
            val yearState = rememberLazyListState(initialDate.year - firstYear)
            val monthState = rememberLazyListState(initialDate.monthValue - 1)
            val dayState = rememberLazyListState(initialDate.dayOfMonth - 1)
            val year = firstYear + centeredWheelIndex(yearState).coerceIn(0, lastYear - firstYear)
            val lastMonth = if (maxDate != null && year == maxDate.year) maxDate.monthValue else 12
            val month = 1 + centeredWheelIndex(monthState).coerceIn(0, lastMonth - 1)
            val yearMonth = YearMonth.of(year, month)
            val lastDay = clampToMonth(yearMonth.atEndOfMonth(), yearMonth, maxDate).dayOfMonth
            val day = 1 + centeredWheelIndex(dayState).coerceIn(0, lastDay - 1)
            val settled = !yearState.isScrollInProgress &&
                !monthState.isScrollInProgress && !dayState.isScrollInProgress
            val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            Column(
                Modifier.then(cardModifier)
                    .widthIn(max = 480.dp).fillMaxWidth()
                    .clip(shape).background(Bot.Surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(bottom = navBottom + 10.dp)
                    .semantics { paneTitle = "选择日期" }
            ) {
                Box(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp)) {
                    DateWheelAction("取消", onCancel, Modifier.align(Alignment.CenterStart))
                    Text(
                        "选择日期",
                        modifier = Modifier.align(Alignment.Center),
                        style = body(17f, FontWeight.SemiBold),
                        color = Bot.Ink
                    )
                    DateWheelAction("确定", { onConfirm(yearMonth.atDay(day)) },
                        Modifier.align(Alignment.CenterEnd), enabled = settled)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DateWheel("年", firstYear..lastYear, year, yearState, rowHeight, Modifier.weight(1.12f))
                    DateWheel("月", 1..lastMonth, month, monthState, rowHeight, Modifier.weight(1f))
                    DateWheel("日", 1..lastDay, day, dayState, rowHeight, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun centeredWheelIndex(state: LazyListState): Int {
    val index by remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo
            val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - middle)
            }?.index ?: state.firstVisibleItemIndex
        }
    }
    return index
}

@Composable
private fun DateWheel(
    label: String,
    values: IntRange,
    value: Int,
    state: LazyListState,
    rowHeight: Dp,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()
    Box(modifier.height(rowHeight * 3)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = label },
            state = state,
            contentPadding = PaddingValues(vertical = rowHeight),
            flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(values.last - values.first + 1) { index ->
                val number = values.first + index
                val current = number == value
                Box(
                    Modifier.fillMaxWidth().height(rowHeight)
                        .botPress { scope.launch { state.animateScrollToItem(index) } }
                        .semantics {
                            contentDescription = "$number$label"
                            selected = current
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        number.toString().padStart(2, '0'),
                        style = body(17f, if (current) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (current) Bot.Ink else Bot.SurfaceMuted
                    )
                }
            }
        }
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().height(rowHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(2) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
            }
        }
    }
}

@Composable
private fun DateWheelAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier.size(width = 60.dp, height = 44.dp).botPress(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = body(14f), color = if (enabled) Bot.ActionSolid else Bot.SurfaceMuted)
    }
}
