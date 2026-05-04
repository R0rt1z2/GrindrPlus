package com.grindrplus.core

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku privilege elevation for version checking and privileged package scanning.
 * All Shizuku-dependent features degrade gracefully when Shizuku is unavailable.
 */
object ShizukuManager {

    sealed class ShizukuState {
        object Unavailable : ShizukuState()
        object PermissionPending : ShizukuState()
        object PermissionGranted : ShizukuState()
        object PermissionDenied : ShizukuState()
    }

    sealed class CompatResult {
        object Compatible : CompatResult()
        data class Incompatible(val installedCode: Long, val expectedCodes: List<Int>) : CompatResult()
        object Unavailable : CompatResult()
    }

    private val _state = mutableStateOf<ShizukuState>(ShizukuState.Unavailable)
    val state: State<ShizukuState> get() = _state

    val isAvailable: Boolean
        get() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    val isGranted: Boolean
        get() = isAvailable && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _state.value = if (isGranted) ShizukuState.PermissionGranted else ShizukuState.PermissionPending
        Logger.d("Shizuku binder received", LogSource.MANAGER)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _state.value = ShizukuState.Unavailable
        Logger.d("Shizuku binder dead", LogSource.MANAGER)
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
            ShizukuState.PermissionGranted
        } else {
            ShizukuState.PermissionDenied
        }
    }

    fun init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Logger.d("Shizuku requires API 28+; skipping init", LogSource.MANAGER)
            return
        }
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }.onFailure {
            Logger.w("Shizuku init failed: ${it.message}", LogSource.MANAGER)
        }
    }

    fun release() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE) {
        if (!isAvailable) return
        runCatching { Shizuku.requestPermission(requestCode) }
            .onFailure { Logger.w("Shizuku permission request failed: ${it.message}", LogSource.MANAGER) }
    }

    /**
     * Returns the long version code of the installed Grindr package, or null if unavailable.
     * Uses privileged IPackageManager when Shizuku is granted; falls back to unprivileged PM.
     */
    suspend fun getGrindrVersionCode(packageName: String = Constants.GRINDR_PACKAGE_NAME): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (isGranted) {
                    getVersionCodePrivileged(packageName)
                } else {
                    getVersionCodeUnprivileged(packageName)
                }
            }.getOrNull()
        }

    /**
     * Checks whether the installed Grindr version is in the supported list.
     */
    suspend fun checkCompatibility(
        packageName: String = Constants.GRINDR_PACKAGE_NAME,
        expectedVersionCodes: List<Int>
    ): CompatResult {
        val installedCode = getGrindrVersionCode(packageName) ?: return CompatResult.Unavailable
        return if (expectedVersionCodes.any { it.toLong() == installedCode }) {
            CompatResult.Compatible
        } else {
            CompatResult.Incompatible(installedCode, expectedVersionCodes)
        }
    }

    private fun getVersionCodePrivileged(packageName: String): Long? {
        val process = Runtime.getRuntime().exec(arrayOf("dumpsys", "package", packageName))
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return VERSION_CODE_REGEX.find(output)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun getVersionCodeUnprivileged(packageName: String): Long? {
        val pm = com.grindrplus.GrindrPlus.context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(packageName, 0)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private const val REQUEST_CODE = 1337
    private val VERSION_CODE_REGEX = Regex("versionCode=(\\d+)")
}
