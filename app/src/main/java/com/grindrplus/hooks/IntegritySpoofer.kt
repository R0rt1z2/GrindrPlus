package com.grindrplus.hooks

import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class IntegritySpoofer : Hook(
    "Integrity Spoofer",
    "Spoofs Google Play Integrity API responses to bypass PairIP attestation"
) {
    companion object {
        // Builds a structurally valid alg:none JWT so any client-side format check passes.
        // Server-side validation will still reject it — this only helps if Grindr parses fields locally.
        private val SPOOFED_TOKEN: String by lazy {
            val enc = java.util.Base64.getUrlEncoder().withoutPadding()
            val header = enc.encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
            val payload = enc.encodeToString(
                """{"appIntegrity":{"appRecognitionVerdict":"PLAY_RECOGNIZED"},"deviceIntegrity":{"deviceRecognitionVerdict":["MEETS_DEVICE_INTEGRITY","MEETS_STRONG_INTEGRITY"]},"accountDetails":{"appLicensingVerdict":"LICENSED"}}""".toByteArray()
            )
            "$header.$payload."
        }
    }

    override fun init() {
        hookIntegrityTokenResponse()
        hookAppIntegrity()
        hookDeviceIntegrity()
        hookAccountDetails()
    }

    private fun hookIntegrityTokenResponse() {
        val cls = findClassOrNull("com.google.android.play.core.integrity.IntegrityTokenResponse")
        if (cls == null) {
            Logger.w("IntegrityTokenResponse not found — skipping", LogSource.MODULE)
            return
        }
        runCatching {
            cls.hook("token", HookStage.AFTER) { param ->
                val current = param.getResult() as? String
                if (current.isNullOrEmpty()) {
                    param.setResult(SPOOFED_TOKEN)
                }
            }
        }.onFailure {
            Logger.w("Failed to hook IntegrityTokenResponse.token: ${it.message}", LogSource.MODULE)
        }
    }

    private fun hookAppIntegrity() {
        val cls = findClassOrNull("com.google.android.play.core.integrity.model.AppIntegrity")
        if (cls == null) {
            Logger.w("AppIntegrity not found — skipping", LogSource.MODULE)
            return
        }
        runCatching {
            cls.hook("getAppRecognitionVerdict", HookStage.AFTER) { param ->
                param.setResult("PLAY_RECOGNIZED")
            }
        }.onFailure {
            Logger.w("Failed to hook AppIntegrity.getAppRecognitionVerdict: ${it.message}", LogSource.MODULE)
        }
    }

    private fun hookDeviceIntegrity() {
        val cls = findClassOrNull("com.google.android.play.core.integrity.model.DeviceIntegrity")
        if (cls == null) {
            Logger.w("DeviceIntegrity not found — skipping", LogSource.MODULE)
            return
        }
        runCatching {
            cls.hook("getDeviceRecognitionVerdict", HookStage.AFTER) { param ->
                param.setResult(listOf("MEETS_DEVICE_INTEGRITY"))
            }
        }.onFailure {
            Logger.w("Failed to hook DeviceIntegrity.getDeviceRecognitionVerdict: ${it.message}", LogSource.MODULE)
        }
    }

    private fun hookAccountDetails() {
        val cls = findClassOrNull("com.google.android.play.core.integrity.model.AccountDetails")
        if (cls == null) {
            Logger.w("AccountDetails not found — skipping", LogSource.MODULE)
            return
        }
        runCatching {
            cls.hook("getAppLicensingVerdict", HookStage.AFTER) { param ->
                param.setResult("LICENSED")
            }
        }.onFailure {
            Logger.w("Failed to hook AccountDetails.getAppLicensingVerdict: ${it.message}", LogSource.MODULE)
        }
    }
}
