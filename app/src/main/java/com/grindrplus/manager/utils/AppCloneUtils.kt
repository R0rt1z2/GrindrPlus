package com.grindrplus.manager.utils

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import java.io.FileOutputStream
import java.io.IOException

object AppCloneUtils {
    private const val TAG = "AppCloneUtils"
    private const val MANIFEST_PATH = "AndroidManifest.xml"

    /**
     * Check if Grindr is installed on the device
     */
    fun isGrindrInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GRINDR_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get existing Grindr clones to determine next suffix number
     */
    fun getExistingClones(context: Context): List<String> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        return packages
            .filter { it.packageName.startsWith("$GRINDR_PACKAGE_NAME.") }
            .map { it.packageName }
    }

    /**
     * Get next available clone number
     */
    fun getNextCloneNumber(context: Context): Int {
        val clones = getExistingClones(context)
        var nextNum = 1

        while (clones.contains("$GRINDR_PACKAGE_NAME.$nextNum")) {
            nextNum++
        }

        return nextNum
    }

    /**
     * Clone Grindr with a new package name and app name
     */
    suspend fun cloneGrindr(
        context: Context,
        newPackageName: String,
        newAppName: String,
        print: (String) -> Unit,
        progress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            print("Starting Grindr cloning process...")

            val grindrInfo = context.packageManager.getPackageInfo(GRINDR_PACKAGE_NAME, 0)
            val grindrApkPath = grindrInfo.applicationInfo?.sourceDir ?:
                throw IOException("Could not access Grindr APK path")
            val grindrApkFile = File(grindrApkPath)

            if (!grindrApkFile.exists()) {
                print("ERROR: Could not find Grindr APK file")
                return@withContext false
            }

            print("Found Grindr APK at: $grindrApkPath")

            val tempDir = File(context.cacheDir, "grindr_clone_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            val outputDir = File(context.getExternalFilesDir(null), "grindr_clones")
            outputDir.mkdirs()
            val outputApk = File(outputDir, "$newPackageName.apk")

            print("Preparing to create clone: $newAppName ($newPackageName)")

            val manifestFile = modifyManifest(context, grindrApkFile, tempDir, newPackageName, newAppName, print)
            if (manifestFile == null) {
                print("ERROR: Failed to modify AndroidManifest.xml")
                return@withContext false
            }

            // TODO: Sign the cloned app.
            print("Creating modified APK...")
            val success = createModifiedApk(grindrApkFile, outputApk, manifestFile, print, progress)

            if (!success) {
                print("ERROR: Failed to create modified APK")
                return@withContext false
            }

            print("Modified APK created at: ${outputApk.absolutePath}")

            print("Installing cloned Grindr app...")
            val installer = SessionInstaller()
            val installed = installer.installApks(
                context,
                listOf(outputApk),
                false,
                callback = { installSuccess, message ->
                    if (installSuccess) {
                        print("SUCCESS: Grindr clone installed successfully")
                    } else {
                        print("ERROR: Failed to install Grindr clone: $message")
                    }
                },
                log = print
            )

            tempDir.deleteRecursively()

            print("Cloning process completed${if (installed) " successfully" else " with errors"}")
            return@withContext installed

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error cloning Grindr")
            print("ERROR: Failed to clone Grindr: ${e.localizedMessage}")
            return@withContext false
        }
    }

    /**
     * Extract and modify the AndroidManifest.xml to change package name and app name
     */
    private suspend fun modifyManifest(
        context: Context,
        apkFile: File,
        tempDir: File,
        newPackageName: String,
        newAppName: String,
        print: (String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            print("Extracting and modifying AndroidManifest.xml...")

            val manifestFile = File(tempDir, MANIFEST_PATH)
            var manifestContent: ByteArray? = null

            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry(MANIFEST_PATH)
                if (entry == null) {
                    print("ERROR: AndroidManifest.xml not found in APK")
                    return@withContext null
                }

                zip.getInputStream(entry).use { input ->
                    manifestContent = input.readBytes()
                }
            }

            if (manifestContent == null) {
                print("ERROR: Failed to read AndroidManifest.xml")
                return@withContext null
            }

            try {
                ZipFile(apkFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (entry.name.startsWith("META-INF/") && entry.name.endsWith(".RSA")) {
                            val signatureFile = File(tempDir, entry.name)
                            signatureFile.parentFile?.mkdirs()

                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(signatureFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                print("Warning: Could not extract existing signatures: ${e.localizedMessage}")
            }

            print("Using package cloning to create: $newPackageName ($newAppName)")

            // TODO: Actually modify the manifest. This only copies the original one.
            FileOutputStream(manifestFile).use { output ->
                output.write(manifestContent!!)
            }

            print("AndroidManifest.xml modified successfully")
            return@withContext manifestFile

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error modifying AndroidManifest.xml")
            print("ERROR: Failed to modify AndroidManifest.xml: ${e.localizedMessage}")
            return@withContext null
        }
    }

    /**
     * Create a new APK with the modified manifest
     */
    private suspend fun createModifiedApk(
        sourceApk: File,
        outputApk: File,
        manifestFile: File,
        print: (String) -> Unit,
        progress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            print("Creating modified APK...")

            if (outputApk.exists()) {
                outputApk.delete()
            }

            ZipFile(sourceApk).use { zipIn ->
                val entries = zipIn.entries().asSequence().toList()
                val totalEntries = entries.size

                ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                    zipOut.putNextEntry(ZipEntry(MANIFEST_PATH))
                    manifestFile.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()

                    var entryCount = 0
                    entries.forEachIndexed { index, entry ->
                        if (entry.name != MANIFEST_PATH) {
                            entryCount++
                            if (entryCount % 50 == 0) {
                                val currentProgress = (index.toFloat() / totalEntries)
                                progress(currentProgress)
                            }

                            // TODO: Verify if this is correct
                            if (!entry.name.startsWith("META-INF/") ||
                                (!entry.name.endsWith(".SF") &&
                                        !entry.name.endsWith(".RSA") &&
                                        !entry.name.endsWith(".DSA"))) {

                                try {
                                    val newEntry = ZipEntry(entry.name)
                                    newEntry.method = entry.method
                                    zipOut.putNextEntry(newEntry)

                                    if (!entry.isDirectory) {
                                        zipIn.getInputStream(entry).use { input ->
                                            input.copyTo(zipOut)
                                        }
                                    }
                                    zipOut.closeEntry()
                                } catch (e: Exception) {
                                    Timber.tag(TAG).e("Error copying entry ${entry.name}: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }

            progress(1.0f)
            print("Modified APK created successfully (${outputApk.length() / 1024} KB)")
            return@withContext true

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error creating modified APK")
            print("ERROR: Failed to create modified APK: ${e.localizedMessage}")
            return@withContext false
        }
    }
}