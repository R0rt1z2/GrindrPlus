package com.grindrplus.hooks

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.core.logd
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.ui.Utils.copyToClipboard
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField

// supported version: 25.20.0
class EnableUnlimited : Hook(
    "Enable unlimited",
    "Enable Grindr Unlimited features"
) {
    private val profileViewState = "com.grindrapp.android.ui.profileV2.model.ProfileViewState"
    private val profileModel = "com.grindrapp.android.persistence.model.Profile"
    private val tabLayoutClass = "com.google.android.material.tabs.TabLayout"

    private val paywallUtils = "dk.c" // search for 'app_restart_required'
    private val persistentAdBannerContainer = "Z4.a" // search for 'GrindrAdContainer grindrAdContainer = (GrindrAdContainer) ViewBindings.findChildViewById(view, R.id.persistent_banner_ad_compose_view);'
    private val subscribeToInterstitialsList = listOf(
        "fa.A\$a" // search for 'com.grindrapp.android.chat.presentation.ui.ChatActivityV2$subscribeToInterstitialAds$1$1$1'
    )
    private val viewsToHide = mapOf(
        "com.grindrapp.android.ui.tagsearch.ProfileTagCascadeFragment\$b" to listOf("upsell_bottom_bar"), // search for 'bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/ProfileTagCascadeFragmentBinding;'
        "com.grindrapp.android.ui.browse.CascadeFragment\$b" to listOf("upsell_bottom_bar", "shuffle_top_bar", "floating_rating_banner", "micros_fab", "right_now_progress_compose_view"), // search for '"bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/FragmentBrowseCascadeBinding;"'
        "com.grindrapp.android.ui.home.HomeActivity\$g" to listOf("persistentAdBannerContainer"), // search for 'ViewBindings.findChildViewById(inflate, R.id.activity_home_content);'
        "com.grindrapp.android.ui.drawer.DrawerProfileFragment\$e" to listOf("plans_title", "store_in_profile_drawer_card", "sideDrawerBoostContainer", "drawer_profile_offer_card"), // search for '"bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/DrawerProfileBinding;"'
        "com.grindrapp.android.radar.presentation.ui.RadarFragment\$c" to listOf("micros_fab", "right_now_fabs_container") // search for 'bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/FragmentRadarBinding;'
    )

    override fun init() {
        val userSessionClass = findClass(GrindrPlus.userSession)

        userSessionClass.hook( // rolesUpdated()
            "W", HookStage.BEFORE // search for 'Intrinsics.checkNotNullParameter(roles, "roles");' in userSession
        ) { param ->
            val roles = param.arg(0) as List<String>
			val allRoles = listOf(
				"Plus",
				"Xtra",
				"Unlimited",
				"Premium",
				"Free_Plus",
//				"Free_Xtra",
				"Free_Unlimited",
				"Free_Premium"
			)

			logi("received roles: $roles, replacing with $allRoles")

			param.setArg(0, allRoles)
		}

        subscribeToInterstitialsList.forEach {
            findClass(it)
                .hook("emit", HookStage.BEFORE) { param ->
                    val modelName = param.arg<Any>(0)::class.java.name
                    if (!modelName.contains("NoInterstitialCreated")
                        && !modelName.contains("OnInterstitialDismissed")
                    ) {
                        param.setResult(null)
                    }
                }
        }

        findClass(tabLayoutClass).hook("addTab", HookStage.AFTER) { param ->
            val blockedTabs = mapOf(
                4 to "Store"
            )

            val tab = param.arg<Any>(0)
            val position = getObjectField(tab, "position") as? Int ?: -1

            logd("Trying to add tab at position $position")

            if (position in blockedTabs.keys) {
                val tabName = blockedTabs[position] ?: "Unknown"

                val tabView = getObjectField(tab, "view") as? View
                tabView?.let { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    logi("Removed tab '$tabName' at position $position")
                }
            }
        }

        viewsToHide.forEach { (className, viewIds) ->
            findClass(className).hook(
                "invoke", HookStage.AFTER
            ) { param ->
                if (param.args().isNotEmpty()) {
                    val rootView = param.arg<View>(0)
                    hideViews(rootView, viewIds)
                }
            }
        }

        findClass(persistentAdBannerContainer).hook("a", HookStage.BEFORE) { param ->
            if (param.args().isNotEmpty()) {
                val rootView = param.arg<View>(0)
				// TODO validate change from persistent_banner_ad_container to persistent_banner_ad_compose_view
                hideViews(rootView, listOf("persistent_banner_ad_compose_view"))
            }
        }

        setOf("isBlockable", "component60").forEach {
            findClass(profileModel).hook(it, HookStage.BEFORE) { param ->
                param.setResult(true)
            }
        }

        findClass(paywallUtils).hook("d", HookStage.BEFORE) { param ->
            val stackTrace = Thread.currentThread().stackTrace.dropWhile {
                !it.toString().contains("LSPHooker") }.drop(1).joinToString("\n")

            android.app.AlertDialog.Builder(GrindrPlus.currentActivity)
                .setTitle("Paywalled Feature Detected")
                .setMessage(
                    "This feature is server-enforced and cannot be bypassed in this version.\n\n" +
                            "If you think this is a mistake, please report it to the developer. " +
                            "You can copy the stack trace below to help with troubleshooting."
                )
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setNegativeButton("Copy Stack Trace") { _, _ ->
                    copyToClipboard(
                        "Stack trace",
                        stackTrace
                    )
                }
                .setPositiveButton("Ok", null)
                .show()

            param.setResult(null)
        }

        findClass(profileViewState).hook("isChatPaywalled", HookStage.BEFORE) { param ->
            param.setResult(false)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun hideViews(rootView: View, viewIds: List<String>) {
        viewIds.forEach { viewId ->
            try {
                val id = rootView.resources.getIdentifier(
                    viewId, "id", "com.grindrapp.android")
                if (id > 0) {
                    val view = rootView.findViewById<View>(id)
                    if (view != null) {
                        logd("View with ID: $viewId found and will be hidden")
                        val params = view.layoutParams
                        params.height = 0
                        view.layoutParams = params
                        view.visibility = View.GONE
                    }
                } else {
                    logd("View with ID: $viewId not found")
                }
            } catch (e: Exception) {
                loge("Error hiding view with ID: $viewId: ${e.message}")
                Logger.writeRaw(e.stackTraceToString())
            }
        }
    }
}