package com.grindrplus.decorated

import com.grindrplus.Hooker
import com.grindrplus.Obfuscation
import de.robv.android.xposed.XposedHelpers
import java.lang.Class

class R {
    class id {
        companion object {
            val CLAZZ: Class<*> by lazy {
                XposedHelpers.findClass(Obfuscation.GApp.R.id, Hooker.pkgParam.classLoader)
            }

            private fun getResId(resId: String) = XposedHelpers.getStaticIntField(
                CLAZZ,
                resId
            )

            val fragment_favorite_recycler_view
                get() = getResId(Obfuscation.GApp.R.id_.fragment_favorite_recycler_view)

            val profile_distance
                get() = getResId(Obfuscation.GApp.R.id_.profile_distance)

            val profile_online_now_icon
                get() = getResId(Obfuscation.GApp.R.id_.profile_online_now_icon)

            val profile_last_seen
                get() = getResId(Obfuscation.GApp.R.id_.profile_last_seen)

            val profile_note_icon
                get() = getResId(Obfuscation.GApp.R.id_.profile_note_icon)

            val profile_display_name
                get() = getResId(Obfuscation.GApp.R.id_.profile_display_name)
        }
    }
}