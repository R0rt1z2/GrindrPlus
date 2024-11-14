package com.grindrplus.commands

import android.app.AlertDialog
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Utils.fetchProfileById
import java.io.BufferedReader
import java.io.InputStreamReader

class Utils(
    recipient: String,
    sender: String
) : CommandModule("Utils", recipient, sender) {

    @Command("shell", help = "Run a shell command and display output")
    fun shell(args: List<String>) {
        if (args.isEmpty()) {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Please provide a shell command to execute"
            )
            return
        }

        val command = args.joinToString(" ")
        val output = StringBuilder()

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = reader.readLine()

            while (line != null) {
                output.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()

            process.waitFor()

        } catch (e: Exception) {
            output.append("Error executing command: ${e.message}")
        }

        GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
            val dialogView = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 40, 60, 40)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val textView = AppCompatTextView(activity).apply {
                text = output.toString()
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(20, 20, 20, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }

            dialogView.addView(textView)

            AlertDialog.Builder(activity)
                .setTitle("Output")
                .setView(dialogView)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    @Command("getProfile", aliases = ["gp"], help = "Fetches profile information")
    fun getProfile(args: List<String>) {
        if (args.size != 1) {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Please provide a profile ID"
            )
            return
        }

        val id = args[0].toLongOrNull()
        if (id == null) {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Please provide valid ID"
            )
            return
        }

        val profile = fetchProfileById(id)

        GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
            val dialogView = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 40, 60, 40)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val textView = AppCompatTextView(activity).apply {
                text = profile.toString()
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(20, 20, 20, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }

            dialogView.addView(textView)

            AlertDialog.Builder(activity)
                .setTitle("Profile")
                .setView(dialogView)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }
}
