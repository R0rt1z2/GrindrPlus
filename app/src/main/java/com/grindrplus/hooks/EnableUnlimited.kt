package com.grindrplus.hooks

import android.annotation.SuppressLint
import android.view.View
import com.grindrplus.core.Logger
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EnableUnlimited : Hook(
    "Enable unlimited",
    "Enable Grindr Unlimited features"
) {
    private val persistentAdBannerContainer = "m6.s3" // search for 'GrindrAdContainer grindrAdContainer = (GrindrAdContainer) ViewBindings.findChildViewById(view, R.id.persistent_banner_ad_container);'
    private val userSession = "Bb.o0" // search for 'com.grindrapp.android.storage.UserSessionImpl$1'
    private val subscribeToInterstitialsList = listOf(
        "K5.O\$a" // search for 'com.grindrapp.android.chat.presentation.ui.ChatActivityV2$subscribeToInterstitialAds$1$1$1'
    )
    private val viewsToHide = mapOf(
        "com.grindrapp.android.ui.tagsearch.ProfileTagCascadeFragment\$c" to listOf("upsell_bottom_bar"), // search for 'bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/ProfileTagCascadeFragmentBinding;'
        "com.grindrapp.android.ui.browse.CascadeFragment\$b" to listOf("upsell_bottom_bar"), // search for '"bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/FragmentBrowseCascadeBinding;"'
        "com.grindrapp.android.ui.home.HomeActivity\$l" to listOf("persistentAdBannerContainer"), // search for 'ViewBindings.findChildViewById(inflate, R.id.activity_home_content)) != null) {'
        "com.grindrapp.android.ui.drawer.DrawerProfileFragment\$e" to listOf("sideDrawerBoostContainer") // search for '"bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/DrawerProfileBinding;"'
    )

    override fun init() {
        val userSessionClass = findClass(userSession)

        userSessionClass.hook( // isNoXtraUpsell()
            "l", HookStage.BEFORE // search for '()) ? false : true;' in userSession
        ) { param ->
            param.setResult(true)
        }

        userSessionClass.hook( // isNoPlusUpsell()
            "G", HookStage.BEFORE // search for 'Role.PLUS, Role.FREE_PLUS' in userSession
        ) { param ->
            param.setResult(true)
        }

        userSessionClass.hook( // isFree()
            "y", HookStage.BEFORE // search for '.isEmpty();' in userSession
        ) { param ->
            param.setResult(false)
        }

        userSessionClass.hook( // isFreeXtra()
            "u", HookStage.BEFORE // search for 'Role.XTRA, Role.FREE_XTRA' in userSession
        ) { param ->
            param.setResult(false)
        }

        userSessionClass.hook( // isFreeUnlimited()
            "E", HookStage.BEFORE // search for 'Role.UNLIMITED, Role.FREE_UNLIMITED' in userSession
        ) { param ->
            param.setResult(true)
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
                hideViews(rootView, listOf("persistent_banner_ad_container"))
            }
        }

        // search for 'variantName, "treatment_exact_count") ?'
        findClass("a2.a").hook("b", HookStage.BEFORE) { param ->
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
                        Logger.d("View with ID: $viewId found and will be hidden")
                        val params = view.layoutParams
                        params.height = 0
                        view.layoutParams = params
                        view.visibility = View.GONE
                    }
                } else {
                    Logger.d("View with ID: $viewId not found")
                }
            } catch (e: Exception) {
                Logger.e("Error hiding view with ID: $viewId: ${e.message}")
            }
        }
    }
}