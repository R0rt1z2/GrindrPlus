package com.grindrplus.ui.fragments

import Database
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.core.Utils.getSystemInfo
import com.grindrplus.ui.Utils
import com.grindrplus.ui.colors.Colors
import java.io.File
import kotlin.system.exitProcess

enum class FileType {
    CONFIG,
    DATABASE,
    LOGS,
    BLOCK_LIST,
    FAVORITES_LIST
}

class SettingsFragment : Fragment() {
    private var fileType: FileType = FileType.CONFIG
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportLauncher: ActivityResultLauncher<Intent>
    private lateinit var subLinearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    when (fileType) {
                        FileType.CONFIG -> exportConfigToUri(uri)
                        FileType.DATABASE -> exportDatabaseToUri(uri)
                        FileType.LOGS -> exportLogsToUri(uri)
                        FileType.BLOCK_LIST -> exportFileToUri(uri, "blocks.txt")
                        FileType.FAVORITES_LIST -> exportFileToUri(uri, "favorites.txt")
                    }
                }
            }
        }

        importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    when (fileType) {
                        FileType.CONFIG -> importConfigFromUri(uri)
                        FileType.DATABASE -> importDatabaseFromUri(uri)
                        FileType.LOGS -> return@also // Do nothing
                        FileType.BLOCK_LIST -> importFileFromUri(uri, "blocks_to_import.txt", "blocks")
                        FileType.FAVORITES_LIST -> importFileFromUri(uri, "favorites_to_import.txt", "favorites")
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
                Toast.makeText(context, "Config imported successfully!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import config!", Toast.LENGTH_LONG).show()
        }
    }

    private fun importFileFromUri(uri: Uri, name: String, type: String) {
        val context = requireContext()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val blocksFile = File(context.filesDir, name)
                inputStream.copyTo(blocksFile.outputStream())
                AlertDialog.Builder(context)
                    .setTitle("File Import")
                    .setMessage("The $type list will be imported on the next app restart. Do you want to proceed?")
                    .setPositiveButton("OK") { _, _ ->
                        closeApp()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        blocksFile.delete()
                    }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import $name!", Toast.LENGTH_LONG).show()
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

    private fun promptImportSelection(fType: FileType) {
        fileType = fType
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if (fileType == FileType.DATABASE) {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3", "application/db", "*/*"))
            } else if (fileType == FileType.CONFIG) {
                type = "application/json"
            } else if (fileType == FileType.BLOCK_LIST || fileType == FileType.FAVORITES_LIST) {
                type = "text/plain"
            }
        }
        importLauncher.launch(intent)
    }

    private fun promptFolderSelection(fType: FileType) {
        fileType = fType
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

    private fun exportLogsToUri(uri: Uri) {
        val context = requireContext()
        val fileName = "grindrplus_logs.txt"
        val mimeType = "text/plain"
        val logFile = File(context.filesDir, "grindrplus.log")

        val info = getSystemInfo(context)
        val logContent = logFile.readText()
        val activeHooks = buildString {
            Config.getHooksSettings().forEach { (hookName, pair) ->
                appendLine("$hookName: ${if (pair.second) "Enabled" else "Disabled"}")
            }
            appendLine("========================================")
        }
        val databases = buildString {
            val dbFiles = context.databaseList()
            if (dbFiles.isNotEmpty()) {
                dbFiles.forEach { dbFile -> appendLine(dbFile) }
                appendLine("========================================")
            }
        }

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
                    outputStream.write(info.toByteArray())
                    outputStream.write(activeHooks.toByteArray())
                    outputStream.write(databases.toByteArray())
                    outputStream.write(logContent.toByteArray())
                    outputStream.flush()
                }
                Toast.makeText(context, "Logs exported successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create file in the selected folder!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export logs!", Toast.LENGTH_LONG).show()
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

    fun exportFileToUri(uri: Uri, name: String) {
        val file = File(requireContext().filesDir, name)

        if (file.exists()) {
            try {
                val childUri = DocumentsContract.buildDocumentUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                )

                val newFileUri = DocumentsContract.createDocument(
                    requireContext().contentResolver,
                    childUri,
                    "text/plain",
                    name
                )

                if (newFileUri != null) {
                    requireContext().contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        file.delete()
                        Toast.makeText(context, "$name exported successfully!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to create file in the selected folder!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to export $name!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "No $name found!", Toast.LENGTH_LONG).show()
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
            if (hookName != "Mod settings" && hookName != "Persistent incognito" && hookName != "Unlimited albums") {
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

        container?.addView(
            createDynamicSettingView(
                context,
                title = "Command Prefix",
                description = "Change the command prefix (default: /)",
                key = "command_prefix",
                defaultValue = "/",
                validation = { input ->
                    when {
                        input.isBlank() -> "Invalid command prefix"
                        input.length > 1 -> "Command prefix must be a single character"
                        !input.matches(Regex("[^a-zA-Z0-9]")) -> "Command prefix must be a special character"
                        else -> null
                    }
                }
            )
        )
        container?.addView(
            createDynamicSettingView(
                context,
                title = "Online indicator duration (mins)",
                description = "Control when the green dot disappears after inactivity",
                key = "online_indicator",
                defaultValue = 5,
                inputType = InputType.TYPE_CLASS_NUMBER,
                validation = { input ->
                    val value = input.toIntOrNull()
                    if (value == null || value <= 0) "Duration must be a positive number" else null
                }
            )
        )
        container?.addView(
            createDynamicSettingView(
                context,
                title = "Favorites grid size",
                description = "Set the number of columns in the favorites grid",
                key = "favorites_grid_columns",
                defaultValue = 3,
                inputType = InputType.TYPE_CLASS_NUMBER,
                validation = { input ->
                    val value = input.toIntOrNull()
                    if (value == null || value <= 0) "Grid size must be a positive number" else null
                }
            )
        )

        // force_old_anti_block_behavior
        container?.addView(createToggleableSettingView(context, "Force old AntiBlock behavior", "Use the old AntiBlock behavior (don't use this, required for testing)", "force_old_anti_block_behavior"))
        container?.addView(createToggleableSettingView(context, "Use toasts for AntiBlock hook", "Instead of receiving Android notifications, use toasts for block/unblock notifications", "anti_block_use_toasts"))
        container?.addView(createToggleableSettingView(context, "Disable profile swipe", "Disable the swipe gesture on profiles", "disable_profile_swipe"))

        val experimentalFeaturesTitle = AppCompatTextView(context).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setTextAppearance(Utils.getId("TextAppearanceH6AllCaps", "styles", context))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = 44
                it.bottomMargin = 49
            }
            typeface = Utils.getFont("ibm_plex_sans_medium", context)
            text = "Experimental Features"
            isAllCaps = true
            setTextColor(Colors.text_secondary_dark_bg)
        }
        container?.addView(experimentalFeaturesTitle)

        container?.addView(
            createDynamicSettingView(
                context,
                title = "Import Blocks Threshold",
                description = "Set the time to wait between each block import (in milliseconds)",
                key = "block_import_threshold",
                defaultValue = 500,
                inputType = InputType.TYPE_CLASS_NUMBER,
                validation = { input ->
                    val value = input.toIntOrNull()
                    if (value == null || value <= 0) "Threshold must be a positive number" else null
                }
            )
        )

        container?.addView(
            createDynamicSettingView(
                context,
                title = "Import Favorites Threshold",
                description = "Set the time to wait between each favorites import (in milliseconds)",
                key = "favorites_import_threshold",
                defaultValue = 500,
                inputType = InputType.TYPE_CLASS_NUMBER,
                validation = { input ->
                    val value = input.toIntOrNull()
                    if (value == null || value <= 0) "Threshold must be a positive number" else null
                }
            )
        )

        container?.addView(createExperimentalFeatureOption(context, "Manage Blocks"))
        container?.addView(createExperimentalFeatureOption(context, "Manage Favorites"))
    }

    private fun createExperimentalFeatureOption(context: Context, title: String): View {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.topMargin = 20
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val textView = AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = title
                textSize = 16f
                setTextColor(Colors.text_primary_dark_bg)
                typeface = Utils.getFont("ibm_plex_sans_medium", context)
            }

            val button = Button(context).apply {
                text = "Open"
                setOnClickListener {
                    showExperimentalFeatureDialog(context, title)
                }
            }

            addView(textView)
            addView(button)
        }
    }

    private fun showExperimentalFeatureDialog(context: Context, featureName: String) {
        val dialogBuilder = AlertDialog.Builder(context).apply {
            setTitle(featureName)
            setMessage(
                "$featureName is an experimental feature. It may result in account bans." +
                        " Use it at your own risk. The developers take no responsibility for any consequences."
            )

            setPositiveButton("Export") { _, _ ->
                when (featureName) {
                    "Manage Blocks" -> {
                        val blocksFile = File(context.filesDir, "blocks.txt")
                        if (!blocksFile.exists()) {
                            Toast.makeText(
                                context,
                                "Use /blocks to populate the block list first.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setPositiveButton
                        }
                        promptFolderSelection(FileType.BLOCK_LIST)
                    }
                    "Manage Favorites" -> {
                        val favoritesFile = File(context.filesDir, "favorites.txt")
                        if (!favoritesFile.exists()) {
                            Toast.makeText(
                                context,
                                "Use /favorites to populate the favorites list first.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setPositiveButton
                        }
                        promptFolderSelection(FileType.FAVORITES_LIST)
                    }
                }
            }

            setNegativeButton("Import") { _, _ ->
                when (featureName) {
                    "Manage Blocks" -> {
                        val pendingFavoritesFile = File(context.filesDir, "favorites_to_import.txt")
                        if (pendingFavoritesFile.exists()) {
                            AlertDialog.Builder(context).apply {
                                setTitle("Import Blocked")
                                setMessage(
                                    "GrindrPlus has detected a pending favorites import. " +
                                            "Please import the favorites list first before importing blocks."
                                )
                                setPositiveButton("OK", null)
                            }.create().show()
                            return@setNegativeButton
                        }
                        promptImportSelection(FileType.BLOCK_LIST)
                    }
                    "Manage Favorites" -> {
                        val pendingBlocksFile = File(context.filesDir, "blocks_to_import.txt")
                        if (pendingBlocksFile.exists()) {
                            AlertDialog.Builder(context).apply {
                                setTitle("Import Blocked")
                                setMessage(
                                    "GrindrPlus has detected a pending blocks import. " +
                                            "Please import the blocks list first before importing favorites."
                                )
                                setPositiveButton("OK", null)
                            }.create().show()
                            return@setNegativeButton
                        }
                        promptImportSelection(FileType.FAVORITES_LIST)
                    }
                }
            }

            setNeutralButton("Cancel", null)
        }

        val dialog = dialogBuilder.create()
        dialog.show()
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

    private fun createDynamicSettingView(
        context: Context,
        title: String,
        description: String,
        key: String,
        defaultValue: Any,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        validation: ((String) -> String?)? = null
    ): View {
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

        val currentValue = Config.get(key, defaultValue).toString()

        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.inputType = inputType
            setText(currentValue)
            setSelection(text.length)
            isFocusable = false
            isClickable = true

            var originalValue = currentValue

            setOnClickListener {
                isFocusableInTouchMode = true
                isFocusable = true
                isClickable = false
                requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }

            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    val newValue = text.toString()
                    if (newValue.isBlank()) {
                        setText(originalValue)
                        Toast.makeText(context, "Input cannot be empty. Reverted to original value.", Toast.LENGTH_SHORT).show()
                    } else if (newValue != originalValue) {
                        val validationMessage = validation?.invoke(newValue)
                        if (validationMessage != null) {
                            Toast.makeText(context, validationMessage, Toast.LENGTH_LONG).show()
                            setText(originalValue)
                        } else {
                            originalValue = newValue
                            Config.put(key, newValue)
                            Toast.makeText(context, "$title set to $newValue", Toast.LENGTH_SHORT).show()
                        }
                    }
                    isFocusable = false
                    isClickable = true
                    true
                } else {
                    false
                }
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    if (text.toString().isBlank() || text.toString() == originalValue) {
                        setText(originalValue)
                        isFocusable = false
                        isClickable = true
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
        horizontalLayout.addView(editText)
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
        popupMenu.menu.add("Export Logs")
        popupMenu.menu.add("Clear Logs")
        popupMenu.menu.add("Export Config")
        popupMenu.menu.add("Import Config")
        popupMenu.menu.add("Export Database")
        popupMenu.menu.add("Import Database")
        popupMenu.menu.add("Reset GrindrPlus")
        popupMenu.menu.add("Reset Config")
        popupMenu.menu.add("Clear Cache")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Export Logs" -> {
                    promptFolderSelection(FileType.LOGS)
                    true
                }
                "Clear Logs" -> {
                    context.filesDir.listFiles { file ->
                        file.name.endsWith(".log") }?.forEach { file ->
                        file.delete()
                    }
                    Toast.makeText(context, "Successfully cleared logs", Toast.LENGTH_SHORT).show()
                    true
                }
                "Export Config" -> {
                    promptFolderSelection(FileType.CONFIG)
                    true
                }
                "Import Config" -> {
                    promptImportSelection(FileType.CONFIG)
                    true
                }
                "Export Database" -> {
                    promptFolderSelection(FileType.DATABASE)
                    true
                }
                "Import Database" -> {
                    promptImportSelection(FileType.DATABASE)
                    true
                }
                "Reset GrindrPlus" -> {
                    showResetConfirmationDialog()
                    true
                }
                "Reset Config" -> {
                    Config.resetConfig()
                    updateUIFromConfig()
                    Toast.makeText(context, "Config reset successfully", Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(context)
                        .setTitle("Config Reset")
                        .setMessage("The app will now close to regenerate the config.")
                        .setPositiveButton("OK") { _, _ ->
                            closeApp()
                        }
                        .setCancelable(false)
                        .show()
                    true
                }
                "Clear Cache" -> {
                    val context = requireContext()
                    val blocksFileImport = File(context.filesDir, "blocks_to_import.txt")
                    val favoritesFileImport = File(context.filesDir, "favorites_to_import.txt")
                    val blocksFile = File(context.filesDir, "blocks.txt")
                    val favoritesFile = File(context.filesDir, "favorites.txt")
                    if (blocksFileImport.exists()) blocksFileImport.delete()
                    if (favoritesFileImport.exists()) favoritesFileImport.delete()
                    if (blocksFile.exists()) blocksFile.delete()
                    if (favoritesFile.exists()) favoritesFile.delete()
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
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
