package com.grindrplus.commands

import android.app.AlertDialog
import android.net.Uri
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.packageName
import com.grindrplus.core.Config
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils.coordsToGeoHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray

class Location(recipient: String, sender: String) : CommandModule("Location", recipient, sender) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    @Command(name = "tp", aliases = ["tp"], help = "Teleport to a location")
    fun teleport(args: List<String>) {
        /**
         * If the user is currently used forced coordinates, don't allow teleportation.
         */
        if (Config.get("forced_coordinates", "") as String != "") {
            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("Teleportation disabled")
                    .setMessage(
                        "GrindrPlus is currently using forced coordinates. " +
                                "Please disable it to use teleportation."
                    )
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Disable") { _, _ ->
                        Config.put("forced_coordinates", "")
                        GrindrPlus.bridgeClient.deleteForcedLocation(packageName)
                        GrindrPlus.showToast(
                            Toast.LENGTH_LONG,
                            "Forced coordinates disabled"
                        )
                    }
                    .show()
            }

            return;
        }

        /**
         * This command is also used to toggle the teleportation feature. If the user hasn't
         * provided any arguments, just toggle teleport.
         */
        if (args.isEmpty()) {
            val status = (Config.get("current_location", "") as String).isEmpty()
            if (!status) {
                Config.put("current_location", "")
                return GrindrPlus.showToast(Toast.LENGTH_LONG, "Teleportation disabled")
            }

            return GrindrPlus.showToast(Toast.LENGTH_LONG, "Please provide a location")
        }

        /**
         * If the user has provided arguments, try to teleport to the location. We currently support
         * different formats for the location:
         * - "lat, lon" (e.g. "37.7749, -122.4194") for latitude and longitude.
         * - "lat" "lon" (e.g. "37.7749" "-122.4194") for latitude and longitude.
         * - "lat lon" (e.g. "37.7749 -122.4194") for latitude and longitude.
         * - "city, country" (e.g. "San Francisco, United States") for city and country.
         */
        when {
            args.size == 1 && args[0] == "off" -> {
                Config.put("current_location", "")
                return GrindrPlus.showToast(Toast.LENGTH_LONG, "Teleportation disabled")
            }
            args.size == 1 && args[0].contains(",") -> {
                val (lat, lon) = args[0].split(",").map { it.toDouble() }
                teleportToCoordinates(lat, lon)
            }
            args.size == 2 && args.all { arg -> arg.toDoubleOrNull() != null } -> {
                val (lat, lon) = args.map { it.toDouble() }
                teleportToCoordinates(lat, lon)
            }
            else -> {
                /**
                 * If we reached this point, the user has provided a name of a city. In this case,
                 * it could be either a saved location or an actual city.
                 */
                coroutineScope.launch {
                    val location = getLocation(args.joinToString(" "))
                    if (location != null) {
                        teleportToCoordinates(location.first, location.second)
                    } else {
                        /**
                         * No valid saved location was found, try to get the actual location. This
                         * is done by using Nominatim's API to get the latitude and longitude.
                         */
                        val apiLocation = getLocationFromNominatimAsync(args.joinToString(" "))
                        if (apiLocation != null) {
                            teleportToCoordinates(apiLocation.first, apiLocation.second)
                        } else {
                            GrindrPlus.showToast(Toast.LENGTH_LONG, "Location not found")
                        }
                    }
                }
                return
            }
        }
    }

    @Command(name = "save", aliases = ["sv"], help = "Save the current location")
    fun save(args: List<String>) {
        if (args.isEmpty()) {
            GrindrPlus.showToast(Toast.LENGTH_LONG, "Please provide a name for the location")
            return
        }

        val name = args[0]

        coroutineScope.launch {
            val location =
                when {
                    args.size == 1 -> Config.get("current_location", "") as String
                    args.size == 2 && args[1].contains(",") -> args[1]
                    args.size == 3 &&
                            args[1].toDoubleOrNull() != null &&
                            args[2].toDoubleOrNull() != null -> "${args[1]},${args[2]}"
                    args.size > 1 ->
                        getLocationFromNominatimAsync(args.drop(1).joinToString(" "))?.let {
                            "${it.first},${it.second}"
                        }
                    else -> ""
                }

            if (!location.isNullOrEmpty()) {
                val coordinates = location.split(",")
                if (coordinates.size == 2) {
                    try {
                        val lat = coordinates[0].toDouble()
                        val lon = coordinates[1].toDouble()

                        val existingLocation = getLocation(name)

                        if (existingLocation != null) {
                            updateLocation(name, lat, lon)
                            GrindrPlus.showToast(Toast.LENGTH_LONG, "Successfully updated $name")
                        } else {
                            addLocation(name, lat, lon)
                            GrindrPlus.showToast(Toast.LENGTH_LONG, "Successfully saved $name")
                        }
                    } catch (e: Exception) {
                        GrindrPlus.showToast(Toast.LENGTH_LONG, "Invalid coordinates format")
                    }
                } else {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Invalid coordinates format")
                }
            } else {
                GrindrPlus.showToast(Toast.LENGTH_LONG, "No location provided")
            }
        }
    }

    @Command(name = "delete", aliases = ["del"], help = "Delete a saved location")
    fun delete(args: List<String>) {
        if (args.isEmpty()) {
            return GrindrPlus.showToast(Toast.LENGTH_LONG, "Please provide a location to delete")
        }

        val name = args.joinToString(" ")

        coroutineScope.launch {
            val location = getLocation(name)
            if (location == null) {
                GrindrPlus.showToast(Toast.LENGTH_LONG, "Location not found")
                return@launch
            }

            deleteLocation(name)
            GrindrPlus.showToast(Toast.LENGTH_LONG, "Location deleted")
        }
    }

    private suspend fun getLocation(name: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val locationDao = GrindrPlus.database.teleportLocationDao()
            val entity = locationDao.getLocation(name)
            return@withContext entity?.let { Pair(it.latitude, it.longitude) }
        }

    private suspend fun addLocation(name: String, latitude: Double, longitude: Double) =
        withContext(Dispatchers.IO) {
            val locationDao = GrindrPlus.database.teleportLocationDao()
            val entity =
                com.grindrplus.persistence.model.TeleportLocationEntity(
                    name = name,
                    latitude = latitude,
                    longitude = longitude
                )
            locationDao.upsertLocation(entity)
        }

    private suspend fun updateLocation(name: String, latitude: Double, longitude: Double) =
        withContext(Dispatchers.IO) {
            val locationDao = GrindrPlus.database.teleportLocationDao()
            val entity =
                com.grindrplus.persistence.model.TeleportLocationEntity(
                    name = name,
                    latitude = latitude,
                    longitude = longitude
                )
            locationDao.upsertLocation(entity)
        }

    private suspend fun deleteLocation(name: String) =
        withContext(Dispatchers.IO) {
            val locationDao = GrindrPlus.database.teleportLocationDao()
            locationDao.deleteLocation(name)
        }

    private suspend fun getLocationFromNominatimAsync(location: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val url =
                "https://nominatim.openstreetmap.org/search?q=${Uri.encode(location)}&format=json"

            val randomUserAgent = getUserAgent()
            val request =
                okhttp3.Request.Builder().url(url).header("User-Agent", randomUserAgent).build()

            return@withContext try {
                OkHttpClient().newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (body.isNullOrEmpty()) return@withContext null

                    val json = JSONArray(body)
                    if (json.length() == 0) return@withContext null

                    val obj = json.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    Pair(lat, lon)
                }
            } catch (e: Exception) {
                val message =
                    "An error occurred while getting the location: ${e.message ?: "Unknown error"}"
                withContext(Dispatchers.Main) { GrindrPlus.showToast(Toast.LENGTH_LONG, message) }
                Logger.apply {
                    e(message)
                    writeRaw(e.stackTraceToString())
                }
                null
            }
        }

    private fun teleportToCoordinates(lat: Double, lon: Double, silent: Boolean = false) {
        Config.put("current_location", "$lat,$lon")
        val geohash = coordsToGeoHash(lat, lon)

        GrindrPlus.executeAsync {
            try {
                GrindrPlus.httpClient.updateLocation(geohash)
            } catch (e: Exception) {
                Logger.e("Error sending geohash to API: ${e.message}")
            }
        }

        if (!silent)
            GrindrPlus.showToast(Toast.LENGTH_LONG, "Teleported to $lat, $lon")
    }

    private fun getUserAgent(): String {
        val chromeVersions = listOf("120.0.0.0", "119.0.0.0", "118.0.0.0", "117.0.0.0", "116.0.0.0")
        val firefoxVersions = listOf("121.0", "120.0", "119.0", "118.0", "117.0")
        val safariVersions = listOf("17.2", "17.1", "17.0", "16.6", "16.5")
        val edgeVersions = listOf("120.0.0.0", "119.0.0.0", "118.0.0.0", "117.0.0.0")

        val windowsVersions = listOf("10.0", "11.0")
        val macVersions = listOf("10_15_7", "11_7_10", "12_7_2", "13_6_3", "14_2_1")

        return when ((1..5).random()) {
            1 -> {
                val version = chromeVersions.random()
                val winVer = windowsVersions.random()
                "Mozilla/5.0 (Windows NT $winVer; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Safari/537.36"
            }
            2 -> {
                val version = chromeVersions.random()
                val macVer = macVersions.random()
                "Mozilla/5.0 (Macintosh; Intel Mac OS X $macVer) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Safari/537.36"
            }
            3 -> {
                val version = firefoxVersions.random()
                val platform = if ((1..2).random() == 1) {
                    "Windows NT ${windowsVersions.random()}; Win64; x64"
                } else {
                    "Macintosh; Intel Mac OS X ${macVersions.random().replace("_", ".")}"
                }
                "Mozilla/5.0 ($platform; rv:$version) Gecko/20100101 Firefox/$version"
            }
            4 -> {
                val safariVer = safariVersions.random()
                val macVer = macVersions.random()
                val webkitVer = "605.1.${(10..20).random()}"
                "Mozilla/5.0 (Macintosh; Intel Mac OS X $macVer) AppleWebKit/$webkitVer (KHTML, like Gecko) Version/$safariVer Safari/$webkitVer"
            }
            5 -> {
                val version = edgeVersions.random()
                val winVer = windowsVersions.random()
                "Mozilla/5.0 (Windows NT $winVer; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Safari/537.36 Edg/$version"
            }
            else -> chromeVersions.random()
        }
    }
}
