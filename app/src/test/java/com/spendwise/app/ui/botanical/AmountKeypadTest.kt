package com.spendwise.app.ui.botanical
import org.junit.Assert.*
import org.junit.Test
class AmountKeypadTest {
 @Test fun matchesReferenceGrid() {
   assertEquals(listOf(listOf("7","8","9","日期"), listOf("4","5","6","+"), listOf("1","2","3","−"),listOf(".","0","退格","完成")), AmountKeypad.rows(true))
   assertEquals("清空", AmountKeypad.rows(false)[0][3])
 }
 @Test fun thirtyPlusFiveAppendsAtEnd() { var text = ""; listOf("3","0","+","5").forEach { text = AmountKeypad.press(text,it) }; assertEquals("30+5",text); assertEquals("35.00",AmountKeypad.press(text,"完成")) }
 @Test fun decimalBackspaceAndClear() { var text = ""; listOf(".","5","0","9").forEach { text = AmountKeypad.press(text,it) }; assertEquals("0.50",text); assertEquals("0.5",AmountKeypad.press(text,"退格")); assertEquals("",AmountKeypad.press(text,"清空")) }
 @Test fun replacesOperatorAndDoesNotCompleteInvalidInput() { assertEquals("30−",AmountKeypad.press("30+","−")); assertEquals("30+",AmountKeypad.press("30+","完成")); assertEquals("",AmountKeypad.press("","+")); assertEquals("30.0+",AmountKeypad.press("30.","+")) }
}
