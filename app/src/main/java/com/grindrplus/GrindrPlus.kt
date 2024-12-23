package com.grindrplus

import Database
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.grindrplus.bridge.BridgeClient
import com.grindrplus.core.Config
import com.grindrplus.core.CoroutineHelper
import com.grindrplus.core.InstanceManager
import com.grindrplus.core.Logger
import com.grindrplus.core.http.Client
import com.grindrplus.core.http.Interceptor
import com.grindrplus.persistence.NewDatabase
import com.grindrplus.utils.HookManager
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
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

    var currentActivity: Activity? = null
        private set

    private val userAgent = "a5.t"
    private val userSession = "com.grindrapp.android.storage.b"
    private val deviceInfo = "j3.t"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun init(modulePath: String, application: Application) {
        Log.d(
            TAG,
            "Initializing GrindrPlus with module path: $modulePath, application: $application"
        )

        this.context = application // do not use .applicationContext as it's null at this point
        this.classLoader =
            DexClassLoader(modulePath, context.cacheDir.absolutePath, null, context.classLoader)
        this.logger = Logger(context.filesDir.absolutePath + "/grindrplus.log")
        this.newDatabase = NewDatabase.create(context)
        this.database = Database(context, context.filesDir.absolutePath + "/grindrplus.db")
        this.hookManager = HookManager()
        this.coroutineHelper = CoroutineHelper(classLoader)
        this.instanceManager = InstanceManager(classLoader)

        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (BuildConfig.DEBUG) {
                    logger.log("Resuming activity: ${activity.javaClass.name}")
                }
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                if (BuildConfig.DEBUG) {
                    logger.log("Pausing activity: ${activity.javaClass.name}")
                }
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
            deviceInfo
        )

        instanceManager.setCallback(userSession) { uSession ->
            instanceManager.setCallback(userAgent) { uAgent ->
                instanceManager.setCallback(deviceInfo) { dInfo ->
                    httpClient = Client(Interceptor(uSession, uAgent, dInfo))
                }
            }
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
                showToast(Toast.LENGTH_LONG, "Operation failed: ${e.message}")
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
}