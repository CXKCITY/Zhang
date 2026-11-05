package com.spendwise.app.ui.botanical

import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.NoteSuggestion

@Composable
fun NoteSuggestionStrip(categoryName: String, suggestions: List<NoteSuggestion>, value: String, onSelect: (String) -> Unit) {
    AnimatedVisibility(visible = suggestions.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text("${categoryName}常用 · 按使用次数排序", style = body(11f), color = Bot.SurfaceMuted,
                modifier = Modifier.padding(bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                suggestions.forEach { suggestion ->
                    val selected = value.trim() == suggestion.text
                    Box(Modifier.heightIn(min = 44.dp).widthIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp)).background(if (selected) Bot.ChipBg else Bot.FieldBg)
                        .botPress { onSelect(suggestion.text) }
                        .semantics { contentDescription = "填入 ${suggestion.text} 已使用 ${suggestion.useCount} 次" }
                        .padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("${suggestion.text} · ${suggestion.useCount}次", style = body(13f),
                            color = if (selected) Bot.ActionSolid else Bot.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
