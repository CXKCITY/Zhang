package com.spendwise.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.os.Build

internal object ReceiptLauncherIcon {
    fun sync(context: Context, dark: Boolean) {
        val lightEntry = ComponentName(context, MainActivity::class.java)
        val darkEntry = ComponentName(context, DarkLauncher::class.java)
        val enabled = if (dark) darkEntry else lightEntry
        val disabled = if (dark) lightEntry else darkEntry
        val manager = context.packageManager
        val currentEnabled = manager.getComponentEnabledSetting(enabled)
        val currentDisabled = manager.getComponentEnabledSetting(disabled)
        val enableNeeded = currentEnabled == COMPONENT_ENABLED_STATE_DISABLED ||
            dark && currentEnabled != COMPONENT_ENABLED_STATE_ENABLED
        val disableNeeded = currentDisabled == COMPONENT_ENABLED_STATE_ENABLED ||
            dark && currentDisabled != COMPONENT_ENABLED_STATE_DISABLED
        if (!enableNeeded && !disableNeeded) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.setComponentEnabledSettings(
                listOf(
                    PackageManager.ComponentEnabledSetting(enabled, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP),
                    PackageManager.ComponentEnabledSetting(disabled, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP),
                )
            )
        } else {
            // Make the new entry available before disabling the old one.
            if (enableNeeded) manager.setComponentEnabledSetting(enabled, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
            if (disableNeeded) manager.setComponentEnabledSetting(disabled, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
        }
    }
}
