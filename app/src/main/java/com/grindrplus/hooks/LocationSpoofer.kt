package com.grindrplus.hooks

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.children
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.ui.Utils
import com.grindrplus.ui.colors.Colors
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField

class LocationSpoofer : Hook(
    "Location spoofer",
    "Spoof your location"
) {
    private val location = "android.location.Location"
    private val chatBottomToolbar = "com.grindrapp.android.chat.presentation.ui.view.ChatBottomToolbar"
    private val appConfiguration = "com.grindrapp.android.platform.config.AppConfiguration"

    override fun init() {
        val locationClass = findClass(location)
        val appConfigurationClass = findClass(appConfiguration)

        if (Build.VERSION.SDK_INT >= 31) {
            locationClass.hook(
                "isMock",
                HookStage.BEFORE
            ) { param ->
                param.setResult(false)
            }
        } else {
            locationClass.hook(
                "isFromMockProvider",
                HookStage.BEFORE
            ) { param ->
                param.setResult(false)
            }
        }

        appConfigurationClass
            .hookConstructor(HookStage.AFTER) { param ->
                // isMockLocationAllowed
                setObjectField(param.thisObject(), "j", true)
            }

        locationClass.hook("getLatitude", HookStage.AFTER) { param ->
            (Config.get("current_location", "") as String).takeIf {
                it.isNotEmpty()
            }?.split(",")?.firstOrNull()
                ?.toDoubleOrNull()?.let {
                    param.setResult(it)
                }
        }

        locationClass.hook("getLongitude", HookStage.AFTER) { param ->
            (Config.get("current_location", "") as String).takeIf {
                it.isNotEmpty()
            }?.split(",")?.lastOrNull()
                ?.toDoubleOrNull()?.let {
                    param.setResult(it)
                }
        }

        findClass(chatBottomToolbar).hookConstructor(HookStage.AFTER) { param ->
            val chatBottomToolbarLinearLayout = param.thisObject() as LinearLayout
            val exampleButton = chatBottomToolbarLinearLayout.children.first()

            val customLocationButton = ImageButton(chatBottomToolbarLinearLayout.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { weight = 1f }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    focusable = ImageButton.FOCUSABLE
                }
                scaleType = ImageView.ScaleType.CENTER
                isClickable = true
                setBackgroundResource(
                    Utils.getId(
                        "image_button_ripple",
                        "drawable",
                        chatBottomToolbarLinearLayout.context
                    )
                )
                setImageResource(
                    Utils.getId(
                        "ic_my_location",
                        "drawable",
                        chatBottomToolbarLinearLayout.context
                    )
                )
                setPadding(
                    exampleButton.paddingLeft,
                    exampleButton.paddingTop,
                    exampleButton.paddingRight,
                    exampleButton.paddingBottom
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    drawable.colorFilter =
                        BlendModeColorFilter(Colors.grindr_middle_gray, BlendMode.SRC_IN)
                } else {
                    @Suppress("DEPRECATION")
                    drawable.colorFilter =
                        PorterDuffColorFilter(Colors.grindr_middle_gray, PorterDuff.Mode.SRC_IN)
                }
            }

            customLocationButton.setOnClickListener {
                var locations = GrindrPlus.database.getLocations()
                var locationNames = locations.map { it.first }
                var coordinatesMap =
                    locations.associate { it.first to "${it.second.first}, ${it.second.second}" }

                if (locations.isEmpty()) {
                    GrindrPlus.showToast(
                        Toast.LENGTH_LONG,
                        "No saved locations"
                    )
                    return@setOnClickListener
                }

                val locationDialogView = LinearLayout(it.context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val textViewCoordinates = TextView(it.context).apply {
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

                val spinnerLocations = Spinner(it.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 16
                        marginStart = 120
                        marginEnd = 140
                    }
                }

                val adapter = object : ArrayAdapter<String>(
                    it.context,
                    android.R.layout.simple_spinner_item,
                    locationNames
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        return getCustomView(position, convertView, parent)
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        return getCustomView(position, convertView, parent)
                    }

                    private fun getCustomView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = (convertView as? TextView) ?: TextView(it.context).apply {
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

                spinnerLocations.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                        ) {
                            textViewCoordinates.text = coordinatesMap[locationNames[position]]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            textViewCoordinates.text = ""
                        }
                    }

                val wrapDrawable = DrawableCompat.wrap(spinnerLocations.background)
                DrawableCompat.setTint(wrapDrawable, Color.WHITE)
                spinnerLocations.background = wrapDrawable

                val buttonCopy = Button(it.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 50
                        marginStart = 140
                        marginEnd = 140
                    }
                    text = "Copy Coordinates"
                    background = Utils.createButtonDrawable(Color.parseColor("#4CAF50"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        Utils.copyToClipboard(
                            "Coordinates",
                            textViewCoordinates.text.toString()
                        )
                    }
                }

                val buttonSet = Button(it.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 15
                        marginStart = 140
                        marginEnd = 140
                    }
                    text = "Teleport to location"
                    background = Utils.createButtonDrawable(Color.parseColor("#2196F3"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        Config.put("current_location", textViewCoordinates.text.toString())
                        GrindrPlus.showToast(
                            Toast.LENGTH_LONG,
                            "Successfully teleported to ${textViewCoordinates.text}"
                        )
                    }
                }

                val buttonOpen = Button(it.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 15
                        marginStart = 140
                        marginEnd = 140
                    }
                    text = "Open in Maps"
                    background = Utils.createButtonDrawable(Color.parseColor("#9C27B0"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    "geo:${textViewCoordinates.text}"
                                )
                            )
                        )
                    }
                }

                val buttonDelete = Button(it.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 15
                        marginStart = 140
                        marginEnd = 140
                    }
                    text = "Delete Location"
                    background = Utils.createButtonDrawable(Color.parseColor("#FF0000"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        val name = spinnerLocations.selectedItem.toString()
                        GrindrPlus.database.deleteLocation(name)

                        locations = GrindrPlus.database.getLocations()
                        locationNames = locations.map { it.first }
                        coordinatesMap = locations.associate {
                            it.first to
                                    "${it.second.first}, ${it.second.second}"
                        }
                        adapter.clear()
                        adapter.addAll(locationNames)

                        GrindrPlus.showToast(Toast.LENGTH_LONG, "Location deleted")
                    }
                }

                for (view in arrayOf(
                    spinnerLocations,
                    textViewCoordinates, buttonCopy,
                    buttonSet, buttonOpen, buttonDelete
                )) {
                    locationDialogView.addView(view)
                }

                AlertDialog.Builder(it.context).apply {
                    setTitle("Teleport Locations")
                    setView(locationDialogView)
                    setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                    show()
                }
            }

            chatBottomToolbarLinearLayout.addView(customLocationButton)
        }
    }
}