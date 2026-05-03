package com.grindrplus.hooks

import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class NativeLibraryGuard : Hook(
    "Native Library Guard",
    "Prevents anti-tampering native libraries from loading"
) {
    companion object {
        private val BLOCKED_LIBS = setOf("pairip", "pairipcore", "integrity_checker")
    }

    override fun init() {
        // Hook the internal method called by all System.loadLibrary paths.
        // setResult(null) is a no-op on void methods — throw instead so the load is aborted.
        runCatching {
            Runtime::class.java.hook("loadLibrary0", HookStage.BEFORE) { param ->
                val libName = param.args().filterIsInstance<String>().lastOrNull() ?: return@hook
                if (BLOCKED_LIBS.any { libName.contains(it, ignoreCase = true) }) {
                    Logger.i("Blocked anti-tampering library (loadLibrary0): $libName", LogSource.MODULE)
                    param.setThrowable(SecurityException("GrindrPlus: blocked anti-tampering lib: $libName"))
                }
            }
        }.onFailure {
            Logger.w("Failed to hook Runtime.loadLibrary0: ${it.message}", LogSource.MODULE)
        }

        // Belt-and-suspenders: hook the public API directly as a fallback.
        runCatching {
            System::class.java.hook("loadLibrary", HookStage.BEFORE) { param ->
                val libName = param.args().filterIsInstance<String>().lastOrNull() ?: return@hook
                if (BLOCKED_LIBS.any { libName.contains(it, ignoreCase = true) }) {
                    Logger.i("Blocked anti-tampering library (System.loadLibrary): $libName", LogSource.MODULE)
                    param.setThrowable(SecurityException("GrindrPlus: blocked anti-tampering lib: $libName"))
                }
            }
        }.onFailure {
            Logger.w("Failed to hook System.loadLibrary: ${it.message}", LogSource.MODULE)
        }

        // PairIP may also load via absolute path (System.load) rather than by name.
        runCatching {
            System::class.java.hook("load", HookStage.BEFORE) { param ->
                val path = param.args().filterIsInstance<String>().lastOrNull() ?: return@hook
                if (BLOCKED_LIBS.any { path.contains(it, ignoreCase = true) }) {
                    Logger.i("Blocked anti-tampering library (System.load): $path", LogSource.MODULE)
                    param.setThrowable(SecurityException("GrindrPlus: blocked anti-tampering lib at: $path"))
                }
            }
        }.onFailure {
            Logger.w("Failed to hook System.load: ${it.message}", LogSource.MODULE)
        }
    }
}
