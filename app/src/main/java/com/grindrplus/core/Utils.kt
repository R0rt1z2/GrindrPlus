package com.grindrplus.core

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import com.grindrplus.ui.Utils.getId
import com.grindrplus.utils.RetrofitUtils
import java.lang.reflect.Proxy
import kotlin.math.pow

object Utils {
    fun createServiceProxy(
        originalService: Any,
        serviceClass: Class<*>,
        blacklist: Array<String> = emptyArray()
    ): Any {
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        val successConstructor =
            GrindrPlus.loadClass(RetrofitUtils.SUCCESS_CLASS_NAME).constructors.firstOrNull()
        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(serviceClass)
        ) { proxy, method, args ->
            if (successConstructor != null && (blacklist.isEmpty() || method.name in blacklist)) {
                successConstructor.newInstance(Unit)
            } else {
                invocationHandler.invoke(proxy, method, args)
            }
        }
    }

    fun openChat(id: String) {
        val chatActivityInnerClass =
            GrindrPlus.loadClass("com.grindrapp.android.chat.presentation.ui.ChatActivityV2\$a")
        val chatArgsClass =
            GrindrPlus.loadClass("com.grindrapp.android.args.ChatArgs")
        val profileTypeClass =
            GrindrPlus.loadClass("com.grindrapp.android.ui.profileV2.model.ProfileType")
        val referrerTypeClass =
            GrindrPlus.loadClass("com.grindrapp.android.base.model.profile.ReferrerType")
        val conversationMetadataClass =
            GrindrPlus.loadClass("com.grindrapp.android.chat.model.DirectConversationMetaData")

        val conversationMetadataInstance = conversationMetadataClass.constructors.first().newInstance(
            id,
            id.substringBefore(":"),
            id.substringAfter(":")
        )

        val profileType = profileTypeClass.getField("FAVORITES").get(null)
        val refererType = referrerTypeClass.getField("UNIFIED_CASCADE").get(null)

        val chatArgsInstance = chatArgsClass.constructors.first().newInstance(
            conversationMetadataInstance,
            "0xDEADBEEF", // str
            profileType,
            refererType,
            "0xDEADBEEF", // str2
            "0xDEADBEEF", // str3
            null,
            null, // chatMediaDrawerArgs
            844
        )

        val method = chatActivityInnerClass.declaredMethods.find {
            it.parameterTypes.size == 2 && it.parameterTypes[1] == chatArgsClass
        }

        val intent = method?.invoke(
            null,
            GrindrPlus.context,
            chatArgsInstance
        ) as Intent?

        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val generalDeepLinksClass =
            GrindrPlus.loadClass("com.grindrapp.android.deeplink.GeneralDeepLinks")
        val startActivityMethod = generalDeepLinksClass.getDeclaredMethod(
            "safedk_Context_startActivity_97cb3195734cf5c9cc3418feeafa6dd6",
            Context::class.java,
            Intent::class.java
        )

        startActivityMethod.invoke(null, GrindrPlus.context, intent)
    }

    fun openProfile(id: String) {
        val referrerTypeClass =
            GrindrPlus.loadClass("com.grindrapp.android.base.model.profile.ReferrerType")
        val referrerType = referrerTypeClass.getField("NOTIFICATION").get(null)
        val profilesActivityInnerClass =
            GrindrPlus.loadClass("com.grindrapp.android.ui.profileV2.ProfilesActivity\$a")

        val method = profilesActivityInnerClass.declaredMethods.find {
            it.parameterTypes.size == 3 && it.parameterTypes[2] == referrerTypeClass
        }

        val intent = method?.invoke(
            null,
            GrindrPlus.context,
            id,
            referrerType
        ) as Intent?
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val generalDeepLinksClass =
            GrindrPlus.loadClass("com.grindrapp.android.deeplink.GeneralDeepLinks")
        val startActivityMethod = generalDeepLinksClass.getDeclaredMethod(
            "safedk_Context_startActivity_97cb3195734cf5c9cc3418feeafa6dd6",
            Context::class.java,
            Intent::class.java
        )

        startActivityMethod.invoke(null, GrindrPlus.context, intent)
    }

    fun weightToNum(isMetric: Boolean, weight: String): Double {
        return if (isMetric) {
            weight.replace("kg", "").toDouble()
        } else {
            weight.replace("lbs", "").toDouble()
        }
    }

    fun heightToNum(isMetric: Boolean, height: String): Double {
        return if (isMetric) {
            height.replace("cm", "").toDouble()
        } else {
            val heightTokens = height.split("\'", "\"")
            heightTokens[0].toDouble() * 12 + heightTokens[1].toDouble()
        }
    }

    fun calculateBMI(isMetric: Boolean, weight: Double, height: Double): Double {
        return if (isMetric) {
            weight / (height / 100).pow(2)
        } else {
            703 * weight / height.pow(2)
        }
    }

    fun w2n(isMetric: Boolean, weight: String): Double {
        return when {
            isMetric -> weight.substringBefore("kg").trim().toDouble()
            else -> weight.substringBefore("lbs").trim().toDouble()
        }
    }

    fun h2n(isMetric: Boolean, height: String): Double {
        return if (isMetric) {
            height.removeSuffix("cm").trim().toDouble()
        } else {
            val (feet, inches) = height.split("'").let {
                it[0].toDouble() to it[1].replace("\"", "").toDouble()
            }
            feet * 12 + inches
        }
    }

    fun safeGetField(obj: Any, fieldName: String): Any? {
        return try {
            obj::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
            }.get(obj)
        } catch (e: Exception) {
            GrindrPlus.logger.log("Failed to get field $fieldName from object $obj")
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        channelId: String = "default_channel_id",
        channelName: String = "Default Channel",
        channelDescription: String = "Default notifications"
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getId("applovin_ic_warning","drawable", context))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    fun getSystemInfo(context: Context, shouldAddSeparator: Boolean = true): String {
        return buildString {
            if (shouldAddSeparator) appendLine("========================================")
            appendLine("Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("ABI(s): ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    "Security patch: ${Build.VERSION.SECURITY_PATCH}"
                else "Security patch: N/A"
            )
            appendLine("Device model: ${Build.MODEL} (${Build.MANUFACTURER})")
            appendLine(
                try {
                    val grindr = context.packageManager.getPackageInfo("com.grindrapp.android", 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        "Grindr: ${grindr.versionName} (${grindr.longVersionCode})"
                    else
                        "Grindr: ${grindr.versionName} (${grindr.versionCode})"
                } catch (e: Exception) {
                    "Grindr: N/A"
                }
            )
            appendLine(
                try {
                    val lspatch = context.packageManager.getPackageInfo("org.lsposed.lspatch", 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        "LSPatch: ${lspatch.versionName} (${lspatch.longVersionCode})"
                    else
                        "LSPatch: ${lspatch.versionName} (${lspatch.versionCode})"
                } catch (e: Exception) {
                    "LSPatch: N/A"
                }
            )
            appendLine("GrindrPlus: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Xposed API: ${Config.get("xposed_version", "N/A") as Int}")
            if (shouldAddSeparator) appendLine("========================================")
        }
    }
}