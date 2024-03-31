package com.grindrplus.modules

import com.grindrplus.Hooker
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils.toggleSetting
import com.grindrplus.core.Utils.logChatMessage
import org.json.JSONObject

class Settings(recipient: String, sender: String) : CommandModule(recipient, sender) {
    @Command(name = "redesign", aliases = ["rd"], help = "Toggle the new Grindr design.")
    private fun redesign(args: List<String>) {
        logChatMessage(toggleSetting("profile_redesign", "Profile redesign"),
            this.recipient, this.recipient)
    }

    @Command(name = "views", aliases = ["vw"], help = "Control whether you want to be hidden from the view list.")
    private fun views(args: List<String>) {
        logChatMessage(toggleSetting("dont_record_views", "Hiding from views"),
            this.recipient, this.recipient)
    }

    @Command(name = "details", aliases = ["dt"], help = "Toggle the profile details feature (BMI, etc).")
    private fun details(args: List<String>) {
        logChatMessage(toggleSetting("show_profile_details", "Profile details"),
            this.recipient, this.recipient)
    }

    @Command(name = "migrate", aliases = ["mg"], help = "Migrate old saved phrases to the new system.")
    private fun migrate(args: List<String>) {
        val oldSavedPhrasesIds = Hooker.sharedPref.getStringSet("phrases", emptySet()) ?: emptySet()
        if (oldSavedPhrasesIds.isEmpty()) {
            return logChatMessage("No old saved phrases found.", this.recipient, this.recipient)
        }

        val oldSavedPhrases = oldSavedPhrasesIds.associateWith {id ->
            val text = Hooker.sharedPref.getString("phrase_${id}_text", "") ?: ""
            val frequency = Hooker.sharedPref.getInt("phrase_${id}_frequency", 0)
            val timestamp = Hooker.sharedPref.getLong("phrase_${id}_timestamp", 0L)
            JSONObject().apply {
                put("text", text)
                put("frequency", frequency)
                put("timestamp", timestamp)
            }
        }

        val newPhrasesConfig = Hooker.config.readMap("phrases")
        var newIdCounter = (newPhrasesConfig.keys().asSequence()
            .mapNotNull { it.toIntOrNull() }
            .maxOrNull() ?: 0) + 1
        oldSavedPhrases.forEach { (_, oldPhrase) ->
            val newId = newIdCounter++
            newPhrasesConfig.put(newId.toString(), oldPhrase)
        }

        Hooker.config.writeConfig("phrases", newPhrasesConfig)
        Hooker.config.writeConfig("id_counter", newIdCounter - 1)

        logChatMessage("Migrated ${oldSavedPhrases.size} old saved phrases to the new system.",
            this.recipient, this.recipient)
    }

    @Command(name = "rmphrases", aliases = ["rmph"], help = "Remove all saved phrases.")
    private fun removePhrases(args: List<String>) {
        Hooker.config.writeConfig("phrases", JSONObject())
        logChatMessage("Removed all saved phrases.", this.recipient, this.recipient)
    }
}