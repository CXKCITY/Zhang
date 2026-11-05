package com.spendwise.app.domain
import org.junit.Assert.*
import org.junit.Test
class BulkExpenseRulesTest {
 @Test fun allowsOnlyTheSelectedExpenseRows() { BulkExpenseRules.validate(setOf(1,2),mapOf(1L to 10L,2L to 11L),setOf(99),setOf(10,11),11) }
 @Test fun rejectsMissingRowsBeforeAnyWrite() { assertThrows(IllegalArgumentException::class.java) { BulkExpenseRules.validate(setOf(1,2),mapOf(1L to 10L),emptySet(),setOf(10),null) } }
 @Test fun rejectsIncomeAndInvalidDestination() {
  assertThrows(IllegalArgumentException::class.java) { BulkExpenseRules.validate(setOf(1),mapOf(1L to 99L),setOf(99),setOf(10),null) }
  assertThrows(IllegalArgumentException::class.java) { BulkExpenseRules.validate(setOf(1),mapOf(1L to 10L),setOf(99),setOf(10),99) }
 }
 @Test fun rejectsUnknownSourceCategory() {
  assertThrows(IllegalArgumentException::class.java) { BulkExpenseRules.validate(setOf(1),mapOf(1L to 77L),emptySet(),setOf(10),null) }
 }
 @Test fun rejectsEmptyOrUnselectedRows() {
  assertThrows(IllegalArgumentException::class.java) { BulkExpenseRules.validate(emptySet(),emptyMap(),emptySet(),setOf(10),null) }
  assertThrows(IllegalArgumentException::class.java) { BulkExpenseRules.validate(setOf(1),mapOf(1L to 10L,2L to 10L),emptySet(),setOf(10),null) }
 }
}
