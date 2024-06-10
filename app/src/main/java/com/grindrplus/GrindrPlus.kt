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
import com.grindrplus.core.Config
import com.grindrplus.core.Logger
import com.grindrplus.persistence.NewDatabase
import com.grindrplus.utils.HookManager
import dalvik.system.DexClassLoader
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

    lateinit var hookManager: HookManager

    var currentActivity: Activity? = null
        private set

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

        /**
         * Emergency reset of the database if the flag is set.
         */
        if ((Config.get("reset_database", false) as Boolean)) {
            logger.log("Resetting database...")
            database.deleteDatabase()
            Config.put("reset_database", false)
        }

        hookManager.init()
    }

    fun runOnMainThread(block: Runnable) {
        Handler(context.mainLooper).post(block)
    }

    fun runOnMainThreadWithCurrentActivity(block: (Activity) -> Unit) {
        runOnMainThread {
            currentActivity?.let { activity ->
                block(activity)
            }
        }
    }

    fun showToast(duration: Int, message: String) {
        runOnMainThread {
            Toast.makeText(context, message, duration).show()
        }
    }

    fun loadClass(name: String): Class<*> {
        return classLoader.loadClass(name)
    }
}