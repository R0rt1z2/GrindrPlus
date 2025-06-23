package com.grindrplus.tasks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.CoroutineHelper.callSuspendFunction
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils.coordsToGeoHash
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.utils.Task
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField

class AlwaysOnline :
    Task(
        id = "Always Online",
        description = "Keeps you online by periodically fetching cascade",
        initialDelayMillis = 30 * 1000,
        intervalMillis = 5 * 60 * 1000
    ) {
    override suspend fun execute() {
        try {
            val serverDrivenCascadeRepoInstance =
                GrindrPlus.instanceManager.getInstance<Any>(GrindrPlus.serverDrivenCascadeRepo)
            val grindrLocationProviderInstance =
                GrindrPlus.instanceManager.getInstance<Any>(GrindrPlus.grindrLocationProvider)

            val location = getObjectField(grindrLocationProviderInstance, "e")
            val latitude = callMethod(location, "getLatitude") as Double
            val longitude = callMethod(location, "getLongitude") as Double
            val geoHash = coordsToGeoHash(latitude, longitude)

            val methodName = "fetchCascadePage"
            val method =
                serverDrivenCascadeRepoInstance!!.javaClass.methods.firstOrNull {
                    it.name == methodName
                } ?: throw IllegalStateException("Unable to find $methodName method")

            val params = arrayOf<Any?>(
                geoHash,
                null,
                false, false, false, false,
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                false,
                1,
                null, null,
                false, false, false,
                null,
                false
            )

            val result = callSuspendFunction { continuation ->
                method.invoke(serverDrivenCascadeRepoInstance, *params, continuation)
            }

            if (result.toString().contains("Success")) {
                logi("AlwaysOnline task executed successfully")
            } else {
                loge("AlwaysOnline task failed: $result")
            }
        } catch (e: Exception) {
            loge("Error in AlwaysOnline task: ${e.message}")
            Logger.writeRaw(e.stackTraceToString())
        }
    }
}
