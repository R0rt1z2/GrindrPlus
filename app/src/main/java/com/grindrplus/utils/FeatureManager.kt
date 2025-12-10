package com.grindrplus.utils

data class Feature(val name: String, var isEnabled: Boolean)

class FeatureManager {
    private val features = mutableMapOf<String, Feature>()

    fun add(feature: Feature) {
        features[feature.name] = feature
    }

    fun isEnabled(featureName: String): Boolean {
        return features[featureName]?.isEnabled ?: false
    }

    fun isManaged(featureName: String): Boolean {
        return features.containsKey(featureName)
    }
}