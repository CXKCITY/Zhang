package com.spendwise.app.ui.botanical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Credit(val name: String, val detail: String, val source: String, val file: String)

private val credits = listOf(
    Credit("SpendWise", "原版账本 · wansiong50-pixel · MIT",
        "https://github.com/wansiong50-pixel/spendwise", "SpendWise-MIT.txt"),
    Credit("AndroidX / Jetpack Compose", "界面、Material 图标、Room、导航与后台任务 · Apache 2.0",
        "https://github.com/androidx/androidx", "Apache-2.0.txt"),
    Credit("Reorderable 2.4.3", "分类拖动排序 · Calvin-LL · Apache 2.0",
        "https://github.com/Calvin-LL/Reorderable/tree/v2.4.3", "Reorderable-2.4.3-LICENSE.txt"),
    Credit("Kotlin / Kotlinx", "标准库、协程与 JSON 序列化 · Apache 2.0",
        "https://github.com/Kotlin", "Apache-2.0.txt"),
    Credit("Barlow Condensed", "字体 · The Barlow Project Authors · OFL 1.1",
        "https://github.com/jpt/barlow", "barlowcondensed-OFL.txt"),
    Credit("DM Sans", "字体 · The DM Sans Project Authors · OFL 1.1",
        "https://github.com/googlefonts/dm-fonts", "dmsans-OFL.txt"),
    Credit("运行依赖与声明", "随应用分发的依赖版本和上游声明",
        "", "Runtime-notices.txt")
)

@Composable
internal fun OpenSourceCredits(visible: Boolean, onDismiss: () -> Unit) {
    var selected by remember(visible) { mutableStateOf<Credit?>(null) }
    val credit = selected
    val assets = LocalContext.current.assets
    val links = LocalUriHandler.current
    BotSheet(
        visible = visible,
        onDismiss = { if (credit == null) onDismiss() else selected = null },
        title = credit?.name ?: "开源致谢"
    ) {
        if (credit == null) {
            Note("清记基于 SpendWise 修改。感谢以下项目及其贡献者")
            credits.forEach { entry ->
                Column(Modifier.padding(top = 12.dp)) {
                    TextLinkButton(entry.name, onClick = { selected = entry }, color = Bot.ActionSolid)
                    Note(entry.detail)
                }
            }
        } else {
            TextLinkButton("返回致谢清单", onClick = { selected = null }, color = Bot.ActionSolid)
            Note(credit.detail, Modifier.padding(vertical = 8.dp))
            if (credit.source.isNotEmpty()) {
                TextLinkButton("查看上游项目", onClick = { runCatching { links.openUri(credit.source) } },
                    color = Bot.ActionSolid)
            }
            val license by produceState("", credit.file) {
                value = withContext(Dispatchers.IO) {
                    runCatching { assets.open("licenses/${credit.file}").bufferedReader().use { it.readText() } }
                        .getOrDefault("暂时无法读取许可证")
                }
            }
            Text(license, style = body(12f, lineHeight = 1.5f), color = Bot.Ink,
                modifier = Modifier.padding(top = 12.dp))
        }
    }
}
