package com.grindrplus.manager

import android.content.Context
import com.grindrplus.utils.SessionInstaller
import com.grindrplus.utils.download
import com.grindrplus.utils.newKeystore
import com.grindrplus.utils.unzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.patch.LSPatch
import org.lsposed.patch.util.Logger
import java.io.File
import java.io.PrintStream


class Installation(
    val context: Context,
    val modVer: String,
    val modUrl: String,
    val grindrUrl: String
) {
    private val keyStore by lazy {
        File(context.cacheDir, "keystore.jks").also {
            if (!it.exists()) newKeystore(it)
        }
    }

    suspend fun install(print: (String) -> Unit) {
        val folder = context.getExternalFilesDir(null)
        val unzipFolder = File(folder, "splitApks/").also { it.mkdirs() }
        val outputDir = File(folder, "LSPatchOutput/").also { it.mkdirs() }
        val modFile = File(folder, "mod-$modVer.zip")

        step("Download Grindr APK") {
            val xapk = File(folder, "grindr-$modVer.xapk")
            if (!xapk.exists()) {
                download(
                    context,
                    xapk,
                    grindrUrl
                ) { print("Grindr apk DL status: ${((it ?: 0f) * 100).toString().take(4)}%") }
            }

            unzipFolder.listFiles().forEach { it.delete() }
            xapk.unzip(unzipFolder)
        }

        step("Download Mod") {
            if (!modFile.exists()) {
                download(
                    context,
                    modFile,
                    modUrl
                ) { print("Mod download status: ${((it ?: 0f) * 100).toString().take(3)}%") }
            }
        }

        step("Patch Grindr APK") {
            outputDir.listFiles().forEach { it.delete() }

            withContext(Dispatchers.IO) {
                LSPatch(
                    object : Logger() {
                        override fun d(p0: String?) {
                            print("LSPOSED D $p0")
                        }

                        override fun i(p0: String?) {
                            print("LSPOSED I $p0")
                        }

                        override fun e(p0: String?) {
                            print("LSPOSED E $p0")
                        }

                    },
                    *(unzipFolder.listFiles()?.map { it.absolutePath }
                        ?.filter { it.endsWith(".apk") }?.toTypedArray()
                        ?: emptyArray()),
                    "-o",
                    outputDir.absolutePath,
                    "-l",
                    "0",
                    "-v",
                    "--force",
                    "-m",
                    modFile.absolutePath,
                    "-k",
                    keyStore.absolutePath,
                    "password",
                    "alias",
                    "password"
                ).doCommandLine()
            }
        }

        step("Install Grindr APK") {
            SessionInstaller(context).installApks(context, outputDir.listFiles().toList(), false)
        }
    }

    suspend fun step(name: String, e: suspend () -> Unit) {
        println("yay new step: $name")
        e() //TODO: UI
    }
}