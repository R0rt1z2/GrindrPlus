package com.grindrplus

import com.grindrplus.core.Config
import android.app.Application
import android.content.Context
import android.widget.Toast
import com.grindrplus.core.Constants
import com.grindrplus.core.Constants.GRINDR_PKG_VERSION_NAME
import com.grindrplus.core.GlobalCache
import com.grindrplus.core.Hooks
import com.grindrplus.core.InitOnce
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.time.Duration.Companion.minutes


class Hooker : IXposedHookLoadPackage {

    companion object {
        var config: Config by InitOnce()
        var globalCache: GlobalCache by InitOnce()
        var pkgParam: LoadPackageParam by InitOnce()
        var appContext: Context by InitOnce()
        var pkgVersionName: String by InitOnce()
        val sharedPref by lazy { appContext.getSharedPreferences("phrases", Context.MODE_PRIVATE) }
    }

    private fun initializePreOnCreateHooks() {
        try {
            Logger.xLog("Starting pre-onCreate hooks...")
            Utils.fetchVersionAndUpdate()
            Hooks.storeChatMessageManager()
            Hooks.localSavedPhrases()
            Hooks.allowMockProvider()
            Hooks.preventRecordProfileViews()
            //DO NOT ENABLE THIS IN PRODUCTION BUILDS!
            //Hooks.trustAllCerts()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog("Error in pre-onCreate hook: $it") }
        }
    }

    private fun initializePostOnCreateHooks() {
        try {
            Logger.xLog("Starting post-onCreate hooks...")
            Hooks.unlimitedProfiles()
            Hooks.allowScreenshotsHook()
            Hooks.hookUserSessionImpl()
            Hooks.hookFeatureGranting()
            Hooks.allowVideocallsOnEmptyChats()
            Hooks.hookOnlineIndicatorDuration(3.minutes)
            Hooks.unlimitedExpiringPhotos()
            Hooks.unlimitedAlbums()
            Hooks.unlimitedTaps()
            Hooks.removeExpirationOnExpiringPhotos()
            Hooks.keepChatsOfBlockedProfiles()
            Hooks.showBlocksInChat()
            Hooks.createChatTerminal()
            Hooks.disableAutomaticMessageDeletion()
            Hooks.dontSendTypingIndicator()
            Hooks.useThreeColumnLayoutForFavorites()
            Hooks.disableAnalytics()
            Hooks.dontSendChatMarkers()
            Hooks.makeMessagesAlwaysRemovable()
            Hooks.modifyProfileDetails()
            Hooks.useMorePreciseDistanceDisplay()
            // Hooks.allowSomeExperiments()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog("Error in post-onCreate hook: $it") }
        }
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != Constants.GRINDR_PKG) return
        pkgParam = lpparam
        config = Config(
            pkgParam.appInfo.dataDir + "/config.json")

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
                    globalCache = GlobalCache(appContext)

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