package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.*

class ReverseRadarTabs : Hook(
    "Reverse radar tabs",
    "Shows the received taps before profile views"
) {

    private inline fun <reified T : Any> getJavaClass() = T::class.java

    private val radarTabs = "m8.b"
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

        val radarTabs = callStaticMethod(findClass(radarTabs), "values") as Array<*>

        findClass("$radarFragment\$a")
            .hookConstructor(HookStage.BEFORE) { param ->
                val tabs = param.arg(2, List::class.java)!!
                val reversed = findClass("kotlinx.collections.immutable.ExtensionsKt")
                    .getMethod("toImmutableList", Iterable::class.java)
                    .invoke(null, tabs.reversed())
                param.setArg(2, reversed)
            }


        //Unfortunately, some methods in RadarFragment use the ordinal of the RadarTab enum to determine the position in the TabLayout,
        //which no longer reflects the actual position in the ViewPager
        findClass("com.google.android.material.tabs.TabLayout")
            .hook("getTabAt", HookStage.BEFORE) { param ->
                if (!Thread.currentThread().stackTrace.any {
                        it.className.contains(radarFragment)
                    }) return@hook
                param.setArg(0, radarTabs.size - 1 - param.arg<Int>(0))
            }

        //This LiveData observer consumes the integer values from a flow and makes the ViewPager scroll to that position
        //This is used to make the ViewPager scroll to the taps page upon clicking such notification.
        //However, this is based on the ordinal values again, so we have to reverse the values, too.
        findClass("Ma.B")
            .hook("onChanged", HookStage.BEFORE) { param ->
                //Check whether this observer is responsible for handling the ViewPager position
                if (getIntField(param.thisObject(), "b") != 1) return@hook
                val position = param.arg<Number>(0).toInt()
                param.setArg(0, Integer.valueOf(radarTabs.size - 1 - position))
            }

        //The modified LiveData observer will no longer scroll to the first position when receiving a value 0.
        //In order to still show the new first tab (TapsFragment) by default, we need to set the initial value to the last index,
        //so that the LiveData observer reverts it back to 0.
        findClass("La.s0")
            .hookConstructor(HookStage.AFTER) { param ->
                val stateFlow = getObjectField(param.thisObject(), "J0")
                //Set initial position to the last page, so that the onChanged hook of the LiveData observer reverts it to the new first page
                callMethod(stateFlow, "setValue", Integer.valueOf(radarTabs.size - 1))
            }
    }

}