package com.spendwise.app.ui.botanical

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

data class LedgerPalette(val background: Color, val surface: Color, val text: Color, val muted: Color,
    val accent: Color, val field: Color, val rule: Color, val heroStart: Color, val heroEnd: Color)

object LedgerAppearance {
    var choice by mutableStateOf("亮色")
    val options = listOf("亮色", "暗色", "拿铁", "摩卡")
    val dark get() = choice == "暗色" || choice == "摩卡"
    private var context: Context? = null
    fun load(c: Context) { context = c.applicationContext; choice = c.getSharedPreferences("appearance", 0).getString("palette", "亮色")?.takeIf { it in options } ?: "亮色" }
    fun select(value: String) { require(value in options); choice = value; context?.getSharedPreferences("appearance", 0)?.edit()?.putString("palette", value)?.apply() }
    fun palette(name: String): LedgerPalette = when(name) {
        "暗色" -> LedgerPalette(Color(0xFF0B1220), Color(0xFF172235), Color(0xFFE5EDF8), Color(0xFFAABAD0), Color(0xFF2563EB), Color(0xFF22324A), Color(0xFF34445E), Color(0xFF0F172A), Color(0xFF1E3A8A))
        "拿铁" -> LedgerPalette(Color(0xFFF5EBDD), Color(0xFFFFFAF2), Color(0xFF432E22), Color(0xFF765C48), Color(0xFF8B5E34), Color(0xFFEDE0CD), Color(0xFFDBCAAF), Color(0xFF6F4E37), Color(0xFF9A6C47))
        "摩卡" -> LedgerPalette(Color(0xFF211827), Color(0xFF33243A), Color(0xFFF4EAF7), Color(0xFFC8B5CF), Color(0xFF7C3AED), Color(0xFF45314F), Color(0xFF634D6D), Color(0xFF46294F), Color(0xFF6A4172))
        else -> LedgerPalette(Color(0xFFF5F7FB), Color.White, Color(0xFF111827), Color(0xFF586579), Color(0xFF2563EB), Color(0xFFF1F4F9), Color(0xFFE1E7F0), Color(0xFF1D4ED8), Color(0xFF2563EB))
    }
    val current get() = palette(choice)
    fun token(role: String, fallback: Color): Color {
        val p = current
        return when(role) {
            "Paper", "PageDeep" -> p.background
            "Surface", "NavBg" -> p.surface
            "Ink", "Navy", "PageInk", "SurfaceInk", "FieldInk", "SecondaryInk" -> p.text
            "Muted", "PageMuted", "SurfaceMuted", "Caption", "FieldLabel", "SheetMuted", "ChartLabel", "NavInk" -> p.muted
            "Blue", "ActionSolid", "Pink", "NavActive", "ChipInk" -> p.accent
            "Rule", "FieldBorder", "NavBorder" -> p.rule
            "DangerBg" -> if (dark) Color(0xFF4A2429) else Color(0xFFFEF2F2)
            "DangerInk" -> if (dark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
            "Orchid" -> p.accent
            "Track", "TileBg", "ChipBg", "SecondaryBg", "FieldBg" -> p.field
            else -> color(fallback)
        }
    }
    fun color(c: Color): Color {
        if (c.alpha < 0.9f) return c
        if (c == Color(0xFF2563EB)) return current.accent
        val l = c.luminance()
        return when { l > 0.82f -> if(c == Color.White) current.surface else current.field
            l < 0.16f -> current.text
            l < 0.4f -> current.muted
            else -> c }
    }
}

@Composable
fun PalettePicker() {
    var open by remember { mutableStateOf(false) }
    BotButton("配色 · ${LedgerAppearance.choice}", onClick = { open = true }, type = ButtonType.Secondary)
    if (open) androidx.compose.material3.AlertDialog(
        containerColor = Bot.Surface, titleContentColor = Bot.Ink, textContentColor = Bot.Ink,
        onDismissRequest = { open = false },
        title = { androidx.compose.material3.Text("选择配色") },
        text = { Column {
            LedgerAppearance.options.forEach { name ->
                val p = LedgerAppearance.palette(name)
                androidx.compose.material3.TextButton(onClick = { LedgerAppearance.select(name); open = false }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        listOf(p.background, p.surface, p.accent).forEach { color -> Box(Modifier.size(18.dp).clip(CircleShape).background(color).border(1.dp, p.rule, CircleShape)) }
                        androidx.compose.material3.Text(if (name == LedgerAppearance.choice) "$name · 当前" else name)
                    }
                }
            }
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { open = false }) { androidx.compose.material3.Text("完成") } }
    )
}
