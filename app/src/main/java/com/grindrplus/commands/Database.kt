package com.grindrplus.commands

import android.graphics.Color
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.grindrplus.GrindrPlus
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.ui.Utils.copyToClipboard
import com.grindrplus.utils.UiHelper.DialogButton
import com.grindrplus.utils.UiHelper.showAlertDialog
import com.grindrplus.utils.UiHelper.showToast

class Database(
    recipient: String,
    sender: String
) : CommandModule("Database", recipient, sender) {
    @Command("list_tables", aliases = ["lts"], help = "List all tables in the database")
    fun listTables(args: List<String>) {
        try {
            val query = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;"
            val tables = DatabaseHelper.query(query).map { it["name"].toString() }

            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                val dialogView = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 40)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tableList = if (tables.isEmpty()) "No tables found."
                    else tables.joinToString("\n")

                val textView = AppCompatTextView(activity).apply {
                    text = tableList
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

                showAlertDialog {
                    title = "Database Tables"
                    view = dialogView
                    positiveButton = DialogButton(
                        text = "Copy",
                        onClick = { copyToClipboard("Database Tables", tableList) }
                    )
                    negativeButton = DialogButton("Close")
                }
            }
        } catch (e: Exception) {
            showToast("Error: ${e.message}", Toast.LENGTH_LONG)
        }
    }

    @Command("list_table", aliases = ["lt"], help = "List all rows from a specific table")
    fun listTable(args: List<String>) {
        if (args.isEmpty()) {
            showToast("Please provide a table name.", Toast.LENGTH_LONG)
            return
        }

        val tableName = args[0]
        try {
            val query = "SELECT * FROM $tableName;"
            val rows = DatabaseHelper.query(query)

            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                val dialogView = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 40)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tableContent = if (rows.isEmpty()) {
                    "No rows found in table $tableName."
                } else {
                    rows.joinToString("\n\n") { row ->
                        row.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                    }
                }

                val textView = AppCompatTextView(activity).apply {
                    text = tableContent
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

                showAlertDialog {
                    title = "Table Content: $tableName"
                    view = dialogView
                    positiveButton = DialogButton(
                        text = "Copy",
                        onClick = { copyToClipboard("Table Content: $tableName", tableContent) }
                    )
                    negativeButton = DialogButton("Close")
                }
            }
        } catch (e: Exception) {
            showToast("Error: ${e.message}", Toast.LENGTH_LONG)
        }
    }

    @Command("list_databases", aliases = ["ldbs"], help = "List all database files in the app's files directory")
    fun listDatabases(args: List<String>) {
        try {
            val context = GrindrPlus.context
            val databases = context.databaseList()

            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                val dialogView = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 40)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val dbList = if (databases.isEmpty()) "No databases found." else databases.joinToString("\n")

                val textView = AppCompatTextView(activity).apply {
                    text = dbList
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

                showAlertDialog {
                    title = "Database Files"
                    view = dialogView
                    positiveButton = DialogButton(
                        text = "Copy",
                        onClick = { copyToClipboard("Database Files", dbList) }
                    )
                    negativeButton = DialogButton("Close")
                }
            }
        } catch (e: Exception) {
            showToast("Error: ${e.message}", Toast.LENGTH_LONG)
        }
    }
}
