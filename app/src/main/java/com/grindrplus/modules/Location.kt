package com.grindrplus.modules

import com.grindrplus.Hooker.Companion.config
import com.grindrplus.Hooker.Companion.sharedPref
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Utils.getLatLngFromLocationName
import com.grindrplus.core.Utils.getLocationPreference
import com.grindrplus.core.Utils.logChatMessage
import com.grindrplus.core.Utils.setLocationPreference
import com.grindrplus.core.Utils.toggleSetting

class Location(recipient: String) : CommandModule(recipient) {
    @Command(name = "teleport", aliases = ["tp"], help = "Teleport to a location.")
    fun teleport(args: List<String>) {
        if (args.isEmpty()) {
            return logChatMessage(toggleSetting("teleport_enabled", "Teleport"),
                this.recipient, this.recipient)
        }

        if (!config.readBoolean("teleport_enabled", false)) {
            config.writeConfig("teleport_enabled", true)
        }

        when {
            args.size == 1 && args[0].contains(",") -> {
                val (lat, lon) = args[0].split(",").map { it.trim().toDouble() }
                setLocationPreference("teleport_location", lat, lon)
                logChatMessage("Teleported to $lat, $lon.", this.recipient, this.recipient)
            }
            args.size == 2 && args.all { it.toDoubleOrNull() != null } -> {
                val lat = args[0].toDouble()
                val lon = args[1].toDouble()
                setLocationPreference("teleport_location", lat, lon)
                logChatMessage("Teleported to $lat, $lon.", this.recipient, this.recipient)
            }
            else -> {
                val aliases = config.readMap("teleport_aliases")
                if (aliases.has(args.joinToString(" "))) {
                    val (lat, lon) = aliases.getString(args.joinToString(" "))
                        .split(",").map { it.trim().toDouble() }
                    setLocationPreference("teleport_location", lat, lon)
                    logChatMessage("Teleported to $lat, $lon.", this.recipient, this.recipient)
                } else {
                    val coordinates = getLatLngFromLocationName(args.joinToString(" "))
                    if (coordinates != null) {
                        setLocationPreference("teleport_location", coordinates.first, coordinates.second)
                        logChatMessage(
                            "Teleported to ${coordinates.first}, ${coordinates.second}.",
                            this.recipient, this.recipient
                        )
                    } else {
                        logChatMessage("Could not find location.", this.recipient, this.recipient)
                    }
                }
            }
        }
    }

    @Command(name = "location", aliases = ["loc"], help = "Get your current location.")
    fun location(args: List<String>) {
        val location = getLocationPreference("teleport_location")
        if (!config.readBoolean("teleport_enabled", false)) {
            logChatMessage("Teleport is disabled.", this.recipient, this.recipient)
        } else {
            if (location != null) {
                logChatMessage(
                    "Current teleport location: ${location.first}, ${location.second}.",
                    this.recipient, this.recipient
                )
            } else {
                logChatMessage("No teleport location set.", this.recipient, this.recipient)
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
                    logChatMessage("Saved $location as ${args[0]}.", this.recipient, this.recipient)
                } else {
                    logChatMessage("No teleport location set.", this.recipient, this.recipient)
                }
            }
            2, 3 -> {
                val coordinatesString = if (args.size == 2) args[1] else "${args[1]},${args[2]}"
                val coordinates = coordinatesString.split(",").map { it.trim() }
                if (coordinates.size == 2 && coordinates.all { it.toDoubleOrNull() != null }) {
                    config.addToMap("teleport_aliases", args[0], coordinates.joinToString(","))
                    logChatMessage("Saved ${coordinates.joinToString(",")} as ${args[0]}.", this.recipient, this.recipient)
                } else {
                    logChatMessage("Invalid coordinates format. Use <lat,lon> or <lat> <lon>.", this.recipient, this.recipient)
                }
            }
            else -> {
                logChatMessage("Invalid command format. Use /save [alias] or /save [alias] [lat,lon].", this.recipient, this.recipient)
            }
        }
    }

    @Command(name = "delete", aliases = ["del"], help = "Delete a saved teleport location.")
    private fun delCommand(args: List<String>) {
        if (args.isEmpty()) {
            logChatMessage("Please specify a teleport location alias.", this.recipient, this.recipient)
        } else {
            val teleportAliases = config.readMap("teleport_aliases")
            if (teleportAliases.has(args[0])) {
                teleportAliases.remove(args[0])
                config.writeConfig("teleport_aliases", teleportAliases)
                logChatMessage("Deleted teleport location ${args[0]}.", this.recipient, this.recipient)
            } else {
                logChatMessage("No teleport location with alias ${args[0]} found.", this.recipient, this.recipient)
            }
        }
    }

    @Command(name = "list", help = "List all saved teleport locations.")
    private fun listCommand(args: List<String>) {
        val teleportAliases = config.readMap("teleport_aliases")
        if (teleportAliases.length() == 0) {
            logChatMessage("No teleport locations saved.", this.recipient, this.recipient)
        } else {
            val teleportLocations = buildString {
                teleportAliases.keys().forEach { alias ->
                    append("$alias: ${teleportAliases.getString(alias)}\n")
                }
            }.trimEnd()
            logChatMessage("Saved teleport locations:\n$teleportLocations", this.recipient, this.recipient)
        }
    }
}