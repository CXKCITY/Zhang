package com.spendwise.app.ui.botanical
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
class SpendingHeatTest {
 @Test fun usesUserLimitAndSaturatesAboveIt() {
  assertEquals(0.5f, SpendingHeat.ratio(5000, 10000), 0.0001f)
  assertEquals(1f, SpendingHeat.ratio(15000, 10000), 0f)
  assertEquals(0f, SpendingHeat.ratio(100, 0), 0f)
 }
 @Test fun truncatesDisplayWithoutChangingRecordedFen() {
  assertEquals("30", SpendingHeat.wholeYuan(3099)); assertEquals("0", SpendingHeat.wholeYuan(99))
 }
 @Test fun supportsBothColorConventions() {
  assertEquals(Color(0xFFFEF3C7), SpendingHeat.color(0,100,true))
  assertEquals(Color(0xFF991B1B), SpendingHeat.color(100,100,true))
  assertEquals(Color(0xFF14532D), SpendingHeat.color(100,100,false))
  assertEquals(SpendingHeat.color(100,100,true), SpendingHeat.color(200,100,true))
 }
 @Test fun textContrastPassesAcrossBothRamps() {
  listOf(true,false).forEach { red -> (0L..100L).forEach { value ->
   val bg = SpendingHeat.color(value,100,red).luminance()
   val fg = SpendingHeat.ink(SpendingHeat.color(value,100,red)).luminance()
   assertTrue((maxOf(bg,fg)+0.05f)/(minOf(bg,fg)+0.05f) >= 4.5f)
  } }
 }
}
