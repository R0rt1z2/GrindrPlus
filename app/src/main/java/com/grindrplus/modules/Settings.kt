package com.grindrplus.modules

import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.grindrplus.Hooker
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Utils.logChatMessage
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast
import org.json.JSONObject

class Settings(recipient: String, sender: String) : CommandModule(recipient, sender) {
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

    @Command(name = "settings", help = "Manage app settings.")
    private fun settingsCommand(args: List<String>) {
        val activity = Hooker.activityHook.getCurrentActivity()
        val settingsDialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(58, 32, 58, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val settings = listOf(
            "Profile redesign" to "profile_redesign",
            "Hide from views" to "dont_record_views",
            "Show profile details" to "show_profile_details",
            "Location spoofing" to "teleport_enabled"
        )

        activity?.runOnUiThread {
            settings.forEach { (name, key) ->
                val switchView = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(10, 10, 10, 10)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val textView = TextView(activity).apply {
                    text = name
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                    )
                    textSize = 18f
                }

                val switch = Switch(activity).apply {
                    isChecked = Hooker.config.readBoolean(key, false)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnCheckedChangeListener { _, isChecked ->
                        Hooker.config.writeConfig(key, isChecked)
                        showToast(Toast.LENGTH_LONG, "$name" +
                                " ${if (isChecked) "enabled" else "disabled"}.")
                    }
                }

                switchView.addView(textView)
                switchView.addView(switch)
                settingsDialogView.addView(switchView)
            }

            AlertDialog.Builder(activity).apply {
                setTitle("Settings")
                setView(settingsDialogView)
                setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }
}