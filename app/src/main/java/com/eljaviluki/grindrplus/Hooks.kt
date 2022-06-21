package com.eljaviluki.grindrplus

import de.robv.android.xposed.XposedHelpers
import android.view.View
import android.view.Window
import de.robv.android.xposed.XC_MethodHook
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.eljaviluki.grindrplus.Obfuscation.GApp
import kotlin.time.Duration

object Hooks {
    /**
     * Allow screenshots in all the views of the application (including expiring photos, albums, etc.)
     *
     * Inspired in the project https://github.com/veeti/DisableFlagSecure
     * Credit and thanks to @veeti!
     */
    fun allowScreenshotsHook() {
        XposedHelpers.findAndHookMethod(
            Window::class.java,
            "setFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    var flags = param.args[0] as Int
                    flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                    param.args[0] = flags
                }
            })
    }

    /**
     * Add extra profile fields with more information:
     * - Profile ID
     * - Last seen (exact date and time)
     */
    fun addExtraProfileFields() {
        val class_ProfileFieldsView = XposedHelpers.findClass(
            GApp.ui.profileV2.ProfileFieldsView,
            Hooker.pkgParam!!.classLoader
        )

        val class_Profile = XposedHelpers.findClass(
            GApp.persistence.model.Profile,
            Hooker.pkgParam!!.classLoader
        )

        val class_ExtendedProfileFieldView = XposedHelpers.findClass(
            GApp.view.ExtendedProfileFieldView,
            Hooker.pkgParam!!.classLoader
        )

        val class_R_color = XposedHelpers.findClass(
            GApp.R.color,
            Hooker.pkgParam!!.classLoader
        )

        XposedHelpers.findAndHookMethod(
            class_ProfileFieldsView,
            GApp.ui.profileV2.ProfileFieldsView_.setProfile,
            class_Profile,
            object : XC_MethodHook() {
                var fieldsViewInstance: Any? = null
                var context: Any? = null

                val labelColorRgb = ContextCompat.getColor(
                    Hooker.appContext!!,
                    XposedHelpers.getStaticIntField(
                        class_R_color,

                        //Original color for vanilla labels: grindr_gray_2
                        //to differentiate a normal field from a special one, the name of the special one will be golden.
                        GApp.R.color_.grindr_gold_star_gay
                    )
                )

                val valueColorId = XposedHelpers.getStaticIntField(
                    class_R_color,
                    GApp.R.color_.grindr_pure_white
                ) //R.color.grindr_pure_white

                override fun afterHookedMethod(param: MethodHookParam) {
                    fieldsViewInstance = param.thisObject
                    context = XposedHelpers.callMethod(
                        fieldsViewInstance,
                        "getContext"
                    ) //Call this.getContext()

                    //Get profile instance in the 1st parameter
                    val profile = param.args[0]

                    addProfileFieldUi(
                        "Profile ID",
                        XposedHelpers.getObjectField(profile, "profileId") as CharSequence,
                        where = 0
                    )

                    addProfileFieldUi(
                        "Last Seen",
                        Utils.toReadableDate(XposedHelpers.getLongField(profile, "seen")),
                        where = 1
                    )

                    //.setVisibility() of param.thisObject to always VISIBLE (otherwise if the profile has no fields, the additional ones will not be shown)
                    XposedHelpers.callMethod(fieldsViewInstance, "setVisibility", View.VISIBLE)
                }

                //By default, the views are added to the end of the list.
                private fun addProfileFieldUi(label: CharSequence, value: CharSequence, where: Int = -1) {
                    val extendedProfileFieldView =
                        XposedHelpers.newInstance(class_ExtendedProfileFieldView, context)

                    XposedHelpers.callMethod(
                        extendedProfileFieldView,
                        GApp.view.ExtendedProfileFieldView_.setLabel,
                        label,
                        labelColorRgb
                    )

                    XposedHelpers.callMethod(
                        extendedProfileFieldView,
                        GApp.view.ExtendedProfileFieldView_.setValue,
                        value,
                        valueColorId
                    )

                    //From View.setContentDescription(...)
                    XposedHelpers.callMethod(
                        extendedProfileFieldView,
                        "setContentDescription",
                        value
                    )

                    //(ProfileFieldsView).addView(Landroid/view/View;)V
                    XposedHelpers.callMethod(
                        fieldsViewInstance,
                        "addView",
                        extendedProfileFieldView,
                        where
                    )
                }
            })
    }

    /**
     * Hook these methods in all the classes that implement IUserSession.
     * isFree()Z (return false)
     * isNoXtraUpsell()Z (return false)
     * isXtra()Z to give Xtra account features.
     * isUnlimited()Z to give Unlimited account features.
     */
    fun hookUserSessionImpl() {
        //Apply the hook to all the classes using lambda expressions
        val class_Feature = XposedHelpers.findClass(
            GApp.model.Feature,
            Hooker.pkgParam!!.classLoader
        )

        listOf(
            XposedHelpers.findClass(
                GApp.storage.UserSession,
                Hooker.pkgParam!!.classLoader
            ),

            XposedHelpers.findClass(
                GApp.storage.UserSession2,
                Hooker.pkgParam!!.classLoader
            )
        ).forEach { userSessionImpl ->
            XposedHelpers.findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.hasFeature_feature,
                class_Feature,
                Constants.Returns.RETURN_TRUE
            )

            XposedHelpers.findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isFree,
                Constants.Returns.RETURN_FALSE
            )

            XposedHelpers.findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isNoXtraUpsell,
                Constants.Returns.RETURN_FALSE
            ) //Not sure what is this for

            XposedHelpers.findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isXtra,
                Constants.Returns.RETURN_TRUE
            )

            XposedHelpers.findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isUnlimited,
                Constants.Returns.RETURN_TRUE
            )
        }
    }

    fun unlimitedExpiringPhotos() {
        val class_ExpiringPhotoStatusResponse = XposedHelpers.findClass(
            GApp.model.ExpiringPhotoStatusResponse,
            Hooker.pkgParam!!.classLoader
        )

        XposedHelpers.findAndHookMethod(
            class_ExpiringPhotoStatusResponse,
            GApp.model.ExpiringPhotoStatusResponse_.getTotal,
            Constants.Returns.RETURN_INTEGER_MAX_VALUE
        )

        XposedHelpers.findAndHookMethod(
            class_ExpiringPhotoStatusResponse,
            GApp.model.ExpiringPhotoStatusResponse_.getAvailable,
            Constants.Returns.RETURN_INTEGER_MAX_VALUE
        )
    }

    /**
     * Grant all the Grindr features (except disabling screenshots).
     * A few more changes may be needed to use all the features.
     */
    fun hookFeatureGranting() {
        val class_Feature = XposedHelpers.findClass(
            GApp.model.Feature,
            Hooker.pkgParam!!.classLoader
        )

        XposedHelpers.findAndHookMethod(
            class_Feature,
            GApp.model.Feature_.isGranted,
            Constants.Returns.RETURN_TRUE
        )

        XposedHelpers.findAndHookMethod(
            class_Feature,
            GApp.model.Feature_.isNotGranted,
            Constants.Returns.RETURN_FALSE
        )

        val class_IUserSession = XposedHelpers.findClass(
            GApp.storage.IUserSession,
            Hooker.pkgParam!!.classLoader
        )

        XposedHelpers.findAndHookMethod(
            class_Feature,
            GApp.model.Feature_.isGranted,
            class_IUserSession,
            Constants.Returns.RETURN_TRUE
        )

        XposedHelpers.findAndHookMethod(
            class_Feature,
            GApp.model.Feature_.isNotGranted,
            class_IUserSession,
            Constants.Returns.RETURN_FALSE
        )
    }

    /**
     * Allow to use SOME (not all of them) hidden features that Grindr developers have not yet made public
     * or they are just testing.
     */
    fun allowSomeExperiments() {
        val class_Experiments = XposedHelpers.findClass(
            GApp.experiment.Experiments,
            Hooker.pkgParam!!.classLoader
        )

        val class_IExperimentsManager = XposedHelpers.findClass(
            GApp.base.Experiment.IExperimentManager,
            Hooker.pkgParam!!.classLoader
        )

        XposedHelpers.findAndHookMethod(
            class_Experiments,
            GApp.experiment.Experiments_.uncheckedIsEnabled_expMgr,
            class_IExperimentsManager,
            Constants.Returns.RETURN_TRUE
        )
    }

    /**
     * Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
     * (both must have sent at least one message to the other) in order to allow videocalls.
     *
     * This hook allows the user to bypass this restriction.
     */
    fun allowVideocallsOnEmptyChats() {
        val class_Continuation = XposedHelpers.findClass(
            "kotlin.coroutines.Continuation",
            Hooker.pkgParam!!.classLoader
        ) //I tried using Continuation::class.java, but that only gives a different Class instance (does not work)

        val class_ChatRepo = XposedHelpers.findClass(
            GApp.persistence.repository.ChatRepo,
            Hooker.pkgParam!!.classLoader
        )

        XposedHelpers.findAndHookMethod(
            class_ChatRepo,
            GApp.persistence.repository.ChatRepo_.checkMessageForVideoCall,
            String::class.java,
            class_Continuation,
            Constants.Returns.RETURN_TRUE
        )
    }

    /**
     * Allow Fake GPS in order to fake location.
     *
     * WARNING: Abusing this feature may result in a permanent ban on your Grindr account.
     */
    fun allowMockProvider() {
        val class_Location = XposedHelpers.findClass(
            "android.location.Location",
            Hooker.pkgParam!!.classLoader
        )
        
        XposedHelpers.findAndHookMethod(
            class_Location,
            "isFromMockProvider",
            Constants.Returns.RETURN_FALSE
        )
    }


    /**
     * Hook online indicator duration:
     *
     * "After closing the app, the profile remains online for 10 minutes. It is misleading. People think that you are rude for not answering, when in reality you are not online."
     *
     * Now, you can limit the Online indicator (green dot) for a custom duration.
     *
     * Inspired in the suggestion made at:
     * https://grindr.uservoice.com/forums/912631-grindr-feedback/suggestions/34555780-more-accurate-online-status-go-offline-when-clos
     *
     * @param duration Duration in milliseconds.
     *
     * @see Duration
     * @see Duration.inWholeMilliseconds
     *
     * @author ElJaviLuki
     */
    fun hookOnlineIndicatorDuration(duration : Duration){
        val class_ProfileUtils = XposedHelpers.findClass(GApp.utils.ProfileUtils, Hooker.pkgParam!!.classLoader)
        XposedHelpers.setStaticLongField(class_ProfileUtils, GApp.utils.ProfileUtils_.onlineIndicatorDuration, duration.inWholeMilliseconds)
    }

    /**
     * Allow unlimited taps on profiles.
     *
     * @author ElJaviLuki
     */
    fun unlimitedTaps() {
        val class_TapsAnimLayout = XposedHelpers.findClass(GApp.view.TapsAnimLayout, Hooker.pkgParam!!.classLoader)
        val class_ChatMessage = XposedHelpers.findClass(GApp.persistence.model.ChatMessage, Hooker.pkgParam!!.classLoader)

        val tapTypeToHook = XposedHelpers.getStaticObjectField(class_ChatMessage, GApp.persistence.model.ChatMessage_.TAP_TYPE_NONE)

        //Reset the tap value to allow multitapping.
        XposedHelpers.findAndHookMethod(
            class_TapsAnimLayout,
            GApp.view.TapsAnimLayout_.setTapType,
            String::class.java,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook(){
                override fun afterHookedMethod(param: MethodHookParam) {
                    XposedHelpers.setObjectField(
                        param.thisObject,
                        GApp.view.TapsAnimLayout_.tapType,
                        tapTypeToHook
                    )
                }
            }
        )

        //Reset taps on long press (allows using tap variants)
        XposedHelpers.findAndHookMethod(
            class_TapsAnimLayout,
            GApp.view.TapsAnimLayout_.getCanSelectVariants,
            Constants.Returns.RETURN_TRUE
        )
    }

    /**
     * Hook the method that returns the duration of the expiring photos.
     * This way, the photos will not expire and you will be able to see them any time you want.
     *
     * @author ElJaviLuki
     */
    fun removeExpirationOnExpiringPhotos() {
        val class_ExpiringImageBody = XposedHelpers.findClass(GApp.model.ExpiringImageBody, Hooker.pkgParam!!.classLoader)
        XposedHelpers.findAndHookMethod(
            class_ExpiringImageBody,
            GApp.model.ExpiringImageBody_.getDuration,
            Constants.Returns.RETURN_LONG_MAX_VALUE
        )
    }
}