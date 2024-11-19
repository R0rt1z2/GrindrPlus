package com.grindrplus.persistence

import com.grindrplus.GrindrPlus
import com.grindrplus.persistence.model.GrindrSettings
import com.grindrplus.persistence.model.SettingsRequest
import de.robv.android.xposed.XposedHelpers.getObjectField

fun GrindrSettings.toGrindrSettings(): Any {
    val grindrSettingsConstructor = GrindrPlus.loadClass(
        "com.grindrapp.android.model.GrindrSettings"
    ).constructors.first()
    return grindrSettingsConstructor.newInstance(
        approximateDistance,
        hideViewedMe,
        incognito,
        locationSearchOptOut
    )
}

fun Any.asSettingsToSettings(): GrindrSettings {
    return GrindrSettings(
        approximateDistance = (getObjectField(this, "approximateDistance") as? Boolean) ?: false,
        hideViewedMe = (getObjectField(this, "hideViewedMe") as? Boolean) ?: false,
        incognito = (getObjectField(this, "incognito") as? Boolean) ?: false,
        locationSearchOptOut = (getObjectField(this, "locationSearchOptOut") as? Boolean) ?: false
    )
}

fun SettingsRequest.toGrindrSettings(): Any {
    val settingsRequestConstructor = GrindrPlus.loadClass(
        "com.grindrapp.android.model.UpdateSettingsRequest"
    ).constructors.first()

    return settingsRequestConstructor.newInstance(
        settings.toGrindrSettings()
    )
}

fun Any.asSettingsRequestToSettingsRequest(): SettingsRequest {
    return SettingsRequest(
        settings = getObjectField(this, "settings").asSettingsToSettings()
    )
}