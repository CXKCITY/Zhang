package com.spendwise.app

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.spendwise.app.backup.AutoBackupWorker
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendwise.app.ui.ExpenseTrackerViewModel
import com.spendwise.app.ui.ExpenseTrackerViewModelFactory
import com.spendwise.app.ui.botanical.BotanicalTheme
import com.spendwise.app.ui.botanical.ExpenseTrackerApp
import com.spendwise.app.ui.botanical.LedgerAppearance

open class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Edge-to-edge with clear bars: the painted hero and the navy pages
        // run under the status bar. Icon colours follow the screen (see the
        // shell) — light over the dark tops, dark over Home's pale paper.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        var showLaunch by mutableStateOf(savedInstanceState == null && ValueAnimator.areAnimatorsEnabled())
        var startLaunch by mutableStateOf(false)
        if (showLaunch) {
            splash.setOnExitAnimationListener { screen ->
                screen.remove()
                startLaunch = true
            }
        }

        val appContainer = (application as ExpenseTrackerApplication).container

        LedgerAppearance.load(this)
        com.spendwise.app.ui.botanical.SpendingPreferences.load(this)
        com.spendwise.app.ui.botanical.CategoryOrder.load(this)
        setContent {
            val viewModel: ExpenseTrackerViewModel = viewModel(
                factory = ExpenseTrackerViewModelFactory(application, appContainer)
            )
            BotanicalTheme {
                Box(Modifier.fillMaxSize()) {
                    ExpenseTrackerApp(viewModel = viewModel, launchCoverVisible = showLaunch)
                    if (showLaunch) {
                        ReceiptLaunchScreen(start = startLaunch) {
                            showLaunch = false
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isChangingConfigurations) return
        val container = (application as ExpenseTrackerApplication).container
        lifecycleScope.launch {
            try {
                container.backupManager.saveStartupBackup()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w("LedgerBackup", "Unable to save startup backup", failure)
            }
        }
        AutoBackupWorker.onOpen(this)
    }

    override fun onStop() {
        super.onStop()
        // Changing the active launcher entry can dismiss its task.
        if (!isChangingConfigurations) ReceiptLauncherIcon.sync(this, LedgerAppearance.dark)
    }
}

class DarkLauncher : MainActivity()
