package com.grindrplus.manager.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeTextFile(name: String, content: String): File {
        val file = tempFolder.newFile(name)
        file.writeText(content)
        return file
    }

    private fun writeValidZip(name: String, entries: Map<String, String>): File {
        val file = tempFolder.newFile(name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, content) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun writeCorruptZip(name: String): File {
        // Looks like a zip by extension but is just garbage bytes — passes the
        // first existence/length check, fails the ZipFile open.
        val file = tempFolder.newFile(name)
        file.writeText("not a zip")
        return file
    }

    // -------- validateFile --------

    @Test
    fun `validateFile returns false for a missing file`() {
        val ghost = File(tempFolder.root, "does-not-exist.zip")
        assertFalse(validateFile(ghost))
    }

    @Test
    fun `validateFile returns false for an empty file`() {
        val empty = tempFolder.newFile("empty.zip")
        assertFalse(validateFile(empty))
    }

    @Test
    fun `validateFile returns true for a valid zip`() {
        val zip = writeValidZip("ok.zip", mapOf("hello.txt" to "hi"))
        assertTrue(validateFile(zip))
    }

    @Test
    fun `validateFile returns true for a valid xapk`() {
        val xapk = writeValidZip("bundle.xapk", mapOf("manifest.json" to "{}"))
        assertTrue(validateFile(xapk))
    }

    @Test
    fun `validateFile deletes a corrupt zip and returns false`() {
        val corrupt = writeCorruptZip("bad.zip")
        assertTrue("precondition: file exists before validate", corrupt.exists())
        assertFalse(validateFile(corrupt))
        assertFalse("corrupt file should have been deleted", corrupt.exists())
    }

    @Test
    fun `validateFile skips zip parse for non-zip extensions`() {
        // Anything not ending in .zip / .xapk passes after the existence check.
        val txt = writeTextFile("readme.txt", "hello")
        assertTrue(validateFile(txt))
    }

    // -------- File.unzip --------

    @Test
    fun `unzip extracts entries into the target directory`() {
        val zip = writeValidZip("good.zip", mapOf(
            "a.txt" to "alpha",
            "nested/b.txt" to "beta",
        ))
        val target = tempFolder.newFolder("out")

        zip.unzip(target)

        assertEquals("alpha", File(target, "a.txt").readText())
        assertEquals("beta", File(target, "nested/b.txt").readText())
    }

    @Test
    fun `unzip throws IOException for a missing zip`() {
        val ghost = File(tempFolder.root, "missing.zip")
        assertThrows(IOException::class.java) {
            ghost.unzip(tempFolder.newFolder("out1"))
        }
    }

    @Test
    fun `unzip throws IOException for an empty file`() {
        val empty = tempFolder.newFile("empty.zip")
        assertThrows(IOException::class.java) {
            empty.unzip(tempFolder.newFolder("out2"))
        }
    }

    @Test
    fun `unzip rejects zip-slip entries with SecurityException`() {
        // Hand-craft a zip with an entry whose path escapes the target dir.
        val malicious = tempFolder.newFile("evil.zip")
        ZipOutputStream(malicious.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../escaped.txt"))
            zip.write("pwned".toByteArray())
            zip.closeEntry()
        }
        val target = tempFolder.newFolder("safe")

        assertThrows(SecurityException::class.java) {
            malicious.unzip(target)
        }

        // The escape file must not have landed outside the target.
        val escapedAttempt = File(target.parentFile, "escaped.txt")
        assertFalse(
            "zip-slip escape file was created at ${escapedAttempt.absolutePath}",
            escapedAttempt.exists()
        )
    }

    @Test
    fun `unzip wraps non-security failures as IOException`() {
        // A zip whose central directory is corrupt should surface as IOException,
        // not the underlying ZipException, so callers have a stable contract.
        val corrupt = writeCorruptZip("broken.zip")
        assertThrows(IOException::class.java) {
            corrupt.unzip(tempFolder.newFolder("out3"))
        }
    }
}
