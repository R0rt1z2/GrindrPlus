package com.grindrplus.commands

import android.app.AlertDialog
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
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

    @Command("prefix", help = "Change the command prefix (default: /)")
    fun prefix(args: List<String>) {
        val prefix = Config.get("command_prefix", "/")
        when {
            args.isEmpty() -> GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "The current command prefix is $prefix"
            )
            args[0].isBlank() -> GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Invalid command prefix"
            )
            args[0] == "reset" || args[0] == "clear" -> {
                Config.put("command_prefix", "/")
                GrindrPlus.showToast(
                    Toast.LENGTH_LONG,
                    "Command prefix reset to /",
                )
            }
            args[0].length > 1 -> GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Command prefix must be a single character"
            )
            !args[0].matches(Regex("[^a-zA-Z0-9]")) -> GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Command prefix must be a special character (no letters or numbers)"
            )
            args[0] == prefix -> GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Command prefix is already set to ${args[0]}"
            )
            else -> {
                Config.put("command_prefix", args[0])
                GrindrPlus.showToast(
                    Toast.LENGTH_LONG,
                    "Command prefix set to ${args[0]}"
                )
            }
        }
    }
}