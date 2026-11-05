package com.spendwise.app.ui.botanical

import java.math.BigDecimal

object AmountExpression {
    fun evaluate(raw: String): Long? = runCatching {
        val text = raw.replace(" ", "")
        require(Regex("[0-9]+(?:\\.[0-9]{0,2})?(?:[+−-][0-9]+(?:\\.[0-9]{0,2})?)*").matches(text))
        val tokens = Regex("[+−-]?[0-9]+(?:\\.[0-9]{0,2})?").findAll(text)
        val result = tokens.fold(BigDecimal.ZERO) { sum, token -> sum + token.value.replace('−', '-').toBigDecimal() }
        require(result >= BigDecimal.ZERO && result <= BigDecimal("999999999.99"))
        result.movePointRight(2).longValueExact()
    }.getOrNull()
}
