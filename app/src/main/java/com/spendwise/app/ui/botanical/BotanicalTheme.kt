package com.spendwise.app.ui.botanical

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spendwise.app.R
import com.spendwise.app.ui.theme.LocalPerfMode
import com.spendwise.app.ui.theme.rememberPerfMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
//  Botanical — the September 2026 redesign (redesign-local/SpendWise.html).
//
//  Painted floral hero on warm paper for Home; midnight-navy pages for
//  everything else; white data cards; Barlow Condensed for headlines and
//  money, DM Sans for supporting text. Values below trace to the prototype's
//  stylesheets (scratch/redesign-complete/work/src/components/generated/
//  Botanical*Styles.ts). CSS px map 1:1 to dp/sp — the prototype was laid out
//  at 402px, the width class of the phones this app targets.
// ═════════════════════════════════════════════════════════════════════════════

/** Colour tokens. The redesign has a single appearance — no light/dark split. */
object Bot {
    val Ink get() = LedgerAppearance.token("Ink", Color(0xFF080C18))
    val Navy get() = LedgerAppearance.token("Navy", Color(0xFF101C3A))
    val Paper get() = LedgerAppearance.token("Paper", Color(0xFFF5F7FB))
    val Muted get() = LedgerAppearance.token("Muted", Color(0xFF697184))
    val Blue get() = LedgerAppearance.token("Blue", Color(0xFF2563EB))
    /** Selected controls: white text on this measures 5.83:1. */
    val ActionSolid get() = LedgerAppearance.token("ActionSolid", Color(0xFF2563EB))
    val Pink get() = LedgerAppearance.token("Pink", Color(0xFF2563EB))
    val Orchid get() = LedgerAppearance.token("Orchid", Color(0xFF93B8FA))

    // Dark pages
    val PageInk get() = LedgerAppearance.token("PageInk", Color(0xFF111827))
    val PageMuted get() = LedgerAppearance.token("PageMuted", Color(0xFF64748B))
    val PageDeep get() = LedgerAppearance.token("PageDeep", Color(0xFFF5F7FB))

    // White surfaces
    val Surface get() = LedgerAppearance.token("Surface", Color.White)
    val SurfaceInk get() = LedgerAppearance.token("SurfaceInk", Color(0xFF080C18))
    val SurfaceMuted get() = LedgerAppearance.token("SurfaceMuted", Color(0xFF505B70))
    val Caption get() = LedgerAppearance.token("Caption", Color(0xFF71798B))
    val Rule get() = LedgerAppearance.token("Rule", Color(0xFFECEEF2))
    val RuleSoft get() = LedgerAppearance.token("RuleSoft", Color(0x33697184))
    val Track get() = LedgerAppearance.token("Track", Color(0xFFEFF0F4))
    val TileBg get() = LedgerAppearance.token("TileBg", Color(0xFFF1F1F4))
    val ChipBg get() = LedgerAppearance.token("ChipBg", Color(0xFFEDF2F8))
    val ChipInk get() = LedgerAppearance.token("ChipInk", Color(0xFF345078))
    val SecondaryBg get() = LedgerAppearance.token("SecondaryBg", Color(0xFFEDF0F5))
    val SecondaryInk get() = LedgerAppearance.token("SecondaryInk", Color(0xFF182444))
    val DangerBg get() = LedgerAppearance.token("DangerBg", Color(0xFFFFF0F2))
    val DangerInk get() = LedgerAppearance.token("DangerInk", Color(0xFFA32442))
    val FieldBg get() = LedgerAppearance.token("FieldBg", Color(0xFFF6F7F9))
    val FieldBorder get() = LedgerAppearance.token("FieldBorder", Color(0xFFE2E6ED))
    val FieldInk get() = LedgerAppearance.token("FieldInk", Color(0xFF172137))
    val FieldLabel get() = LedgerAppearance.token("FieldLabel", Color(0xFF505B70))
    val SheetMuted get() = LedgerAppearance.token("SheetMuted", Color(0xFF677084))
    val Scrim get() = LedgerAppearance.token("Scrim", Color(0x8C030815))

    // Money
    val Positive get() = SpendingPreferences.gainColor
    val Negative get() = SpendingPreferences.lossColor
    val PositiveOnDark get() = SpendingPreferences.gainColor
    val NegativeOnDark get() = SpendingPreferences.lossColor

    // Budgets
    val BudgetOk get() = SpendingPreferences.gainColor
    val BudgetNear get() = Color(0xFF8A5200)
    val BudgetOver get() = SpendingPreferences.lossColor

    // Charts
    val IncomeBar get() = Color(0xFF172554)
    val ExpenseBar get() = LedgerAppearance.current.accent
    val ChartLabel get() = LedgerAppearance.token("ChartLabel", Color(0xFF56647B))
    val TrendGood get() = SpendingPreferences.gainColor
    val TrendCaution get() = SpendingPreferences.lossColor

    /** Spending heatmap ramp, lightest to darkest. */
    val Heat = listOf(
        Color(0xFFEFF6FF),
        Color(0xFFDBEAFE),
        Color(0xFF93C5FD),
        Color(0xFF3B82F6),
        Color(0xFF1D4ED8)
    )

    // Navigation pill
    val NavBg get() = LedgerAppearance.token("NavBg", Color(0xFFFFFFFF))
    val NavBorder get() = LedgerAppearance.token("NavBorder", Color(0x24080C18))
    val NavInk get() = LedgerAppearance.token("NavInk", Color(0xFF64748B))
    val NavActive get() = LedgerAppearance.token("NavActive", Color(0xFF2563EB))

    /** `linear-gradient(165deg,#05080e 0%,#142342 65%,#38467a 100%)` */
    val PageGradient: Brush get() = CssLinearGradient(
        angleDegrees = 165f,
        stops = listOf(0f to Bot.PageDeep, 0.65f to Bot.Paper, 1f to Bot.Surface)
    )
}

/**
 * CSS `linear-gradient(<angle>, …)`: 0° points up, angles turn clockwise, and
 * the gradient line is long enough that the first and last stops land exactly
 * on the box's corners. Compose's linearGradient takes explicit points, so the
 * line is derived from the size at draw time.
 */
@Immutable
class CssLinearGradient(
    private val angleDegrees: Float,
    private val stops: List<Pair<Float, Color>>
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val length = abs(size.width * dx) + abs(size.height * dy)
        val center = Offset(size.width / 2f, size.height / 2f)
        val half = Offset(dx * length / 2f, dy * length / 2f)
        return LinearGradientShader(
            from = center - half,
            to = center + half,
            colors = stops.map { it.second },
            colorStops = stops.map { it.first }
        )
    }

    override fun equals(other: Any?): Boolean =
        other is CssLinearGradient && other.angleDegrees == angleDegrees && other.stops == stops

    override fun hashCode(): Int = 31 * angleDegrees.hashCode() + stops.hashCode()
}

/** Headlines, money and row titles. */
val Barlow = FontFamily(
    Font(R.font.barlow_condensed_extralight, FontWeight.ExtraLight),
    Font(R.font.barlow_condensed_light, FontWeight.Light),
    Font(R.font.barlow_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold)
)

/** Supporting text, labels and controls. */
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)

private val NoPadding = PlatformTextStyle(includeFontPadding = false)
private val CenteredLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * Barlow Condensed style. [lineHeight] is a multiple of the size (CSS
 * unitless line-height); null keeps the font's own metrics.
 */
fun display(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
    lineHeight: Float? = null,
    color: Color = Color.Unspecified
): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextStyle.Default.lineHeight,
    lineHeightStyle = if (lineHeight != null) CenteredLines else null,
    platformStyle = NoPadding,
    color = color
)

/** DM Sans style; same conventions as [display]. */
fun body(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Float? = null,
    letterSpacing: Float = 0f,
    color: Color = Color.Unspecified
): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextStyle.Default.lineHeight,
    lineHeightStyle = if (lineHeight != null) CenteredLines else null,
    platformStyle = NoPadding,
    color = color
)

/** `.b-kicker`: 11px uppercase eyebrow with 1.8px tracking. */
fun kickerStyle(color: Color = Bot.PageMuted): TextStyle =
    body(11f, FontWeight.Medium, letterSpacing = 1.8f, color = color)

/** Tabular figures for amounts that line up in columns. */
internal val TabularNums = "tnum"

private fun botanicalColorScheme() = lightColorScheme(
    primary = Bot.ActionSolid,
    onPrimary = Color.White,
    secondary = Bot.ActionSolid,
    onSecondary = Color.White,
    background = Bot.PageDeep,
    onBackground = Bot.Ink,
    surface = Bot.Surface,
    onSurface = Bot.Ink,
    surfaceVariant = Bot.FieldBg,
    onSurfaceVariant = Bot.SurfaceMuted,
    outline = Bot.FieldBorder,
    error = Bot.Negative,
    onError = Color.White
)

/**
 * App theme. Keeps the existing font-scale clamp (0.85–1.3): every layout
 * pairs sp text with dp containers, and an unbounded accessibility scale
 * pushes labels out of their frames. Scrollables bounce past their edges
 * (iOS) instead of stretching.
 */
@Composable
fun BotanicalTheme(content: @Composable () -> Unit) {
    val perf = rememberPerfMode()
    val baseDensity = LocalDensity.current
    val clampedDensity = remember(baseDensity) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale
        )
    }
    CompositionLocalProvider(
        LocalPerfMode provides perf,
        LocalDensity provides clampedDensity,
        LocalOverscrollFactory provides rememberBounceOverscrollFactory()
    ) {
        MaterialTheme(colorScheme = botanicalColorScheme(), content = content)
    }
}

/** Letter spacing in em, for text that is later scaled to fit. */
internal fun emSpacing(px: Float, size: Float) = (px / size).em

