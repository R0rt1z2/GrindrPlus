package com.grindrplus

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.grindrplus.bridge.BridgeClient
import com.grindrplus.core.Config
import com.grindrplus.core.InstanceManager
import com.grindrplus.core.Logger
import com.grindrplus.core.LogSource
import com.grindrplus.core.TaskScheduler
import com.grindrplus.utils.TaskManager
import com.grindrplus.core.Utils.handleImports
import com.grindrplus.core.http.Client
import com.grindrplus.core.http.Interceptor
import com.grindrplus.persistence.GPDatabase
import com.grindrplus.utils.HookManager
import com.grindrplus.utils.PCHIP
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.system.measureTimeMillis

@SuppressLint("StaticFieldLeak")
object GrindrPlus {
    lateinit var context: Context
        private set
    lateinit var classLoader: ClassLoader
        private set
    lateinit var database: GPDatabase
        private set
    lateinit var bridgeClient: BridgeClient
        internal set
    lateinit var instanceManager: InstanceManager
        private set
    lateinit var httpClient: Client
        private set
    lateinit var packageName: String
        private set

    lateinit var hookManager: HookManager

    var shouldTriggerAntiblock = true
    var blockCaller: String = ""
    var isImportingSomething = false
    var myProfileId: String = ""
    var hasCheckedVersions = false
    var shouldShowVersionMismatchDialog = false
    var shouldShowBridgeConnectionError = false

    var spline = PCHIP(
        listOf(
            1238563200L to 0,          // 2009-04-01
            1285027200L to 1000000,    // 2010-09-21
            1462924800L to 35512000,   // 2016-05-11
            1501804800L to 132076000,  // 2017-08-04
            1546547829L to 201948000,  // 2019-01-03
            1618531200L to 351220000,  // 2021-04-16
            1636150385L to 390338000,  // 2021-11-05
            1637963460L to 394800000,  // 2021-11-26
            1680393600L to 505225000,  // 2023-04-02
            1717200000L to 630495000,  // 2024-06-01
            1717372800L to 634942000,  // 2024-06-03
            1729950240L to 699724000,  // 2024-10-26
            1732986600L to 710609000,  // 2024-11-30
            1733349060L to 711676000,  // 2024-12-04
            1735229820L to 718934000,  // 2024-12-26
            1738065780L to 730248000,  // 2025-01-29
            1739059200L to 733779000,  // 2025-02-09
            1741564800L to 744008000   // 2025-03-10
        )
    )

    val currentActivity: Activity?
        get() = currentActivityRef?.get()

    internal val userAgent = "W6.h" // search for 'grindr3/'
    internal val userSession = "pd.W" // search for 'com.grindrapp.android.storage.UserSessionImpl$1'
    private val deviceInfo =
        "y4.B" // search for 'AdvertisingIdClient.Info("00000000-0000-0000-0000-000000000000", true)'
    internal val grindrLocationProvider = "u9.d" // search for 'system settings insufficient for location request, attempting to resolve'
    internal val serverDrivenCascadeRepo = "com.grindrapp.android.persistence.repository.ServerDrivenCascadeRepo"

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val taskScheduer = TaskScheduler(ioScope)
    internal val taskManager = TaskManager(taskScheduer)
    private var currentActivityRef: WeakReference<Activity>? = null

    private val splineDataEndpoint =
        "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/spline.json"

    fun init(modulePath: String, application: Application,
             versionCodes: IntArray, versionNames: Array<String>) {
        this.context = application
        this.bridgeClient = BridgeClient(context)

        Logger.initialize(context, bridgeClient, true)
        Logger.i("Initializing GrindrPlus...", LogSource.MODULE)

        checkVersionCodes(versionCodes, versionNames)
        val connected = runBlocking {
            try {
                withTimeout(10000) {
                    bridgeClient.connectWithRetry(5, 1000)
                }
            } catch (e: Exception) {
                Logger.e("Connection timeout: ${e.message}", LogSource.MODULE)
                false
            }
        }

        if (!connected) {
            Logger.e("Failed to connect to the bridge service", LogSource.MODULE)
            shouldShowBridgeConnectionError = true
        }

        Config.initialize(application.packageName)
        val newModule = File(context.filesDir, "grindrplus.dex")
        File(modulePath).copyTo(newModule, true)
        newModule.setReadOnly()

        this.classLoader =
            DexClassLoader(newModule.absolutePath, null, null, context.classLoader)
        this.database = GPDatabase.create(context)
        this.hookManager = HookManager()
        this.instanceManager = InstanceManager(classLoader)
        this.packageName = context.packageName

        if (bridgeClient.shouldRegenAndroidId(packageName)) {
            Logger.i("Generating new Android device ID", LogSource.MODULE)
            val androidId = java.util.UUID.randomUUID()
                .toString().replace("-", "").lowercase().take(16)
            Config.put("android_device_id", androidId)
        }

        val forcedCoordinates = bridgeClient.getForcedLocation(packageName)

        if (forcedCoordinates.isNotEmpty()) {
            val parts = forcedCoordinates.split(",").map { it.trim() }
            if (parts.size != 2 || parts.any { it.toDoubleOrNull() == null }) {
                Logger.w("Invalid forced coordinates format: $forcedCoordinates", LogSource.MODULE)
            } else {
                if (parts[0] == "0.0" && parts[1] == "0.0") {
                    Logger.w("Ignoring forced coordinates: $forcedCoordinates", LogSource.MODULE)
                } else {
                    Logger.i("Using forced coordinates: $forcedCoordinates", LogSource.MODULE)
                    Config.put("forced_coordinates", forcedCoordinates)
                }
            }
        } else if (Config.get("forced_coordinates", "") != "") {
            Logger.i("Clearing previously set forced coordinates", LogSource.MODULE)
            Config.put("forced_coordinates", "")
        }

        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (shouldShowBridgeConnectionError) {
                    showBridgeConnectionError(activity)
                    shouldShowBridgeConnectionError = false
                }

                if (shouldShowVersionMismatchDialog) {
                    showVersionMismatchDialog(activity)
                    shouldShowVersionMismatchDialog = false
                }

                handleImports(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                Logger.d("Resuming activity: ${activity.javaClass.name}", LogSource.MODULE)
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                Logger.d("Pausing activity: ${activity.javaClass.name}", LogSource.MODULE)
                if (currentActivity == activity) {
                    currentActivityRef = null
                }
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })

        if (shouldShowVersionMismatchDialog) {
            Logger.i("Version mismatch detected, stopping initialization", LogSource.MODULE)
            return
        }

        try {
            instanceManager.hookClassConstructors(
                userAgent,
                userSession,
                deviceInfo,
                grindrLocationProvider,
                serverDrivenCascadeRepo
            )

            instanceManager.setCallback(userSession) { uSession ->
                myProfileId = getObjectField(uSession, "t") as String
                instanceManager.setCallback(userAgent) { uAgent ->
                    instanceManager.setCallback(deviceInfo) { dInfo ->
                        httpClient = Client(Interceptor(uSession, uAgent, dInfo))
                        taskManager.registerTasks() // Tasks require httpClient
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.e("Failed to hook critical classes: ${t.message}", LogSource.MODULE)
            Logger.writeRaw(t.stackTraceToString())
            showToast(Toast.LENGTH_LONG, "Failed to hook critical classes: ${t.message}")
            return
        }

        fetchRemoteData(splineDataEndpoint) { points ->
            spline = PCHIP(points)
            Logger.i("Updated spline with remote data", LogSource.MODULE)
        }

        try {
            val initTime = measureTimeMillis { init() }
            Logger.i("Initialization completed in $initTime ms", LogSource.MODULE)
        } catch (t: Throwable) {
            Logger.e("Failed to initialize: ${t.message}", LogSource.MODULE)
            Logger.writeRaw(t.stackTraceToString())
            showToast(Toast.LENGTH_LONG, "Failed to initialize: ${t.message}")
            return
        }
    }

    private fun init() {
        Logger.i("Initializing GrindrPlus...", LogSource.MODULE)

        if ((Config.get("reset_database", false) as Boolean)) {
            Logger.i("Resetting database...", LogSource.MODULE)
            database.clearAllTables()
            Config.put("reset_database", false)
        }

        hookManager.init()
    }


    fun runOnMainThread(appContext: Context? = null, block: (Context) -> Unit) {
        val useContext = appContext ?: context
        Handler(useContext.mainLooper).post {
            block(useContext)
        }
    }

    fun runOnMainThreadWithCurrentActivity(block: (Activity) -> Unit) {
        runOnMainThread {
            currentActivity?.let { activity ->
                block(activity)
            } ?: Logger.e("Cannot execute action - no active activity", LogSource.MODULE)
        }
    }

    fun executeAsync(block: suspend () -> Unit) {
        ioScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Logger.e("Async operation failed: ${e.message}", LogSource.MODULE)
                Logger.writeRaw(e.stackTraceToString())
            }
        }
    }

    fun showToast(duration: Int, message: String, appContext: Context? = null) {
        val useContext = appContext ?: context
        runOnMainThread(useContext) {
            Toast.makeText(useContext, message, duration).show()
        }
    }

    fun loadClass(name: String): Class<*> {
        return classLoader.loadClass(name)
    }

    fun restartGrindr(timeout: Long = 0, toast: String? = null) {
        toast?.let { showToast(Toast.LENGTH_LONG, it) }

        if (timeout > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
            }, timeout)
        } else {
            val intent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun checkVersionCodes(versionCodes: IntArray, versionNames: Array<String>) {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }

        val isVersionNameSupported = pkgInfo.versionName in versionNames
        val isVersionCodeSupported = versionCodes.any { it.toLong() == versionCode }

        if (!isVersionNameSupported || !isVersionCodeSupported) {
            val installedInfo = "${pkgInfo.versionName} (code: $versionCode)"
            val expectedInfo = "${versionNames.joinToString(", ")} " +
                    "(code: ${BuildConfig.TARGET_GRINDR_VERSION_CODES.joinToString(", ")})"
            shouldShowVersionMismatchDialog = true
            Logger.w("Version mismatch detected. Installed: $installedInfo, Required: $expectedInfo", LogSource.MODULE)
        }

        hasCheckedVersions = true
    }

    private fun showVersionMismatchDialog(activity: Activity) {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            val installedInfo = "${pkgInfo.versionName} (code: $versionCode)"
            val expectedInfo = "${BuildConfig.TARGET_GRINDR_VERSION_NAMES.joinToString(", ")} " +
                    "(code: ${BuildConfig.TARGET_GRINDR_VERSION_CODES.joinToString(", ")})"

            val dialog = android.app.AlertDialog.Builder(activity)
                .setTitle("GrindrPlus: Version Mismatch")
                .setMessage("Incompatible Grindr version detected.\n\n" +
                        "• Installed: $installedInfo\n" +
                        "• Required: $expectedInfo\n\n" +
                        "GrindrPlus has been disabled. Please install a compatible Grindr version.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .create()
            dialog.show()
            Logger.i("Version mismatch dialog shown", LogSource.MODULE)
        } catch (e: Exception) {
            Logger.e("Failed to show version mismatch dialog: ${e.message}", LogSource.MODULE)
            showToast(Toast.LENGTH_LONG, "Version mismatch detected. Please install a compatible Grindr version.")
        }
    }

    private fun showBridgeConnectionError(activity: Activity? = null) {
        try {
            val targetActivity = activity ?: currentActivity

            if (targetActivity != null) {
                val dialog = android.app.AlertDialog.Builder(targetActivity)
                    .setTitle("Bridge Connection Failed")
                    .setMessage("Failed to connect to the bridge service. The module will not work properly.\n\n" +
                            "This may be caused by:\n" +
                            "• Battery optimization settings\n" +
                            "• System killing background processes\n" +
                            "• App being force stopped\n\n" +
                            "Try restarting the app or reinstalling the module.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .create()

                targetActivity.runOnUiThread {
                    dialog.show()
                }

                Logger.i("Bridge connection error dialog shown", LogSource.MODULE)
            } else {
                showToast(Toast.LENGTH_LONG, "Bridge service connection failed - module features unavailable")
            }
        } catch (e: Exception) {
            Logger.e("Failed to show bridge error dialog: ${e.message}", LogSource.MODULE)
            showToast(Toast.LENGTH_LONG, "Bridge service connection failed - module features unavailable")
        }
    }

    private fun fetchRemoteData(url: String, callback: (List<Pair<Long, Int>>) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.e("Failed to fetch remote data: ${e.message}", LogSource.MODULE)
                Logger.writeRaw(e.stackTraceToString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    try {
                        val jsonArray = JSONArray(jsonString)
                        val parsedPoints = mutableListOf<Pair<Long, Int>>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val time = obj.getLong("time")
                            val id = obj.getInt("id")
                            parsedPoints.add(time to id)
                        }

                        callback(parsedPoints)
                    } catch (e: Exception) {
                        Logger.e("Failed to parse remote data: ${e.message}", LogSource.MODULE)
                        Logger.writeRaw(e.stackTraceToString())
                    }
                }
            }
        })
    }
}