package com.grindrplus.modules

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import com.grindrplus.Hooker
import com.grindrplus.Hooker.Companion.config
import com.grindrplus.Hooker.Companion.sharedPref
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Utils.getLatLngFromLocationName
import com.grindrplus.core.Utils.getLocationPreference
import com.grindrplus.core.Utils.setLocationPreference
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast
import com.grindrplus.core.Utils.toggleSetting

class Location(recipient: String, sender: String) : CommandModule(recipient, sender) {
    @Command(name = "teleport", aliases = ["tp"], help = "Teleport to a location.")
    fun teleport(args: List<String>) {
        if (args.isEmpty()) {
            return showToast(Toast.LENGTH_LONG,
                toggleSetting("teleport_enabled", "Teleport"))
        }

        if (!config.readBoolean("teleport_enabled", false)) {
            config.writeConfig("teleport_enabled", true)
        }

        when {
            args.size == 1 && args[0].contains(",") -> {
                val (lat, lon) = args[0].split(",").map { it.trim().toDouble() }
                setLocationPreference("teleport_location", lat, lon)
                showToast(Toast.LENGTH_LONG, "Teleported to $lat, $lon.")
            }
            args.size == 2 && args.all { it.toDoubleOrNull() != null } -> {
                val lat = args[0].toDouble()
                val lon = args[1].toDouble()
                setLocationPreference("teleport_location", lat, lon)
                showToast(Toast.LENGTH_LONG, "Teleported to $lat, $lon.")
            }
            else -> {
                val aliases = config.readMap("teleport_aliases")
                if (aliases.has(args.joinToString(" "))) {
                    val (lat, lon) = aliases.getString(args.joinToString(" "))
                        .split(",").map { it.trim().toDouble() }
                    setLocationPreference("teleport_location", lat, lon)
                    showToast(Toast.LENGTH_LONG, "Teleported to $lat, $lon.")
                } else {
                    val coordinates = getLatLngFromLocationName(args.joinToString(" "))
                    if (coordinates != null) {
                        setLocationPreference("teleport_location", coordinates.first, coordinates.second)
                        showToast(Toast.LENGTH_LONG, "Teleported to ${coordinates.first}, ${coordinates.second}.")
                    } else {
                        showToast(Toast.LENGTH_LONG, "Invalid location.")
                    }
                }
            }
        }
    }

    @Command(name = "location", aliases = ["loc"], help = "Get your current location.")
    fun location(args: List<String>) {
        val location = getLocationPreference("teleport_location")
        if (!config.readBoolean("teleport_enabled", false)) {
            return showToast(Toast.LENGTH_LONG, "Teleport is disabled.")
        } else {
            if (location != null) {
                showDialog("Current teleport location",
                    "Current teleport location: ${location.first}, ${location.second}.",
                    "OK", {}, "Copy location",
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val clipboard = Hooker.appContext.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Location",
                                "${location.first}, ${location.second}"))
                            showToast(Toast.LENGTH_LONG, "Location copied to clipboard.")
                        }
                    }
                )
            } else {
                showToast(Toast.LENGTH_LONG, "No teleport location set.")
            }
        }
    }

    @Command(name = "save", help = "Save the current teleport location or alternatively, specify coordinates.")
    fun save(args: List<String>) {
        when (args.size) {
            1 -> {
                val location = sharedPref.getString("teleport_location", null)
                if (location != null) {
                    config.addToMap("teleport_aliases", args[0], location)
                    showToast(Toast.LENGTH_LONG, "Saved $location as ${args[0]}.")
                } else {
                    showToast(Toast.LENGTH_LONG, "No teleport location set.")
                }
            }
            2, 3 -> {
                val coordinatesString = if (args.size == 2) args[1] else "${args[1]},${args[2]}"
                val coordinates = coordinatesString.split(",").map { it.trim() }
                if (coordinates.size == 2 && coordinates.all { it.toDoubleOrNull() != null }) {
                    config.addToMap("teleport_aliases", args[0], coordinates.joinToString(","))
                    showToast(Toast.LENGTH_LONG, "Saved ${coordinates.joinToString(",")} as ${args[0]}.")
                } else {
                    showToast(Toast.LENGTH_LONG, "Invalid coordinates format. Use <lat,lon> or <lat> <lon>.")
                }
            }
            else -> {
                showToast(Toast.LENGTH_LONG, "Invalid command format. Use /save [alias] or /save [alias] [lat,lon].")
            }
        }
    }

    @Command(name = "delete", aliases = ["del"], help = "Delete a saved teleport location.")
    private fun delCommand(args: List<String>) {
        if (args.isEmpty()) {
            showToast(Toast.LENGTH_LONG, "Please specify a teleport location alias.")
        } else {
            val teleportAliases = config.readMap("teleport_aliases")
            if (teleportAliases.has(args[0])) {
                showDialog("Delete teleport location",
                    "Are you sure you want to delete the teleport location ${args[0]}?",
                    "Yes",
                    {
                        teleportAliases.remove(args[0])
                        config.writeConfig("teleport_aliases", teleportAliases)
                        showToast(Toast.LENGTH_LONG, "Deleted teleport location ${args[0]}.")
                    },
                    "No", {}
                )
            } else {
                showToast(Toast.LENGTH_LONG, "Teleport location ${args[0]} not found.")
            }
        }
    }

    @Command(name = "list", help = "List all saved teleport locations.")
    private fun listCommand(args: List<String>) {
        val teleportAliases = config.readMap("teleport_aliases")
        if (teleportAliases.length() == 0) {
            showToast(Toast.LENGTH_LONG, "No teleport locations saved.")
        } else {
            val teleportLocations = buildString {
                teleportAliases.keys().forEach { alias ->
                    append("$alias: ${teleportAliases.getString(alias)}\n")
                }
            }.trimEnd()
            showDialog("Saved teleport locations", teleportLocations,
                "OK", {}, "Copy locations",
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val clipboard = Hooker.appContext.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Locations", teleportLocations))
                        showToast(Toast.LENGTH_LONG, "Locations copied to clipboard.")
                    }
                }
            )
        }
    }
}