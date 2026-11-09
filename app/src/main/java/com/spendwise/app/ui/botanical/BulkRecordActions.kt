package com.spendwise.app.ui.botanical

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import kotlinx.coroutines.launch

@Composable
fun BulkRecordActions(
    selected: Set<Long>, records: List<Expense>, categories: List<Category>,
    onSelectionChange: (Set<Long>) -> Unit,
    onManage: suspend (Set<Long>, Long?) -> String?,
    modifier: Modifier = Modifier
) {
    var action by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(emptySet<Long>()) }
    var pendingAmount by remember { mutableStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    fun request(kind: String) {
        pending = selected.toSet()
        pendingAmount = records.filter { it.id in pending }.sumOf { it.amountCents }
        error = null; target = null; action = kind
    }
    androidx.compose.animation.AnimatedVisibility(visible = selected.isNotEmpty(), modifier = modifier) {
        Panel {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("已选 ${selected.size} 笔", color = Bot.Ink, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onSelectionChange(records.map { it.id }.toSet()) }) { Text("全选当前结果") }
                    TextButton(onClick = { onSelectionChange(emptySet()) }) { Text("取消") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotButton("修改分类", onClick = { request("category") }, type = ButtonType.Secondary, dmSans = true, modifier = Modifier.weight(1f))
                    BotButton("删除", onClick = { request("delete") }, type = ButtonType.Danger, dmSans = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    if (action != null) {
        val deleting = action == "delete"
        AlertDialog(onDismissRequest = { if (!busy) action = null },
            containerColor = Bot.Surface, titleContentColor = Bot.Ink, textContentColor = Bot.Ink,
            title = { Text(if (deleting) "删除 ${pending.size} 笔记录" else "修改 ${pending.size} 笔记录的分类") },
            text = { Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                Text("已选 ${pending.size} 笔 · 合计 ${formatRm(pendingAmount)}")
                if (deleting) {
                    Text("删除后可在底部提示中撤销 支出统计和预算会同步更新", modifier = Modifier.padding(top = 12.dp))
                } else {
                    Text("只修改分类 金额 日期和消费内容保持不变", modifier = Modifier.padding(top=12.dp))
                    categories.filter { !it.isIncomeAdjustment }.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = target == category.id, enabled = !busy, onClick = { target = category.id })
                            TextButton(enabled = !busy, onClick = { target = category.id }) { Text(category.name) }
                        }
                    }
                }
                error?.let { Text(it, color = Bot.DangerInk, modifier = Modifier.padding(top=12.dp)) }
            } },
            confirmButton = { BotButton(if (busy) "处理中" else if (deleting) "确认删除" else "确认修改",
                enabled = !busy && pending.isNotEmpty() && (deleting || target != null),
                type = if (deleting) ButtonType.Danger else ButtonType.Primary,
                dmSans = true, onClick = {
                    val ids = pending.toSet(); val destination = if (deleting) null else target
                    busy = true
                    scope.launch {
                        try {
                            val failure = onManage(ids, destination)
                            if (failure == null) { onSelectionChange(emptySet()); action = null }
                            else error = failure
                        } finally { busy = false }
                    }
                }) },
            dismissButton = { TextButton(enabled = !busy, onClick = { action = null }) { Text("取消") } }
        )
    }
}
