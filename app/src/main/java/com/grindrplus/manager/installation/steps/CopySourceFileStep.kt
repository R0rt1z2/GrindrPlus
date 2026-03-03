package com.grindrplus.manager.installation.steps

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.validateFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Copy user-selected local file to the app's cache folder
 */
class CopySourceFileStep(
    private val tergetFile: File,
    private val uri: Uri,
    private val fileType: String
) : BaseStep() {
    override val name = "Copy $fileType"

    override suspend fun doExecute(context: Context, print: Print) {
        print("Copying $fileType file...")

        val expectedSize = withContext(Dispatchers.IO) {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else -1L
                } ?: -1L
        }

        if (tergetFile.exists() && tergetFile.length() > 0) {
            val sizeMatches = expectedSize == -1L || tergetFile.length() == expectedSize
            if (sizeMatches && validateFile(tergetFile, "Copy")) {
                print("Existing $fileType file found with matching size, skipping copy")
                return
            } else {
                val reason =
                    if (!sizeMatches) "size mismatch (expected $expectedSize, got ${tergetFile.length()})" else "corruption"
                Timber.tag("Copy").w("Existing file ${tergetFile.name} is $reason, copying again")
                tergetFile.delete()
            }
        }

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tergetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open input stream for URI: $uri")

            if (expectedSize != -1L && tergetFile.length() != expectedSize) {
                tergetFile.delete()
                throw IOException("File size mismatch for $fileType: expected $expectedSize, got ${tergetFile.length()}")
            }
        }

        if (!tergetFile.exists() || tergetFile.length() <= 0) {
            throw IOException("Failed to copy $fileType: file is missing or empty")
        }

        print("$fileType copied")
    }
}