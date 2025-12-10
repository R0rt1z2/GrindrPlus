package com.grindrplus.manager.utils

import com.github.diamondminer88.zip.ZipReader
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Validates that a downloaded file is complete and not corrupted
 */
fun validateFile(file: File): Boolean {
    if (!file.exists() || file.length() <= 0) {
        return false
    }

    if (file.name.endsWith(".zip") || file.name.endsWith(".xapk")) {
        try {
            ZipFile(file).close()
            return true
        } catch (e: Exception) {
            Timber.tag("Download").e("Invalid ZIP file: ${e.localizedMessage}")
            file.delete()
            return false
        }
    }

    return true
}

/**
 * Unzips a file to the specified directory with proper error handling
 *
 * @param unzipLocationRoot The target directory (or null to use same directory)
 * @throws IOException If extraction fails
 */
fun File.unzip(unzipLocationRoot: File? = null) {
    if (!exists() || length() <= 0) {
        throw IOException("ZIP file doesn't exist or is empty: $absolutePath")
    }

    val rootFolder =
        unzipLocationRoot ?: File(parentFile!!.absolutePath + File.separator + nameWithoutExtension)

    if (!rootFolder.exists()) {
        if (!rootFolder.mkdirs()) {
            throw IOException("Failed to create output directory: ${rootFolder.absolutePath}")
        }
    }

    try {
        ZipFile(this).use { zip ->
            val entries = zip.entries().asSequence().toList()

            if (entries.isEmpty()) {
                throw IOException("ZIP file is empty: $absolutePath")
            }

            for (entry in entries) {
                val outputFile = File(rootFolder.absolutePath + File.separator + entry.name)

                // cute zip slip vulnerability
                if (!outputFile.canonicalPath.startsWith(rootFolder.canonicalPath + File.separator)) {
                    throw SecurityException("ZIP entry is outside of target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        throw IOException("Failed to create directory: ${outputFile.absolutePath}")
                    }
                } else {
                    outputFile.parentFile?.let {
                        if (!it.exists() && !it.mkdirs()) {
                            throw IOException("Failed to create parent directory: ${it.absolutePath}")
                        }
                    }

                    zip.getInputStream(entry).use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        if (unzipLocationRoot != null && unzipLocationRoot.exists()) {
            unzipLocationRoot.deleteRecursively()
        }

        when (e) {
            is SecurityException -> throw e
            else -> throw IOException("Failed to extract ZIP file: ${e.localizedMessage}", e)
        }
    }
}

