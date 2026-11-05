package com.spendwise.app.ui.botanical

object AmountKeypad {
    fun rows(hasDate: Boolean) = listOf(
        listOf("7", "8", "9", if (hasDate) "日期" else "清空"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "−"),
        listOf(".", "0", "退格", "完成")
    )
    fun press(text: String, key: String): String {
        if (key == "清空") return ""
        if (key == "退格") return text.dropLast(1)
        if (key == "完成") return AmountExpression.evaluate(text)?.let { com.spendwise.app.domain.MoneyFormatter.centsToInput(it) } ?: text
        if (key == "+" || key == "−") {
            if (text.isEmpty()) return text
            val base = if (text.last() == '+' || text.last() == '−') text.dropLast(1) else text
            return (if (base.endsWith('.')) base + "0" else base) + key
        }
        val part = text.substringAfterLast('+').substringAfterLast('−')
        if (key == ".") return if ('.' in part) text else text + if (part.isEmpty()) "0." else "."
        if (key.length != 1 || key[0] !in '0'..'9' || text.length >= 64) return text
        if ('.' in part && part.substringAfter('.').length >= 2) return text
        return if (part == "0") text.dropLast(1) + key else text + key
    }
}
