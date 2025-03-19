package com.grindrplus

import Database
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.grindrplus.bridge.BridgeClient
import com.grindrplus.core.Config
import com.grindrplus.core.CoroutineHelper
import com.grindrplus.core.InstanceManager
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils.handleImports
import com.grindrplus.core.http.Client
import com.grindrplus.core.http.Interceptor
import com.grindrplus.persistence.NewDatabase
import com.grindrplus.utils.HookManager
import com.grindrplus.utils.PCHIP
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.system.measureTimeMillis

private const val TAG = "GrindrPlus"

@SuppressLint("StaticFieldLeak")
object GrindrPlus {
    lateinit var context: Context
        private set
    lateinit var classLoader: ClassLoader
        private set
    lateinit var logger: Logger
        private set
    lateinit var newDatabase: NewDatabase
        private set
    lateinit var database: Database
        private set
    lateinit var bridgeClient: BridgeClient
        private set
    lateinit var coroutineHelper: CoroutineHelper
        private set
    lateinit var instanceManager: InstanceManager
        private set
    lateinit var httpClient: Client
        private set

    lateinit var hookManager: HookManager
    lateinit var translations: JSONObject
    lateinit var localeTag: String

    var shouldTriggerAntiblock = true
    var blockCaller: String = ""
    var isImportingSomething = false
    var myProfileId: String = ""

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

    var currentActivity: Activity? = null
        private set

    private val userAgent = "e6.x" // search for 'grindr3/'
    private val userSession = "sa.T" // search for 'com.grindrapp.android.storage.UserSessionImpl$1'
    private val deviceInfo =
        "V3.t" // search for 'AdvertisingIdClient.Info("00000000-0000-0000-0000-000000000000", true)'
    private val profileRepo = "com.grindrapp.android.persistence.repository.ProfileRepo"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val splineDataEndpoint =
        "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/spline.json"

    fun init(modulePath: String, application: Application, logger: Logger) {
        Log.d(
            TAG,
            "Initializing GrindrPlus with module path: $modulePath, application: $application"
        )

        this.context = application // do not use .applicationContext as it's null at this point
        this.classLoader =
            DexClassLoader(modulePath, context.cacheDir.absolutePath, null, context.classLoader)
        this.logger = logger
        this.newDatabase = NewDatabase.create(context)
        this.database = Database(context, context.filesDir.absolutePath + "/grindrplus.db")
        this.hookManager = HookManager()
        this.coroutineHelper = CoroutineHelper(classLoader)
        this.instanceManager = InstanceManager(classLoader)

        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                handleImports(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                logger.debug("Resuming activity: ${activity.javaClass.name}")
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                logger.log("Pausing activity: ${activity.javaClass.name}")
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })

        instanceManager.hookClassConstructors(
            userAgent,
            userSession,
            deviceInfo,
            profileRepo
        )

        instanceManager.setCallback(userSession) { uSession ->
            myProfileId = getObjectField(uSession, "r") as String
            instanceManager.setCallback(userAgent) { uAgent ->
                instanceManager.setCallback(deviceInfo) { dInfo ->
                    httpClient = Client(Interceptor(uSession, uAgent, dInfo))
                }
            }
        }

        fetchRemoteData(splineDataEndpoint) { points ->
            spline = PCHIP(points)
            logger.log("Updated spline with remote data")
        }

        try {
            val initTime = measureTimeMillis { init() }
            logger.log("Initialization completed in $initTime ms.")
        } catch (e: Exception) {
            logger.log("Failed to initialize: ${e.message}")
            showToast(Toast.LENGTH_LONG, "Failed to initialize: ${e.message}")
        }
    }

    private fun init() {
        logger.log("Initializing GrindrPlus...")
        Config.initialize(context)

//        bridgeClient = BridgeClient(context).apply {
//            connect {
//                localeTag = Config.get("locale", "") as String?
//                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        context.resources.configuration.locales.get(0).toLanguageTag()
//                    } else {
//                        context.resources.configuration.locale.toLanguageTag()
//                    }
//                translations = getTranslation(localeTag) ?: JSONObject()
//            }
//        }

        /**
         * Emergency reset of the database if the flag is set.
         */
        if ((Config.get("reset_database", false) as Boolean)) {
            logger.log("Resetting database...")
            database.deleteDatabase()
            Config.put("reset_database", false)
        }

        Config.put("xposed_version", XposedBridge.getXposedVersion())

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
            }
        }
    }

    fun executeAsync(block: suspend () -> Unit) {
        ioScope.launch {
            try {
                block()
            } catch (e: Exception) {
                logger.log("Async operation failed: ${e.message}")
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

    fun getTranslation(key: String, vararg placeholders: Pair<String, String>): String {
        var translation = translations.optString(key, key)

        placeholders.forEach { (placeholder, value) ->
            translation = translation.replace("{$placeholder}", value)
        }

        return translation
    }

    fun getTranslation(key: String): String {
        return translations.optString(key, key)
    }

    fun reloadTranslations(locale: String) {
        localeTag = locale
        translations = bridgeClient.getTranslation(localeTag) ?: JSONObject()
    }

    private fun fetchRemoteData(url: String, callback: (List<Pair<Long, Int>>) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                GrindrPlus.logger.apply {
                    log("Failed to fetch remote data: ${e.message}")
                    writeRaw(e.stackTraceToString())
                }
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
                        GrindrPlus.logger.apply {
                            log("Failed to parse remote data: ${e.message}")
                            writeRaw(e.stackTraceToString())
                        }
                    }
                }
            }
        })
    }
}