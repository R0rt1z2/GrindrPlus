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

// 2nd for Grindr, 4th for mod
class CopyStep(
    private val file: File,
    private val uri: Uri,
    private val fileType: String
) : BaseStep() {
    override val name = "Copying $fileType"

    override suspend fun doExecute(context: Context, print: Print) {
        print("Copying $fileType file...")

        val expectedSize = withContext(Dispatchers.IO) {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else -1L
                } ?: -1L
        }

        if (file.exists() && file.length() > 0) {
            val sizeMatches = expectedSize == -1L || file.length() == expectedSize
            if (sizeMatches && validateFile(file, "Copy")) {
                print("Existing $fileType file found with matching size, skipping copy")
                return
            } else {
                val reason =
                    if (!sizeMatches) "size mismatch (expected $expectedSize, got ${file.length()})" else "corruption"
                Timber.tag("Copy").w("Existing file ${file.name} is $reason, copying again")
                file.delete()
            }
        }

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open input stream for URI: $uri")

            if (expectedSize != -1L && file.length() != expectedSize) {
                file.delete()
                throw IOException("File size mismatch for $fileType: expected $expectedSize, got ${file.length()}")
            }
        }

        if (!file.exists() || file.length() <= 0) {
            throw IOException("Failed to copy $fileType: file is missing or empty")
        }

        print("$fileType copied")
    }
}