package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class SignatureSpoofer : Hook(
    "Signature Spoofer",
    "Allow logging in with Google while using LSPatch"
) {
    @OptIn(ExperimentalStdlibApi::class)
    private val packageSignature = "823f5a17c33b16b4775480b31607e7df35d67af8".hexToByteArray()
    private val androidUtilsLight = "com.google.android.gms.common.util.AndroidUtilsLight"
    private val getPackageCertificateHashBytes = "getPackageCertificateHashBytes"

    override fun init() {

        // GMS Login
        findClass(androidUtilsLight)
            .hook(getPackageCertificateHashBytes, HookStage.BEFORE) {
                    param ->
                param.setResult(packageSignature)
            }
    }
}