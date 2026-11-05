package com.spendwise.app.domain

import org.junit.Assert.*
import org.junit.Test

class RenminbiTest {
    @Test fun acceptsChineseCurrencyPrefixesAndRoundsToFen() {
        assertEquals(123456L, MoneyFormatter.parseToCents("人民币 1,234.56"))
        assertEquals(1235L, MoneyFormatter.parseToCents("￥12.345"))
        assertEquals(990L, MoneyFormatter.parseToCents("CNY9.90"))
    }
    @Test fun rejectsOverflowNegativeAndZeroTransactions() {
        assertNull(MoneyFormatter.parseToCents("92233720368547758.08"))
        assertNull(MoneyFormatter.parseToCents("-0.01"))
        assertNull(MoneyFormatter.parseToCents("0"))
        assertEquals(0L, MoneyFormatter.parseToCents("0", allowZero = true))
    }
    @Test fun preservesFenForLargeBalances() {
        assertEquals("¥ 92,233,720,368,547,758.07", MoneyFormatter.formatCents(Long.MAX_VALUE))
    }
}
