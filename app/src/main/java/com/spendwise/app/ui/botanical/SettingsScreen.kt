package com.spendwise.app.ui.botanical

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.data.BackupSettings
import java.time.Instant
import java.time.format.DateTimeFormatter

private val SavedAt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "26 Sept, 14:05" */
fun backupTimeLabel(millis: Long): String {
    val at = Instant.ofEpochMilli(millis).atZone(KL_ZONE)
    return "${shortDateLabel(at.toLocalDate())}, ${at.format(SavedAt)}"
}

/** Human name for a SAF tree URI ("Download/SpendWise" → "清记"). */
fun backupFolderName(treeUri: String): String = runCatching {
    val decoded = Uri.decode(Uri.parse(treeUri).lastPathSegment.orEmpty())
    decoded.substringAfter(':').substringAfterLast('/').ifBlank { decoded.substringBefore(':') }
}.getOrNull().orEmpty().ifBlank { "备份文件夹" }

@Composable
fun SettingsScreen(
    backup: BackupSettings,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onRecurring: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onToggleDaily: (Boolean) -> Unit,
    onChooseFolder: () -> Unit,
    onDownloadCopy: () -> Unit
) {
    val folder = backup.treeUri?.let(::backupFolderName)
    var creditsOpen by remember { mutableStateOf(false) }
    DarkPage {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            TopBar("设置", "返回首页", onBack, modifier = Modifier.padding(bottom = 0.dp))
            PalettePicker()
            SpendingSettingsControl()
            SettingsGroup("我的账本") {
                SettingsRow(BotIcons.Grid, "分类", "分类图标与月度预算", chevron = true, onClick = onCategories)
                SettingsRow(BotIcons.Repeat, "周期记账", "为固定支出自动记账", chevron = true, last = true, onClick = onRecurring)
            }
            SettingsGroup("数据备份") {
                SettingsRow(
                    BotIcons.Download,
                    "立即备份",
                    when {
                        backup.lastBackupMillis != null -> "上次备份 · ${backupTimeLabel(backup.lastBackupMillis)}"
                        folder != null -> "保存到 $folder"
                        else -> "将消费记录 分类和规则保存为文件"
                    },
                    onClick = onBackupNow
                )
                SettingsRow(BotIcons.Upload, "从备份恢复", "JSON 备份 · 恢复前先检查", chevron = true, onClick = onRestore)
                SettingsRow(
                    BotIcons.Repeat,
                    "每日自动备份",
                    "每天保存到指定文件夹",
                    onClick = { onToggleDaily(!backup.enabled) },
                    trailing = {
                        BotToggle(checked = backup.enabled, onToggle = { onToggleDaily(!backup.enabled) }, label = "每日自动备份")
                    }
                )
                SettingsRow(
                    BotIcons.Wallet,
                    if (folder != null) "更换备份文件夹" else "选择备份文件夹",
                    folder ?: "选择此设备上的文件夹",
                    chevron = true,
                    last = true,
                    onClick = onChooseFolder
                )
            }
            Note(
                "清记会每天在指定文件夹保存一份备份 保留最近七份自动备份 手动保存的备份不会被删除",
                Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)
            )
            val shownError = rememberRetained(backup.lastError)
            Reveal(visible = backup.lastError != null) {
                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp)) {
                    Box(Modifier.size(width = 3.dp, height = 20.dp).background(Color(0xFFDB81A9)))
                    Text(
                        shownError.orEmpty(),
                        style = body(13f, lineHeight = 1.6f),
                        color = Bot.Ink,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
            Reveal(visible = folder != null) {
                TextLinkButton(
                    "另存一份备份",
                    onClick = onDownloadCopy,
                    color = Bot.PageMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            SettingsGroup("关于") {
                SettingsRow(BotIcons.Info, "开源致谢", "贡献者、源代码与许可证", chevron = true,
                    last = true, onClick = { creditsOpen = true })
            }
            Note("清记 ${com.spendwise.app.BuildConfig.VERSION_NAME} · 本地账本\n基于 SpendWise · MIT 开源许可", Modifier.padding(start = 4.dp, top = 28.dp, bottom = 16.dp))
        }
        OpenSourceCredits(creditsOpen) { creditsOpen = false }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            title,
            style = body(14f, FontWeight.Medium, lineHeight = 1.4f),
            color = Bot.PageMuted,
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, bottom = 10.dp)
                .semantics { heading() }
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x0DFFFFFF))
                .border(1.dp, Color(0x1CFFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp)
        ) { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    chevron: Boolean = false,
    last: Boolean = false,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().botRowPress(pressedColor = Bot.Rule, onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Bot.ChipBg)
                    .border(1.dp, Bot.Rule, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) { BotIcon(icon, tint = Bot.Ink) }
            Column(Modifier.weight(1f)) {
                Text(title, style = display(23f), color = Bot.Ink)
                // "Last saved …" and friends crossfade when they change.
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = {
                        fadeIn(BotMotion.smooth(0.3f))
                            .togetherWith(fadeOut(BotMotion.smooth(0.2f)))
                            .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                    },
                    label = "settingsSubtitle"
                ) { line ->
                    Text(
                        line,
                        style = body(13f, lineHeight = 1.5f),
                        color = Bot.SurfaceMuted,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
            when {
                trailing != null -> trailing()
                chevron -> BotIcon(BotIcons.Next, size = 16.dp, tint = Bot.Ink)
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
    }
}
