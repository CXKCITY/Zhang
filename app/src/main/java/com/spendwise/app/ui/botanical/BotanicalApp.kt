package com.spendwise.app.ui.botanical


import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendwise.app.backup.AutoBackupWorker
import com.spendwise.app.data.CategoryDeletion
import com.spendwise.app.domain.Category
import com.spendwise.app.export.BackupPreview
import com.spendwise.app.export.BackupResult
import com.spendwise.app.ui.DeleteCategoryResult
import com.spendwise.app.ui.ExpenseTrackerViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private object Routes {
    const val Home = "home"
    const val Calendar = "calendar"
    const val Insights = "insights"
    const val Accounts = "accounts"
    const val Categories = "categories"
    const val Recurring = "recurring"
    const val Settings = "settings"
}

private enum class Tab(val route: String, val label: String, val icon: () -> ImageVector) {
    Home(Routes.Home, "首页", { BotIcons.Home }),
    Calendar(Routes.Calendar, "消费日历", { BotIcons.Calendar }),
    Insights(Routes.Insights, "统计", { BotIcons.Insights })
}

private val TabRoutes = Tab.entries.map { it.route }.toSet()

private data class CategoryFormRequest(val categoryId: Long?, val income: Boolean)
private data class AccountFormRequest(val accountId: Long?)
private data class RuleFormRequest(val ruleId: Long?)
private data class BreakdownRequest(val categoryId: Long, val period: InsightsPeriod)

/** What the entry card was opened for: a new entry of [kind] on [date], or an existing one. */
private data class EntryRequest(
    val kind: EntryKind = EntryKind.Expense,
    val date: LocalDate? = null,
    val expenseId: Long = -1L,
    val transferId: Long = -1L
)

/** Keeps an open entry card across process death, like the route arguments it replaces. */
private val EntryRequestSaver = Saver<EntryRequest?, String>(
    save = { request -> request?.let { "${it.kind.name}|${it.date ?: ""}|${it.expenseId}|${it.transferId}" } },
    restore = { saved ->
        val parts = saved.split('|')
        runCatching {
            EntryRequest(
                kind = EntryKind.valueOf(parts[0]),
                date = parts[1].takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                expenseId = parts[2].toLong(),
                transferId = parts[3].toLong()
            )
        }.getOrNull()
    }
)

/**
 * Pages push in from the right over a parallax-shifted, dimmed page (iOS
 * navigation); popping reverses it, and the predictive back gesture scrubs
 * the pop under the finger. The three tabs crossfade.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs(): Boolean =
    initialState.destination.route in TabRoutes && targetState.destination.route in TabRoutes

private val pushEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (betweenTabs()) fadeIn(BotMotion.Fade)
    else slideInHorizontally(BotMotion.PageOffset) { it }
}
private val pushExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (betweenTabs()) fadeOut(BotMotion.Fade)
    else slideOutHorizontally(BotMotion.PageOffset) { -it / 4 } + fadeOut(BotMotion.PageFade, targetAlpha = 0.45f)
}
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (betweenTabs()) fadeIn(BotMotion.Fade)
    else slideInHorizontally(BotMotion.PageOffset) { -it / 4 } + fadeIn(BotMotion.PageFade, initialAlpha = 0.45f)
}
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (betweenTabs()) fadeOut(BotMotion.Fade)
    else slideOutHorizontally(BotMotion.PageOffset) { it }
}

/**
 * The app shell: navigation between the three tabs and the secondary pages,
 * the floating tab pill, and every sheet. Sheets live here so they cover the
 * tab pill and survive tab switches.
 */
@Composable
fun ExpenseTrackerApp(viewModel: ExpenseTrackerViewModel, launchCoverVisible: Boolean = false) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = remember { BotToastState() }
    suspend fun recordAction(block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: IllegalArgumentException) {
        e.message ?: "操作失败 请重试"
    } catch (e: IllegalStateException) {
        e.message ?: "操作失败 请重试"
    } catch (_: Exception) {
        "操作失败 请重试"
    }

    fun offerUndo(message: String, restore: suspend () -> Unit) {
        toast.show(message) {
            scope.launch {
                val error = recordAction { restore() }
                if (toast.undoAction == null) {
                    toast.show(if (error == null) "已撤销删除" else "撤销失败 记录未恢复")
                }
            }
        }
    }

    val rawState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val state = rawState.copy(categories = CategoryOrder.sort(rawState.categories))
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val aggregates by viewModel.monthlyAggregates.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val monthTransfers by viewModel.selectedMonthTransfers.collectAsStateWithLifecycle()
    val allRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val rules = allRules.filter { !it.isIncome }
    val entryCounts by viewModel.categoryEntryCounts.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val backupSettings by viewModel.autoBackupSettings.collectAsStateWithLifecycle()
    val activityRange by viewModel.activityRange.collectAsStateWithLifecycle()

    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = route in TabRoutes

    val incomeIds = remember(state.categories) { incomeCategoryIds(state.categories) }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val names = remember(state.accounts, state.archivedAccounts) { accountNames(state.accounts + state.archivedAccounts) }
    val monthItems = remember(state.expenses, monthTransfers, incomeIds) {
        ledgerItems(state.expenses, monthTransfers, incomeIds).filter { it.kind == EntryKind.Expense }
    }

    // ── Overlay state ───────────────────────────────────────────────────
    var periodPickerOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<LedgerItem?>(null) }
    var dayOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var breakdown by remember { mutableStateOf<BreakdownRequest?>(null) }
    var categoryForm by remember { mutableStateOf<CategoryFormRequest?>(null) }
    var migrationFor by remember { mutableStateOf<Long?>(null) }
    var accountForm by remember { mutableStateOf<AccountFormRequest?>(null) }
    var ruleForm by remember { mutableStateOf<RuleFormRequest?>(null) }
    var confirm by remember { mutableStateOf<ConfirmRequest?>(null) }
    var restoreReview by remember { mutableStateOf<RestoreReview?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }
    var homeHeroCovered by remember { mutableStateOf(false) }
    val editCache = remember { mutableMapOf<String, LedgerItem>() }

    // The entry card is a modal over the app, as on iOS: the app behind it
    // recedes (smaller, rounded, dimmed) while the card is up, following the
    // card as it's dragged. [entryModal] is 1 when the card is up, 0 when it
    // has left; the request is kept while it animates away.
    var entryRequest by rememberSaveable(stateSaver = EntryRequestSaver) { mutableStateOf<EntryRequest?>(null) }
    var entryShown by remember { mutableStateOf<EntryRequest?>(null) }
    val entryModal = remember { Animatable(0f) }
    LaunchedEffect(entryRequest) {
        val request = entryRequest
        if (request != null) {
            entryShown = request
            entryModal.animateTo(1f, BotMotion.SheetOpen)
        } else if (entryShown != null) {
            try {
                entryModal.animateTo(0f, BotMotion.SheetClose)
            } finally {
                if (entryRequest == null) entryShown = null
            }
        }
    }

    // Activity filters live here so the filter sheet can sit above the tab pill.
    var activityKind by rememberSaveable { mutableStateOf(ActivityKind.Expense) }
    var activitySearch by rememberSaveable { mutableStateOf("") }
    var activityAccount by rememberSaveable { mutableStateOf<Long?>(null) }
    var activityCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    val activityQuery = ActivityQuery(activityKind, activitySearch, activityAccount, activityCategory)
    val clearActivityFilters = {
        activityKind = ActivityKind.Expense
        activitySearch = ""
        activityAccount = null
        activityCategory = null
        viewModel.setActivityRange(null)
    }

    var homeFlipTarget by remember { mutableStateOf<YearMonth?>(null) }
    var homeFlipDirection by remember { mutableStateOf(0) }
    var homeShortcutTarget by remember { mutableStateOf<YearMonth?>(null) }
    val changeMonth: (YearMonth) -> Unit = {
        homeShortcutTarget = null
        homeFlipTarget = null
        viewModel.setSelectedMonth(it)
        viewModel.setActivityRange(null)
    }
    val goTab: (String) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openEntry: (EntryKind, LocalDate) -> Unit = { kind, date ->
        entryRequest = EntryRequest(kind = kind, date = date)
    }
    val editItem: (LedgerItem) -> Unit = { item ->
        editCache[item.key] = item
        detail = null
        entryRequest = when (item) {
            is LedgerItem.Entry -> EntryRequest(expenseId = item.expense.id)
            is LedgerItem.Move -> EntryRequest(transferId = item.transfer.id)
        }
    }
    val openCategoryForm: (Category?, Boolean) -> Unit = { category, income ->
        categoryForm = CategoryFormRequest(category?.id, category?.isIncomeAdjustment ?: income)
    }

    // ── Launchers ───────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val reviewBackup: (Uri, String) -> Unit = { uri, name ->
        scope.launch {
            when (val preview = viewModel.previewBackup(uri)) {
                is BackupPreview.Ready -> restoreReview = RestoreReview(uri, name, preview)
                is BackupPreview.Invalid -> toast.show(preview.reason)
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) reviewBackup(uri, displayName(context, uri))
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, AutoBackupWorker.URI_PERMISSION_FLAGS)
            viewModel.setAutoBackupFolder(uri)
            viewModel.setAutoBackupEnabled(true)
            toast.show("已选择备份文件夹 自动备份已开启")
        }
    }
    val saveCopy = {
        val stamp = LocalDateTime.now(KL_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        exportLauncher.launch("qingji-backup-$stamp.json")
    }

    // ── Messages ────────────────────────────────────────────────────────
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    LaunchedEffect(backupResult) {
        val result = backupResult ?: return@LaunchedEffect
        toast.show(
            when (result) {
                is BackupResult.ExportSuccess -> "备份已保存 · ${result.expenseCount} 笔记录"
                is BackupResult.ImportSuccess -> "已合并备份 · 新增 ${result.expenseCount} 笔记录"
                is BackupResult.Failure -> result.reason
            }
        )
        viewModel.clearBackupResult()
    }
    val caughtUp by viewModel.recurringCatchUpCount.collectAsStateWithLifecycle()
    LaunchedEffect(caughtUp) {
        val n = caughtUp ?: return@LaunchedEffect
        toast.show("已补记 $n 笔周期记录")
        viewModel.clearRecurringCatchUp()
    }
    val message by viewModel.userMessage.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        toast.show(text)
        viewModel.clearUserMessage()
    }

    // Due recurring entries catch up whenever the app returns to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) viewModel.runRecurringCatchUp()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Light status icons over the dark tops of every page; dark ones once
    // Home's paper has scrolled under the status bar. The navigation handle
    // is dark over Home's paper and light over the navy pages.
    val activity = LocalActivity.current
    val view = LocalView.current
    val entryUp = entryShown != null
    val paperUnderStatusBar = route == Routes.Home && homeHeroCovered && !entryUp
    val lightLaunchBars = !LedgerAppearance.dark
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = if (launchCoverVisible) lightLaunchBars
                else !LedgerAppearance.dark && (route != Routes.Home || paperUnderStatusBar)
            isAppearanceLightNavigationBars = if (launchCoverVisible) lightLaunchBars
                else !LedgerAppearance.dark && !entryUp
        }
    }
    val statusScrim by animateColorAsState(
        targetValue = when (route) {
            Routes.Home -> if (homeHeroCovered) Bot.Paper else LedgerAppearance.current.heroStart
            else -> Bot.PageDeep.copy(alpha = 0.95f)
        },
        animationSpec = BotMotion.smooth(0.3f),
        label = "statusScrim"
    )
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val recessTop = with(density) { (statusTop + 8.dp).toPx() }
    val recessRadius = with(density) { 20.dp.toPx() }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBottom = maxOf(20.dp, navInset)
    val tabClearance = 70.dp + navBottom + 16.dp
    val pageClearance = navInset + 32.dp

    val navBackdrop = rememberNavBackdrop()
    val edgePreviews = mapOf(
        Routes.Home to rememberNavBackdrop(),
        Routes.Settings to rememberNavBackdrop(),
    )
    val edgeImages = remember { mutableMapOf<String, ImageBitmap>() }
    var capturingEdgePage by remember { mutableStateOf(false) }
    fun openPage(destination: String) {
        if (capturingEdgePage) return
        val source = route
        val layer = edgePreviews[source]?.layer
        if (source == null || layer == null || layer.size.width == 0) {
            navController.navigate(destination)
            return
        }
        capturingEdgePage = true
        scope.launch {
            try {
                edgeImages[source] = layer.toImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                edgeImages.remove(source)
            } finally {
                capturingEdgePage = false
            }
            if (navController.currentBackStackEntry == backStack) navController.navigate(destination)
        }
    }
    val edgeOffset = remember(route) { mutableStateOf(0f) }
    var edgePopFrom by remember { mutableStateOf<String?>(null) }
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    CompositionLocalProvider(LocalToast provides toast) {
        Box(Modifier.fillMaxSize().background(Bot.PageDeep)) {
            // Everything under the entry card: it recedes while the card is up.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = entryModal.value.coerceIn(0f, 1f)
                        if (p > 0f) {
                            val scale = 1f - 0.08f * p
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            translationY = recessTop * p
                            shape = RoundedCornerShape(recessRadius * p)
                            clip = true
                        }
                    }
            ) {
                Box(Modifier.fillMaxSize().drawBehind {
                    if (edgeOffset.value > 0f) {
                        edgeImages[previousRoute]?.let { drawImage(it) }
                    }
                }) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.Home,
                    modifier = Modifier
                        .edgeSwipeBack(
                            enabled = !onTab && previousRoute != null && !entryUp,
                            page = route,
                            offset = edgeOffset,
                            scope = scope,
                            onBack = {
                                if (navController.currentBackStackEntry == backStack) {
                                    edgePopFrom = route
                                    navController.popBackStack()
                                    scope.launch {
                                        kotlinx.coroutines.delay(350)
                                        if (edgePopFrom == route) edgePopFrom = null
                                    }
                                }
                            },
                        )
                        .graphicsLayer { translationX = edgeOffset.value }
                        .recordNavBackdrop(if (onTab || route == null) navBackdrop else null)
                        .drawWithContent {
                            val preview = edgePreviews[route]
                            if (preview != null && edgeOffset.value == 0f) {
                                preview.layer.record { this@drawWithContent.drawContent() }
                                drawLayer(preview.layer)
                            } else drawContent()
                        },
                    enterTransition = pushEnter,
                    exitTransition = pushExit,
                    popEnterTransition = {
                        if (initialState.destination.route == edgePopFrom) EnterTransition.None else popEnter(this)
                    },
                    popExitTransition = {
                        if (initialState.destination.route == edgePopFrom) ExitTransition.None else popExit(this)
                    }
                ) {
                    composable(Routes.Home) {
                        val loadedMonth = state.summary.month
                        val shortcut = remember(loadedMonth) { homeShortcutTarget == loadedMonth }
                        val enteringDirection = remember(loadedMonth) {
                            if (homeFlipTarget == loadedMonth) homeFlipDirection else 0
                        }
                        LaunchedEffect(loadedMonth) {
                            if (homeFlipTarget == loadedMonth) homeFlipTarget = null
                            if (homeShortcutTarget == loadedMonth) homeShortcutTarget = null
                        }
                        AnimatedContent(
                            targetState = Triple(state, monthItems, shortcut),
                            contentKey = { it.first.summary.month },
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            transitionSpec = {
                                if (!targetState.third) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else {
                                    val older = targetState.first.summary.month < initialState.first.summary.month
                                    val motion = tween<IntOffset>(durationMillis = 190)
                                    slideInVertically(motion) { if (older) it else -it } togetherWith
                                        slideOutVertically(motion) { if (older) -it else it }
                                }
                            },
                            label = "Home month"
                        ) { (page, items, _) ->
                            val pageMonth = page.summary.month
                            val isCurrentPage = month == pageMonth
                            val flipMonth: (Int) -> Unit = { direction ->
                                val next = pageMonth.plusMonths(direction.toLong())
                                if (month == pageMonth && homeFlipTarget != next &&
                                    next <= YearMonth.from(todayKl())
                                ) {
                                    homeFlipDirection = direction
                                    homeFlipTarget = next
                                    viewModel.setSelectedMonth(next)
                                }
                            }
                            val spentByCategory = remember(items) {
                                items.groupingBy { (it as LedgerItem.Entry).expense.categoryId }
                                    .fold(0L) { amount, item -> amount + item.cents }
                            }
                            HomeScreen(
                                month = pageMonth,
                                items = items,
                                categories = page.categories,
                                accounts = page.accounts + page.archivedAccounts,
                                query = activityQuery,
                                range = activityRange,
                                bottomPadding = tabClearance,
                                canFlipMonth = isCurrentPage,
                                enteringDirection = remember(pageMonth) { enteringDirection },
                                onFlipMonth = flipMonth,
                                onJumpMonth = { target ->
                                    if (month == pageMonth && homeShortcutTarget != target) {
                                        homeFlipTarget = null
                                        homeShortcutTarget = target
                                        viewModel.setSelectedMonth(target)
                                    }
                                },
                                onQueryChange = {
                                    activityKind = it.kind
                                    activitySearch = it.search
                                    activityAccount = it.accountId
                                    activityCategory = it.categoryId
                                },
                                onClearRange = { viewModel.setActivityRange(null) },
                                onClearAll = clearActivityFilters,
                                hero = {
                                    HomeHero(
                                        month = pageMonth,
                                        totalExpense = page.summary.totalExpenseCents,
                                        categories = page.categories,
                                        spentByCategory = spentByCategory,
                                        onMonthChange = { target ->
                                            homeFlipTarget = null
                                            viewModel.setSelectedMonth(target)
                                        },
                                        onOpenPeriodPicker = { periodPickerOpen = true },
                                        onOpenSettings = { openPage(Routes.Settings) },
                                        onOpenCategories = { openPage(Routes.Categories) },
                                        onCategory = { breakdown = BreakdownRequest(it.id, InsightsPeriod.OfMonth(pageMonth)) },
                                        onAdd = { openEntry(EntryKind.Expense, todayKl()) }
                                    )
                                },
                                onHeroCoveredChange = { if (pageMonth == state.summary.month) homeHeroCovered = it },
                                onAdd = { openEntry(EntryKind.Expense, todayKl()) },
                                onOpenFilters = { filtersOpen = true },
                                onOpenItem = { detail = it },
                                onManage = { ids, categoryId ->
                                    recordAction {
                                        val restore = viewModel.manageExpenses(ids, categoryId)
                                        if (restore == null) toast.show("已修改分类 ${ids.size} 笔记录")
                                        else offerUndo("已删除 ${ids.size} 笔记录", restore)
                                    }
                                }
                            )
                        }
                    }
                    composable(Routes.Calendar) {
                        SpendingCalendarScreen(
                            month = month,
                            dailyTotals = state.summary.dailyTotals,
                            monthSpend = state.summary.totalExpenseCents,
                            bottomPadding = tabClearance,
                            onMonthChange = changeMonth,
                            onOpenPeriodPicker = { periodPickerOpen = true },
                            onDay = { dayOpen = it.toString() }
                        )
                    }
                    composable(Routes.Insights) {
                        val yearExpenses by viewModel.insightsYearExpenses.collectAsStateWithLifecycle()
                        InsightsScreen(
                            state = state,
                            month = month,
                            yearExpenses = yearExpenses,
                            aggregates = aggregates,
                            bottomPadding = tabClearance,
                            onMonthChange = changeMonth,
                            onOpenPeriodPicker = { periodPickerOpen = true },
                            onCategory = { category, period -> breakdown = BreakdownRequest(category.id, period) },
                            onDay = { dayOpen = it.toString() },
                            onExportCsv = { period ->
                                when (period) {
                                    is InsightsPeriod.OfMonth -> viewModel.exportCsv(
                                        monthLabel(period.month),
                                        period.month.toString(),
                                        period.month.atDay(1),
                                        period.month.plusMonths(1).atDay(1)
                                    )
                                    is InsightsPeriod.OfYear -> viewModel.exportCsv(
                                        period.year.toString(),
                                        period.year.toString(),
                                        LocalDate.of(period.year, 1, 1),
                                        LocalDate.of(period.year + 1, 1, 1)
                                    )
                                }
                            }
                        )
                    }
                    composable(Routes.Accounts) {
                        AccountsScreen(
                            accounts = state.accounts,
                            archivedAccounts = state.archivedAccounts,
                            totalBalance = state.totalBalanceCents,
                            bottomPadding = pageClearance,
                            onBack = { navController.popBackStack() },
                            onAdd = { accountForm = AccountFormRequest(null) },
                            onEdit = { accountForm = AccountFormRequest(it.id) },
                            onRestore = {
                                viewModel.unarchiveAccount(it.id)
                                toast.show("账户已恢复")
                            }
                        )
                    }
                    composable(Routes.Categories) {
                        CategoriesScreen(
                            categories = state.categories,
                            budgets = state.budgets,
                            month = month,
                            spentByCategory = remember(state.summary) {
                                (state.summary.categoryTotals + state.summary.incomeCategoryTotals)
                                    .associate { it.categoryId to it.totalCents }
                            },
                            bottomPadding = pageClearance,
                            onBack = { navController.popBackStack() },
                            onAdd = { income -> openCategoryForm(null, income) },
                            onEdit = { openCategoryForm(it, it.isIncomeAdjustment) }
                        )
                    }
                    composable(Routes.Recurring) {
                        RecurringScreen(
                            rules = rules,
                            categories = state.categories,
                            bottomPadding = pageClearance,
                            onBack = { navController.popBackStack() },
                            onAdd = { ruleForm = RuleFormRequest(null) },
                            onEdit = { ruleForm = RuleFormRequest(it.id) },
                            onToggle = { rule ->
                                viewModel.setRecurringRulePaused(rule.id, paused = !rule.isPaused)
                                toast.show(if (rule.isPaused) "周期记账已恢复 暂停期间的日期已跳过" else "周期记账已暂停")
                            },
                            onCheck = viewModel::checkDueRecurring
                        )
                    }
                    composable(Routes.Settings) {
                        SettingsScreen(
                            backup = backupSettings,
                            bottomPadding = pageClearance,
                            onBack = { navController.popBackStack() },
                            onAccounts = { openPage(Routes.Accounts) },
                            onCategories = { openPage(Routes.Categories) },
                            onRecurring = { openPage(Routes.Recurring) },
                            onBackupNow = {
                                val folder = backupSettings.treeUri
                                if (folder != null) {
                                    viewModel.backupNow()
                                    toast.show("正在备份到 ${backupFolderName(folder)}")
                                } else {
                                    saveCopy()
                                }
                            },
                            onRestore = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                            onRestoreStartup = { reviewBackup(viewModel.startupBackupUri(), "最近自动备份") },
                            onToggleDaily = { enable ->
                                when {
                                    enable && backupSettings.treeUri == null -> folderLauncher.launch(null)
                                    else -> {
                                        viewModel.setAutoBackupEnabled(enable)
                                        toast.show(if (enable) "自动备份已开启" else "自动备份已暂停")
                                    }
                                }
                            },
                            onChooseFolder = { folderLauncher.launch(null) },
                            onDownloadCopy = saveCopy
                        )
                    }
                }

                }

                // Keeps status-bar icons legible over content scrolling beneath them.
                Box(Modifier.fillMaxWidth().height(statusTop).background(statusScrim))

                // The tab pill drops away on pages and rises back on the tabs. It's
                // simply there at launch (the start destination is a tab).
                val lastTab = rememberRetained(route?.takeIf { it in TabRoutes })
                val navShown = remember { MutableTransitionState(true) }
                navShown.targetState = onTab || route == null
                AnimatedVisibility(
                    visibleState = navShown,
                    enter = slideInVertically(BotMotion.smooth(0.42f, IntOffset.VisibilityThreshold)) { it * 2 } + fadeIn(BotMotion.Fade),
                    exit = slideOutVertically(BotMotion.smooth(0.34f, IntOffset.VisibilityThreshold)) { it * 2 } + fadeOut(BotMotion.Fade),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = navBottom)
                ) {
                    BottomNav(active = lastTab, onSelect = goTab, backdrop = navBackdrop)
                }
            }

            // ── Entry card ─────────────────────────────────────────────────
            val shownEntry = entryShown
            if (shownEntry != null) {
                // Dims the receded app and keeps its touches off while the card is up.
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(Color.Black, alpha = 0.34f * entryModal.value.coerceIn(0f, 1f)) }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                )
                val target = remember(shownEntry) {
                    when {
                        shownEntry.expenseId > 0 -> {
                            val id = shownEntry.expenseId
                            val known = editCache["e$id"] as? LedgerItem.Entry
                                ?: (monthItems.firstOrNull { it.key == "e$id" } as? LedgerItem.Entry)
                                ?: recentExpenses.firstOrNull { it.id == id }
                                    ?.let { LedgerItem.Entry(it, it.categoryId in incomeIds) }
                            known?.let { EntryTarget.EditExpense(it.expense, it.isIncome) }
                        }
                        shownEntry.transferId > 0 -> {
                            val id = shownEntry.transferId
                            val known = editCache["t$id"] as? LedgerItem.Move
                                ?: (monthItems.firstOrNull { it.key == "t$id" } as? LedgerItem.Move)
                            known?.let { EntryTarget.EditTransfer(it.transfer) }
                        }
                        else -> EntryTarget.New(kind = shownEntry.kind, date = shownEntry.date ?: todayKl())
                    }
                }
                if (target == null) {
                    // The entry vanished (deleted elsewhere, or the process was restored).
                    LaunchedEffect(Unit) { entryRequest = null }
                } else {
                    key(shownEntry) {
                        EntryScreen(
                            target = target,
                            categories = state.categories,
                            accounts = state.accounts,
                            recentExpenses = recentExpenses,
                            formError = formError,
                            presentation = entryModal,
                            active = entryRequest != null,
                            onClearError = viewModel::clearFormError,
                            onSaveExpense = viewModel::saveExpense,
                            onNoteSuggestions = viewModel::noteSuggestions,
                            onSaveTransfer = viewModel::saveTransfer,
                            onCreateCategory = viewModel::createCategory,
                            onClose = { entryRequest = null },
                            onSaved = { date, updated ->
                                entryRequest = null
                                changeMonth(YearMonth.from(date))
                                if (!updated) goTab(Routes.Home)
                                toast.show(if (updated) "记录已更新" else "记录已保存")
                            }
                        )
                    }
                }
            }

            // ── Sheets ─────────────────────────────────────────────────────
            val dayDate = dayOpen?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            DaySheet(
                date = dayDate,
                items = remember(monthItems, dayDate) { monthItems.filter { it.date == dayDate } },
                categoriesById = categoriesById,
                names = names,
                onOpen = { detail = it },
                onAdd = { date ->
                    dayOpen = null
                    openEntry(EntryKind.Expense, date)
                },
                onDismiss = { dayOpen = null }
            )

            val breakdownCategory = breakdown?.let { request -> categoriesById[request.categoryId] }
            val yearForBreakdown = (breakdown?.period as? InsightsPeriod.OfYear)
            val breakdownYear = if (yearForBreakdown != null) {
                viewModel.insightsYearExpenses.collectAsStateWithLifecycle().value
            } else emptyList()
            BreakdownSheet(
                category = breakdownCategory,
                periodLabel = breakdown?.period?.label().orEmpty(),
                items = remember(breakdown, state.expenses, breakdownYear, incomeIds) {
                    val request = breakdown
                    if (request == null) emptyList() else {
                        val source = if (request.period is InsightsPeriod.OfYear) breakdownYear else state.expenses
                        ledgerItems(source.filter { it.categoryId == request.categoryId }, emptyList(), incomeIds)
                    }
                },
                categoriesById = categoriesById,
                names = names,
                onOpen = { detail = it },
                onEditCategory = {
                    breakdown = null
                    openCategoryForm(it, it.isIncomeAdjustment)
                },
                onDismiss = { breakdown = null }
            )

            EntryDetailSheet(
                item = detail?.let { current -> monthItems.firstOrNull { it.key == current.key } ?: current },
                categoriesById = categoriesById,
                names = names,
                loadHistory = viewModel::merchantHistoryFor,
                onEdit = { item ->
                    dayOpen = null
                    breakdown = null
                    editItem(item)
                },
                onDelete = { item ->
                    confirm = ConfirmRequest(
                        title = "删除这笔记录",
                        body = "删除后可在底部提示中撤销",
                        action = "删除记录"
                    ) {
                        scope.launch {
                            val error = recordAction {
                                val restore = when (item) {
                                    is LedgerItem.Entry -> viewModel.deleteExpense(item.expense.id)
                                    is LedgerItem.Move -> viewModel.deleteTransfer(item.transfer.id)
                                }
                                if (detail?.key == item.key) detail = null
                                offerUndo("记录已删除", restore)
                            }
                            if (error != null) toast.show(error)
                        }
                    }
                },
                onOpenHistory = { expense -> detail = LedgerItem.Entry(expense, expense.categoryId in incomeIds) },
                onDismiss = { detail = null }
            )

            val editingCategory = categoryForm?.categoryId?.let { categoriesById[it] }
            val categoryUsed = editingCategory != null &&
                ((entryCounts[editingCategory.id] ?: 0) > 0 || rules.any { it.categoryId == editingCategory.id })
            CategoryFormSheet(
                visible = categoryForm != null,
                existing = editingCategory,
                initialIncome = categoryForm?.income ?: false,
                budgetCents = editingCategory?.let { c -> state.budgets.firstOrNull { it.categoryId == c.id }?.monthlyLimitCents } ?: 0L,
                used = categoryUsed,
                onSave = { name, iconName, color, isIncome, budget ->
                    val existing = editingCategory
                    val error = if (existing == null) {
                        viewModel.createCategory(name, iconName, color, isIncome, budget)
                    } else {
                        viewModel.updateCategory(existing.id, name, iconName, color, isIncome, budget)
                    }
                    if (error == null) {
                        categoryForm = null
                        toast.show("分类已保存")
                    }
                    error
                },
                onDelete = editingCategory?.let { category ->
                    {
                        if (!category.isIncomeAdjustment && state.categories.count { !it.isIncomeAdjustment } <= 1) {
                            toast.show("至少保留一个支出分类")
                        } else if (categoryUsed) {
                            categoryForm = null
                            migrationFor = category.id
                        } else {
                            confirm = ConfirmRequest(
                                title = "删除 ${category.name}",
                                body = "此分类没有关联记录 可以删除",
                                action = "删除分类"
                            ) {
                                scope.launch {
                                    when (val result = viewModel.deleteCategory(category.id)) {
                                        DeleteCategoryResult.Deleted -> {
                                            categoryForm = null
                                            toast.show("已删除 ${category.name}")
                                        }
                                        is DeleteCategoryResult.Blocked -> toast.show(result.reason)
                                        is DeleteCategoryResult.NeedsStrategy -> {
                                            categoryForm = null
                                            migrationFor = category.id
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                onDismiss = { categoryForm = null }
            )

            val migrating = migrationFor?.let { categoriesById[it] }
            CategoryMigrationSheet(
                category = migrating,
                entryCount = migrating?.let { entryCounts[it.id] } ?: 0,
                ruleCount = migrating?.let { c -> rules.count { it.categoryId == c.id } } ?: 0,
                destinations = migrating?.let { c ->
                    state.categories.filter { it.id != c.id && it.isIncomeAdjustment == c.isIncomeAdjustment }
                }.orEmpty(),
                hasBudget = migrating?.let { c -> state.budgets.any { it.categoryId == c.id } } ?: false,
                onCancel = {
                    val id = migrationFor
                    migrationFor = null
                    if (id != null) categoryForm = CategoryFormRequest(id, migrating?.isIncomeAdjustment ?: false)
                },
                onMove = { destination ->
                    val category = migrating ?: return@CategoryMigrationSheet
                    scope.launch {
                        val error = recordAction {
                            check(viewModel.deleteCategoryWithStrategy(category.id, CategoryDeletion.Migrate(destination))) { "分类未删除 至少保留一个支出分类" }
                        }
                        if (error == null) {
                            migrationFor = null
                            toast.show("已迁移记录并删除 ${category.name}")
                        } else toast.show(error)
                    }
                },
                onDeleteAll = {
                    val category = migrating ?: return@CategoryMigrationSheet
                    scope.launch {
                        val error = recordAction {
                            check(viewModel.deleteCategoryWithStrategy(category.id, CategoryDeletion.DeleteExpenses)) { "分类未删除 至少保留一个支出分类" }
                        }
                        if (error == null) {
                            migrationFor = null
                            toast.show("已删除 ${category.name} 及关联记录")
                        } else toast.show(error)
                    }
                }
            )

            val editingAccount = accountForm?.accountId?.let { id ->
                (state.accounts + state.archivedAccounts).firstOrNull { it.id == id }
            }
            AccountFormSheet(
                visible = accountForm != null,
                existing = editingAccount,
                onSave = { name, type, starting, iconName, color ->
                    val existing = editingAccount
                    val error = if (existing == null) {
                        viewModel.createAccount(name, type, starting, iconName, color)
                    } else {
                        viewModel.updateAccount(existing.id, name, type, starting, iconName, color)
                    }
                    if (error == null) {
                        accountForm = null
                        toast.show("账户已保存")
                    }
                    error
                },
                onArchive = {
                    val account = editingAccount
                    if (account == null) null else {
                        val error = viewModel.archiveAccount(account.id)
                        if (error == null) {
                            accountForm = null
                            toast.show("账户已归档")
                        }
                        error
                    }
                },
                onRestore = {
                    editingAccount?.let { viewModel.unarchiveAccount(it.id) }
                    accountForm = null
                    toast.show("账户已恢复")
                },
                onDismiss = { accountForm = null }
            )

            val editingRule = ruleForm?.ruleId?.let { id -> rules.firstOrNull { it.id == id } }
            RuleFormSheet(
                visible = ruleForm != null,
                existing = editingRule,
                categories = state.categories,
                accounts = state.accounts,
                onSave = { id, amount, categoryId, accountId, name, notes, cadence, firstDate ->
                    val error = viewModel.saveRecurringRule(id, amount, categoryId, accountId, name, notes, cadence, firstDate)
                    if (error == null) {
                        ruleForm = null
                        toast.show("周期记账已保存")
                    }
                    error
                },
                onDelete = editingRule?.let { rule ->
                    {
                        confirm = ConfirmRequest(
                            title = "删除周期记账",
                            body = "已有交易记录会保留 此规则不再自动记账",
                            action = "删除规则"
                        ) {
                            viewModel.deleteRecurringRule(rule.id)
                            ruleForm = null
                            toast.show("周期规则已删除")
                        }
                    }
                },
                onDismiss = { ruleForm = null }
            )

            FilterSheet(
                visible = filtersOpen,
                month = month,
                query = activityQuery,
                range = activityRange,
                accounts = state.accounts,
                archivedAccounts = state.archivedAccounts,
                categories = state.categories,
                monthItems = monthItems,
                loadEntries = viewModel::loadEntries,
                onApply = { accountId, categoryId, range ->
                    activityAccount = accountId
                    activityCategory = categoryId
                    viewModel.setActivityRange(range)
                    if (range != null && (month.atEndOfMonth() < range.from || month.atDay(1) > range.to)) {
                        viewModel.setSelectedMonth(YearMonth.from(range.to))
                    }
                    filtersOpen = false
                },
                onDismiss = { filtersOpen = false }
            )

            RestoreReviewSheet(
                review = restoreReview,
                onConfirm = { review ->
                    restoreReview = null
                    viewModel.importBackup(review.uri)
                },
                onDismiss = { restoreReview = null }
            )

            ConfirmSheet(request = confirm, onDismiss = { confirm = null })

            CalendarPicker(
                visible = periodPickerOpen,
                mode = CalendarMode.Month,
                initial = month.atDay(1),
                onCancel = { periodPickerOpen = false },
                onConfirm = {
                    changeMonth(YearMonth.from(it))
                    periodPickerOpen = false
                }
            )

            BotToastHost(
                state = toast,
                bottomOffset = if (onTab) navBottom + 80.dp else navBottom + 12.dp
            )
        }
    }
}

/**
 * The floating tab pill: Home, Calendar, Insights, each with a label. The
 * dark selection glides from tab to tab on a lively spring, stretching a
 * little with its speed, and each label turns white as it passes beneath.
 */
@Composable
private fun BottomNav(
    active: String?,
    onSelect: (String) -> Unit,
    backdrop: NavBackdrop?,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val index = Tab.entries.indexOfFirst { it.route == active }.coerceAtLeast(0)
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val currentIndex by rememberUpdatedState(index)
    val selectTab by rememberUpdatedState(onSelect)
    val position by animateFloatAsState(
        dragPosition ?: index.toFloat(),
        spring(dampingRatio = 0.72f, stiffness = stiffnessFor(0.31f)),
        label = "navLensPosition"
    )
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(shape)
            .navGlass(backdrop)
            .border(1.dp, Brush.verticalGradient(listOf(
                Color.White.copy(alpha = if (LedgerAppearance.dark) 0.34f else 0.95f),
                Bot.NavBorder,
                Bot.NavBorder.copy(alpha = 0.8f)
            )), shape)
            .pointerInput(Unit) {
                fun positionAt(x: Float) =
                    (x / size.width.coerceAtLeast(1) * Tab.entries.size - 0.5f)
                        .coerceIn(0f, Tab.entries.lastIndex.toFloat())
                detectHorizontalDragGestures(
                    onDragStart = { dragPosition = positionAt(it.x) },
                    onDragCancel = { dragPosition = null },
                    onDragEnd = {
                        val target = dragPosition?.let { (it + 0.5f).toInt() }
                        dragPosition = null
                        if (target != null && target != currentIndex) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            selectTab(Tab.entries[target].route)
                        }
                    }
                ) { change, _ ->
                    change.consume()
                    dragPosition = positionAt(change.position.x)
                }
            }
            .padding(5.dp)
            .drawBehind {
                val gap = 4.dp.toPx()
                val count = Tab.entries.size
                val tab = (size.width - gap * (count - 1)) / count
                val stretch = if (dragPosition == null) 0f else 0.08f
                val width = tab * (1f + stretch)
                val left = position.coerceIn(0f, Tab.entries.lastIndex.toFloat()) * (tab + gap) - (width - tab) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(
                        lerp(Bot.NavActive, Color.White, 0.15f), Bot.NavActive, Bot.NavActive
                    )),
                    topLeft = Offset(left, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tab.entries.forEachIndexed { i, tab ->
            val selected = tab.route == active
            val cover = (1f - abs(position - i)).coerceIn(0f, 1f)
            val ink = lerp(Bot.NavInk, Color.White, cover)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .botPress(scale = 1f, role = Role.Tab) {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(tab.route)
                        }
                    }
                    .semantics {
                        this.selected = selected
                        contentDescription = tab.label
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BotIcon(tab.icon(), size = 22.dp, tint = ink)
                Text(tab.label, style = body(12f, FontWeight.Medium, lineHeight = 1.2f), color = ink, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: "备份文件"

