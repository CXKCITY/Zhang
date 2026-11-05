package com.spendwise.app.ui.botanical
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
class PaletteContrastTest {
 @Test fun summaryTextIsReadableInEveryPalette() {
  try { LedgerAppearance.options.forEach { name ->
    LedgerAppearance.select(name)
    val a = Bot.Ink.luminance(); val b = Bot.Surface.luminance()
    val contrast = (maxOf(a,b)+0.05f)/(minOf(a,b)+0.05f)
    assertTrue("$name contrast $contrast", contrast >= 4.5f)
  } } finally { LedgerAppearance.select("亮色") }
 }
 @Test fun palettesHaveDifferentSurfaceAndHeroColors() {
   val palettes = LedgerAppearance.options.map { LedgerAppearance.palette(it) }
   assertEquals(4, palettes.map { it.background }.distinct().size)
   assertEquals(4, palettes.map { it.surface }.distinct().size)
   assertEquals(4, palettes.map { it.heroStart }.distinct().size)
 }
 @Test fun controlsAndSecondaryTextHaveReadableContrast() {
   LedgerAppearance.options.forEach { name ->
     val p = LedgerAppearance.palette(name)
     fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Float {
       val x=a.luminance(); val y=b.luminance(); return (maxOf(x,y)+0.05f)/(minOf(x,y)+0.05f)
     }
     assertTrue(name, contrast(p.text,p.field) >= 4.5f)
     assertTrue(name, contrast(p.muted,p.surface) >= 4.5f)
     assertTrue(name, contrast(androidx.compose.ui.graphics.Color.White,p.accent) >= 4.5f)
   }
 }
}
