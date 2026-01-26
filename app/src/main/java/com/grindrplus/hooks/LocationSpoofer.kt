package com.grindrplus.hooks

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.core.logw
import com.grindrplus.persistence.model.TeleportLocationEntity
import com.grindrplus.ui.Utils
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy

class LocationSpoofer : Hook(
    "Location spoofer",
    "Spoof your location"
) {
    private val location = "android.location.Location"
    private val chatBottomToolbar = "com.grindrapp.android.chat.presentation.ui.view.ChatBottomToolbar"
    private val appConfiguration = "com.grindrapp.android.platform.config.AppConfiguration"
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    var gpsLocationLatitude: Double = 0.0
    var gpsLocationLongitude: Double = 0.0


    override fun init() {
        val locationClass = findClass(location)

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

        locationClass.hook("getLatitude", HookStage.AFTER) { param ->
            val gpsLatitude = param.getResult() as Double?
            if (gpsLatitude != null)
                gpsLocationLatitude = gpsLatitude;

            (Config.get("forced_coordinates", Config.get("current_location", "")) as String).takeIf {
                it.isNotEmpty()
            }?.split(",")?.firstOrNull()
                ?.toDoubleOrNull()?.let {
                    param.setResult(it)
                }
        }

        locationClass.hook("getLongitude", HookStage.AFTER) { param ->
            val gpsLongitude = param.getResult() as Double?
            if (gpsLongitude != null)
                gpsLocationLongitude = gpsLongitude;

            (Config.get("forced_coordinates", Config.get("current_location", "")) as String).takeIf {
                it.isNotEmpty()
            }?.split(",")?.lastOrNull()
                ?.toDoubleOrNull()?.let {
                    param.setResult(it)
                }
        }

        findClass(chatBottomToolbar).hookConstructor(HookStage.AFTER) { param ->
            val chatBottomToolbarLinearLayout = param.thisObject() as LinearLayout
            val exampleButton = chatBottomToolbarLinearLayout.children.first()

            var locationButtonExists = false
            if (Config.get("do_gui_safety_checks", true) as Boolean) {
                locationButtonExists = chatBottomToolbarLinearLayout.children.any { view ->
                    if (view is ImageButton) {
                        view.tag == "custom_location_button" ||
                                view.contentDescription == "Teleport"
                    } else false
                }
            }

            if (locationButtonExists) {
                logw("Location button already exists?")
                return@hookConstructor
            }

            val customLocationButton = ImageButton(chatBottomToolbarLinearLayout.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { weight = 1f }
                focusable = ImageButton.FOCUSABLE
                scaleType = ImageView.ScaleType.CENTER
                isClickable = true
                tag = "custom_location_button"
                contentDescription = "Teleport"
                setBackgroundResource(
                    Utils.getId(
                        "image_button_ripple",
                        "drawable",
                        GrindrPlus.context
                    )
                )
                setImageResource(
                    Utils.getId(
                        "ic_my_location",
                        "drawable",
                        GrindrPlus.context
                    )
                )
                setPadding(
                    exampleButton.paddingLeft,
                    exampleButton.paddingTop,
                    exampleButton.paddingRight,
                    exampleButton.paddingBottom
                )

                val grindrGray = "#9e9ea8".toColorInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    drawable.colorFilter =
                        BlendModeColorFilter(grindrGray, BlendMode.SRC_IN)
                } else {
                    @Suppress("DEPRECATION")
                    drawable.colorFilter =
                        PorterDuffColorFilter(grindrGray, PorterDuff.Mode.SRC_IN)
                }
            }

            customLocationButton.setOnClickListener {
                manageLocationsDialog(it.context)
            }

            chatBottomToolbarLinearLayout.addView(customLocationButton)
        }
    }

    private fun manageLocationsDialog(context: Context) {
        coroutineScope.launch {
            var locations: List<TeleportLocationEntity>

            val locationDialogView = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // ScrollView + RadioGroup
            val scrollView = ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    360
                ).apply {
                    topMargin = 16
                    marginStart = 70
                    marginEnd = 70
                    bottomMargin = 16
                }
            }

            val radioGroup = RadioGroup(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = RadioGroup.VERTICAL
            }

            // Add RadioButtons dynamically
            suspend fun refreshLocations(newSelectedLocatioName: String = "") {
                locations = getLocations()
                val selectedLocationName = newSelectedLocatioName.ifEmpty {
                    Config.get("current_location_name", "") as String
                }

                withContext(Dispatchers.Main) {
                    radioGroup.clearCheck()
                    radioGroup.removeAllViews()

                    val radioButtonLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    val teleportOffRadioButton = RadioButton(context).apply {
                        layoutParams = radioButtonLayoutParams
                        text = "teleport off"
                        id = View.generateViewId()
                    }
                    radioGroup.addView(teleportOffRadioButton)
                    var selectedLocationViewId = teleportOffRadioButton.id

                    locations.forEach { location ->
                        val radioButton = RadioButton(context).apply {
                            layoutParams = radioButtonLayoutParams
                            text = location.name
                            id = View.generateViewId()
                            tag = location
                        }
                        if (location.name == selectedLocationName)
                            selectedLocationViewId = radioButton.id

                        radioGroup.addView(radioButton)
                    }

                    radioGroup.check(selectedLocationViewId)
                }
            }

            fun getSelectedLocation() : TeleportLocationEntity? {
                if (radioGroup.checkedRadioButtonId == -1)
                    return null

                val radioButton: RadioButton = radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                val location = radioButton.tag as TeleportLocationEntity?

                return location
            }

            refreshLocations()

            scrollView.addView(radioGroup)
            locationDialogView.addView(scrollView)

            // Buttons
            val buttonLayoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 16
                marginStart = 16
                marginEnd = 16
                bottomMargin = 16
            }

            val buttonAdd = Button(context).apply {
                layoutParams = buttonLayoutParams
                text = "Add"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#4CAF50"))
                setOnClickListener {
                    addSavedLocationDialog(context) { location ->
                        coroutineScope.launch {
                            withContext(Dispatchers.Main) {
                                addLocation(location)
                            }
                            refreshLocations(location.name)
                        }
                    }
                }
            }

            val buttonSet = Button(context).apply {
                layoutParams = buttonLayoutParams
                text = "Teleport"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#2196F3"))
                setOnClickListener {
                    val location = getSelectedLocation()

                    if (location == null) {
                        GrindrPlus.showToast(Toast.LENGTH_SHORT, "No location selected")
                        return@setOnClickListener
                    }

                    val coordinates = location.let { "${it.latitude}, ${it.longitude}" }
                    Config.put("current_location", coordinates)
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Teleported to $coordinates")
                }
            }

            val buttonOpenMaps = Button(context).apply {
                layoutParams = buttonLayoutParams
                text = "View in Maps"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#9C27B0"))
                setOnClickListener {
                    val location = getSelectedLocation()

                    if (location == null) {
                        GrindrPlus.showToast(Toast.LENGTH_SHORT, "No location selected")
                        return@setOnClickListener
                    }

                    val coordinates = location.let { "${it.latitude}, ${it.longitude}" }
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coordinates")))
                }
            }

            val buttonDelete = Button(context).apply {
                layoutParams = buttonLayoutParams
                text = "Delete"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#FF0000"))
                setOnClickListener {
                    val location = getSelectedLocation()

                    if (location == null) {
                        GrindrPlus.showToast(Toast.LENGTH_SHORT, "No location selected")
                        return@setOnClickListener
                    }

                    coroutineScope.launch {
                        deleteLocation(location.name)
                        refreshLocations()
                    }
                }
            }

            // Add buttons into horizontal LinearLayouts
            val rowLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 70
                marginEnd = 70
            }

            val row1 = LinearLayout(context).apply {
                layoutParams = rowLayoutParams
                orientation = LinearLayout.HORIZONTAL
                addView(buttonAdd)
//                addView(buttonSet)
            }

            val row2 = LinearLayout(context).apply {
                layoutParams = rowLayoutParams
                orientation = LinearLayout.HORIZONTAL
                addView(buttonOpenMaps)
                addView(buttonDelete)
            }

            locationDialogView.addView(row1)
            locationDialogView.addView(row2)

            fun teleport() {
                val location = getSelectedLocation()

                if (location == null) {
                    Config.put("current_location", "")
                    Config.put("current_location_name", "")
                    GrindrPlus.showToast(Toast.LENGTH_SHORT, "Teleporting stopped")
                    return
                }

                val coordinates = location.let { "${it.latitude}, ${it.longitude}" }
                Config.put("current_location", coordinates)
                Config.put("current_location_name", location.name)
                GrindrPlus.showToast(Toast.LENGTH_LONG, "Teleported to ${location.name}")
            }

            AlertDialog.Builder(context).apply {
                setTitle("Teleport Locations")
                setView(locationDialogView)
                setPositiveButton("OK") { dialog, _ -> teleport() }
                setNegativeButton("Close", null)
                show()
            }
        }
    }

    private fun addSavedLocationDialog(context: Context, onLocationAdd: (TeleportLocationEntity) -> Unit) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 2, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val inputName = EditText(context).apply {
            hint = "Name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 70
                marginEnd = 70
                bottomMargin = 16
            }
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }

        val inputLatitude = EditText(context).apply {
            hint = "Latitude"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 70
                marginEnd = 70
                bottomMargin = 16
            }
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }

        val inputLongitude = EditText(context).apply {
            hint = "Longitude"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 70
                marginEnd = 70
                bottomMargin = 24
            }
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }

        val buttonLoadGps = Button(context).apply {
            text = "Load from GPS"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = 70
                marginEnd = 70
                bottomMargin = 16
            }
            setOnClickListener {
                if (gpsLocationLatitude == 0.0 || gpsLocationLongitude == 0.0) {
                    GrindrPlus.showToast(Toast.LENGTH_SHORT, "Unable to load GPS location")
                    return@setOnClickListener
                }

                inputLatitude.setText(gpsLocationLatitude.toString())
                inputLongitude.setText(gpsLocationLongitude.toString())
            }
        }

        val buttonPickOnMap = Button(context).apply {
            text = "Pick on map"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = 70
                marginEnd = 70
                bottomMargin = 16
            }
            setOnClickListener {
                mapsLocationPickerDialog(context, { tle ->
                    inputLatitude.setText(tle.latitude.toString())
                    inputLongitude.setText(tle.longitude.toString())
                })
            }
        }

        // add all views
        container.addView(inputName)
        container.addView(inputLatitude)
        container.addView(inputLongitude)
        container.addView(buttonLoadGps)
        container.addView(buttonPickOnMap)

        suspend fun saveLocation(): Boolean {
            val name = inputName.text.toString()
            val lat = inputLatitude.text.toString().toDoubleOrNull()
            val lon = inputLongitude.text.toString().toDoubleOrNull()

            if (lat == null || lon == null || name.isEmpty()) {
                GrindrPlus.showToast(Toast.LENGTH_SHORT, "All fields are required")
                return false
            }

            val locations = getLocations()
            val locationNames = locations.map { it.name }

            if (locationNames.contains(name)) {
                GrindrPlus.showToast(Toast.LENGTH_SHORT, "Location with this name already exists")
                return false
            }

            val location = TeleportLocationEntity(name, lat, lon);
            onLocationAdd(location)

            return true
        }

        AlertDialog.Builder(context).apply {
            setTitle("Add Location")
            setView(container)
            setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            setPositiveButton("Save", null)

            val dialog = show()
            // set listener here instead of in setPositiveButton to be able to prevent the dialog from closing
            // setPositiveButton listener always closes it
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                coroutineScope.launch {
                    if (saveLocation())
                        dialog.dismiss()
                }
            }
        }
    }

    private fun mapsLocationPickerDialog(context: Context, onLocationPicked: (TeleportLocationEntity) -> Unit) {
        var selectedLatLng: Any? = null
        var marker: Any? = null

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 64, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // create google maps mapView
        val mapViewConstructor = findClass("com.google.android.gms.maps.MapView").getConstructor(Context::class.java)
        val mapView = mapViewConstructor.newInstance(context) as View
        callMethod(mapView, "onCreate", arrayOf(Bundle::class.java), null)
        callMethod(mapView, "onResume")

        mapView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            820
        )

        container.addView(mapView)

        // create initial LatLng (set to current gps position)
        val latLngClass = findClass("com.google.android.gms.maps.model.LatLng")
        selectedLatLng = XposedHelpers.newInstance(latLngClass, gpsLocationLatitude, gpsLocationLongitude)

        // create marker options
        val markerOptionsClass = findClass("com.google.android.gms.maps.model.MarkerOptions")
        val markerOptions = markerOptionsClass
            .getConstructor()
            .newInstance()

        // set initial marker position
        callMethod(markerOptions, "position", selectedLatLng)


        // implement OnMapClickListener via proxy
        val onMapClickListener = Proxy.newProxyInstance(
            context.classLoader,
            arrayOf(findClass("com.google.android.gms.maps.GoogleMap\$OnMapClickListener"))
        ) { _, method, args ->

            if (method.name == "onMapClick") {
                selectedLatLng = args?.get(0)  // Any

                if (marker != null)
                    callMethod(marker, "setPosition", selectedLatLng)
            }
            null
        }

        // create camera update to zoom the map to initial location
        val cameraUpdateFactoryClass = findClass("com.google.android.gms.maps.CameraUpdateFactory")
        val cameraUpdate = XposedHelpers.callStaticMethod(
            cameraUpdateFactoryClass,
            "newLatLngZoom",
            selectedLatLng,
            14f   // zoom level
        )

        // implement OnMapReadyCallback via proxy
        val onMapReadyListener = Proxy.newProxyInstance(
            context.classLoader,
            arrayOf(findClass("com.google.android.gms.maps.OnMapReadyCallback"))
        ) { _, method, args ->

            if (method.name == "onMapReady") {
                val googleMap = args?.get(0)
                marker = callMethod(
                    googleMap,
                    "addMarker",
                    markerOptions
                )

                callMethod(googleMap, "setOnMapClickListener", onMapClickListener)
                callMethod(googleMap, "moveCamera", cameraUpdate)
            }
            null
        }

        callMethod(mapView, "getMapAsync", onMapReadyListener)


        AlertDialog.Builder(context).apply {
            setTitle("Pick Location")
            setView(container)
            setNegativeButton("Cancel", null)
            setPositiveButton("OK", null)

            val dialog = show()
            // set listener here instead of in setPositiveButton to be able to prevent the dialog from closing
            // setPositiveButton listener always closes it
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                coroutineScope.launch {
                    if (selectedLatLng != null) {
                        val latitude = getObjectField(selectedLatLng, "latitude") as Double
                        val longitude = getObjectField(selectedLatLng, "longitude") as Double

                        val location = TeleportLocationEntity("maps-pick", latitude, longitude)
                        onLocationPicked(location)

                        dialog.dismiss()

                    } else {
                        GrindrPlus.showToast(Toast.LENGTH_SHORT, "No location selected")
                    }
                }
            }
        }

    }
    private suspend fun getLocations(): List<TeleportLocationEntity> = withContext(Dispatchers.IO) {
        return@withContext GrindrPlus.database.teleportLocationDao().getLocations()
    }

    private suspend fun addLocation(location: TeleportLocationEntity) = withContext(Dispatchers.IO) {
        GrindrPlus.database.teleportLocationDao().addLocation(location)
    }

    private suspend fun deleteLocation(name: String) = withContext(Dispatchers.IO) {
        GrindrPlus.database.teleportLocationDao().deleteLocation(name)
    }
}