package com.grindrplus

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.grindrplus.Constants.GRINDR_PKG_VERSION_NAME
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.time.Duration.Companion.minutes

class Hooker : IXposedHookLoadPackage {

    companion object {
        lateinit var pkgParam: LoadPackageParam
        lateinit var appContext: Context
        lateinit var pkgVersionName: String
        val sharedPref by lazy { appContext.getSharedPreferences("phrases", Context.MODE_PRIVATE) }
    }

    private fun initializePreOnCreateHooks() {
        try {
            Logger.xLog("Starting pre-onCreate hooks...")
            Hooks.hookAppUpdates()
            Hooks.storeChatMessageManager()
            Hooks.localSavedPhrases()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog("Error in pre-onCreate hook: $it") }
        }
    }

    private fun initializePostOnCreateHooks() {
        try {
            Logger.xLog("Starting post-onCreate hooks...")
            Hooks.unlimitedProfiles()
            Hooks.allowScreenshotsHook()
            Hooks.allowMockProvider()
            Hooks.hookUserSessionImpl()
            Hooks.hookFeatureGranting()
            Hooks.allowVideocallsOnEmptyChats()
            Hooks.hookOnlineIndicatorDuration(3.minutes)
            Hooks.unlimitedExpiringPhotos()
            Hooks.unlimitedTaps()
            Hooks.removeExpirationOnExpiringPhotos()
            Hooks.preventRecordProfileViews()
            // Hooks.makeMessagesAlwaysRemovable()
            Hooks.keepChatsOfBlockedProfiles()
            Hooks.showBlocksInChat()
            Hooks.disableAutomaticMessageDeletion()
            Hooks.dontSendTypingIndicator()
            // Hooks.dontSendChatMarkers()
            Hooks.useThreeColumnLayoutForFavorites()
            Hooks.disableAnalytics()
            // Hooks.addExtraProfileFields()
            // Hooks.allowSomeExperiments()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog("Error in post-onCreate hook: $it") }
        }
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != Constants.GRINDR_PKG) return
        pkgParam = lpparam

        initializePreOnCreateHooks()

        findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    appContext = (param.thisObject as Application)
                        .applicationContext
                    pkgVersionName = appContext.packageManager
                        .getPackageInfo(appContext.packageName, 0).versionName

                    if (pkgVersionName != GRINDR_PKG_VERSION_NAME) {
                        return Toast.makeText(
                            appContext,
                            "This hook is for client version $GRINDR_PKG_VERSION_NAME. " +
                                 "(Current: $pkgVersionName) Hook will not be loaded.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    initializePostOnCreateHooks()
                }
            }
        )
    }
}