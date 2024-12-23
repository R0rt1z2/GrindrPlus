package com.grindrplus.ui.fragments

import Database
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.materialswitch.MaterialSwitch
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.ui.Utils
import com.grindrplus.ui.colors.Colors
import java.io.File
import kotlin.system.exitProcess

class SettingsFragment : Fragment() {
    private var isDatabaseOperation: Boolean = false
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportLauncher: ActivityResultLauncher<Intent>
    private lateinit var subLinearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    if (isDatabaseOperation) {
                        exportDatabaseToUri(uri)
                    } else {
                        exportConfigToUri(uri)
                    }
                }
            }
        }

        importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    if (isDatabaseOperation) {
                        importDatabaseFromUri(uri)
                    } else {
                        importConfigFromUri(uri)
                    }
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
            setBackgroundColor(Colors.grindr_dark_amoled_black)
            id = Utils.getId("activity_content", "id", context)
        }

        val customToolbar = createCustomToolbarWithMenu(context)
        rootLayout.addView(customToolbar)

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
            setBackgroundColor(Colors.grindr_dark_amoled_black)
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
            setBackgroundColor(Colors.grindr_dark_amoled_black)
            elevation = 6f
        }

        val toolbarTitle = TextView(context).apply {
            text = "Mod Settings"
            textSize = 16f
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
        }

        toolbar.addView(toolbarTitle)

        toolbar.menu.add(Menu.NONE, 1, Menu.NONE, "Export config").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        toolbar.menu.add(Menu.NONE, 2, Menu.NONE, "Import config").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        toolbar.menu.add(Menu.NONE, 3, Menu.NONE, "Export database").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        toolbar.menu.add(Menu.NONE, 4, Menu.NONE, "Import database").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        toolbar.menu.add(Menu.NONE, 5, Menu.NONE, "Reset GrindrPlus").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    promptFolderSelection(false)
                    true
                }
                2 -> {
                    promptImportSelection(false)
                    true
                }
                3 -> {
                    promptFolderSelection(true)
                    true
                }
                4 -> {
                    promptImportSelection(true)
                    true
                }
                5 -> {
                    showResetConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        // Set black amoled background color
        setOverflowButtonColor(toolbar, Colors.grindr_off_white)

        appBarLayout.addView(toolbar)
        container.addView(appBarLayout)
    }

    private fun setOverflowButtonColor(toolbar: Toolbar, color: Int) {
        val overflowIcon = toolbar.overflowIcon
        overflowIcon?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        toolbar.overflowIcon = overflowIcon
    }

    private fun importDatabaseFromUri(uri: Uri) {
        val context = requireContext()
        val backupPath = File(context.cacheDir, "grindrplus_backup.db").absolutePath
        val databasePath = context.filesDir.absolutePath + "/grindrplus.db"

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val backupFile = File(backupPath)
                inputStream.copyTo(backupFile.outputStream())

                val database = Database(context, databasePath)
                val restored = database.restoreDatabase(backupPath)

                if (restored) {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Database imported successfully!", context)
                    showImportSuccessDialog()
                } else {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Failed to restore database!", context)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GrindrPlus.showToast(Toast.LENGTH_LONG, "Failed to import database!", context)
        }
    }

    private fun importConfigFromUri(uri: Uri) {
        val context = requireContext()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val configJson = inputStream.bufferedReader().use { it.readText() }
                Config.importFromJson(configJson)
                updateUIFromConfig()
                GrindrPlus.showToast(Toast.LENGTH_LONG, "Config imported successfully!", context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GrindrPlus.showToast(Toast.LENGTH_LONG, "Failed to import config!", context)
        }
    }

    private fun showImportSuccessDialog() {
        val context = requireContext()
        AlertDialog.Builder(context)
            .setTitle("Database Import")
            .setMessage("The database has been successfully imported. The app will now close to apply the changes.")
            .setPositiveButton("OK") { _, _ ->
                closeApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun promptImportSelection(isDatabase: Boolean) {
        isDatabaseOperation = isDatabase
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if (isDatabase) {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3", "application/db", "*/*"))
            } else {
                type = "application/json"
            }
        }
        importLauncher.launch(intent)
    }

    private fun promptFolderSelection(isDatabase: Boolean) {
        isDatabaseOperation = isDatabase
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        exportLauncher.launch(intent)
    }

    private fun exportDatabaseToUri(uri: Uri) {
        val context = requireContext()
        val databasePath = "${context.filesDir.absolutePath}/grindrplus.db"
        val databaseFile = File(databasePath)

        try {
            val childUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )

            val newFileUri = DocumentsContract.createDocument(
                context.contentResolver,
                childUri,
                "application/x-sqlite3",
                "grindrplus_backup.db"
            )

            if (newFileUri != null) {
                context.contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
                    databaseFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    GrindrPlus.showToast(
                        Toast.LENGTH_LONG,
                        "Database exported successfully!",
                        context
                    )
                }
            } else {
                GrindrPlus.showToast(
                    Toast.LENGTH_LONG,
                    "Failed to create file in the selected folder!",
                    context
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Failed to export database!",
                context
            )
        }
    }


    private fun exportConfigToUri(uri: Uri) {
        val context = requireContext()
        val fileName = "grindrplus_config.json"
        val mimeType = "application/json"
        val configJson = Config.getConfigJson()

        try {
            val childUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )

            val newFileUri = DocumentsContract.createDocument(
                context.contentResolver,
                childUri,
                mimeType,
                fileName
            )

            if (newFileUri != null) {
                context.contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
                    outputStream.write(configJson.toByteArray())
                    outputStream.flush()
                }
                Toast.makeText(context, "Config exported successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create file in the selected folder!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export config!", Toast.LENGTH_LONG).show()
        }
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

        container?.addView(createToggleableSettingView(context, "Force Incognito Mode", "Always appear as offline to other users", "force_incognito"))
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
        closeApp()
    }

    fun closeApp() {
        val activity = requireActivity()
        activity.finishAffinity()
        exitProcess(0)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
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

        val hookSwitch = Switch(context).apply {
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

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun createToggleableSettingView(context: Context, title: String, description: String, key: String): View {
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

        val currentValue = Config.get(key, false) as Boolean
        val settingSwitch = Switch(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isChecked = currentValue
            setOnCheckedChangeListener { _, isChecked ->
                Config.put(key, isChecked)
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
        horizontalLayout.addView(settingSwitch)

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

    private fun showPopupMenu(anchor: View, context: Context) {
        val popupMenu = PopupMenu(context, anchor)
        popupMenu.menu.add("Export Config")
        popupMenu.menu.add("Import Config")
        popupMenu.menu.add("Export Database")
        popupMenu.menu.add("Import Database")
        popupMenu.menu.add("Reset GrindrPlus")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Export Config" -> {
                    promptFolderSelection(false)
                    true
                }
                "Import Config" -> {
                    promptImportSelection(false)
                    true
                }
                "Export Database" -> {
                    promptFolderSelection(true)
                    true
                }
                "Import Database" -> {
                    promptImportSelection(true)
                    true
                }
                "Reset GrindrPlus" -> {
                    showResetConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun createCustomToolbarWithMenu(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Colors.grindr_dark_amoled_black)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 56f, context.resources.displayMetrics
                ).toInt()
            ).apply {
                setPadding(50, 0, 16, 0)
            }

            val title = TextView(context).apply {
                text = "Mod Settings"
                textSize = 20f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also {
                    it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                }
            }
            addView(title)

            val menuIcon = ImageView(context).apply {
                setImageResource(Utils.getId(
                    "abc_ic_menu_overflow_material",
                    "drawable",
                    context
                ))
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }
                setOnClickListener { view ->
                    showPopupMenu(view, context)
                }
            }
            addView(menuIcon)
        }
    }
}
