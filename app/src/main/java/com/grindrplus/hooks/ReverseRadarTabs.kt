package com.grindrplus.hooks

import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.ui.Utils
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

class ReverseRadarTabs : Hook(
    "Reverse radar tabs",
    "Shows the received taps before profile views"
) {

    private inline fun <reified T : Any> getJavaClass() = T::class.java

    private val radarTabs = "W7.c"
    private val radarFragment = "com.grindrapp.android.radar.presentation.ui.RadarFragment"

    override fun init() {
        //Alternative which just scrolls to the tab fragment instead
        /*val viewPagerClass = findClass("androidx.viewpager2.widget.ViewPager2")
        findClass("com.grindrapp.android.radar.presentation.ui.RadarFragment")
            .hook("onViewCreated", HookStage.AFTER) {
                val view = it.arg<View>(0)
                val viewPager = view.findViewById<View>(
                    Utils.getId(
                        "radar_view_pager",
                        "id",
                        GrindrPlus.context
                    )
                )
                val setCurrentItem = viewPagerClass.getMethod("setCurrentItem", Int::class.java, Boolean::class.java)
                viewPager.post {
                    setCurrentItem.invoke(viewPager, 1, false)
                }
                //viewPagerClass.getMethod("setCurrentItem", Int::class.java).invoke(viewPager, 1)
            }*/

        val radarTabs = XposedHelpers.callStaticMethod(findClass(radarTabs), "values") as Array<*>

        findClass("$radarFragment\$a")
            .hookConstructor(HookStage.BEFORE) { param ->
                val tabs = param.arg(2, List::class.java)!!
                val reversed = findClass("kotlinx.collections.immutable.ExtensionsKt")
                    .getMethod("toImmutableList", Iterable::class.java)
                    .invoke(null, tabs.reversed())
                param.setArg(2, reversed)
            }


        //Unfortunately, some methods in RadarFragment use the ordinal of the RadarTab enum to determine the position in the TabLayout
        findClass("com.google.android.material.tabs.TabLayout")
            .hook("getTabAt", HookStage.BEFORE) { param ->
                if (!Thread.currentThread().stackTrace.any {
                        it.className.contains(radarFragment)
                    }) return@hook
                param.setArg(0, radarTabs.size - 1 - param.arg<Int>(0))
            }
    }

}