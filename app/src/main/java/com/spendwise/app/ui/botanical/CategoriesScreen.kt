package com.spendwise.app.ui.botanical

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.MoneyFormatter
import java.time.YearMonth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import com.spendwise.app.domain.CategoryOrderRules
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState



private enum class CategoryKind(val label: String) { Expense("支出"), Income("收入") }

@Composable
fun CategoriesScreen(
    categories: List<Category>, budgets: List<Budget>, month: YearMonth,
    spentByCategory: Map<Long, Long>, bottomPadding: Dp, onBack: () -> Unit,
    onAdd: (income: Boolean) -> Unit, onEdit: (Category) -> Unit
) {
    var draft by remember { mutableStateOf(CategoryOrder.sort(categories).filter { !it.isIncomeAdjustment }) }
    var dragging by remember { mutableStateOf(false) }
    val limits = remember(budgets) { budgets.associate { it.categoryId to it.monthlyLimitCents } }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(categories, dragging) {
        if (!dragging) draft = CategoryOrder.sort(categories).filter { !it.isIncomeAdjustment }
    }
    fun move(from: Int, to: Int, save: Boolean = false) {
        val byId = draft.associateBy { it.id }
        val ids = CategoryOrderRules.moveTo(draft.map { it.id }, from, to)
        draft = ids.mapNotNull { byId[it] }
        if (save) CategoryOrder.save(ids)
    }
    val reorderState = rememberReorderableLazyListState(
        listState, scrollThresholdPadding = PaddingValues(top = pageTopPadding(), bottom = bottomPadding)
    ) { from, to ->
        val source = draft.indexOfFirst { it.id == from.key }
        val destination = draft.indexOfFirst { it.id == to.key }
        if (source >= 0 && destination >= 0) {
            move(source, destination)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    DarkPage {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item(key = "top") {
                TopBar("分类", "返回设置", onBack) {
                    CircleIconButton(BotIcons.Plus, "添加分类", onClick = { onAdd(false) }, size = 46.dp)
                }
            }
            item(key = "intro") {
                Subtitle("拖动右侧手柄排序 松手自动保存", Modifier.padding(bottom = 16.dp))
            }
            items(draft, key = { it.id }) { category ->
                ReorderableItem(reorderState, key = category.id) { isDragging ->
                    val lift by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "categoryLift")
                    val spent = spentByCategory[category.id] ?: 0L
                    val limit = limits[category.id] ?: 0L
                    val index = draft.indexOfFirst { it.id == category.id }
                    val shape = RoundedCornerShape(18.dp)
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            .zIndex(if (isDragging) 1f else 0f)
                            .shadow(lift, shape).clip(shape).background(Bot.Surface)
                            .semantics {
                                contentDescription = "${category.name} 本月支出 ${formatRm(spent)}"
                                customActions = listOfNotNull(
                                    if (index > 0) CustomAccessibilityAction("上移") { move(index,index-1,true); true } else null,
                                    if (index < draft.lastIndex) CustomAccessibilityAction("下移") { move(index,index+1,true); true } else null
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f).botPress(onClick = { onEdit(category) }).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ArtTile(category.art.drawable, size = 44.dp, imageSize = 38.dp)
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = body(18f, FontWeight.SemiBold), color = Bot.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (limit > 0L) "预算 ${formatRm(limit)}" else "未设置月预算",
                                    style = body(11f), color = if (limit > 0L && spent > limit) Bot.Negative else Bot.SurfaceMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top=5.dp))
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 110.dp)) {
                                Money(spent, style = moneyStyle(19f), color = Bot.Ink, abbreviate = true, align = Alignment.End)
                                Text("本月支出", style = body(10f), color = Bot.SurfaceMuted, modifier = Modifier.padding(top=4.dp))
                            }
                        }
                        IconButton(onClick = {}, modifier = Modifier.draggableHandle(
                            onDragStarted = { dragging = true; haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragStopped = { CategoryOrder.save(draft.map { it.id }); dragging = false }
                        )) {
                            BotIcon(Icons.Outlined.DragHandle, size = 24.dp, tint = Bot.SurfaceMuted,
                                modifier = Modifier.semantics { contentDescription = "拖动排序 ${category.name}" })
                        }
                    }
                }
            }
            item(key = "note") {
                Note("${monthLabel(month)} · 点击分类可编辑名称 图标与预算", Modifier.padding(top = 12.dp))
            }
        }
    }
}

/**
 * New / edit category. The kind can change only while nothing is filed under
 * the category (moving entries between income and spending by accident would
 * rewrite every total). Built-ins can't be deleted.
 */
@Composable
fun CategoryFormSheet(
    visible: Boolean,
    existing: Category?,
    initialIncome: Boolean,
    budgetCents: Long,
    used: Boolean,
    onSave: (name: String, iconName: String, color: Long, isIncome: Boolean, budget: String) -> String?,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(if (visible) FormKey(existing, initialIncome) else null)
    BotSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = if (shown?.category != null) "编辑分类" else "新建分类"
    ) {
        if (shown != null) {
            CategoryFormBody(
                existing = shown.category,
                initialIncome = shown.income,
                budgetCents = budgetCents,
                used = used,
                onSave = onSave,
                onDelete = onDelete
            )
        }
    }
}

private data class FormKey(val category: Category?, val income: Boolean)

@Composable
private fun CategoryFormBody(
    existing: Category?,
    initialIncome: Boolean,
    budgetCents: Long,
    used: Boolean,
    onSave: (name: String, iconName: String, color: Long, isIncome: Boolean, budget: String) -> String?,
    onDelete: (() -> Unit)?
) {
    val builtIn = existing != null && !existing.isCustom
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var income by remember(existing, initialIncome) { mutableStateOf(existing?.isIncomeAdjustment ?: initialIncome) }
    var art by remember(existing, initialIncome) {
        mutableStateOf(existing?.art ?: if (initialIncome) CategoryArt.Salary else CategoryArt.Food)
    }
    var budget by remember(existing, budgetCents) { mutableStateOf(budgetCents) }
    var error by remember(existing) { mutableStateOf<String?>(null) }

    if (existing == null || (!builtIn && !used)) {
        PillSegment(
            options = listOf(CategoryKind.Expense),
            selected = if (income) CategoryKind.Income else CategoryKind.Expense,
            onSelect = { income = it == CategoryKind.Income },
            label = { it.label },
            tone = SegmentTone.OnLight,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
    FieldLabel("分类名称")
    BotTextField(
        value = name,
        onValueChange = {
            name = it
            error = null
        },
        placeholder = "例如 咖啡",
        maxLength = 24,
        isError = error != null,
        contentLabel = "分类名称"
    )
    FormCaption("图标", Modifier.padding(top = 20.dp, bottom = 8.dp))
    ArtGrid(selected = art, onSelect = { art = it })
    // Income categories have no budget; the field folds away.
    Reveal(visible = !income) {
        Column {
            FormCaption("月度预算 · 选填", Modifier.padding(top = 20.dp))
            AmountInput(cents = budget, onChange = { budget = it }, label = "月度预算")
        }
    }
    AnimatedError(error, Modifier.padding(vertical = 16.dp))
    BotButton(
        "保存分类",
        onClick = {
            error = onSave(
                name,
                art.key,
                art.storedColor,
                income,
                if (!income && budget > 0) MoneyFormatter.centsToInput(budget) else ""
            )
        },
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
    )
    if (builtIn) {
        Text(
            "内置分类支持修改名称 图标和预算 无法删除",
            style = body(12f, lineHeight = 1.6f),
            color = Bot.SheetMuted,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    if (existing != null && existing.isCustom && onDelete != null) {
        TextLinkButton("删除分类", onClick = onDelete, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        if (used) {
            FormCaption("删除分类前 请先选择迁移或删除关联记录和周期规则")
        }
    }
}

/** The sixteen illustrations, six to a row (four on narrow phones). */
@Composable
private fun ArtGrid(selected: CategoryArt, onSelect: (CategoryArt) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 300.dp) 4 else 6
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryArt.entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { art ->
                        val isSelected = art == selected
                        val ring = animatedColor(if (isSelected) Bot.ActionSolid else Bot.ActionSolid.copy(alpha = 0f), "artRing")
                        val tile = animatedColor(if (isSelected) Color(0xFFE6EFFA) else Color(0xFFF0F2F6), "artTile")
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 64.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .border(2.dp, ring, RoundedCornerShape(18.dp))
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tile)
                                    .botPress { onSelect(art) }
                                    .semantics {
                                        contentDescription = "${art.label} icon"
                                        this.selected = isSelected
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painterResource(art.drawable),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(0.92f)
                                )
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private enum class MigrationMode(val label: String) { Move("迁移记录"), Delete("删除记录") }

/**
 * Deleting a category that still has entries or recurring rules: move them
 * to another category of the same kind, or delete them too. Deleting needs
 * an explicit tick; nothing happens by default.
 */
@Composable
fun CategoryMigrationSheet(
    category: Category?,
    entryCount: Int,
    ruleCount: Int,
    destinations: List<Category>,
    hasBudget: Boolean,
    onCancel: () -> Unit,
    onMove: (Long) -> Unit,
    onDeleteAll: () -> Unit
) {
    val shown = rememberRetained(category)
    BotSheet(visible = category != null, onDismiss = onCancel, title = "删除 ${shown?.name.orEmpty()}") {
        if (shown != null) {
            var mode by remember(shown) { mutableStateOf(MigrationMode.Move) }
            var destination by remember(shown) { mutableStateOf<Category?>(null) }
            var confirmed by remember(shown) { mutableStateOf(false) }
            val entries = "$entryCount 笔记录"
            val rules = "$ruleCount 条周期规则"
            SheetText("${shown.name} 共关联 $entries 和 $rules")
            PillSegment(
                options = MigrationMode.entries,
                selected = mode,
                onSelect = {
                    mode = it
                    confirmed = false
                },
                label = { it.label },
                tone = SegmentTone.OnLight
            )
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    fadeIn(BotMotion.smooth(0.3f))
                        .togetherWith(fadeOut(BotMotion.smooth(0.2f)))
                        .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                },
                label = "migrationMode"
            ) { shownMode ->
                Column {
                    if (shownMode == MigrationMode.Move) {
                        Support(
                            "将记录移到其他分类 保留全部历史 金额和账户余额不变",
                            Modifier.padding(top = 16.dp)
                        )
                        if (destinations.isNotEmpty()) {
                            FieldLabel("迁移到分类", modifier = Modifier.padding(top = 16.dp))
                            SelectField(
                                options = destinations,
                                selected = destination,
                                onSelect = { destination = it },
                                label = { it.name },
                                placeholder = "选择分类",
                                contentLabel = "迁移到分类"
                            )
                        } else {
                            Support(
                                "请先创建其他${if (shown.isIncomeAdjustment) "收入" else "支出"}分类 再返回这里",
                                Modifier.padding(top = 12.dp)
                            )
                        }
                        if (hasBudget) {
                            Support(
                                "当前分类的预算会被移除 目标分类预算保持不变",
                                Modifier.padding(top = 12.dp)
                            )
                        }
                        ButtonRow {
                            BotButton("取消", onClick = onCancel, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                            BotButton(
                                "迁移记录并删除分类",
                                onClick = { destination?.let { onMove(it.id) } },
                                enabled = destination != null,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    } else {
                        Support(
                            "将删除此分类 预算 $entries 和 $rules 账户余额与统计会重新计算 关联规则不再自动记账",
                            Modifier.padding(top = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                                .heightIn(min = 44.dp)
                                .clickable(role = Role.Checkbox) { confirmed = !confirmed }
                                .semantics { stateDescription = if (confirmed) "已勾选" else "未勾选" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(animatedColor(if (confirmed) Bot.DangerInk else Color.White, "tickFill"))
                                    .border(1.5.dp, animatedColor(if (confirmed) Bot.DangerInk else Color(0xFF8D94A4), "tickEdge"), RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = confirmed,
                                    enter = fadeIn(BotMotion.smooth(0.18f)) + scaleIn(BotMotion.bouncy(0.3f), initialScale = 0.4f),
                                    exit = fadeOut(BotMotion.smooth(0.14f)) + scaleOut(BotMotion.smooth(0.2f), targetScale = 0.6f)
                                ) {
                                    BotIcon(BotIcons.Check, size = 14.dp, tint = Bot.Ink)
                                }
                            }
                            Text(
                                "删除 ${shown.name} 中全部记录和规则",
                                style = body(14f, FontWeight.Medium, lineHeight = 1.5f),
                                color = Bot.Ink
                            )
                        }
                        Support("此操作无法撤销 如需保留记录 请先备份")
                        ButtonRow {
                            BotButton("取消", onClick = onCancel, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                            BotButton(
                                "删除分类与记录",
                                onClick = onDeleteAll,
                                type = ButtonType.Danger,
                                enabled = confirmed,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}
