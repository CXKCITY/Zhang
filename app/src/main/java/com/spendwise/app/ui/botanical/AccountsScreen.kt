package com.spendwise.app.ui.botanical

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.MoneyFormatter
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    archivedAccounts: List<Account>,
    totalBalance: Long,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
    onRestore: (Account) -> Unit
) {
    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item(key = "top") {
                Box(itemMotion()) {
                    TopBar("我的账户", "返回", onBack) {
                        CircleIconButton(BotIcons.Plus, "添加账户", onClick = onAdd, size = 46.dp)
                    }
                }
            }
            item(key = "total") {
                Column(itemMotion()) {
                    Kicker("共 ${accounts.size} 个账户", Modifier.padding(bottom = 8.dp))
                    Money(
                        cents = totalBalance,
                        style = moneyStyle(58f, letterSpacingPx = -2f),
                        color = Bot.Ink,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                        animate = true
                    )
                }
            }
            items(accounts, key = { it.id }) { account ->
                AccountCard(account, onEdit = { onEdit(account) }, modifier = itemMotion().padding(bottom = 18.dp))
            }
            if (accounts.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        modifier = itemMotion(),
                        title = "创建第一个账户",
                        body = "添加银行卡 电子钱包或现金账户",
                        action = "添加账户",
                        onAction = onAdd
                    )
                }
            }
            item(key = "add") {
                BotButton(
                    "添加其他账户",
                    onClick = onAdd,
                    type = ButtonType.Secondary,
                    icon = BotIcons.Plus,
                    modifier = itemMotion().fillMaxWidth()
                )
            }
            if (archivedAccounts.isNotEmpty()) {
                item(key = "archived") {
                    Column(itemMotion()) {
                        Box(Modifier.fillMaxWidth().padding(top = 26.dp).height(1.dp).background(Color(0x25FFFFFF)))
                        Kicker("已归档 · 历史记录保留", Modifier.padding(top = 22.dp, bottom = 8.dp))
                    }
                }
                items(archivedAccounts, key = { "archived-${it.id}" }) { account ->
                    Row(
                        modifier = itemMotion()
                            .fillMaxWidth()
                            .padding(vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Box(
                            Modifier
                                .size(43.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x0EFFFFFF))
                                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) { BotIcon(BotIcons.Wallet, tint = Bot.Ink) }
                        Column(Modifier.weight(1f)) {
                            Text(account.name, style = display(23f), color = Bot.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                formatRm(account.currentBalanceCents),
                                style = body(13f, lineHeight = 1.5f),
                                color = Bot.SurfaceMuted,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                        TextLinkButton("恢复", onClick = { onRestore(account) }, color = Bot.PageMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(account: Account, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 224.dp)
            .clip(shape)
    ) {
        Box(Modifier.matchParentSize().background(Bot.Surface))
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtImage(accountIconFor(account.type), size = 44.dp)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(99.dp))
                        .botPress(onClick = onEdit)
                        .semantics { contentDescription = "编辑 ${account.name}" }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BotIcon(BotIcons.Edit, size = 15.dp, tint = Bot.Ink)
                    Text("编辑", style = display(18f), color = Bot.Ink)
                }
            }
            Text(
                account.name,
                style = display(32f, FontWeight.Light, lineHeight = 1.15f),
                color = Bot.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 34.dp, bottom = 6.dp)
            )
            Money(
                cents = account.currentBalanceCents,
                style = moneyStyle(46f, letterSpacingPx = -2f),
                color = Bot.Ink,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${account.type.displayLabel}账户",
                style = body(10f, letterSpacing = 0.5f),
                color = Bot.SurfaceMuted,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

private enum class BalanceSign(val label: String) { Positive("资产"), Negative("负债") }

/**
 * New / edit account. Editing sets the account's *current* balance; the
 * difference is folded into its opening balance so every existing entry
 * stays put.
 */
@Composable
fun AccountFormSheet(
    visible: Boolean,
    existing: Account?,
    onSave: (name: String, type: AccountType, startingBalance: String, iconName: String, color: Long) -> String?,
    onArchive: suspend () -> String?,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(if (visible) AccountKey(existing) else null)
    BotSheet(visible = visible, onDismiss = onDismiss, title = if (shown?.account != null) "编辑账户" else "新建账户") {
        if (shown != null) AccountFormBody(shown.account, onSave, onArchive, onRestore)
    }
}

private data class AccountKey(val account: Account?)

@Composable
private fun AccountFormBody(
    existing: Account?,
    onSave: (name: String, type: AccountType, startingBalance: String, iconName: String, color: Long) -> String?,
    onArchive: suspend () -> String?,
    onRestore: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing) { mutableStateOf(existing?.type ?: AccountType.Bank) }
    val current = existing?.currentBalanceCents ?: 0L
    var amount by remember(existing) { mutableStateOf(kotlin.math.abs(current)) }
    var sign by remember(existing) { mutableStateOf(if (current < 0) BalanceSign.Negative else BalanceSign.Positive) }
    var scene by remember(existing) {
        mutableStateOf(existing?.let { accountSceneFor(it.iconName, it.type) } ?: AccountScene.defaultFor(AccountType.Bank))
    }
    var sceneTouched by remember(existing) { mutableStateOf(existing != null) }
    // Save errors are about the name (blank or taken); archive errors are
    // about the account's history, so only the former outline the field.
    var error by remember(existing) { mutableStateOf<String?>(null) }
    var nameError by remember(existing) { mutableStateOf(false) }

    FieldLabel("账户名称")
    BotTextField(
        value = name,
        onValueChange = {
            name = it
            error = null
            nameError = false
        },
        placeholder = "例如 招商银行",
        maxLength = 60,
        isError = nameError,
        contentLabel = "账户名称"
    )
    FieldLabel("账户类型", modifier = Modifier.padding(top = 16.dp))
    SelectField(
        options = AccountTypeOrder,
        selected = type,
        onSelect = {
            type = it
            // A new account's landscape follows its type until the user picks one.
            if (!sceneTouched) scene = AccountScene.defaultFor(it)
        },
        label = { it.displayLabel },
        contentLabel = "账户类型"
    )
    FormCaption(if (existing != null) "当前余额" else "期初余额", Modifier.padding(top = 20.dp))
    AmountInput(cents = amount, onChange = { amount = it }, label = "账户余额")
    PillSegment(
        options = BalanceSign.entries,
        selected = sign,
        onSelect = { sign = it },
        label = { it.label },
        tone = SegmentTone.OnLight
    )
    if (existing != null) {
        InlineNote(
            "调整余额会更新期初余额 已有交易记录保持不变",
            Modifier.padding(top = 16.dp)
        )
    }
    AnimatedError(error, Modifier.padding(top = 16.dp))
    BotButton(
        "保存账户",
        onClick = {
            val target = if (sign == BalanceSign.Negative) -amount else amount
            // Keep every recorded movement: shift the opening balance by the
            // difference between the balance typed and the balance now.
            val starting = if (existing != null) {
                target - (existing.currentBalanceCents - existing.startingBalanceCents)
            } else target
            error = onSave(name, type, MoneyFormatter.centsToInput(starting), scene.key, existing?.color ?: DEFAULT_ACCOUNT_COLOR)
            nameError = error != null
        },
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
    )
    if (existing != null) {
        TextLinkButton(
            if (existing.isArchived) "恢复账户" else "归档账户",
            onClick = {
                if (existing.isArchived) {
                    onRestore()
                } else {
                    scope.launch {
                        nameError = false
                        error = onArchive()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
    }
}

/** Slate, as the data layer seeds; the redesign draws accounts from their artwork. */
private const val DEFAULT_ACCOUNT_COLOR = 0xFF64748BL
