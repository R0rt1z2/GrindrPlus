package com.grindrplus.modules

import android.widget.Toast
import com.grindrplus.Hooker
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Utils.toggleSetting
import com.grindrplus.core.Utils.logChatMessage
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast
import org.json.JSONObject

class Settings(recipient: String, sender: String) : CommandModule(recipient, sender) {
    @Command(name = "redesign", aliases = ["rd"], help = "Toggle the new Grindr design.")
    private fun redesign(args: List<String>) {
        showDialog("Redesign", "Toggling this setting will change the design " +
                "of the profile view. This change requires a restart of the app. Do you want to proceed?",
            "Yes", { showToast(Toast.LENGTH_LONG,
                toggleSetting("profile_redesign", "Profile Redesign")) },
            "No", {}
        )
    }

    @Command(name = "views", aliases = ["vw"], help = "Control whether you want to be hidden from the view list.")
    private fun views(args: List<String>) {
        showDialog("Views", "Toggling this setting will change whether you are hidden from " +
                "the view list. This change requires a restart of the app. Do you want to proceed?",
            "Yes", { showToast(Toast.LENGTH_LONG,
                toggleSetting("dont_record_views", "Hiding from views")) },
            "No", {}
        )
    }

    @Command(name = "details", aliases = ["dt"], help = "Toggle the profile details feature (BMI, etc).")
    private fun details(args: List<String>) {
        showDialog("Profile Details", "Toggling this setting will change whether profile details " +
                "(such as BMI) are shown. This change requires a restart of the app. Do you want to proceed?",
            "Yes", { showToast(Toast.LENGTH_LONG,
                toggleSetting("show_profile_details", "Profile details")) },
            "No", {}
        )
    }

    @Command(name = "migrate", aliases = ["mg"], help = "Migrate old saved phrases to the new system.")
    private fun migrate(args: List<String>) {
        val oldSavedPhrasesIds = Hooker.sharedPref.getStringSet("phrases", emptySet()) ?: emptySet()
        if (oldSavedPhrasesIds.isEmpty()) {
            return showToast(Toast.LENGTH_LONG, "No old saved phrases found.")
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

        showToast(Toast.LENGTH_LONG, "Migrated ${oldSavedPhrases.size} phrases.")
    }

    @Command(name = "rmphrases", aliases = ["rmph"], help = "Remove all saved phrases.")
    private fun removePhrases(args: List<String>) {
        showDialog("Remove Phrases", "Are you sure you want to remove all saved phrases?",
            "Yes",
            {
                Hooker.config.writeConfig("phrases", JSONObject())
                showToast(Toast.LENGTH_LONG, "Removed all saved phrases.")
            },
            "No", {}
        )

        logChatMessage("Removed all saved phrases.", this.recipient, this.recipient)
    }

    @Command(name = "profileDelay", aliases = ["pd"], help = "Toggle the profile delay workaround.")
    private fun profileDelay(args: List<String>) {
        showDialog("Profile Delay", "Toggling this setting will change whether the profile delay workaround is enabled. " +
                "This change requires a restart of the app. Do you want to proceed?",
            "Yes", { showToast(Toast.LENGTH_LONG,
                toggleSetting("profile_delay_workaround", "Profile Delay Workaround")) },
            "No", {}
        )
    }
}