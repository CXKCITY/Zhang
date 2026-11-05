package com.spendwise.app.ui.botanical

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.MoneyFormatter

object SpendingPreferences {
    var dailyLimitCents by mutableStateOf(0L)
        private set
    var redMeansLoss by mutableStateOf(true)
        private set
    var prompted by mutableStateOf(false)
        private set
    private var context: Context? = null
    fun load(c: Context) {
        context = c.applicationContext
        val p = c.getSharedPreferences("spending_preferences", 0)
        dailyLimitCents = p.getLong("daily_limit", 0L).coerceAtLeast(0L)
        redMeansLoss = p.getBoolean("red_means_loss", true)
        prompted = p.getBoolean("prompted", false)
    }
    fun markPrompted() { prompted = true; context?.getSharedPreferences("spending_preferences", 0)?.edit()?.putBoolean("prompted", true)?.apply() }
    fun save(limit: Long, redLoss: Boolean) {
        require(limit > 0)
        dailyLimitCents = limit; redMeansLoss = redLoss; prompted = true
        context?.getSharedPreferences("spending_preferences", 0)?.edit()?.putLong("daily_limit", limit)?.putBoolean("red_means_loss", redLoss)?.putBoolean("prompted", true)?.apply()
    }
    val lossColor get() = if (redMeansLoss) { if (LedgerAppearance.dark) Color(0xFFFCA5A5) else Color(0xFF991B1B) } else { if (LedgerAppearance.dark) Color(0xFF86EFAC) else Color(0xFF166534) }
    val gainColor get() = if (redMeansLoss) { if (LedgerAppearance.dark) Color(0xFF86EFAC) else Color(0xFF166534) } else { if (LedgerAppearance.dark) Color(0xFFFCA5A5) else Color(0xFF991B1B) }
}

object SpendingHeat {
    fun ratio(cents: Long, limit: Long): Float = if (limit <= 0L) 0f else (cents.toDouble() / limit).coerceIn(0.0, 1.0).toFloat()
    fun color(cents: Long, limit: Long, redLoss: Boolean): Color {
        val ratio = ratio(cents, limit)
        val low = Color(0xFFFEF3C7)
        val middle = if (redLoss) Color(0xFFF59E0B) else Color(0xFF84CC16)
        val high = if (redLoss) Color(0xFF991B1B) else Color(0xFF14532D)
        return if (ratio <= 0.5f) lerp(low, middle, ratio * 2f) else lerp(middle, high, (ratio - 0.5f) * 2f)
    }
    fun ink(background: Color): Color = if (background.luminance() < 0.179f) Color.White else Color.Black
    fun wholeYuan(cents: Long): String = (cents.coerceAtLeast(0L) / 100L).toString()
}

@Composable
fun SpendingSettingsControl(autoAsk: Boolean = false) {
    var open by remember { mutableStateOf(autoAsk && !SpendingPreferences.prompted && SpendingPreferences.dailyLimitCents == 0L) }
    TextButton(onClick = { open = true }) {
        Text(if (SpendingPreferences.dailyLimitCents > 0) "每日上限 ${formatRm(SpendingPreferences.dailyLimitCents)} · 设置" else "设置每日消费上限")
    }
    if (open) {
        var amount by remember { mutableStateOf(if (SpendingPreferences.dailyLimitCents > 0) MoneyFormatter.centsToInput(SpendingPreferences.dailyLimitCents) else "") }
        var redLoss by remember { mutableStateOf(SpendingPreferences.redMeansLoss) }
        val parsed = MoneyFormatter.parseToCents(amount)
        LaunchedEffect(Unit) { SpendingPreferences.markPrompted() }
        AlertDialog(
            containerColor = Bot.Surface, titleContentColor = Bot.Ink, textContentColor = Bot.Ink,
            onDismissRequest = { open = false },
            title = { Text("每天最多接受消费多少") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("每日消费上限 · 元") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotEmpty() && parsed == null)
                Text("日历按当天消费占上限的比例着色 达到或超过上限时颜色最深")
                Text("红绿含义")
                Row {
                    RadioButton(selected = redLoss, onClick = { redLoss = true })
                    TextButton(onClick = { redLoss = true }) { Text("红色表示支出或亏损") }
                }
                Row {
                    RadioButton(selected = !redLoss, onClick = { redLoss = false })
                    TextButton(onClick = { redLoss = false }) { Text("绿色表示支出或亏损") }
                }
                Text(if (redLoss) "消费日历：浅黄 → 深红" else "消费日历：浅黄 → 深绿")
            } },
            confirmButton = { TextButton(enabled = parsed != null, onClick = { parsed?.let { SpendingPreferences.save(it, redLoss); open = false } }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("稍后设置") } }
        )
    }
}
