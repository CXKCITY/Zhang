package com.spendwise.app.ui.botanical
import com.spendwise.app.domain.MonthlyAggregate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test
class ExpenseChartDataTest {
 @Test fun annualAlwaysContainsTwelveMonths() {
  val bars = ExpenseChartData.year(2026, listOf(MonthlyAggregate(YearMonth.of(2026,11),4000,9000),MonthlyAggregate(YearMonth.of(2025,11),888,0)))
  assertEquals(12,bars.size); assertEquals(4000L,bars[10].cents); assertEquals(0L,bars[0].cents)
  assertEquals("2026年12月",bars.last().label)
 }
 @Test fun monthUsesActualNumberOfDaysIncludingLeapYears() {
  assertEquals(29,ExpenseChartData.month(YearMonth.of(2024,2), emptyMap()).size)
  val bars=ExpenseChartData.month(YearMonth.of(2026,2),mapOf(2 to 1234L,29 to 999L))
  assertEquals(28,bars.size); assertEquals(1234L,bars[1].cents); assertEquals(0L,bars[0].cents)
 }
 @Test fun pieSelectionHandlesBoundariesAndEmptyData() {
  assertEquals(0, ExpenseChartData.sliceIndex(0.0,listOf(25L,75L)))
  assertEquals(1, ExpenseChartData.sliceIndex(0.25,listOf(25L,75L)))
  assertEquals(1, ExpenseChartData.sliceIndex(1.0,listOf(25L,75L)))
  assertNull(ExpenseChartData.sliceIndex(0.0, listOf(0L,0L)))
 }

 @Test fun categoryColorsStayUniqueBeyondTheBasePalette() {
  val colors = categoryChartColors(48)
  assertEquals(48, colors.toSet().size)
  assertEquals(colors.take(16), categoryChartColors(16))
 }
}
