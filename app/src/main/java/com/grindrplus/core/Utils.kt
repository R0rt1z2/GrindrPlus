package com.grindrplus.core

import android.content.Context
import android.content.Intent
import com.grindrplus.GrindrPlus
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
            GrindrPlus.loadClass("com.grindrapp.android.ui.chat.ChatActivityV2\$a")
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
        val profilesActivityClass =
            GrindrPlus.loadClass("com.grindrapp.android.ui.profileV2.ProfilesActivity")
        val profilesActivityInstance = profilesActivityClass.let { safeGetField(it, "u0") }
        val referrerTypeClass =
            GrindrPlus.loadClass("com.grindrapp.android.base.model.profile.ReferrerType")
        val referrerType = referrerTypeClass.getField("NOTIFICATION").get(null)
        val profilesActivityInnerClass =
            GrindrPlus.loadClass("com.grindrapp.android.ui.profileV2.ProfilesActivity\$a")

        val method = profilesActivityInnerClass.declaredMethods.find {
            it.parameterTypes.size == 3 && it.parameterTypes[2] == referrerTypeClass
        }

        val intent = method?.invoke(
            profilesActivityInstance,
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

    fun fetchProfileById(id: Long): Any {
        val profileRepoClass =
            GrindrPlus.loadClass("com.grindrapp.android.persistence.repository.ProfileRepo")
        val fetchProfileByIdMethod = profileRepoClass.declaredMethods.find {
            it.name == "fetchProfileById"
        }
        val profileRepoInstance  = GrindrPlus.instanceManager.getInstance<Any>("com.grindrapp.android.persistence.repository.ProfileRepo")

        val result = GrindrPlus.coroutineHelper.callSuspendFunction { continuation ->
            fetchProfileByIdMethod?.invoke(profileRepoInstance, id, continuation)
        }

        return result
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
}