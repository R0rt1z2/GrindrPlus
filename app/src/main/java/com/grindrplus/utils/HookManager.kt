package com.grindrplus.utils

import com.grindrplus.core.Config
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import com.grindrplus.hooks.AllowScreenshots
import com.grindrplus.hooks.AntiBlock
import com.grindrplus.hooks.AntiDetection
import com.grindrplus.hooks.BanManagement
import com.grindrplus.hooks.ChatIndicators
import com.grindrplus.hooks.ChatTerminal
import com.grindrplus.hooks.DisableAnalytics
import com.grindrplus.hooks.DisableBoosting
import com.grindrplus.hooks.DisableShuffle
import com.grindrplus.hooks.DisableUpdates
import com.grindrplus.hooks.EmptyCalls
import com.grindrplus.hooks.EnableUnlimited
import com.grindrplus.hooks.ExpiringMedia
import com.grindrplus.hooks.FeatureGranting
import com.grindrplus.hooks.LocalSavedPhrases
import com.grindrplus.hooks.LocationSpoofer
import com.grindrplus.hooks.NotificationAlerts
import com.grindrplus.hooks.OnlineIndicator
import com.grindrplus.hooks.ProfileDetails
import com.grindrplus.hooks.ProfileViews
import com.grindrplus.hooks.QuickBlock
import com.grindrplus.hooks.StatusDialog
import com.grindrplus.hooks.TimberLogging
import com.grindrplus.hooks.UnlimitedAlbums
import com.grindrplus.hooks.UnlimitedProfiles
import com.grindrplus.hooks.UnlockExplorer
import com.grindrplus.hooks.WebSocketAlive
import kotlin.reflect.KClass

class HookManager {
    private var hooks = mutableMapOf<KClass<out Hook>, Hook>()

    fun registerHooks(init: Boolean = true) {
        // Xposed hook registration via XposedHelpers.findAndHookMethod is synchronous reflection.
        // It must run on the calling thread (Xposed class-loading context) — no coroutine dispatcher.
        val hookList = listOf(
            AllowScreenshots(),
            AntiBlock(),
            AntiDetection(),
            BanManagement(),
            ChatIndicators(),
            ChatTerminal(),
            DisableAnalytics(),
            DisableBoosting(),
            DisableShuffle(),
            DisableUpdates(),
            EmptyCalls(),
            EnableUnlimited(),
            ExpiringMedia(),
            FeatureGranting(),
            LocalSavedPhrases(),
            LocationSpoofer(),
            NotificationAlerts(),
            OnlineIndicator(),
            ProfileDetails(),
            ProfileViews(),
            QuickBlock(),
            TimberLogging(),
            UnlimitedAlbums(),
            UnlimitedProfiles(),
            UnlockExplorer(),
            StatusDialog(),
//                WebSocketAlive()
        )

        hookList.forEach { hook ->
            Config.initHookSettings(hook.hookName, hook.hookDesc, false)
        }

        if (!init) return

        hooks = hookList.associateBy { it::class }.toMutableMap()

        hooks.values.forEach { hook ->
            if (Config.isHookEnabled(hook.hookName)) {
                try {
                    hook.init()
                    Logger.s("Initialized hook: ${hook.hookName}")
                } catch (e: Throwable) {
                    Logger.e(
                        "Failed to initialize hook ${hook.hookName}: ${e.message}",
                        LogSource.MODULE
                    )
                    Logger.writeRaw(e.stackTraceToString())
                }
            } else {
                Logger.i("Hook ${hook.hookName} is disabled.")
            }
        }
    }

    fun reloadHooks() {
        hooks.values.forEach { hook -> hook.cleanup() }
        hooks.clear()
        registerHooks()
        Logger.s("Reloaded hooks")
    }

    fun init() {
        registerHooks()
    }
}