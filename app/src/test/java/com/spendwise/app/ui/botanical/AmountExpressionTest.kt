package com.spendwise.app.ui.botanical
import org.junit.Assert.*
import org.junit.Test
class AmountExpressionTest {
 @Test fun directYuanInput() { assertEquals(3000L, AmountExpression.evaluate("30")); assertEquals(3050L, AmountExpression.evaluate("30.50")) }
 @Test fun addsAndSubtractsExactly() { assertEquals(3030L, AmountExpression.evaluate("30+0.50−0.20")); assertEquals(30L, AmountExpression.evaluate("0.1+0.2")); assertEquals(100L, AmountExpression.evaluate("3-2")) }
 @Test fun rejectsIncompleteNegativeOverflowAndMultiplication() { listOf("30+", "1-2", "999999999.99+0.01", "2*3", "2/3", "1.234", "").forEach { assertNull(it, AmountExpression.evaluate(it)) } }
}
