package com.grindrplus.tasks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils.coordsToGeoHash
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.utils.Task
import de.robv.android.xposed.XposedHelpers.callMethod

class AlwaysOnline : Task(
    id = "Always Online",
    description = "Keeps you online by periodically fetching cascade",
    initialDelayMillis = 30 * 1000,
    intervalMillis = 10 * 60 * 1000
) {
    override suspend fun execute() {
        val grindrLocationProviderInstance =
            GrindrPlus.instanceManager.getInstance<Any>(GrindrPlus.grindrLocationProvider)

        val location = callMethod(grindrLocationProviderInstance, "a")
        val latitude = callMethod(location, "getLatitude") as Double
        val longitude = callMethod(location, "getLongitude") as Double
        val geoHash = coordsToGeoHash(latitude, longitude)

        val result = GrindrPlus.httpClient.fetchCascade(geoHash)

        if (result.has("items")) {
            val profileCount = result.optJSONArray("items")?.length() ?: 0
            logi("Fetched $profileCount profiles")
        } else {
            loge("Failed to fetch cascade")
            Logger.writeRaw(result.toString())
        }
    }
}