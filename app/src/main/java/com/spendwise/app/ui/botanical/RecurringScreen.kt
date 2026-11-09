package com.spendwise.app.ui.botanical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurrenceSchedule
import com.spendwise.app.domain.RecurringRule
import java.time.LocalDate
import kotlin.math.roundToLong

private enum class RuleFilter(val label: String) { Active("进行中"), Paused("已暂停") }

/** What active recurring expenses add up to in an average month. */
fun estimatedMonthlyExpenses(rules: List<RecurringRule>): Long = rules
    .filter { !it.isPaused && !it.isIncome }
    .sumOf {
        when (it.cadence) {
            RecurrenceCadence.Weekly -> it.amountCents * 52.0 / 12.0
            RecurrenceCadence.Monthly -> it.amountCents.toDouble()
            RecurrenceCadence.Yearly -> it.amountCents / 12.0
        }
    }
    .roundToLong()

@Composable
fun RecurringScreen(
    rules: List<RecurringRule>,
    categories: List<Category>,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (RecurringRule) -> Unit,
    onToggle: (RecurringRule) -> Unit,
    onCheck: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(RuleFilter.Active) }
    val shown = rules
        .filter { if (filter == RuleFilter.Active) !it.isPaused else it.isPaused }
        .sortedBy { it.nextDueEpochDay }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item(key = "top") {
                Box(itemMotion()) {
                    TopBar("周期记账", "返回设置", onBack) {
                        CircleIconButton(BotIcons.Plus, "添加周期记账", onClick = onAdd, size = 46.dp)
                    }
                }
            }
            item(key = "summary") {
                Column(itemMotion()) {
                    Kicker("预计每月固定支出", Modifier.padding(bottom = 8.dp))
                    Money(
                        cents = estimatedMonthlyExpenses(rules),
                        style = moneyStyle(58f, letterSpacingPx = -2f),
                        color = Bot.Ink,
                        modifier = Modifier.fillMaxWidth(),
                        animate = true
                    )
                    Subtitle("根据启用中的周期支出计算", Modifier.padding(top = 10.dp))
                    PillSegment(
                        options = RuleFilter.entries,
                        selected = filter,
                        onSelect = { filter = it },
                        label = { it.label },
                        modifier = Modifier.padding(top = 20.dp, bottom = 3.dp)
                    )
                }
            }
            items(shown, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    category = categoriesById[rule.categoryId],
                    onToggle = { onToggle(rule) },
                    onEdit = { onEdit(rule) },
                    modifier = itemMotion()
                )
            }
            if (shown.isEmpty()) {
                item(key = "empty-$filter") {
                    EmptyState(
                        modifier = itemMotion(),
                        title = if (filter == RuleFilter.Active) "暂无进行中的周期记账" else "暂无暂停的周期记账",
                        body = if (filter == RuleFilter.Active) "添加房租 订阅或工资等周期记录" else "暂停的周期记账会显示在这里",
                        action = if (filter == RuleFilter.Active) "添加周期记账" else null,
                        onAction = if (filter == RuleFilter.Active) onAdd else null
                    )
                }
            }
            item(key = "footer") {
                Column(itemMotion()) {
                    BotButton(
                        "检查到期记录",
                        onClick = onCheck,
                        type = ButtonType.Secondary,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    )
                    Note(
                        "打开账时会自动补记到期账单 暂停期间不记账 恢复后从下一次到期日继续",
                        Modifier.padding(top = 25.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: RecurringRule, category: Category?, onToggle: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val art = category?.art ?: categoryArtFor(rule.categoryName, rule.categoryIconName, rule.isIncome)
    Column(
        modifier
            .padding(top = 13.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Bot.Surface)
            .padding(19.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtImage(art.drawable, size = 45.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    rule.merchant.ifBlank { rule.categoryName },
                    style = display(24f, FontWeight.Medium, lineHeight = 1.2f),
                    color = Bot.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${rule.cadence.displayLabel} · ${if (rule.isIncome) "收入" else "支出"}",
                    style = body(11f),
                    color = LedgerAppearance.color(Color(0xFF7C8293))
                )
            }
            BotToggle(
                checked = !rule.isPaused,
                onToggle = onToggle,
                label = "启用 ${rule.merchant.ifBlank { rule.categoryName }}",
                offColor = Color(0xFFD9DCE3)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Money(
                cents = rule.amountCents,
                style = moneyStyle(38f, letterSpacingPx = -1f),
                color = if (rule.isIncome) Bot.Positive else Bot.Ink,
                modifier = Modifier.weight(1f)
            )
            TextLinkButton("编辑", onClick = onEdit, color = LedgerAppearance.color(Color(0xFF667184)))
        }
        Box(Modifier.fillMaxWidth().padding(top = 20.dp).height(1.dp).background(Bot.Rule))
        Row(Modifier.fillMaxWidth().padding(top = 15.dp)) {
            Text(
                "下次 · ${dateLabel(LocalDate.ofEpochDay(rule.nextDueEpochDay))}",
                style = body(11f),
                color = Bot.Ink,
                modifier = Modifier.weight(1f)
            )
            Text(if (rule.isPaused) "已暂停" else "自动记账", style = body(11f), color = Bot.Ink)
        }
    }
}

private enum class RuleKind(val label: String) { Expense("支出"), Income("收入") }

/**
 * New / edit recurring rule. A new rule starts on its first date (a past date
 * backfills once). Editing shows the next date; picking another date moves
 * the schedule, and it continues from the next occurrence after today.
 */
@Composable
fun RuleFormSheet(
    visible: Boolean,
    existing: RecurringRule?,
    categories: List<Category>,
    accounts: List<Account>,
    onSave: (id: Long?, amount: String, categoryId: Long?, accountId: Long?, name: String, notes: String, cadence: RecurrenceCadence, firstDate: String) -> String?,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(if (visible) RuleKey(existing) else null)
    var picking by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var calendarStart by remember { mutableStateOf(todayKl()) }
    Box(Modifier.fillMaxSize()) {
        BotSheet(visible = visible, onDismiss = onDismiss, title = if (shown?.rule != null) "编辑周期记账" else "新建周期记账") {
            if (shown != null) {
                RuleFormBody(
                    existing = shown.rule,
                    categories = categories,
                    accounts = accounts,
                    pickedDate = pickedDate,
                    onPickDate = { current ->
                        calendarStart = current
                        picking = true
                    },
                    onConsumePicked = { pickedDate = null },
                    onSave = onSave,
                    onDelete = onDelete
                )
            }
        }
        CalendarPicker(
            visible = visible && picking,
            mode = CalendarMode.Date,
            initial = calendarStart,
            maxDate = null,
            onCancel = { picking = false },
            onConfirm = {
                pickedDate = it
                picking = false
            }
        )
    }
}

private data class RuleKey(val rule: RecurringRule?)

@Composable
private fun RuleFormBody(
    existing: RecurringRule?,
    categories: List<Category>,
    accounts: List<Account>,
    pickedDate: LocalDate?,
    onPickDate: (current: LocalDate) -> Unit,
    onConsumePicked: () -> Unit,
    onSave: (id: Long?, amount: String, categoryId: Long?, accountId: Long?, name: String, notes: String, cadence: RecurrenceCadence, firstDate: String) -> String?,
    onDelete: (() -> Unit)?
) {
    val today = todayKl()
    var kind by remember(existing) { mutableStateOf(if (existing?.isIncome == true) RuleKind.Income else RuleKind.Expense) }
    var name by remember(existing) { mutableStateOf(existing?.merchant.orEmpty()) }
    var cents by remember(existing) { mutableStateOf(existing?.amountCents ?: 0L) }
    var cadence by remember(existing) { mutableStateOf(existing?.cadence ?: RecurrenceCadence.Monthly) }
    val shownStart = existing?.let { LocalDate.ofEpochDay(it.nextDueEpochDay) } ?: today
    var date by remember(existing) { mutableStateOf(shownStart) }
    var dateChanged by remember(existing) { mutableStateOf(false) }
    var accountId by remember(existing) { mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember(existing) {
        mutableStateOf(existing?.categoryId ?: categories.firstOrNull { !it.isIncomeAdjustment }?.id)
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember(existing) { mutableStateOf<String?>(null) }
    var errorField by remember(existing) { mutableStateOf<String?>(null) }

    LaunchedEffect(pickedDate) {
        val picked = pickedDate ?: return@LaunchedEffect
        date = picked
        dateChanged = picked != shownStart
        onConsumePicked()
    }
    val kindCategories = categories.filter { it.isIncomeAdjustment == (kind == RuleKind.Income) }
    // Unchanged date + cadence keeps the original anchor, so a Jan-31 rule
    // doesn't collapse onto Feb 28 just because the form was saved.
    val anchor = if (existing != null && !dateChanged) LocalDate.ofEpochDay(existing.anchorEpochDay) else date
    val following = RecurrenceSchedule.nextOccurrence(date, cadence, anchor)

    PillSegment(
        options = listOf(RuleKind.Expense),
        selected = kind,
        onSelect = {
            kind = it
            categoryId = categories.firstOrNull { c -> c.isIncomeAdjustment == (it == RuleKind.Income) }?.id
        },
        label = { it.label },
        tone = SegmentTone.OnLight,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    FieldLabel("名称")
    BotTextField(
        value = name,
        onValueChange = {
            name = it
            error = null
        },
        placeholder = "例如 每月房租",
        maxLength = 80,
        isError = errorField == "name",
        contentLabel = "名称"
    )
    AmountInput(
        cents = cents,
        onChange = {
            cents = it
            error = null
        },
        error = if (errorField == "amount") error else null
    )
    AnimatedError(if (errorField == "amount") error else null, Modifier.padding(bottom = 12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            FieldLabel("重复周期")
            SelectField(
                options = RecurrenceCadence.entries,
                selected = cadence,
                onSelect = { cadence = it },
                label = { it.displayLabel },
                contentLabel = "重复周期"
            )
        }
        DateField(if (existing != null) "下次日期" else "首次日期", date, Modifier.weight(1f)) { onPickDate(date) }
    }
    FormCaption(
        "再下次 ${dateLabel(following)}" +
            if (cadence != RecurrenceCadence.Weekly) " 当月没有对应日期时 使用月末最后一天" else "",
        Modifier.padding(top = 12.dp)
    )
    val dateNote = when {
        existing == null && date <= today -> "保存时会补记截至今天的到期记录 每个日期只记一次"
        existing != null && dateChanged && date <= today -> "修改规则不会补记过去的日期 从今天之后的下一次日期继续"
        else -> null
    }
    val shownNote = rememberRetained(dateNote)
    Reveal(visible = dateNote != null) {
        InlineNote(shownNote.orEmpty(), Modifier.padding(top = 12.dp))
    }
    FieldLabel("分类", modifier = Modifier.padding(top = 16.dp))
    SelectField(
        options = kindCategories,
        selected = kindCategories.firstOrNull { it.id == categoryId },
        onSelect = { categoryId = it.id },
        label = { it.name },
        placeholder = "选择分类",
        isError = errorField == "category",
        contentLabel = "分类"
    )
    FieldLabel("消费内容 · 选填", modifier = Modifier.padding(top = 16.dp))
    BotTextField(
        value = notes,
        onValueChange = { notes = it },
        singleLine = false,
        minHeight = 80.dp,
        maxLength = 1000,
        contentLabel = "消费内容"
    )
    AnimatedError(if (errorField != "amount") error else null, Modifier.padding(top = 16.dp))
    BotButton(
        "保存周期记账",
        onClick = {
            fun fail(message: String, field: String) {
                error = message
                errorField = field
            }
            when {
                name.isBlank() -> fail("请输入周期记账名称", "name")
                cents <= 0L -> fail("请输入大于 ¥0.00 的金额", "amount")
                accounts.none { it.id == accountId } -> fail("账本正在准备 请稍后重试", "account")
                kindCategories.none { it.id == categoryId } -> fail("请选择分类", "category")
                else -> {
                    val result = onSave(
                        existing?.id,
                        MoneyFormatter.centsToInput(cents),
                        categoryId,
                        accountId,
                        name,
                        notes,
                        cadence,
                        anchor.toString()
                    )
                    if (result != null) fail(result, "form")
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
    )
    if (existing != null && onDelete != null) {
        TextLinkButton("删除周期记账", onClick = onDelete, modifier = Modifier.fillMaxWidth())
    }
}
