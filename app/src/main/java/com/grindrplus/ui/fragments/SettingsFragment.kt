package com.grindrplus.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.grindrplus.core.Config
import com.grindrplus.ui.Utils
import com.grindrplus.ui.colors.Colors
import kotlin.system.exitProcess

class SettingsFragment : Fragment() {

    private lateinit var importConfigLauncher: ActivityResultLauncher<Intent>
    private lateinit var subLinearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        importConfigLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    importConfigFromUri(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        Config.initialize(context)

        val rootLayout = CoordinatorLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Colors.grindr_dark_gray_3)
            id = Utils.getId("activity_content", "id", context)
        }

        val fragmentContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            id = Utils.getId("activity_fragment_container", "id", context)
        }

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).also {
                it.topMargin = getActionBarSize(context)
            }
        }

        val linearLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Colors.grindr_dark_gray_2)
            dividerDrawable = Utils.getDrawable("settings_divider", context)
            orientation = LinearLayout.VERTICAL
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }

        subLinearLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(50, 10, 50, 10)
            orientation = LinearLayout.VERTICAL
        }

        linearLayout.addView(subLinearLayout)
        scrollView.addView(linearLayout)
        fragmentContainer.addView(scrollView)
        rootLayout.addView(fragmentContainer)

        setupToolbar(context, fragmentContainer)
        updateUIFromConfig()

        return rootLayout
    }

    private fun setupToolbar(context: Context, container: FrameLayout) {
        val appBarLayout = AppBarLayout(context).apply {
            setBackgroundColor(Colors.grindr_transparent)
            elevation = 0f
        }

        val toolbar = Toolbar(context).apply {
            id = Utils.getId("fragment_toolbar", "id", context)
            setBackgroundColor(Colors.grindr_dark_gray_3)
            elevation = 6f
        }

        val toolbarTitle = AppCompatTextView(context).apply {
            text = "Mod Settings"
            textSize = 16f
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
        }

        toolbar.addView(toolbarTitle)

        toolbar.menu.add(Menu.NONE, 1, Menu.NONE, "Export current config").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        toolbar.menu.add(Menu.NONE, 2, Menu.NONE, "Import config from file").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        toolbar.menu.add(Menu.NONE, 3, Menu.NONE, "Reset GrindrPlus").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    exportConfig()
                    true
                }
                2 -> {
                    promptFileSelection()
                    true
                }
                3 -> {
                    showResetConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        appBarLayout.addView(toolbar)
        container.addView(appBarLayout)
    }

    private fun promptFileSelection() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importConfigLauncher.launch(intent)
    }

    private fun importConfigFromUri(uri: Uri) {
        val context = requireContext()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val configJson = inputStream.bufferedReader().use { it.readText() }
                Config.importFromJson(configJson)
                updateUIFromConfig()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun exportConfig() {
        val configJson = Config.getConfigJson()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, configJson)
        }

        startActivity(Intent.createChooser(shareIntent, "Share config"))
    }

    private fun updateUIFromConfig() {
        subLinearLayout.removeAllViews()
        addViewsToContainer(subLinearLayout)
    }

    private fun addViewsToContainer(container: LinearLayout?) {
        val context = requireContext()

        val manageHooksTitle = AppCompatTextView(context).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setTextAppearance(Utils.getId("TextAppearanceH6AllCaps", "styles", context))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.topMargin = 44
                params.bottomMargin = 49
            }
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
            text = "Manage Hooks"
            isAllCaps = true
            setTextColor(Colors.text_secondary_dark_bg)
        }
        container?.addView(manageHooksTitle)

        val hooks = Config.getHooksSettings()
        hooks.forEach { (hookName, pair) ->
            if (hookName != "Mod settings") {
                val hookView = createHookSwitch(context, hookName, pair.second, pair.first)
                container?.addView(hookView)
            }
        }

        val otherSettingsTitle = AppCompatTextView(context).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setTextAppearance(Utils.getId("TextAppearanceH6AllCaps", "styles", context))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.topMargin = 44
                params.bottomMargin = 49
            }
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
            text = "Other Settings"
            isAllCaps = true
            setTextColor(Colors.text_secondary_dark_bg)
        }
        container?.addView(otherSettingsTitle)
        container?.addView(createDynamicSettingView(context, "Online indicator duration (mins)", "Control when your green dot disappears after inactivity", "online_indicator"))
        container?.addView(createDynamicSettingView(context, "Favorites grid size", "Customize grid size of the layout for the favorites tab", "favorites_grid_columns"))
    }

    private fun showResetConfirmationDialog() {
        val context = requireContext()
        AlertDialog.Builder(context).apply {
            setTitle("Reset GrindrPlus")
            setMessage("This will reset the database and the config of the mod, which means your cached albums/pictures will be gone, as well as saved phrases and locations.")
            setPositiveButton("Yes") { _, _ ->
                resetConfigAndCloseApp()
            }
            setNegativeButton("No", null)
        }.create().show()
    }

    private fun resetConfigAndCloseApp() {
        Config.resetConfig(true)
        val activity = requireActivity()
        activity.finishAffinity()
        exitProcess(0)
    }

    private fun createHookSwitch(
        context: Context,
        hookName: String,
        initialState: Boolean,
        description: String
    ): View {
        val hookVerticalLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.topMargin = 44
                params.bottomMargin = 44
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val hookHorizontalLayout = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val hookTitle = AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.weight = 1f
                it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
            textSize = 16f
            text = hookName
        }

        val hookSwitch = SwitchCompat(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isChecked = initialState
            setOnCheckedChangeListener { _, isChecked ->
                Config.setHookEnabled(hookName, isChecked)
            }
        }

        val hookDescription = AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 4f,
                    resources.displayMetrics
                ).toInt()
            }
            setTextColor(Colors.text_primary_dark_bg)
            typeface = Utils.getFont("ibm_plex_sans_fonts", context)
            setTextColor(Colors.grindr_light_gray_0)
            text = description
        }

        hookHorizontalLayout.addView(hookTitle)
        hookHorizontalLayout.addView(hookSwitch)
        hookVerticalLayout.addView(hookHorizontalLayout)
        hookVerticalLayout.addView(hookDescription)

        return hookVerticalLayout
    }

    private fun createDynamicSettingView(context: Context, title: String, description: String, key: String): View {
        val settingLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.topMargin = 44
                params.bottomMargin = 44
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val horizontalLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val settingTitle = AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).also {
                it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
            textSize = 16f
            text = title
        }

        val currentValue = Config.get(key, 3) as Int

        val durationEditText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentValue.toString())
            setSelection(text.length)
            isFocusable = false
            isClickable = true
            setOnClickListener {
                isFocusableInTouchMode = true
                isFocusable = true
                isClickable = false
                requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    isFocusable = false
                    isClickable = true
                    if (text.toString().toIntOrNull() != null) {
                        Config.put(key, text.toString().toIntOrNull()!!)
                    }
                    true
                } else {
                    false
                }
            }
            setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    isFocusable = false
                    isClickable = true
                    if (text.toString().toIntOrNull() != null) {
                        Config.put(key, text.toString().toIntOrNull()!!)
                    }
                }
            }
        }

        val settingDescription = AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 4f,
                    resources.displayMetrics
                ).toInt()
            }
            setTextColor(Colors.text_primary_dark_bg)
            typeface = Utils.getFont("ibm_plex_sans_fonts", context)
            setTextColor(Colors.grindr_light_gray_0)
            text = description
        }

        horizontalLayout.addView(settingTitle)
        horizontalLayout.addView(durationEditText)
        settingLayout.addView(horizontalLayout)
        settingLayout.addView(settingDescription)

        return settingLayout
    }

    private fun getActionBarSize(context: Context): Int {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(
                typedValue.data,
                context.resources.displayMetrics
            )
        }
        return 0
    }
}
