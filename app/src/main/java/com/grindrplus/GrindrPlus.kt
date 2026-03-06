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
import com.grindrplus.core.EventManager
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
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.callMethod
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
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.system.measureTimeMillis
import androidx.core.net.toUri
import com.grindrplus.utils.UiHelper.DialogButton
import com.grindrplus.utils.UiHelper.Icon
import com.grindrplus.utils.UiHelper.showAlertDialog
import com.grindrplus.utils.UiHelper.showToast
import timber.log.Timber

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

    private var isInitialized = false
    private var isMainInitialized = false
    private var isInstanceManagerInitialized = false

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

    internal val userAgent = "as.r" // search for 'grindr3/'
    internal val userSession = "com.grindrapp.android.usersession.b" // search for 'com.grindrapp.android.storage.UserSessionImpl$1'
    private val deviceInfo =
        "ek.y" // search for 'AdvertisingIdClient.Info("00000000-0000-0000-0000-000000000000", true)'
    internal val grindrLocationProvider = "nz.e" // search for 'system settings insufficient for location request, attempting to resolve'
    internal val serverDrivenCascadeRepo = "com.grindrapp.android.persistence.repository.ServerDrivenCascadeRepo"
    internal val ageVerificationActivity = "com.grindrapp.android.ageverification.presentation.ui.AgeVerificationActivity"
    internal val browseExploreActivity = "com.grindrapp.android.ui.browse.BrowseExploreMapActivity"
    internal val serverNotification = "com.grindrapp.android.network.websocket.model.WebSocketNotification\$ServerNotification"

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val taskScheduer = TaskScheduler(ioScope)
    internal val taskManager = TaskManager(taskScheduer)
    private var currentActivityRef: WeakReference<Activity>? = null

    private val splineDataEndpoint =
        "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/spline.json"

    val serverNotifications = EventManager.serverNotifications

    fun init(application: Application,
             versionCodes: IntArray, versionNames: Array<String>) {

        if (isInitialized) {
            Logger.d("GrindrPlus already initialized, skipping", LogSource.MODULE)
            return
        }

        setupCrashLogging()

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

        // Legacy cleanup: remove the grindrplus.dex file that was unnecessarily copied on every launch.
        // This line can be removed in a future version once all users have updated.
        context.filesDir.resolve("grindrplus.dex").delete()

        this.classLoader = context.classLoader
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

        registerActivityLifecycleCallbacks(application)

        if (shouldShowVersionMismatchDialog) {
            Logger.i("Version mismatch detected, stopping initialization", LogSource.MODULE)
            return
        }

        try {
            Logger.e("Setting Up Instance Manager")
            setupInstanceManager()
            Logger.e("Setting Up Notification Hook")
            setupServerNotificationHook()
        } catch (t: Throwable) {
            Logger.e("Failed to hook critical classes: ${t.message}", LogSource.MODULE)
            Logger.writeRaw(t.stackTraceToString())
            showToast("Failed to hook critical classes: ${t.message}", Toast.LENGTH_LONG)
            return
        }

        fetchRemoteData(splineDataEndpoint) { points ->
            spline = PCHIP(points)
            Logger.i("Updated spline with remote data", LogSource.MODULE)
        }

        try {
            val initTime = measureTimeMillis { initializeCore() }
            Logger.i("Initialization completed in $initTime ms", LogSource.MODULE)
            isInitialized = true
        } catch (t: Throwable) {
            Logger.e("Failed to initialize: ${t.message}", LogSource.MODULE)
            Logger.writeRaw(t.stackTraceToString())
            showToast("Failed to initialize: ${t.message}", Toast.LENGTH_LONG)
            return
        }
    }

    private fun setupServerNotificationHook() {
        try {
            classLoader.loadClass(serverNotification).hookConstructor(HookStage.AFTER) { param ->
                try {
                    val serverNotification = param.thisObject()
                    val typeValue = callMethod(serverNotification, "getTypeValue") as String
                    val notificationId = callMethod(serverNotification, "getNotificationId") as String?
                    val payload = callMethod(serverNotification, "getPayload") as JSONObject?
                    val status = callMethod(serverNotification, "getStatus") as Int?
                    val refValue = callMethod(serverNotification, "getRefValue") as String?

                    EventManager.emitServerNotification(typeValue, notificationId, payload, status, refValue)
                    Logger.d("ServerNotification hooked and event emitted: $typeValue", LogSource.MODULE)
                } catch (e: Exception) {
                    Logger.e("Failed to emit server notification event: ${e.message}", LogSource.MODULE)
                }
            }
        } catch (e: Exception) {
            Logger.e("Failed to setup server notification hook: ${e.message}", LogSource.MODULE)
        }
    }

    private fun registerActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                when {
                    activity.javaClass.name == ageVerificationActivity -> {
                        showAgeVerificationComplianceDialog(activity)
                    }
                    activity.javaClass.name == browseExploreActivity -> {
                        if ((Config.get("maps_api_key", "") as String).isEmpty()) {
                            if (!bridgeClient.isLSPosed()) {
                                showMapsApiKeyDialog(activity)
                            }
                        }
                    }
                    shouldShowBridgeConnectionError -> {
                        showBridgeConnectionError(activity)
                        shouldShowBridgeConnectionError = false
                    }
                    shouldShowVersionMismatchDialog -> {
                        showVersionMismatchDialog(activity)
                        shouldShowVersionMismatchDialog = false
                    }
                }

                if (isImportingSomething) {
                    handleImports(activity)
                }
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
    }

    private fun setupInstanceManager() {
        if (isInstanceManagerInitialized) {
            Logger.d("InstanceManager already initialized, skipping", LogSource.MODULE)
            return
        }

        instanceManager.hookClassConstructors(
            userAgent,
            userSession,
            deviceInfo,
            grindrLocationProvider,
            serverDrivenCascadeRepo
        )

        instanceManager.setCallback(userSession) { uSession ->
            instanceManager.setCallback(userAgent) { uAgent ->
                instanceManager.setCallback(deviceInfo) { dInfo ->
                    httpClient = Client(Interceptor(uSession, uAgent, dInfo))
                    executeAsync {
                        kotlinx.coroutines.delay(1500)
                        fetchOwnUserId()
                    }
                    taskManager.registerTasks()
                }
            }
        }

        isInstanceManagerInitialized = true
    }

    private fun initializeCore() {
        if (isMainInitialized) {
            Logger.d("Core already initialized, skipping", LogSource.MODULE)
            return
        }

        Logger.i("Initializing GrindrPlus core...", LogSource.MODULE)

        if ((Config.get("reset_database", false) as Boolean)) {
            Logger.i("Resetting database...", LogSource.MODULE)
            database.clearAllTables()
            Config.put("reset_database", false)
        }

        hookManager.init()
        isMainInitialized = true
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

    fun loadClass(name: String): Class<*> {
        return classLoader.loadClass(name)
    }

    fun restartGrindr(timeout: Long = 0, toast: String? = null) {
        toast?.let { showToast(it, Toast.LENGTH_LONG) }

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
        runCatching {
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

            showAlertDialog {
                title = "GrindrPlus: Version Mismatch"
                message = "Incompatible Grindr version detected.\n\n" +
                        "• Installed: $installedInfo\n" +
                        "• Required: $expectedInfo\n\n" +
                        "GrindrPlus has been disabled. Please install a compatible Grindr version."
                icon = Icon.ResId(android.R.drawable.ic_dialog_alert)
                cancellable = false
                this.activity = activity
            }
            Logger.i("Version mismatch dialog shown", LogSource.MODULE)
        }.onFailure { e ->
            Logger.e("Failed to show version mismatch dialog: ${e.message}", LogSource.MODULE)
            showToast(
                "Version mismatch detected. Please install a compatible Grindr version.",
                Toast.LENGTH_LONG
            )
        }
    }

    private fun showBridgeConnectionError(activity: Activity? = null) {
        val targetActivity = activity ?: currentActivity
        if (targetActivity == null) {
            showToast(
                "Bridge service connection failed - module features unavailable",
                Toast.LENGTH_LONG
            )
            return
        }

        showAlertDialog {
            title = "Bridge Connection Failed"
            message = "Failed to connect to the bridge service. The module will not work properly.\n\n" +
                    "This may be caused by:\n" +
                    "• Battery optimization settings\n" +
                    "• System killing background processes\n" +
                    "• App being force stopped\n\n" +
                    "Try restarting the app or reinstalling the module."
            icon = Icon.ResId(android.R.drawable.ic_dialog_alert)
            cancellable = false
            this.activity = targetActivity
        }
        Logger.i("Bridge connection error dialog shown", LogSource.MODULE)
    }

    private fun showAgeVerificationComplianceDialog(activity: Activity) {
        showAlertDialog {
            title = "Age Verification Required"
            message = "You are accessing Grindr from a country where age verification is legally mandated.\n\n" +
                    "LEGAL COMPLIANCE NOTICE:\n" +
                    "GrindrPlus does NOT bypass, disable, or interfere with age verification systems. Any attempt to circumvent age verification requirements may be illegal\n\n" +
                    "MANDATORY REQUIREMENTS:\n" +
                    "1. Complete age verification using the official Grindr application\n" +
                    "2. Comply with all legally required verification processes\n" +
                    "3. Install GrindrPlus only after successful verification through official channels\n\n" +
                    "WARNING:\n" +
                    "The developers of this module are not responsible for any legal consequences resulting from non-compliance with age verification requirements."
            positiveButton = DialogButton(
                text = "I Understand",
                onClick = {
                    activity.finish()
                    showToast(
                        "Please complete age verification in the official Grindr app first, then reinstall GrindrPlus",
                        Toast.LENGTH_LONG
                    )
                }
            )
            negativeButton = DialogButton(
                text = "Exit App",
                onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }
            )
            icon = Icon.ResId(android.R.drawable.ic_dialog_alert)
            cancellable = false
            this.activity = activity
        }
        Logger.i("Age verification compliance dialog shown", LogSource.MODULE)
    }

    private fun showMapsApiKeyDialog(context: Context) {
        showAlertDialog {
            title = "Maps API Key Required"
            message = "Maps functionality requires a Google Maps API key for LSPatch users due to signature validation issues.\n\n" +
                    "Quick Setup:\n" +
                    "1. Create a Google Cloud project at console.cloud.google.com\n" +
                    "2. Enable: Maps SDK for Android, Geocoding API, Maps JavaScript API\n" +
                    "3. Create API key with NO restrictions\n" +
                    "4. Add key to GrindrPlus settings\n" +
                    "5. REINSTALL GrindrPlus (restart won't work)\n\n" +
                    "Note: Google may request credit card for free tier."
            positiveButton = DialogButton(
                text = "Open Console",
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = "https://console.cloud.google.com/".toUri()
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.applicationContext.startActivity(intent)
                    }.onFailure {
                        showToast(
                            "Unable to open browser. Please visit console.cloud.google.com manually",
                            Toast.LENGTH_LONG
                        )
                    }
                }
            )
            negativeButton = DialogButton("Dismiss")
            icon = Icon.ResId(android.R.drawable.ic_dialog_info)
            this.activity = context as? Activity
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

    private fun fetchOwnUserId() {
        executeAsync {
            try {
                Logger.d("Fetching own user ID...", LogSource.MODULE)
                val response = httpClient.sendRequest(
                    url = "https://grindr.mobi/v5/me/profile",
                    method = "GET"
                )

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        val jsonResponse = JSONObject(responseBody)
                        val profilesArray = jsonResponse.optJSONArray("profiles")

                        if (profilesArray != null && profilesArray.length() > 0) {
                            val profile = profilesArray.getJSONObject(0)
                            val profileId = profile.optString("profileId")

                            if (profileId.isNotEmpty()) {
                                myProfileId = profileId
                            } else {
                                Logger.w("Profile ID field is empty in response", LogSource.MODULE)
                            }
                        } else {
                            Logger.w("No profiles array found in response", LogSource.MODULE)
                        }
                    } else {
                        Logger.w("Empty response body from profile endpoint", LogSource.MODULE)
                    }
                } else {
                    Logger.e("Failed to fetch own profile: HTTP ${response.code}", LogSource.MODULE)
                }
            } catch (e: Exception) {
                Logger.e("Error fetching own user ID: ${e.message}", LogSource.MODULE)
                Logger.writeRaw(e.stackTraceToString())
            }
        }
    }

    private fun setupCrashLogging() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Logger.e("Uncaught exception in thread: ${thread.name}", LogSource.MODULE)
                Logger.e("Exception: ${throwable.javaClass.simpleName}: ${throwable.message}", LogSource.MODULE)
                Logger.writeRaw("Thread: ${thread.name} (id=${thread.id})")
                Logger.writeRaw("Exception: ${throwable.javaClass.name}")
                Logger.writeRaw("Message: ${throwable.message}")
                Logger.writeRaw("Stack trace:")
                Logger.writeRaw(throwable.stackTraceToString())

                throwable.cause?.let { cause ->
                    Logger.writeRaw("Caused by: ${cause.javaClass.name}: ${cause.message}")
                    Logger.writeRaw(cause.stackTraceToString())
                }
            } catch (e: Exception) {
                Timber.tag("GrindrPlus").e("Failed to log crash: ${e.message}")
                Timber.tag("GrindrPlus").e("Original crash: ${throwable.message}")
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}