package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.persistence.asSettingsRequestToSettingsRequest
import com.grindrplus.persistence.asSettingsToSettings
import com.grindrplus.persistence.toGrindrSettings
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.RetrofitUtils.createSuccess
import com.grindrplus.utils.RetrofitUtils.getSuccessValue
import com.grindrplus.utils.RetrofitUtils.isGET
import com.grindrplus.utils.RetrofitUtils.isPUT
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import com.grindrplus.utils.withSuspendResult
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.setObjectField

class PersistentIncognito : Hook(
    "Persistent incognito",
    "Makes incognito mode persistent"
) {
    private val meRestService = "C3.q"
    private val grindrSettings = "com.grindrapp.android.model.GrindrSettings"
    private val drawerProfileFragment = "com.grindrapp.android.ui.drawer.DrawerProfileFragment"

    private var hasRequestBeenSent = false

    override fun init() {
        RetrofitUtils.hookService(
            findClass(meRestService),
        ) { originalHandler, proxy, method, args ->
            val result = originalHandler.invoke(proxy, method, args)
            when {
                method.isGET("v3/me/prefs/settings") -> handleGetSettings(args, result)
                method.isPUT("v3/me/prefs/settings") -> handlePutSettings(args, result)
                else -> result
            }
        }

        findClass(grindrSettings).hookConstructor(HookStage.AFTER) { param ->
            val incognito = Config.get("incognito_mode", false) as Boolean
            setObjectField(param.thisObject(), "incognito", incognito)
        }

        findClass(drawerProfileFragment)
            .hook("J", HookStage.BEFORE) { param ->
                val incognito = param.arg(0) as Boolean
                val viewModel = callMethod(param.thisObject(), "H")
                callMethod(viewModel, "I", incognito)
                Config.put("incognito_mode", incognito)
                incognito.let {
                    if (it) GrindrPlus.httpClient.enableIncognito()
                    else GrindrPlus.httpClient.disableIncognito()
                }
                param.setResult(null)
            }
    }

    private fun handleGetSettings(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            GrindrPlus.logger.log(result.getSuccessValue().toString())
            val settings = result.getSuccessValue().asSettingsToSettings()
            val incognito = Config.get("incognito_mode", false) as Boolean

            if (incognito && !settings.incognito && !hasRequestBeenSent) {
                GrindrPlus.logger.log("API incognito is disabled but local incognito is enabled?")
                GrindrPlus.httpClient.enableIncognito()
                hasRequestBeenSent = true
            }

            createSuccess(settings.copy(incognito = incognito).toGrindrSettings())
        }

    private fun handlePutSettings(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            val settingsRequest = args[0]?.asSettingsRequestToSettingsRequest()
            val incognito = Config.get("incognito_mode", false) as Boolean
            val settings = settingsRequest?.settings?.asSettingsToSettings()!!
            if (settings.incognito != incognito) {
                incognito.let {
                    if (it) GrindrPlus.httpClient.enableIncognito()
                    else GrindrPlus.httpClient.disableIncognito()
                }
            }
            result
        }
}