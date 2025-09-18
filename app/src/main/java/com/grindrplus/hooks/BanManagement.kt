package com.grindrplus.hooks

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.loadClass
import com.grindrplus.GrindrPlus.restartGrindr
import com.grindrplus.core.Config
import com.grindrplus.core.Logger
import com.grindrplus.core.constants.GrindrApiError
import com.grindrplus.core.constants.GrindrApiError.Companion.getErrorDescription
import com.grindrplus.core.logd
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.core.logw
import com.grindrplus.ui.Utils.copyToClipboard
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.RetrofitUtils.getFailValue
import com.grindrplus.utils.RetrofitUtils.isFail
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import com.grindrplus.utils.withSuspendResult
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.getObjectField
import org.json.JSONObject

class BanManagement : Hook(
    "Ban management",
    "Provides comprehensive ban management tools (detailed ban info, etc.)"
) {
    private val authServiceClass = "x5.h" // search for 'v3/users/password-validation'
    private val materialButton = "com.google.android.material.button.MaterialButton"
    private val bannedFragment = "com.grindrapp.android.ui.account.banned.BannedFragment"
    private val deviceUtility = "Ue.m" // search for 'Settings.Secure.getString(context.getContentResolver(), "android_id")'
    private val bannedArgs = "B5.a" // search for 'new StringBuilder("BannedArgs(bannedType=")'
    private var bannedInfo: JSONObject = JSONObject()

    @SuppressLint("DiscouragedApi")
    override fun init() {
        val authService = findClass(authServiceClass)

        RetrofitUtils.hookService(
            authService,
        ) { originalHandler, proxy, method, args ->
            val result = originalHandler.invoke(proxy, method, args)
            val isLogin = args.size > 1 && args[1] != null &&
                    args[1]!!::class.java.name.contains("LoginEmailRequest")
            when {
                isLogin -> {
                    withSuspendResult(args, result) { args, result ->
                        try {
                            handleLoginResult(result)
                        } catch (_: Throwable) {
                            // Ignore exceptions here
                        }

                        return@withSuspendResult result
                    }
                }
            }

            result
        }

        findClass(deviceUtility).hook("g", HookStage.AFTER) { param ->
            val androidId = Config.get("android_device_id", "") as String
            if (androidId.isNotEmpty()) {
                param.setResult(androidId)
            }
        }

        findClass(bannedArgs).hookConstructor(HookStage.AFTER) { param ->
            val args = param.args()
            val json = JSONObject()
            json.put("code", args[0].toString())
            json.put("message", args[1])
            json.put("mail", args[2])
            json.put("phoneNumber", args[3])
            json.put("dialCode", args[4])
            json.put("isBanAutomated", args[5])
            json.put("subReason", args[6])
            bannedInfo = json
        }

        findClass(bannedFragment)
            .hook("onViewCreated", HookStage.AFTER) { param ->
                try {
                    val view = param.args()[0] as View
                    val context = view.context

                    val manageSubscriptionId = context.resources.getIdentifier("manage_subscription", "id", context.packageName)
                    val manageSubscriptionButton = view.findViewById<View>(manageSubscriptionId)

                    val buttonLayoutId = context.resources.getIdentifier("layout_ban_screen_buttons", "id", context.packageName)
                    val buttonLayout = view.findViewById<LinearLayout>(buttonLayoutId)

                    if (manageSubscriptionButton != null && buttonLayout != null) {
                        val primaryButtonStyle = context.resources
                            .getIdentifier("PrimaryButton", "style", context.packageName)

                        val newButton = loadClass(materialButton).getDeclaredConstructor(
                            Context::class.java,
                            loadClass("android.util.AttributeSet"),
                            Int::class.java
                        ).newInstance(context, null, primaryButtonStyle) as View

                        safeCallMethod(newButton, "setLayoutParams", manageSubscriptionButton.layoutParams)
                        safeCallMethod(newButton, "setBackground", safeCallMethod(manageSubscriptionButton, "getBackground"))
                        safeCallMethod(newButton, "setTextColor", safeCallMethod(manageSubscriptionButton, "getTextColors"))
                        safeCallMethod(newButton, "setBackgroundTintList", safeCallMethod(manageSubscriptionButton, "getBackgroundTintList"))

                        try {
                            val horizontalPadding = safeCallMethod(manageSubscriptionButton, "getPaddingLeft") as? Int ?: dpToPx(context, 8)
                            val verticalPadding = dpToPx(context, 14)
                            safeCallMethod(newButton, "setPadding", horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                        } catch (e: Exception) {
                            loge("Error copying padding: ${e.message}")
                        }

                        safeCallMethod(newButton, "setCornerRadius", safeCallMethod(manageSubscriptionButton, "getCornerRadius"))
                        safeCallMethod(newButton, "setRippleColor", safeCallMethod(manageSubscriptionButton, "getRippleColor"))
                        safeCallMethod(newButton, "setStrokeColor", safeCallMethod(manageSubscriptionButton, "getStrokeColor"))
                        safeCallMethod(newButton, "setStrokeWidth", safeCallMethod(manageSubscriptionButton, "getStrokeWidth"))
                        safeCallMethod(newButton, "setElevation", safeCallMethod(manageSubscriptionButton, "getElevation"))

                        safeCallMethod(newButton, "setTextAlignment", safeCallMethod(manageSubscriptionButton, "getTextAlignment"))
                        safeCallMethod(newButton, "setGravity", safeCallMethod(manageSubscriptionButton, "getGravity"))
                        safeCallMethod(newButton, "setMinHeight", dpToPx(context, 38))
                        safeCallMethod(newButton, "setText", "Show Ban Details")

                        safeCallMethod(newButton, "setOnClickListener", View.OnClickListener {
                            if (bannedInfo.length() == 0) {
                                Toast.makeText(context, "No ban details available", Toast.LENGTH_SHORT).show()
                            } else {
                                createBanDetailsDialog(context, bannedInfo)
                            }
                        })

                        buttonLayout.addView(newButton)
                    }
                } catch (e: Exception) {
                    loge("BannedFragment: Error in hook: ${e.message}")
                    Logger.writeRaw(e.stackTraceToString())
                }
            }
    }

    private fun handleLoginResult(result: Any) {
        if (result.isFail()) {
            val body = JSONObject(getObjectField(
                result.getFailValue(), "b") as String)
            if (body.has("reason")) {
                logi("Intercepted a banned response!")
                bannedInfo = body
            } else {
                logw("User is not banned, but failed to login?")
            }
        } else {
            logd("User is not banned, login should be successful")
        }
    }

    private fun createBanDetailsDialog(context: Context, bannedInfo: JSONObject) {
        val message = StringBuilder()
        message.append("Your account has been banned from " +
                "Grindr and the server replied with the following information:\n\n")

        val mappings = mapOf(
            "code" to "Details",
            "message" to "Message",
            "profileId" to "Profile ID",
            "type" to "Type",
            "reason" to "Reason",
            "isBanAutomated" to "Automated Ban",
            "thirdPartyUserIdToShow" to "Third Party User ID",
            "subReason" to "Sub Reason",
            "mail" to "Mail",
            "phoneNumber" to "Phone Number",
            "dialCode" to "Dial Code"
        )

        val isDeviceBan = bannedInfo.optString("code", "").let { code ->
            code.isNotEmpty() && GrindrApiError.isErrorType(
                code.toIntOrNull() ?: code, GrindrApiError.ERR_DEVICE_BANNED)
        }

        mappings.forEach { (key, label) ->
            if (bannedInfo.has(key) && !bannedInfo.isNull(key)) {
                val value = when (key) {
                    "isBanAutomated" -> if (bannedInfo.getBoolean(key)) "Yes" else "No"
                    "code" -> {
                        val codeValue = bannedInfo.getString(key)
                        getErrorDescription(codeValue.toIntOrNull() ?: codeValue)
                    }
                    else -> bannedInfo.getString(key)
                }

                if (value != null && value.isNotEmpty() && value != "null") {
                    message.append("• $label: $value\n")
                }
            }
        }

        if (isDeviceBan) {
            message.append("\nYour device has been banned rather than just your account. " +
                    "This may or may not mean your account is also banned. " +
                    "You can bypass this restriction by generating different device information.")
        }

        val dialog = android.app.AlertDialog.Builder(GrindrPlus.currentActivity)
            .setTitle("Ban Details")
            .setMessage(message.toString())
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        if (isDeviceBan) {
            dialog.setNeutralButton("Generate New Device ID") { _, _ ->
                val uuid = java.util.UUID.randomUUID()
                val newDeviceId = uuid.toString().replace("-", "")
                Config.put("android_device_id", newDeviceId)
                restartGrindr(1500, "New device ID generated. Grindr will restart now.")
            }
        } else {
            dialog.setNeutralButton("Copy JSON") { _, _ ->
                copyToClipboard("Ban Details", bannedInfo.toString(2))
                Toast.makeText(
                    GrindrPlus.currentActivity,
                    "Ban details copied to clipboard",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        GrindrPlus.currentActivity?.runOnUiThread {
            dialog.create().show()
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun safeCallMethod(obj: Any?, methodName: String, vararg args: Any?): Any? {
        if (obj == null) return null

        return try {
            XposedHelpers.callMethod(obj, methodName, *args)
        } catch (e: Exception) {
            loge("Failed to call method: $methodName: ${e.message}")
            null
        }
    }
}