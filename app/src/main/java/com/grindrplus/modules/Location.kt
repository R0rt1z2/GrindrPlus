package com.grindrplus.modules

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.get
import com.grindrplus.Hooker
import com.grindrplus.Hooker.Companion.config
import com.grindrplus.Hooker.Companion.sharedPref
import com.grindrplus.R
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Logger
import com.grindrplus.core.Utils.copyToClipboard
import com.grindrplus.core.Utils.createButtonDrawable
import com.grindrplus.core.Utils.getKeysFromJSONObject
import com.grindrplus.core.Utils.getLatLngFromLocationName
import com.grindrplus.core.Utils.getLocationPreference
import com.grindrplus.core.Utils.setLocationPreference
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast
import com.grindrplus.core.Utils.toggleSetting
import de.robv.android.xposed.XposedHelpers


class Location(recipient: String, sender: String) : CommandModule(recipient, sender) {
    @Command(name = "teleport", aliases = ["tp"], help = "Teleport to a location.")
    fun teleport(args: List<String>) {
        if (args.isEmpty()) {
            toggleSetting("teleport_enabled")
            return showToast(Toast.LENGTH_LONG, "Teleport is now " +
                    "${if (config.readBoolean("teleport_enabled", 
                            false)) "enabled" else "disabled"}.")
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
                            copyToClipboard("Location", "${location.first}, ${location.second}")
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

    @Command(name = "lc", help = "Manage saved teleport locations.")
    private fun locationCommand(args: List<String>) {
        val teleportAliases = config.readMap("teleport_aliases")
        var aliases = getKeysFromJSONObject(teleportAliases)
        val activity = Hooker.activityHook.getCurrentActivity()

        if (aliases.isEmpty()) {
            showToast(Toast.LENGTH_LONG, "No locations saved.")
            return
        }

        val locationDialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val textViewCoordinates = TextView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
                marginStart = 70
                marginEnd = 70
            }
            gravity = Gravity.CENTER_HORIZONTAL
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val spinnerLocations = Spinner(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
                marginStart = 120
                marginEnd = 140
            }
        }

        val adapter = object : ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_item, aliases) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return getCustomView(position, convertView, parent)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return getCustomView(position, convertView, parent)
            }

            private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = (convertView as? TextView) ?: TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    minHeight = 120
                    gravity = (Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL)
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    background = ColorDrawable(Color.TRANSPARENT)
                }
                view.text = getItem(position)
                return view
            }
        }
        spinnerLocations.adapter = adapter

        spinnerLocations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                textViewCoordinates.text = teleportAliases[aliases[position]].toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                textViewCoordinates.text = ""
            }
        }

        val buttonCopy = Button(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 50
                marginStart = 140
                marginEnd = 140
            }
            text = "Copy Coordinates"
            background = createButtonDrawable(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Coordinates", textViewCoordinates.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(activity, "Coordinates copied to clipboard.", Toast.LENGTH_SHORT).show()
            }
        }

        val buttonSet = Button(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
                marginStart = 140
                marginEnd = 140
            }
            text = "Teleport to location"
            background = createButtonDrawable(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                teleport(listOf(textViewCoordinates.text.toString()))
            }
        }

        val buttonOpen = Button(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
                marginStart = 140
                marginEnd = 140
            }
            text = "Open in Maps"
            background = createButtonDrawable(Color.parseColor("#9C27B0"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                context.startActivity(Intent(
                    Intent.ACTION_VIEW, Uri.parse("geo:${textViewCoordinates.text}")
                ))
            }
        }

        val buttonDelete = Button(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
                marginStart = 140
                marginEnd = 140
            }
            text = "Delete Location"
            background = createButtonDrawable(Color.parseColor("#FF0000"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val selectedPosition = spinnerLocations.selectedItemPosition
                val selectedAlias = aliases[selectedPosition]

                teleportAliases.remove(selectedAlias)
                config.writeConfig("teleport_aliases", teleportAliases)
                teleportAliases.remove(selectedAlias)

                aliases = getKeysFromJSONObject(teleportAliases)

                adapter.clear()
                adapter.addAll(aliases)
                adapter.notifyDataSetChanged()

                if (aliases.isNotEmpty()) {
                    textViewCoordinates.text = teleportAliases[aliases[0]].toString()
                } else {
                    textViewCoordinates.text = "No saved locations."
                    Toast.makeText(Hooker.appContext, "No locations left.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        for (view in arrayOf(spinnerLocations,
            textViewCoordinates, buttonCopy,
            buttonSet, buttonOpen, buttonDelete)) {
            locationDialogView.addView(view)
        }

        activity.runOnUiThread {
            AlertDialog.Builder(activity).apply {
                setTitle("Teleport Locations")
                setView(locationDialogView)
                setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }
}