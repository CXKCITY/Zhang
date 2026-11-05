package com.spendwise.app.ui.botanical

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.spendwise.app.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spendwise.app.ui.theme.LocalPerfMode
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

// ── Press feedback ───────────────────────────────────────────────────────────

private val PressDown = BotMotion.interactive<Float>(0.16f)
private val PressTap = BotMotion.interactive<Float>(0.09f)
private val PressRelease = spring<Float>(dampingRatio = 0.55f, stiffness = stiffnessFor(0.34f))

/**
 * Buttons sink to [scale] under the finger and spring back when released,
 * with a touch of overshoot — iOS's pressed state, no ripple. The spring is
 * interruptible, and a tap too quick to see (one in a scrolling list lands
 * press and release together) still dips before it rebounds. Reduced
 * motion keeps the tap and drops the transform.
 */
fun Modifier.botPress(
    enabled: Boolean = true,
    scale: Float = 0.96f,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val reduced = LocalPerfMode.current.reducedMotion
    val press = remember { Animatable(1f) }
    val animated = enabled && !reduced && scale != 1f
    LaunchedEffect(interaction, animated, scale) {
        if (!animated) {
            press.snapTo(1f)
            return@LaunchedEffect
        }
        interaction.interactions.collectLatest { event ->
            when (event) {
                is PressInteraction.Press -> press.animateTo(scale, PressDown)
                is PressInteraction.Release -> {
                    if (press.value > 1f - (1f - scale) * 0.6f) press.animateTo(scale, PressTap)
                    press.animateTo(1f, PressRelease)
                }
                is PressInteraction.Cancel -> press.animateTo(1f, PressRelease)
            }
        }
    }
    this
        .graphicsLayer {
            val s = press.value
            scaleX = s
            scaleY = s
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Rows keep their size and tint instead (`.b-tx:active { background: #e4e9f1 }`),
 * so the reading target never moves under the finger. Like a table cell,
 * the highlight lands at once and fades out after release; a quick tap
 * still flashes it.
 */
fun Modifier.botRowPress(
    pressedColor: Color = Color(0xFFE4E9F1),
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val highlight = remember { Animatable(0f) }
    LaunchedEffect(interaction) {
        interaction.interactions.collectLatest { event ->
            when (event) {
                is PressInteraction.Press -> highlight.animateTo(1f, BotMotion.interactive(0.1f))
                is PressInteraction.Release -> {
                    if (highlight.value < 0.6f) highlight.animateTo(1f, BotMotion.interactive(0.07f))
                    highlight.animateTo(0f, BotMotion.smooth(0.45f))
                }
                is PressInteraction.Cancel -> highlight.animateTo(0f, BotMotion.smooth(0.3f))
            }
        }
    }
    this
        .drawBehind {
            val shown = highlight.value.coerceIn(0f, 1f)
            if (shown > 0f) drawRect(pressedColor, alpha = shown)
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

/** Dims a disabled control, easing between the two states. */
@Composable
private fun Modifier.enabledAlpha(enabled: Boolean, disabled: Float = 0.4f): Modifier {
    val alpha by animateFloatAsState(if (enabled) 1f else disabled, BotMotion.smooth(0.3f), label = "enabled")
    return graphicsLayer { this.alpha = alpha }
}

@Composable
fun BotIcon(icon: ImageVector, size: Dp = 22.dp, tint: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

// ── Money ────────────────────────────────────────────────────────────────────

/** Money type: Barlow Condensed with em tracking (it scales with the figure) and tabular digits. */
fun moneyStyle(size: Float, weight: FontWeight = FontWeight.Medium, letterSpacingPx: Float = 0f): TextStyle =
    display(size, weight).copy(
        letterSpacing = emSpacing(letterSpacingPx, size),
        fontFeatureSettings = TabularNums
    )

private fun moneyText(
    cents: Long,
    plusSign: Boolean,
    centsScale: Float,
    currencyWeight: FontWeight,
    short: Boolean
): AnnotatedString = buildAnnotatedString {
    append(
        when {
            cents < 0 -> "−"
            plusSign -> "+"
            else -> ""
        }
    )
    withStyle(SpanStyle(fontWeight = currencyWeight)) { append("¥ ") }
    if (short) {
        append(shortAmount(cents))
    } else {
        val amount = formatAmount(cents)
        append(amount.substringBefore('.'))
        withStyle(SpanStyle(fontSize = centsScale.em)) { append("." + amount.substringAfter('.')) }
    }
}

/**
 * A money figure. It never wraps and is never cut off: when it is wider than
 * its space it shrinks uniformly (iOS minimumScaleFactor / the prototype's
 * MoneyFit), and where [abbreviate] allows, a figure that would shrink below
 * 60% switches to the short form ("¥ 888.89M") first. Screen readers always
 * hear the exact amount. [animate] makes a headline figure roll its digits
 * when the value changes; list rows leave it off.
 */
@Composable
fun Money(
    cents: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    plusSign: Boolean = false,
    centsScale: Float = 0.65f,
    currencyWeight: FontWeight = FontWeight.Light,
    abbreviate: Boolean = false,
    minScale: Float = 0.35f,
    align: Alignment.Horizontal = Alignment.Start,
    animate: Boolean = false
) {
    val full = remember(cents, plusSign, centsScale, currencyWeight) {
        moneyText(cents, plusSign, centsScale, currencyWeight, short = false)
    }
    val short = remember(cents, plusSign, abbreviate) {
        if (abbreviate) moneyText(cents, plusSign, 1f, currencyWeight, short = true) else null
    }
    val resolved = if (color == Color.Unspecified) style else style.copy(color = color)
    val spoken = formatRm(cents, plusSign)
    val rolls = animate && !LocalPerfMode.current.reducedMotion
    // Digits roll up when the figure rises and down when it falls.
    val previous = remember { LongArray(1) { cents } }
    val rising = cents >= previous[0]
    SideEffect { previous[0] = cents }
    val rollingSlot = remember { RollingSlot() }
    SubcomposeLayout(modifier.semantics { contentDescription = spoken }) { constraints ->
        fun line(slot: String, text: AnnotatedString, textStyle: TextStyle) =
            subcompose(slot) { Text(text = text, style = textStyle, maxLines = 1, softWrap = false) }
                .first()
                .measure(Constraints())

        var text = full
        var textStyle = resolved
        var placeable = line("full", full, resolved)
        if (constraints.hasBoundedWidth && placeable.width > constraints.maxWidth && placeable.width > 0) {
            if (short != null && constraints.maxWidth.toFloat() / placeable.width < 0.6f) {
                text = short
                placeable = line("short", short, resolved)
            }
            val ratio = constraints.maxWidth.toFloat() / placeable.width
            if (ratio < 1f) {
                textStyle = resolved.copy(fontSize = resolved.fontSize * max(ratio * 0.98f, minScale))
                placeable = line("scaled", text, textStyle)
            }
        }
        if (rolls) {
            placeable = subcompose("rolling", rollingSlot.content(text, textStyle, rising))
                .first()
                .measure(Constraints())
        }
        val width = placeable.width.coerceIn(constraints.minWidth, max(constraints.maxWidth, constraints.minWidth))
        val height = max(placeable.height, constraints.minHeight)
        layout(width, height) {
            val x = when (align) {
                Alignment.CenterHorizontally -> (width - placeable.width) / 2
                Alignment.End -> width - placeable.width
                else -> 0
            }
            placeable.place(x, (height - placeable.height) / 2)
        }
    }
}

/**
 * A zero-width space set in the line's own size, so every one-glyph cell
 * gets the full line box and cells of different sizes (the smaller cents)
 * still share one baseline.
 */
private val LineStrut = AnnotatedString("​", SpanStyle(letterSpacing = 0.sp))

/**
 * Splits a money line into cells: each digit and separator on its own, and
 * runs of other characters ("−¥ ") kept together so their kerning holds.
 */
internal fun moneyGlyphs(text: AnnotatedString): List<AnnotatedString> {
    val cells = ArrayList<AnnotatedString>(text.length)
    var start = 0
    while (start < text.length) {
        val c = text[start]
        var end = start + 1
        if (!c.isDigit() && c != '.' && c != ',') {
            while (end < text.length && !text[end].isDigit() && text[end] != '.' && text[end] != ',') end++
        }
        cells += LineStrut + text.subSequence(start, end)
        start = end
    }
    return cells
}

/**
 * Hands the rolling line's slot the same content lambda while its text,
 * style and direction are unchanged, so re-measuring (as the line eases to
 * a new width, every frame) doesn't recompose it.
 */
private class RollingSlot {
    private var text: AnnotatedString? = null
    private var style: TextStyle? = null
    private var rising = true
    private var content: @Composable () -> Unit = {}

    fun content(text: AnnotatedString, style: TextStyle, rising: Boolean): @Composable () -> Unit {
        if (text != this.text || style != this.style || rising != this.rising) {
            this.text = text
            this.style = style
            this.rising = rising
            content = { RollingLine(text, style, rising) }
        }
        return content
    }
}

private val RollOffset = BotMotion.snappy<IntOffset>(0.36f, IntOffset.VisibilityThreshold)
private val RollSize = BotMotion.snappy<IntSize>(0.36f, IntSize.VisibilityThreshold)

/**
 * A figure whose characters roll when they change, like iOS's numeric-text
 * transition. Positions count from the right, so the cents stay put while
 * the figure grows or shrinks on the left; a changed glyph slides out as the
 * new one slides in — upward when the value rises, downward when it falls —
 * and the line eases to its new width.
 */
@Composable
private fun RollingLine(text: AnnotatedString, style: TextStyle, rising: Boolean) {
    val cells = remember(text) { moneyGlyphs(text) }
    // Slots only ever grow, so a position that empties can animate away.
    val slots = remember { IntArray(1) }
    slots[0] = max(slots[0], cells.size)
    Row(Modifier.clearAndSetSemantics { }) {
        for (slot in slots[0] - 1 downTo 0) {
            val cell = cells.getOrNull(cells.size - 1 - slot)
            key(slot) {
                AnimatedContent(
                    targetState = cell,
                    transitionSpec = {
                        val direction = if (rising) 1 else -1
                        (slideInVertically(RollOffset) { h -> direction * h / 2 } + fadeIn(BotMotion.smooth(0.26f)))
                            .togetherWith(slideOutVertically(RollOffset) { h -> -direction * h / 2 } + fadeOut(BotMotion.smooth(0.2f)))
                            .using(SizeTransform(clip = false) { _, _ -> RollSize })
                    },
                    contentAlignment = Alignment.Center,
                    label = "moneyDigit"
                ) { glyph ->
                    if (glyph != null) Text(glyph, style = style, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** Colour and sign for an entry amount on a white surface. */
@Composable
fun EntryMoney(
    cents: Long,
    kind: EntryKind,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    centsScale: Float = 0.8f,
    align: Alignment.Horizontal = Alignment.Start
) {
    Money(
        cents = if (kind == EntryKind.Expense) -cents else cents,
        style = style,
        modifier = modifier,
        color = when (kind) {
            EntryKind.Expense -> Bot.Negative
            EntryKind.Income -> Bot.Positive
            EntryKind.Transfer -> Bot.Ink
        },
        plusSign = kind == EntryKind.Income,
        centsScale = centsScale,
        align = align
    )
}

enum class EntryKind(val label: String) { Expense("支出"), Income("收入"), Transfer("转账") }

// ── Text atoms ───────────────────────────────────────────────────────────────

/** Uppercase eyebrow (`.b-kicker`). */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = Bot.PageMuted) {
    Text(text.uppercase(), style = kickerStyle(color), modifier = modifier)
}

@Composable
fun Subtitle(text: String, modifier: Modifier = Modifier, color: Color = Bot.PageMuted) {
    Text(text, style = body(14f, lineHeight = 1.5f, color = color), modifier = modifier)
}

/** `.b-note`: quiet footnote on dark pages. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier, color: Color = Bot.PageMuted) {
    Text(text, style = body(14f, lineHeight = 1.5f, color = color), modifier = modifier)
}

/** `.b-support`: secondary line inside white surfaces. */
@Composable
fun Support(text: String, modifier: Modifier = Modifier, color: Color = Bot.SurfaceMuted) {
    Text(text, style = body(14f, lineHeight = 1.5f, color = color), modifier = modifier)
}

// ── Buttons ──────────────────────────────────────────────────────────────────

enum class IconButtonTone { OnDark, OnLight, Plain }

/** 44–46dp circular icon button (`.b-icon`). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tone: IconButtonTone = IconButtonTone.OnLight,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val (bg, border, ink) = when (tone) {
        IconButtonTone.OnDark -> Triple(Color(0x18FFFFFF), Color(0x40FFFFFF), Color.White)
        IconButtonTone.OnLight -> Triple(Color(0xFFF0F1F3), Color(0xFFE4E6E9), Bot.Ink)
        IconButtonTone.Plain -> Triple(Color.Transparent, Color.Transparent, Color.White)
    }
    Box(
        modifier = modifier
            .size(size)
            .enabledAlpha(enabled)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = iconSize, tint = tint ?: ink)
    }
}

enum class ButtonType { Primary, Secondary, Danger }

/**
 * `.b-primary` / `.b-secondary` / `.b-danger`: 99px pills, 48dp tall. The
 * default label is Barlow Condensed 22; flows that sit next to form text
 * (Entry, Filters) pass [dmSans].
 */
@Composable
fun BotButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    dmSans: Boolean = false,
    color: Color? = null,
    horizontalPadding: Dp = 22.dp
) {
    val (bg, ink) = when (type) {
        ButtonType.Primary -> (color ?: Bot.Blue) to Color.White
        ButtonType.Secondary -> Bot.SecondaryBg to Bot.SecondaryInk
        ButtonType.Danger -> Bot.DangerBg to Bot.DangerInk
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .enabledAlpha(enabled)
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .botPress(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            BotIcon(icon, size = 18.dp, tint = ink)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = if (dmSans) body(16f, FontWeight.Medium, lineHeight = 1.3f) else display(22f, FontWeight.Medium),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** `.b-text-button`: bare Barlow label, 44dp touch height. */
@Composable
fun TextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF737889),
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .enabledAlpha(enabled)
            .botPress(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = display(19f), color = color)
    }
}

/** Section-head action ("查看统计", "预算"): quiet tinted pill. */
@Composable
fun ChipButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Bot.ChipBg)
            .botPress(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = body(13f, FontWeight.Medium, lineHeight = 1.4f), color = Bot.ChipInk)
    }
}

// ── Surfaces ─────────────────────────────────────────────────────────────────

/** White data card (`.b-panel`): 28dp radius, 20dp padding. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, BotMotion.smooth(0.45f)) }
    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 18.dp.toPx()
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Bot.Surface)
            .padding(padding)
    ) { content() }
}

/** Card title with an optional trailing action (`.b-section-head`). */
@Composable
fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    titleSize: Float = 27f,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = display(titleSize, FontWeight.Medium, letterSpacing = -0.5f, lineHeight = 1.15f),
            color = Bot.SurfaceInk,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke(this)
    }
}

/**
 * Budget / category share bar (`.b-track`). It fills from empty when it
 * appears and eases to a new level when the numbers change.
 */
@Composable
fun Track(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 5.dp) {
    val target = ratio.coerceIn(0f, 1f)
    val fill = remember { Animatable(0f) }
    LaunchedEffect(target) { fill.animateTo(target, BotMotion.smooth(0.62f)) }
    val tint = animatedColor(color, "track")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(Bot.Track)
            .drawBehind {
                val width = size.width * fill.value
                if (width > 0f) {
                    drawRoundRect(
                        color = tint,
                        size = androidx.compose.ui.geometry.Size(width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    )
                }
            }
    )
}

/** Illustrated category/account disc on a quiet rounded tile. */
@Composable
fun ArtTile(
    @DrawableRes art: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imageSize: Dp = 46.dp,
    background: Color = Bot.TileBg,
    radius: Dp = 17.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            BotIcon(categoryUiIcon(art), size = imageSize * 0.58f, tint = Bot.ActionSolid)
        } else {
            BotIcon(BotIcons.Transfer, size = 24.dp, tint = Bot.Ink)
        }
    }
}

@Composable
fun ArtImage(@DrawableRes art: Int, size: Dp, modifier: Modifier = Modifier, tint: Color = Bot.ActionSolid) {
    BotIcon(categoryUiIcon(art), size = size * 0.72f, tint = tint, modifier = modifier)
}

/**
 * One ledger line (`.b-tx`): illustrated tile, name + context, signed amount.
 * The amount may take up to 62% of the row; the name keeps at least 88dp and
 * truncates first — exact amounts are never shortened here.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    title: String,
    meta: String,
    cents: Long,
    kind: EntryKind,
    @DrawableRes art: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    showDivider: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false
) {
    Column(modifier.fillMaxWidth().background(animatedColor(if (selected) Bot.ChipBg else Bot.Surface, "recordSelection"))) {
        BoxWithConstraints(Modifier.fillMaxWidth().then(
            if (onLongClick == null) Modifier.botRowPress(onClick = onClick)
            else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "多选记录")
        )) {
            val maxAmount = maxWidth * 0.62f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(visible = selectionMode) {
                    androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onClick() })
                }
                if (!selectionMode) ArtTile(art)
                Column(Modifier.weight(1f).widthIn(min = 88.dp)) {
                    Text(
                        title,
                        style = display(23f, lineHeight = 1.2f),
                        color = Bot.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        meta,
                        style = body(13f, lineHeight = 1.5f),
                        color = Bot.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                EntryMoney(
                    cents = cents,
                    kind = kind,
                    style = moneyStyle(25f, letterSpacingPx = -0.8f),
                    modifier = Modifier.widthIn(max = maxAmount)
                )
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
        }
    }
}

/**
 * Equal-width cells that all take the tallest cell's height, like a CSS grid
 * row. Measures twice via subcomposition because the cells hold [Money],
 * which can't answer intrinsic-size queries.
 */
@Composable
fun EqualHeightRow(
    cells: List<@Composable () -> Unit>,
    columns: Int,
    gap: Dp,
    modifier: Modifier = Modifier
) {
    SubcomposeLayout(modifier.fillMaxWidth()) { constraints ->
        val gapPx = gap.roundToPx()
        val cellWidth = ((constraints.maxWidth - gapPx * (columns - 1)) / columns).coerceAtLeast(0)
        val natural = cells.mapIndexed { index, cell ->
            subcompose("measure$index", cell).first().measure(Constraints(minWidth = cellWidth, maxWidth = cellWidth))
        }
        val height = natural.maxOfOrNull { it.height } ?: 0
        val placeables = cells.mapIndexed { index, cell ->
            subcompose("place$index", cell).first().measure(Constraints.fixed(cellWidth, height))
        }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable -> placeable.place(index * (cellWidth + gapPx), 0) }
        }
    }
}

// ── Header controls ──────────────────────────────────────────────────────────

/**
 * Month control (`.b-period`): previous, the month pill that opens the month
 * picker, next. Months after the current one are unreachable — the ledger
 * has no future entries to show.
 */
@Composable
fun PeriodSelector(
    month: YearMonth,
    onChange: (YearMonth) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    compact: Boolean = false
) {
    val ink = if (onDark) Color.White else Bot.Ink
    val current = YearMonth.from(todayKl())
    Row(
        modifier = if (compact) modifier.offset(x = (-8).dp) else modifier.offset(x = (-14).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PeriodArrow(BotIcons.ChevronLeft, "上个月", enabled = true, ink = ink) { onChange(month.minusMonths(1)) }
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .botPress(onClick = onOpenPicker)
                .then(if (compact) Modifier.padding(vertical = 4.dp).heroGlass(100.dp)
                      else Modifier.clip(RoundedCornerShape(100.dp)).background(Color(0x33D9D9D9)))
                .semantics { contentDescription = "${monthLabel(month)} 选择月份" }
                .padding(horizontal = if (compact) 9.dp else 14.dp, vertical = if (compact) 6.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // The label slides the way time moves: later months come in from the right.
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(BotMotion.smooth(0.34f, IntOffset.VisibilityThreshold)) { w -> direction * w / 3 } +
                        fadeIn(BotMotion.smooth(0.26f)))
                        .togetherWith(
                            slideOutHorizontally(BotMotion.smooth(0.34f, IntOffset.VisibilityThreshold)) { w -> -direction * w / 3 } +
                                fadeOut(BotMotion.smooth(0.18f))
                        )
                        .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                },
                label = "periodLabel"
            ) { shown ->
                Text(
                    monthLabel(shown),
                    style = body(if (compact) 12f else 15f, FontWeight.Medium),
                    color = ink,
                    maxLines = 1,
                    softWrap = false
                )
            }
            BotIcon(BotIcons.ChevronDown, size = if (compact) 14.dp else 18.dp, tint = ink)
        }
        PeriodArrow(BotIcons.ChevronRight, "下个月", enabled = month < current, ink = ink) {
            if (month < current) onChange(month.plusMonths(1))
        }
    }
}

@Composable
private fun PeriodArrow(icon: ImageVector, label: String, enabled: Boolean, ink: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(44.dp)
            .enabledAlpha(enabled)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = 18.dp, tint = ink)
    }
}

// ── Segments ─────────────────────────────────────────────────────────────────

enum class SegmentTone { OnDark, OnLight, Entry }

/** Pill segmented control (`.b-segment`). */
@Composable
fun <T> PillSegment(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    tone: SegmentTone = SegmentTone.OnDark,
    isEnabled: (T) -> Boolean = { true }
) {
    val containerBg: Color
    val containerBorder: Color
    val pillBg: Color
    val pillBorder: Color
    val selectedInk: Color
    val quietInk: Color
    when (tone) {
        SegmentTone.OnDark -> {
            containerBg = Bot.ChipBg; containerBorder = Bot.Rule
            pillBg = Bot.ActionSolid; pillBorder = Color.Transparent; selectedInk = Color.White; quietInk = Bot.Ink
        }
        SegmentTone.OnLight -> {
            containerBg = Color(0xFFEDF0F4); containerBorder = Color(0xFFE1E5ED)
            pillBg = Color.White; pillBorder = Color.Transparent; selectedInk = Bot.Navy; quietInk = Bot.Ink
        }
        SegmentTone.Entry -> {
            containerBg = Color(0x80FFFFFF); containerBorder = Color(0x90FFFFFF)
            pillBg = Bot.ActionSolid; pillBorder = Bot.ActionSolid; selectedInk = Color.White; quietInk = Color(0xFF24344F)
        }
    }
    val haptics = LocalHapticFeedback.current
    val index = options.indexOf(selected).coerceAtLeast(0)
    // One pill glides between the options (UISegmentedControl), a little springy.
    val position = animateFloatAsState(index.toFloat(), BotMotion.snappy(0.34f), label = "segment")
    val shape = RoundedCornerShape(99.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(containerBg)
            .border(1.dp, containerBorder, shape)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(4.dp)
                .layout { measurable, constraints ->
                    val gap = 4.dp.roundToPx()
                    val count = options.size.coerceAtLeast(1)
                    val width = ((constraints.maxWidth - gap * (count - 1)) / count).coerceAtLeast(0)
                    val pill = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        pill.placeRelative((position.value * (width + gap)).roundToInt(), 0)
                    }
                }
                .then(
                    if (tone != SegmentTone.Entry) {
                        Modifier.shadow(3.dp, shape, clip = false, ambientColor = Color(0x11000000), spotColor = Color(0x11000000))
                    } else Modifier
                )
                .clip(shape)
                .background(pillBg)
                .border(1.dp, pillBorder, shape)
        )
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { i, option ->
                val isSelected = option == selected
                val enabled = isEnabled(option)
                // How much of the pill sits under this option right now.
                val cover = (1f - abs(position.value - i)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = if (tone == SegmentTone.Entry) 44.dp else 42.dp)
                        .enabledAlpha(enabled)
                        .then(
                            if (tone == SegmentTone.Entry) {
                                // The Entry segments are tiles of their own; each clears as the pill arrives.
                                Modifier
                                    .clip(shape)
                                    .background(Color(0x40FFFFFF).copy(alpha = 0.25f * (1f - cover)))
                                    .border(1.dp, Color(0x50FFFFFF).copy(alpha = 0.31f * (1f - cover)), shape)
                            } else Modifier.clip(shape)
                        )
                        .botPress(enabled = enabled && !isSelected, role = Role.Tab) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(option)
                        }
                        .semantics { this.selected = isSelected }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label(option),
                        style = body(14f, FontWeight.Medium, lineHeight = 1.4f),
                        color = lerp(quietInk, selectedInk, cover),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Data tabs (Activity type, Insights scope, By category): a rule with the
 * selected tab underlined, so they read as views of the data rather than
 * as more buttons.
 */
@Composable
fun <T> TabSegment(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    inPanel: Boolean = false,
    fontSize: Float = 14f
) {
    val rule = Bot.Rule
    val accent = Bot.ActionSolid
    val activeInk = Bot.SurfaceInk
    val quietInk = if (inPanel) Bot.SurfaceMuted else Bot.PageMuted
    val haptics = LocalHapticFeedback.current
    val index = options.indexOf(selected).coerceAtLeast(0)
    // The underline slides to the chosen tab rather than jumping.
    val position = animateFloatAsState(index.toFloat(), BotMotion.snappy(0.34f), label = "tabUnderline")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRect(rule, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - stroke), size = androidx.compose.ui.geometry.Size(size.width, stroke))
                val tab = size.width / options.size.coerceAtLeast(1)
                val underline = 2.dp.toPx()
                drawRect(
                    accent,
                    topLeft = androidx.compose.ui.geometry.Offset(position.value * tab, size.height - underline),
                    size = androidx.compose.ui.geometry.Size(tab, underline)
                )
            }
    ) {
        options.forEachIndexed { i, option ->
            val isSelected = option == selected
            val cover = (1f - abs(position.value - i)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) {
                        if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(option)
                    }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = body(fontSize, if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = lerp(quietInk, activeInk, cover),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// ── Toggle ───────────────────────────────────────────────────────────────────

/**
 * 44×27 switch (`.b-toggle`); [offColor] differs on navy pages and white
 * cards. Like UISwitch, the thumb stretches while pressed and springs across
 * with a little give.
 */
@Composable
fun BotToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    offColor: Color = Color(0x28FFFFFF),
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val position = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = stiffnessFor(0.32f)),
        label = "toggle"
    )
    val stretch = animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = BotMotion.smooth(0.2f),
        label = "toggleStretch"
    )
    val track = animatedColor(if (checked) Bot.Blue else offColor, "toggleTrack")
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 27.dp)
            .enabledAlpha(enabled)
            .clip(RoundedCornerShape(99.dp))
            .background(track)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch
            ) {
                haptics.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                onToggle()
            }
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "On" else "关闭"
            }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val base = 21.dp.roundToPx()
                    val width = base + (6.dp.toPx() * stretch.value).roundToInt()
                    val thumb = measurable.measure(Constraints.fixed(width, base))
                    layout(constraints.maxWidth, base) {
                        thumb.placeRelative(((constraints.maxWidth - width) * position.value).roundToInt(), 0)
                    }
                }
                .shadow(1.5.dp, RoundedCornerShape(99.dp), clip = false, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                .clip(RoundedCornerShape(99.dp))
                .background(Bot.Surface)
        )
    }
}

// ── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    onDark: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Bot.ChipBg),
            contentAlignment = Alignment.Center
        ) {
            BotIcon(BotIcons.Wallet, size = 32.dp, tint = Bot.ActionSolid)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = display(29f, lineHeight = 1.15f),
            color = Bot.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = body(12f, lineHeight = 1.7f),
            color = LedgerAppearance.color(Color(0xFF8D97AC)),
            textAlign = TextAlign.Center
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            BotButton(action, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Form parts ───────────────────────────────────────────────────────────────

enum class FieldTone { Sheet, Entry }

@Composable
fun FieldLabel(text: String, tone: FieldTone = FieldTone.Sheet, modifier: Modifier = Modifier) {
    Text(
        text,
        style = if (tone == FieldTone.Entry) body(13f, FontWeight.SemiBold, lineHeight = 1.4f)
        else body(13f, FontWeight.Medium, lineHeight = 1.4f),
        color = Bot.FieldLabel,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

/**
 * Text input. Sheet fields are soft grey boxes in DM Sans; Entry fields are
 * white pills set in Barlow Condensed so what you type reads like the rest
 * of the ledger.
 */
@Composable
fun BotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    tone: FieldTone = FieldTone.Sheet,
    singleLine: Boolean = true,
    minHeight: Dp = if (tone == FieldTone.Entry) 48.dp else 49.dp,
    maxLength: Int = Int.MAX_VALUE,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    focusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    contentLabel: String? = null
) {
    val entry = tone == FieldTone.Entry
    val textStyle = if (entry) display(24f, lineHeight = 1.15f, color = Bot.FieldInk)
    else body(14f, lineHeight = 1.4f, color = Bot.FieldInk)
    val shape = RoundedCornerShape(if (entry) 24.dp else 17.dp)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(Bot.ActionSolid),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .semantics {
                if (contentLabel != null) contentDescription = contentLabel
                if (isError) error("请检查输入内容")
            },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(Bot.FieldBg)
                    .border(
                        BorderStroke(1.dp, if (isError) Color(0xFFD9788C) else if (entry) Color.Transparent else Bot.FieldBorder),
                        shape
                    )
                    .padding(
                        horizontal = if (entry) 19.dp else 14.dp,
                        vertical = if (entry) 7.dp else 12.dp
                    ),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = if (entry) Color(0xFF897A7A) else Color(0xFF8D94A4),
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE
                    )
                }
                inner()
            }
        }
    )
}

/** Dropdown styled as a sheet field (`<select>` in the prototype). */
@Composable
fun <T> SelectField(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "选择",
    isEnabled: (T) -> Boolean = { true },
    isError: Boolean = false,
    contentLabel: String
) {
    var open by remember { mutableStateOf(false) }
    // The menu matches the field's width, like the native picker it stands in for.
    var fieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = it.width }
                .heightIn(min = 49.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Bot.FieldBg)
                .border(1.dp, if (isError) Color(0xFFD9788C) else Bot.FieldBorder, RoundedCornerShape(17.dp))
                .clickable(role = Role.DropdownList) { open = true }
                .semantics { contentDescription = "$contentLabel: ${selected?.let(label) ?: placeholder}" }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected?.let(label) ?: placeholder,
                style = body(14f, lineHeight = 1.4f),
                color = if (selected == null) Color(0xFF8D94A4) else Bot.FieldInk,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BotIcon(BotIcons.ChevronDown, size = 18.dp, tint = Bot.FieldInk)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Bot.Surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(with(density) { fieldWidth.toDp() })
        ) {
            options.forEach { option ->
                val enabled = isEnabled(option)
                DropdownMenuItem(
                    text = {
                        Text(
                            label(option),
                            style = body(14f, if (option == selected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (enabled) Bot.FieldInk else Bot.FieldInk.copy(alpha = 0.4f)
                        )
                    },
                    enabled = enabled,
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

/** `.b-error`: soft rose box with the fix in plain words. */
@Composable
fun ErrorBox(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(12f, lineHeight = 1.5f),
        color = LedgerAppearance.color(Color(0xFFA12D46)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Bot.DangerBg)
            .border(1.dp, Color(0xFFEFC3CD), RoundedCornerShape(15.dp))
            .padding(12.dp)
            .semantics { error(text) }
    )
}

/** `.b-inline-note`: explanatory aside inside a form. */
@Composable
fun InlineNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(12f, lineHeight = 1.6f),
        color = LedgerAppearance.color(Color(0xFF5F6F8D)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEEF2F9))
            .padding(horizontal = 15.dp, vertical = 13.dp)
    )
}

@Composable
fun FormCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(13f, FontWeight.Medium, lineHeight = 1.4f),
        color = Bot.FieldLabel,
        modifier = modifier
    )
}

/** One character of a typed amount, with the identity it keeps while digits shift. */
internal data class AmountGlyph(val key: String, val text: String)

/**
 * The glyphs of a fixed-sen amount, left to right. Each typed digit keeps its
 * key ("d0" is the first digit typed) as later digits push it left — past the
 * decimal point and the thousands commas — so it can glide to its new place.
 * Placeholder zeros, the point and each comma have keys of their own.
 */
internal fun amountGlyphs(cents: Long): List<AmountGlyph> {
    val digits = if (cents <= 0L) "" else cents.toString()
    val count = digits.length
    val whole = max(0, count - 2)
    val glyphs = ArrayList<AmountGlyph>(count + 6)
    if (whole == 0) glyphs += AmountGlyph("whole0", "0")
    for (j in 0 until whole) {
        if (j > 0 && (whole - j) % 3 == 0) glyphs += AmountGlyph("comma${(whole - j) / 3}", ",")
        glyphs += AmountGlyph("d$j", digits[j].toString())
    }
    glyphs += AmountGlyph("point", ".")
    when (count) {
        0 -> {
            glyphs += AmountGlyph("sen1", "0")
            glyphs += AmountGlyph("sen0", "0")
        }
        1 -> {
            glyphs += AmountGlyph("sen1", "0")
            glyphs += AmountGlyph("d0", digits[0].toString())
        }
        else -> {
            glyphs += AmountGlyph("d${count - 2}", digits[count - 2].toString())
            glyphs += AmountGlyph("d${count - 1}", digits[count - 1].toString())
        }
    }
    return glyphs
}

private val HiddenSelection = TextSelectionColors(handleColor = Color.Transparent, backgroundColor = Color.Transparent)

/**
 * Fixed-sen amount (`AmountInput`): digits fill from the right — typing 1, 2,
 * 3 reads 0.01, 0.12, 1.23 — and the decimal is never typed. Each new digit
 * rises in at the right while the others glide left, and once the figure no
 * longer fits it eases smaller, returning to full size as it shortens.
 * Capped at ¥ 999,999,999.99 (11 digits), as the prototype.
 */
@Composable
fun AmountInput(
    cents: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    entry: Boolean = false,
    label: String = "金额",
    error: String? = null,
    dateText: String = "日期",
    onPickDate: (() -> Unit)? = null,
    initiallyOpen: Boolean = entry,
    keyboardAllowed: Boolean = true,
    onPadOpenChange: (Boolean) -> Unit = {}
) {
    var expression by remember { mutableStateOf(if (cents == 0L) "" else com.spendwise.app.domain.MoneyFormatter.centsToInput(cents)) }
    val result = AmountExpression.evaluate(expression)
    var keypadOpen by remember { mutableStateOf(initiallyOpen) }
    LaunchedEffect(keypadOpen) { onPadOpenChange(keypadOpen) }
    androidx.activity.compose.BackHandler(enabled = keypadOpen && keyboardAllowed) { keypadOpen = false }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val ink = if (entry) Color.White else Bot.Ink
    LaunchedEffect(error) { if (error != null) keypadOpen = true }
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).botPress {
                focus.clearFocus(); keyboard?.hide(); keypadOpen = true
            }.semantics { contentDescription = "$label ${expression.ifEmpty { "0" }} 点击打开数字键盘" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¥", style = body(28f), color = ink, modifier = Modifier.padding(end = 8.dp))
            Text(expression.ifEmpty { "0.00" }, style = body(30f, FontWeight.Medium), color = ink,
                maxLines = 2, modifier = Modifier.weight(1f))
            TextLinkButton(
                if (keypadOpen && keyboardAllowed) "收起键盘" else "展开键盘",
                onClick = {
                    if (keypadOpen && keyboardAllowed) keypadOpen = false
                    else { focus.clearFocus(); keyboard?.hide(); keypadOpen = true }
                },
                color = ink
            )
        }
        if (expression.isNotEmpty() && result == null) {
            Text("请补全算式 金额需在 0 至 999999999.99 之间", style = body(12f), color = ink)
        } else if (expression.any { it == '+' || it == '−' }) {
            Text("合计 ${formatRm(result ?: 0L)}", style = body(14f), color = ink)
        }
        Reveal(visible = keypadOpen && keyboardAllowed) {
            Column(Modifier.padding(top = 12.dp).clip(RoundedCornerShape(12.dp))) {
                AmountKeypad.rows(onPickDate != null).forEach { keys ->
                    Row {
                        keys.forEach { key ->
                            val done = key == "完成"
                            val ink = if (done) Color.White else Bot.Ink
                            Box(
                                Modifier.weight(1f).height(62.dp)
                                    .background(if (done) Bot.ActionSolid else Bot.Surface)
                                    .border(0.5.dp, Bot.Rule)
                                    .botPress {
                                        focus.clearFocus(); keyboard?.hide()
                                        if (key == "日期") {
                                            onPickDate?.invoke()
                                        } else {
                                            expression = AmountKeypad.press(expression, key)
                                            onChange(AmountExpression.evaluate(expression) ?: 0L)
                                            if (done && AmountExpression.evaluate(expression) != null) {
                                                keypadOpen = false
                                            }
                                        }
                                    }.semantics { contentDescription = if (key == "日期") "选择日期 $dateText" else key },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "退格" -> BotIcon(Icons.Outlined.Backspace, size = 25.dp, tint = ink)
                                    "日期" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        BotIcon(BotIcons.Calendar, size = 17.dp, tint = ink)
                                        Text(dateText, style = body(13f), color = ink, maxLines = 1)
                                    }
                                    else -> Text(key, style = body(if (key.length == 1) 26f else 17f), color = ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Material Icons are Apache 2.0 licensed and remain crisp at every density.
private fun categoryUiIcon(art: Int): ImageVector = when (art) {
    R.drawable.category_food -> Icons.Outlined.Restaurant
    R.drawable.category_shopping -> Icons.Outlined.ShoppingBag
    R.drawable.category_bills -> Icons.Outlined.ReceiptLong
    R.drawable.category_groceries -> Icons.Outlined.ShoppingCart
    R.drawable.category_transport -> Icons.Outlined.DirectionsBus
    R.drawable.category_health -> Icons.Outlined.LocalHospital
    R.drawable.category_salary -> Icons.Outlined.Payments
    R.drawable.category_freelance -> Icons.Outlined.WorkOutline
    R.drawable.category_investment -> Icons.Outlined.TrendingUp
    R.drawable.category_petrol -> Icons.Outlined.LocalGasStation
    R.drawable.category_toll -> Icons.Outlined.LocalParking
    R.drawable.category_rent -> Icons.Outlined.Home
    R.drawable.category_utilities -> Icons.Outlined.Bolt
    R.drawable.category_phone -> Icons.Outlined.Wifi
    R.drawable.category_family -> Icons.Outlined.PeopleOutline
    R.drawable.category_zakat -> Icons.Outlined.VolunteerActivism
    R.drawable.account_icon_bank -> Icons.Outlined.AccountBalance
    R.drawable.account_icon_credit -> Icons.Outlined.CreditCard
    R.drawable.account_icon_ewallet -> Icons.Outlined.AccountBalanceWallet
    else -> Icons.Outlined.Wallet
}
