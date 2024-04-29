package com.grindrplus.hooks

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.getObjectField

class ModSettings : Hook(
    "Mod settings",
    "GrindrPlus settings"
) {
    private val settingsViewModelBinding = "q7.v0"
    private val settingsActivity = "com.grindrapp.android.ui.settings.SettingsActivity"
    private val hooksFragment = "com.grindrplus.ui.fragments.HooksFragment"

    override fun init() {
        val nestedScrollViewClass = findClass("androidx.core.widget.NestedScrollView") ?: return
        val getChildAtNestedScrollView = nestedScrollViewClass.getMethod(
            "getChildAt",
            Int::class.java
        )

        val fragmentClass = findClass("androidx.fragment.app.Fragment") ?: return
        val fragmentActivityClass = findClass("androidx.fragment.app.FragmentActivity") ?: return
        val getSupportFragmentManager = fragmentActivityClass.getMethod("getSupportFragmentManager")
        val fragmentManagerClass = findClass("androidx.fragment.app.FragmentManager") ?: return
        val beginTransaction = fragmentManagerClass.getMethod("beginTransaction")
        val fragmentTransactionClass =
            findClass("androidx.fragment.app.FragmentTransaction") ?: return
        val addFragmentTransaction =
            fragmentTransactionClass.getMethod("add", Int::class.java, fragmentClass)
        val commitFragmentTransaction = fragmentTransactionClass.getMethod("commit")

        findClass(settingsActivity)
            ?.hook("onCreate", HookStage.AFTER) { param ->
                val activity = param.thisObject as Activity

                val settingsViewBindingLazy = getObjectField(param.thisObject, "a0")
                val settingsViewBinding = settingsViewBindingLazy::class
                    .java.getMethod("getValue").invoke(settingsViewBindingLazy)
                val settingsViewBindingRoot = settingsViewBinding::class
                    .java.getMethod("getRoot").invoke(settingsViewBinding) as RelativeLayout
                val settingsNestedScrollView = settingsViewBindingRoot.getChildAt(1)

                if (nestedScrollViewClass.isInstance(settingsNestedScrollView)) {
                    val settingsScrollingContentLayout = getChildAtNestedScrollView
                        .invoke(settingsNestedScrollView, 0) as LinearLayout
                    val settingsExampleContainer = settingsScrollingContentLayout
                        .getChildAt(2) as LinearLayout
                    val settingsExampleHeader = settingsExampleContainer.getChildAt(0) as TextView
                    val settingsExampleSubContainer =
                        settingsExampleContainer.getChildAt(1) as LinearLayout
                    val settingsExampleSubContainerTextView =
                        settingsExampleSubContainer.getChildAt(0) as TextView

                    val modContainer = LinearLayout(activity).apply {
                        layoutParams = settingsExampleContainer.layoutParams
                        orientation = settingsExampleContainer.orientation
                        setPadding(
                            settingsExampleContainer.paddingLeft,
                            settingsExampleContainer.paddingTop,
                            settingsExampleContainer.paddingRight,
                            settingsExampleContainer.paddingBottom
                        )
                        id = View.generateViewId()
                        textAlignment = settingsExampleContainer.textAlignment
                    }

                    val modHeader = TextView(activity).apply {
                        text = "GrindrPlus"
                        layoutParams = settingsExampleHeader.layoutParams
                        setPadding(
                            settingsExampleHeader.paddingLeft, settingsExampleHeader.paddingTop,
                            settingsExampleHeader.paddingRight, settingsExampleHeader.paddingBottom
                        )
                        textSize = 14f
                        isAllCaps = true
                        setTypeface(
                            settingsExampleHeader.typeface,
                            settingsExampleHeader.typeface.style
                        )
                        setTextColor(settingsExampleHeader.currentTextColor)
                    }

                    val modSubContainer = LinearLayout(activity).apply {
                        layoutParams = settingsExampleSubContainer.layoutParams
                        orientation = settingsExampleSubContainer.orientation
                        setPadding(
                            settingsExampleSubContainer.paddingLeft,
                            settingsExampleSubContainer.paddingTop,
                            settingsExampleSubContainer.paddingRight,
                            settingsExampleSubContainer.paddingBottom
                        )
                        id = View.generateViewId()
                        textAlignment = settingsExampleSubContainer.textAlignment
                    }

                    val modSubContainerTextView = TextView(activity).apply {
                        text = "Mod Settings"
                        layoutParams = settingsExampleSubContainerTextView.layoutParams
                        setPadding(
                            settingsExampleSubContainerTextView.paddingLeft,
                            settingsExampleSubContainerTextView.paddingTop,
                            settingsExampleSubContainerTextView.paddingRight,
                            settingsExampleSubContainerTextView.paddingBottom
                        )
                        textSize = 16f
                        setTypeface(
                            settingsExampleSubContainerTextView.typeface,
                            settingsExampleSubContainerTextView.typeface.style
                        )
                        setTextColor(settingsExampleSubContainerTextView.currentTextColor)
                        visibility = View.VISIBLE
                    }

                    modSubContainerTextView.setOnClickListener {
                        val hooksFragmentInstance = findClass(hooksFragment)?.newInstance()
                        val supportFragmentManager = getSupportFragmentManager.invoke(activity)
                        val fragmentTransaction = beginTransaction.invoke(supportFragmentManager)
                        addFragmentTransaction.invoke(
                            fragmentTransaction,
                            android.R.id.content,
                            hooksFragmentInstance
                        )
                        commitFragmentTransaction.invoke(fragmentTransaction)
                    }

                    modSubContainer.addView(modSubContainerTextView, 0)
                    modContainer.addView(modHeader, 0)
                    modContainer.addView(modSubContainer, 1)
                    settingsScrollingContentLayout.addView(modContainer, 0)
                }
            }

        findClass(settingsViewModelBinding)
            ?.hookConstructor(HookStage.AFTER) { param ->
                val versionTextView = param.arg(55) as TextView
                versionTextView.setOnClickListener {
                    GrindrPlus.showToast(
                        Toast.LENGTH_LONG,
                        "GrindrPlus v${BuildConfig.VERSION_NAME}"
                    )
                }
            }
    }

}