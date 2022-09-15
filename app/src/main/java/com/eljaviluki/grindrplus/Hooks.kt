package com.eljaviluki.grindrplus

import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.Window
import de.robv.android.xposed.XC_MethodHook
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.eljaviluki.grindrplus.Constants.Returns.RETURN_TRUE
import com.eljaviluki.grindrplus.Constants.Returns.RETURN_FALSE
import com.eljaviluki.grindrplus.Constants.Returns.RETURN_INTEGER_MAX_VALUE
import com.eljaviluki.grindrplus.Constants.Returns.RETURN_LONG_MAX_VALUE
import com.eljaviluki.grindrplus.Obfuscation.GApp
import com.eljaviluki.grindrplus.decorated.persistence.model.Profile
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.*
import kotlin.time.Duration

object Hooks {
    /**
     * Allow screenshots in all the views of the application (including expiring photos, albums, etc.)
     *
     * Inspired in the project https://github.com/veeti/DisableFlagSecure
     * Credit and thanks to @veeti!
     */
    fun allowScreenshotsHook() {
        findAndHookMethod(
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
        val class_ProfileFieldsView = findClass(
            GApp.ui.profileV2.ProfileFieldsView,
            Hooker.pkgParam.classLoader
        )

        val class_Profile = findClass(
            GApp.persistence.model.Profile,
            Hooker.pkgParam.classLoader
        )

        val class_ExtendedProfileFieldView = findClass(
            GApp.view.ExtendedProfileFieldView,
            Hooker.pkgParam.classLoader
        )

        val class_R_color = findClass(
            GApp.R.color,
            Hooker.pkgParam.classLoader
        )

        val class_Continuation = findClass(
            "kotlin.coroutines.Continuation",
            Hooker.pkgParam.classLoader
        ) //I tried using Continuation::class.java, but that only gives a different Class instance (does not work)


        val class_Intrinsics = findClass(
            "kotlin.jvm.internal.Intrinsics",
            Hooker.pkgParam.classLoader
        )

        val checkNotNullParameterMethod = findMethodExact(class_Intrinsics, "checkNotNullParameter", Object::class.java, String::class.java)

        findAndHookMethod(
            class_ProfileFieldsView,
            GApp.ui.profileV2.ProfileFieldsView_.setProfile,
            class_Profile,
            class_Continuation,
            object : XC_MethodHook() {
                var fieldsViewInstance: Any? = null
                val context: Any? by lazy {
                    callMethod(
                        fieldsViewInstance,
                        "getContext"
                    )
                }

                val labelColorRgb = ContextCompat.getColor(
                    Hooker.appContext!!,
                    getStaticIntField(
                        class_R_color,

                        //Original color for vanilla labels: grindr_gray_2
                        //to differentiate a normal field from a special one, the name of the special one will be golden.
                        GApp.R.color_.grindr_gold_star_gay
                    )
                )

                val valueColorId = getStaticIntField(
                    class_R_color,
                    GApp.R.color_.grindr_pure_white
                ) //R.color.grindr_pure_white

                override fun afterHookedMethod(param: MethodHookParam) {
                    fieldsViewInstance = param.thisObject

                    //Get profile instance in the 1st parameter
                    val profile = Profile(param.args[0])

                    addProfileFieldUi(
                        "Profile ID",
                        profile.profileId,
                        where = 0
                    )

                    val lastSeen = profile.seen
                    addProfileFieldUi(
                        "Last Seen",
                        if (lastSeen != 0L) Utils.toReadableDate(lastSeen) else "N/A",
                        where = 1
                    )

                    val weight : Double = profile.weight
                    val height : Double = profile.height
                    if(weight != 0.0 && height != 0.0) {
                        addProfileFieldUi(
                            "Body Mass Index",
                            Utils.getBmiDescription(weight, height),
                            where = 2
                        )
                    }

                    //.setVisibility() of param.thisObject to always VISIBLE (otherwise if the profile has no fields, the additional ones will not be shown)
                    callMethod(fieldsViewInstance, "setVisibility", View.VISIBLE)
                }

                //By default, the views are added to the end of the list.
                private fun addProfileFieldUi(label: CharSequence, value: CharSequence, where: Int = -1) {
                    val hooked = XposedBridge.hookMethod(checkNotNullParameterMethod, XC_MethodReplacement.DO_NOTHING)
                    val extendedProfileFieldView =
                        newInstance(class_ExtendedProfileFieldView, context, null as AttributeSet?)
                    hooked.unhook()

                    callMethod(
                        extendedProfileFieldView,
                        GApp.view.ExtendedProfileFieldView_.setLabel,
                        label,
                        labelColorRgb
                    )

                    callMethod(
                        extendedProfileFieldView,
                        GApp.view.ExtendedProfileFieldView_.setValue,
                        value,
                        valueColorId
                    )

                    //From View.setContentDescription(...)
                    callMethod(
                        extendedProfileFieldView,
                        "setContentDescription",
                        value
                    )

                    //(ProfileFieldsView).addView(Landroid/view/View;)V
                    callMethod(
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
        val class_Feature = findClass(
            GApp.model.Feature,
            Hooker.pkgParam.classLoader
        )

        listOf(
            findClass(
                GApp.storage.UserSession,
                Hooker.pkgParam.classLoader
            ),

            findClass(
                GApp.storage.UserSession2,
                Hooker.pkgParam.classLoader
            )
        ).forEach { userSessionImpl ->
            findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.hasFeature_feature,
                class_Feature,
                RETURN_TRUE
            )

            findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isFree,
                RETURN_FALSE
            )

            findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isNoXtraUpsell,
                RETURN_FALSE
            ) //Not sure what is this for

            findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isXtra,
                RETURN_TRUE
            )

            findAndHookMethod(
                userSessionImpl,
                GApp.storage.IUserSession_.isUnlimited,
                RETURN_TRUE
            )
        }
    }

    fun unlimitedExpiringPhotos() {
        val class_ExpiringPhotoStatusResponse = findClass(
            GApp.model.ExpiringPhotoStatusResponse,
            Hooker.pkgParam.classLoader
        )

        findAndHookMethod(
            class_ExpiringPhotoStatusResponse,
            GApp.model.ExpiringPhotoStatusResponse_.getTotal,
            RETURN_INTEGER_MAX_VALUE
        )

        findAndHookMethod(
            class_ExpiringPhotoStatusResponse,
            GApp.model.ExpiringPhotoStatusResponse_.getAvailable,
            RETURN_INTEGER_MAX_VALUE
        )
    }

    /**
     * Grant all the Grindr features (except disabling screenshots).
     * A few more changes may be needed to use all the features.
     */
    fun hookFeatureGranting() {
        val class_Feature = findClass(
            GApp.model.Feature,
            Hooker.pkgParam.classLoader
        )

        findAndHookMethod(
            class_Feature,
            GApp.model.Feature_.isGranted,
            RETURN_TRUE
        )

        val class_IUserSession = findClass(
            GApp.storage.IUserSession,
            Hooker.pkgParam.classLoader
        )

        findAndHookMethod(
            class_Feature,
            GApp.model.Feature_.isGranted,
            class_IUserSession,
            RETURN_TRUE
        )
    }

    /**
     * Allow to use SOME (not all of them) hidden features that Grindr developers have not yet made public
     * or they are just testing.
     */
    fun allowSomeExperiments() {
        val class_Experiments = findClass(
            GApp.experiment.Experiments,
            Hooker.pkgParam.classLoader
        )

        val class_IExperimentsManager = findClass(
            GApp.base.Experiment.IExperimentsManager,
            Hooker.pkgParam.classLoader
        )

        findAndHookMethod(
            class_Experiments,
            GApp.experiment.Experiments_.uncheckedIsEnabled_expMgr,
            class_IExperimentsManager,
            RETURN_TRUE
        )
    }

    /**
     * Allow videocalls on empty chats: Grindr checks that both users have chatted with each other
     * (both must have sent at least one message to the other) in order to allow videocalls.
     *
     * This hook allows the user to bypass this restriction.
     */
    fun allowVideocallsOnEmptyChats() {
        val class_Continuation = findClass(
            "kotlin.coroutines.Continuation",
            Hooker.pkgParam.classLoader
        ) //I tried using Continuation::class.java, but that only gives a different Class instance (does not work)

        val class_ChatRepo = findClass(
            GApp.persistence.repository.ChatRepo,
            Hooker.pkgParam.classLoader
        )

        findAndHookMethod(
            class_ChatRepo,
            GApp.persistence.repository.ChatRepo_.checkMessageForVideoCall,
            String::class.java,
            class_Continuation,
            RETURN_TRUE
        )
    }

    /**
     * Allow Fake GPS in order to fake location.
     *
     * WARNING: Abusing this feature may result in a permanent ban on your Grindr account.
     */
    fun allowMockProvider() {
        val class_Location = findClass(
            "android.location.Location",
            Hooker.pkgParam.classLoader
        )
        
        findAndHookMethod(
            class_Location,
            "isFromMockProvider",
            RETURN_FALSE
        )

        if(Build.VERSION.SDK_INT >= 31) {
            findAndHookMethod(
                class_Location,
                "isMock",
                RETURN_FALSE
            )
        }
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
        val class_ProfileUtils = findClass(GApp.utils.ProfileUtils, Hooker.pkgParam.classLoader)
        setStaticLongField(class_ProfileUtils, GApp.utils.ProfileUtils_.onlineIndicatorDuration, duration.inWholeMilliseconds)
    }

    /**
     * Allow unlimited taps on profiles.
     *
     * @author ElJaviLuki
     */
    fun unlimitedTaps() {
        val class_TapsAnimLayout = findClass(GApp.view.TapsAnimLayout, Hooker.pkgParam.classLoader)
        val class_ChatMessage = findClass(GApp.persistence.model.ChatMessage, Hooker.pkgParam.classLoader)

        val tapTypeToHook = getStaticObjectField(class_ChatMessage, GApp.persistence.model.ChatMessage_.TAP_TYPE_NONE)

        //Reset the tap value to allow multitapping.
        findAndHookMethod(
            class_TapsAnimLayout,
            GApp.view.TapsAnimLayout_.setTapType,
            String::class.java,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook(){
                override fun afterHookedMethod(param: MethodHookParam) {
                    setObjectField(
                        param.thisObject,
                        GApp.view.TapsAnimLayout_.tapType,
                        tapTypeToHook
                    )
                }
            }
        )

        //Reset taps on long press (allows using tap variants)
        findAndHookMethod(
            class_TapsAnimLayout,
            GApp.view.TapsAnimLayout_.getCanSelectVariants,
            RETURN_TRUE
        )
    }

    /**
     * Hook the method that returns the duration of the expiring photos.
     * This way, the photos will not expire and you will be able to see them any time you want.
     *
     * @author ElJaviLuki
     */
    fun removeExpirationOnExpiringPhotos() {
        val class_ExpiringImageBody = findClass(GApp.model.ExpiringImageBody, Hooker.pkgParam.classLoader)
        findAndHookMethod(
            class_ExpiringImageBody,
            GApp.model.ExpiringImageBody_.getDuration,
            RETURN_LONG_MAX_VALUE
        )
    }
}