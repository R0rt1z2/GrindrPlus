package com.grindrplus.decorated.tabs.model

import com.grindrplus.Hooker
import com.grindrplus.Obfuscation
import com.grindrplus.decorated.persistence.model.ChatMessage
import de.robv.android.xposed.XposedHelpers

class TapType(
    private var instance: Any = XposedHelpers.newInstance(
    ChatMessage.CLAZZ
)) {
    var analyticsKey: String? //FIXME: Use a decorator for album
        get() = XposedHelpers.getObjectField(instance, "analyticsKey") as String?
        set(value) = XposedHelpers.setObjectField(instance, "analyticsKey", value)
    companion object {
        val CLAZZ: Class<*> by lazy {
            XposedHelpers.findClass(Obfuscation.GApp.taps.model.TapType, Hooker.pkgParam.classLoader)
        }

        val FRIENDLY: Any?
            get() {
                return XposedHelpers.getStaticObjectField(CLAZZ, "FRIENDLY")
            }
        val HOT: Any?
            get() {
                return XposedHelpers.getStaticObjectField(CLAZZ, "HOT")
            }
        val LOOKING: Any?
            get() {
                return XposedHelpers.getStaticObjectField(CLAZZ, "LOOKING")
            }
        val NONE: Any?
            get() {
                return XposedHelpers.getStaticObjectField(CLAZZ, "NONE")
            }

        fun values(): Any? {
            return XposedHelpers.callStaticMethod(CLAZZ, "values")
        }
    }
}
