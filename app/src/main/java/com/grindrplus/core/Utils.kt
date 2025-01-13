package com.grindrplus.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.context
import com.grindrplus.GrindrPlus.httpClient
import com.grindrplus.GrindrPlus.isImportingSomething
import com.grindrplus.GrindrPlus.shouldTriggerAntiblock
import com.grindrplus.ui.Utils.getId
import com.grindrplus.utils.RetrofitUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    @SuppressLint("SetTextI18n")
    fun showProgressDialog(
        context: Context,
        message: String,
        onCancel: () -> Unit,
        onRunInBackground: (updateProgress: (Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit,
        successMessage: String = "All blocks have been imported!",
        failureMessage: String = "Something went wrong. Please try again."
    ) {
        lateinit var dialog: AlertDialog

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
        }

        val textView = TextView(context).apply {
            text = "$message (0%)"
            textSize = 16f
            setPadding(20, 20, 20, 20)
        }

        val cancelButton = Button(context).apply {
            text = "Cancel"
            setOnClickListener {
                onCancel()
                dialog.dismiss()
            }
        }

        val backgroundButton = Button(context).apply {
            text = "Run in Background"
            setOnClickListener {
                dialog.dismiss()
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(progressBar)
            addView(textView)
            addView(cancelButton)
            addView(backgroundButton)
        }

        dialog = AlertDialog.Builder(context)
            .setCancelable(false)
            .setView(container)
            .create()

        dialog.show()

        onRunInBackground({ progress ->
            progressBar.progress = progress
            textView.text = "$message ($progress%)"
        }) { success ->
            container.removeAllViews()

            val resultIcon = TextView(context).apply {
                text = if (success) "✅" else "❌"
                textSize = 40f
                setPadding(20, 20, 20, 20)
                gravity = android.view.Gravity.CENTER
            }

            val resultMessage = TextView(context).apply {
                text = if (success) successMessage else failureMessage
                textSize = 18f
                setPadding(20, 20, 20, 20)
                gravity = android.view.Gravity.CENTER
            }

            val closeButton = Button(context).apply {
                text = "Close"
                setOnClickListener {
                    dialog.dismiss()
                }
            }

            container.apply {
                addView(resultIcon)
                addView(resultMessage)
                addView(closeButton)
            }
        }
    }

    fun showWarningDialog(context: Context, message: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Warning")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Proceed") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
            .create()
            .show()
    }

    fun handleImports(activity: Activity) {
        val homeActivity = "com.grindrapp.android.ui.home.HomeActivity"

        if (activity.javaClass.name == homeActivity && !isImportingSomething) {
            val favoritesFile = context.getFileStreamPath("favorites_to_import.txt")
            val blocksFile = context.getFileStreamPath("blocks_to_import.txt")

            if (favoritesFile.exists() && blocksFile.exists()) {
                showWarningDialog(
                    context = activity,
                    message = "Favorites and Blocks import files detected. GrindrPlus will process the favorites list first. " +
                            "Blocks import will be done on the next app restart.",
                    onConfirm = {
                        val threshold = (Config.get("favorites_import_threshold", "500") as String).toInt()
                        val favorites = favoritesFile.readLines()

                        if (favorites.size > 50 && threshold < 1000) {
                            showWarningDialog(
                                context = activity,
                                message = "High number of favorites and low threshold detected. " +
                                        "Continuing may result in your account being banned. Do you want to proceed?",
                                onConfirm = {
                                    startFavoritesImport(activity, favorites, favoritesFile, threshold)
                                },
                                onCancel = {
                                    GrindrPlus.logger.log("Favorites import canceled by the user.")
                                }
                            )
                        } else {
                            startFavoritesImport(activity, favorites, favoritesFile, threshold)
                        }
                    },
                    onCancel = {
                        GrindrPlus.logger.log("Imports canceled by the user.")
                    }
                )
            } else if (favoritesFile.exists()) {
                val threshold = (Config.get("favorites_import_threshold", "500") as String).toInt()
                val favorites = favoritesFile.readLines()

                if (favorites.size > 50 && threshold < 1000) {
                    showWarningDialog(
                        context = activity,
                        message = "High number of favorites and low threshold detected. " +
                                "Continuing may result in your account being banned. Do you want to proceed?",
                        onConfirm = {
                            startFavoritesImport(activity, favorites, favoritesFile, threshold)
                        },
                        onCancel = {
                            isImportingSomething = false
                            GrindrPlus.logger.log("Favorites import canceled by the user.")
                        }
                    )
                } else {
                    startFavoritesImport(activity, favorites, favoritesFile, threshold)
                }
            } else if (blocksFile.exists()) {
                val threshold = (Config.get("block_import_threshold", "500") as String).toInt()
                val blocks = blocksFile.readLines()

                if (blocks.size > 100 && threshold < 1000) {
                    showWarningDialog(
                        context = activity,
                        message = "High number of blocks and low threshold detected. " +
                                "Continuing may result in your account being banned. Do you want to proceed?",
                        onConfirm = {
                            startBlockImport(activity, blocks, blocksFile, threshold)
                        },
                        onCancel = {
                            isImportingSomething = false
                            GrindrPlus.logger.log("Block import canceled by the user.")
                        }
                    )
                } else {
                    startBlockImport(activity, blocks, blocksFile, threshold)
                }
            }
        }
    }

    private fun startFavoritesImport(
        activity: Activity,
        favorites: List<String>,
        favoritesFile: File,
        threshold: Int
    ) {
        try {

            showProgressDialog(
                context = activity,
                message = "Importing favorites...",
                successMessage = "Favorites import completed.",
                failureMessage = "Favorites import failed.",
                onCancel = {
                    isImportingSomething = false
                    GrindrPlus.logger.log("Favorites import canceled by the user.")
                },
                onRunInBackground = { updateProgress, onComplete ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            favorites.forEachIndexed { index, id ->
                                val profileId = id.split("|||")[0]
                                val note = id.split("|||")[1]
                                val phoneNumber = id.split("|||")[2]
                                httpClient.favorite(
                                    profileId,
                                    silent = true,
                                    reflectInDb = false
                                )
                                if (note.isNotEmpty() || phoneNumber.isNotEmpty()) {
                                    httpClient.addProfileNote(
                                        profileId,
                                        note,
                                        phoneNumber,
                                        silent = true
                                    )
                                }
                                favoritesFile.writeText(favorites.drop(index + 1).joinToString("\n"))
                                val progress = ((index + 1) * 100) / favorites.size
                                updateProgress(progress)
                                Thread.sleep(threshold.toLong())
                            }

                            withContext(Dispatchers.Main) {
                                favoritesFile.delete()
                                onComplete(true)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                val message = "An error occurred while importing favorites: ${e.message ?: "Unknown error"}"
                                GrindrPlus.apply {
                                    showToast(Toast.LENGTH_LONG, message)
                                    logger.log(message)
                                    logger.writeRaw(e.stackTraceToString())
                                }
                                onComplete(false)
                            }
                        } finally {
                            isImportingSomething = false
                        }
                    }
                }
            )
        } catch (e: Exception) {
            GrindrPlus.apply {
                showToast(Toast.LENGTH_LONG, "An error occurred while importing favorites: ${e.message ?: "Unknown error"}")
                logger.log("An error occurred while importing favorites: ${e.message ?: "Unknown error"}")
                logger.writeRaw(e.stackTraceToString())
            }
        }
    }

    private fun startBlockImport(
        activity: Activity,
        blocks: List<String>,
        blocksFile: File,
        threshold: Int
    ) {
        try {
            shouldTriggerAntiblock = false

            showProgressDialog(
                context = activity,
                message = "Importing blocks...",
                successMessage = "Block import completed.",
                failureMessage = "Block import failed.",
                onCancel = {
                    isImportingSomething = false
                    GrindrPlus.logger.log("Block import canceled by the user.")
                },
                onRunInBackground = { updateProgress, onComplete ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            blocks.forEachIndexed { index, id ->
                                httpClient.blockUser(
                                    id,
                                    silent = true,
                                    reflectInDb = false
                                )
                                blocksFile.writeText(blocks.drop(index + 1).joinToString("\n"))
                                val progress = ((index + 1) * 100) / blocks.size
                                updateProgress(progress)
                                Thread.sleep(threshold.toLong())
                            }

                            withContext(Dispatchers.Main) {
                                blocksFile.delete()
                                onComplete(true)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                val message = "An error occurred while importing blocks: ${e.message ?: "Unknown error"}"
                                GrindrPlus.apply {
                                    showToast(Toast.LENGTH_LONG, message)
                                    logger.log(message)
                                    logger.writeRaw(e.stackTraceToString())
                                }
                                onComplete(false)
                            }
                        } finally {
                            shouldTriggerAntiblock = true
                            isImportingSomething = false
                        }
                    }
                }
            )
        } catch (e: Exception) {
            GrindrPlus.apply {
                shouldTriggerAntiblock = true
                showToast(Toast.LENGTH_LONG, "An error occurred while importing blocks: ${e.message ?: "Unknown error"}")
                logger.log("An error occurred while importing blocks: ${e.message ?: "Unknown error"}")
                logger.writeRaw(e.stackTraceToString())
            }
        }
    }
}