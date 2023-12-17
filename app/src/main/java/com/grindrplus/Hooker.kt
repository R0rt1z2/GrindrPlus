package com.grindrplus

import android.app.Application
import android.content.Context
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.time.Duration.Companion.minutes


class Hooker : IXposedHookLoadPackage {

    fun toastInvalidVersionName() {
        Toast.makeText(
            appContext,
            "This hook is for client version $TARGET_PKG_VERSION_NAME. (Current: $pkgVersionName) Hook will not be loaded.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != Constants.GRINDR_PKG) return
        pkgParam = lpparam

        try {
            Hooks.hookAppUpdates()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog(it) }
        }

        //This is a quick and dirty fix, but these should be called before the application context is created.

        try {
            Hooks.storeChatMessageManager()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog(it) }
        }


        try {
            Hooks.localSavedPhrases()
        } catch (e: Exception) {
            e.message?.let { Logger.xLog(it) }
        }

        findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.xLog("Application.onCreate()")
                    appContext = (param.thisObject as Application).applicationContext
                    pkgVersionName = appContext.packageManager
                        .getPackageInfo(appContext.packageName, 0).versionName

                    if (pkgVersionName != TARGET_PKG_VERSION_NAME) {
                        toastInvalidVersionName()
                        return
                    }

                    try {
                        Hooks.unlimitedProfiles()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.allowScreenshotsHook()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.allowMockProvider()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.hookUserSessionImpl()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.hookFeatureGranting()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.allowVideocallsOnEmptyChats()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        //I've set this to max 3 min. If we make an UI for Hook Settings, we'll let the user to change this.
                        Hooks.hookOnlineIndicatorDuration(3.minutes)
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.unlimitedExpiringPhotos()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.unlimitedTaps()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.removeExpirationOnExpiringPhotos()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.preventRecordProfileViews()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    //try {
                    //    Hooks.makeMessagesAlwaysRemovable()
                    //} catch (e: Exception) {
                    //    e.message?.let { Logger.xLog(it) }
                    //}

                    try {
                        Hooks.keepChatsOfBlockedProfiles()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.showBlocksInChat()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.disableAutomaticMessageDeletion()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.dontSendTypingIndicator()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    // R0rt1z2: This is not required as of 9.17.4 because
                    // both `addDisplayedExtension` and `addReceivedExtension`
                    // have been dropped from `ChatMarkersManager`.
                    // try {
                    //     Hooks.dontSendChatMarkers()
                    // } catch (e: Exception) {
                    //     e.message?.let { Logger.xLog(it) }
                    // }*/

                    try {
                        Hooks.useThreeColumnLayoutForFavorites()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    try {
                        Hooks.disableAnalytics()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }

                    /*try {
                        Hooks.addExtraProfileFields()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }*/

                    /*try {
                        Hooks.allowSomeExperiments()
                    } catch (e: Exception) {
                        e.message?.let { Logger.xLog(it) }
                    }*/
                }
            }
        )
    }

    companion object {
        const val TARGET_PKG_VERSION_NAME = "9.17.4"

        var pkgParam: LoadPackageParam by InitOnce()
        var appContext: Context by InitOnce()
        var pkgVersionName: String by InitOnce()
        val sharedPref by lazy { appContext.getSharedPreferences("phrases", Context.MODE_PRIVATE) }
    }
}