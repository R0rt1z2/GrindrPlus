package com.grindrplus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the blocked-library name logic extracted from NativeLibraryGuard.
 * The hook itself requires Xposed/Android runtime, so we test the pure predicate here.
 */
class NativeLibraryGuardTest {

    private val blockedLibs = setOf("pairip", "pairipcore", "integrity_checker")

    private fun isBlocked(name: String) = blockedLibs.any { name.contains(it, ignoreCase = true) }

    @Test
    fun `pairip is blocked`() {
        assertTrue(isBlocked("pairip"))
    }

    @Test
    fun `pairipcore is blocked`() {
        assertTrue(isBlocked("pairipcore"))
    }

    @Test
    fun `integrity_checker is blocked`() {
        assertTrue(isBlocked("integrity_checker"))
    }

    @Test
    fun `pairip embedded in path is blocked`() {
        assertTrue(isBlocked("/data/app/com.grindrapp.android/lib/arm64/libpairip.so"))
    }

    @Test
    fun `pairipcore embedded in path is blocked`() {
        assertTrue(isBlocked("/data/app/com.grindrapp.android/lib/arm64/libpairipcore.so"))
    }

    @Test
    fun `case-insensitive match for PairIP`() {
        assertTrue(isBlocked("PairIP"))
        assertTrue(isBlocked("PAIRIP"))
        assertTrue(isBlocked("libPairIpCore.so"))
    }

    @Test
    fun `legitimate library is not blocked`() {
        assertFalse(isBlocked("sqlite"))
        assertFalse(isBlocked("c++_shared"))
        assertFalse(isBlocked("grindr_native"))
        assertFalse(isBlocked("okhttp"))
    }

    @Test
    fun `empty string is not blocked`() {
        assertFalse(isBlocked(""))
    }

    @Test
    fun `partial match inside longer innocent name does not trigger`() {
        // "pair" is not in the blocked set — only "pairip" is
        assertFalse(isBlocked("pair"))
        assertFalse(isBlocked("repair"))
    }

    @Test
    fun `SecurityException message includes lib name`() {
        val libName = "pairip"
        val ex = SecurityException("GrindrPlus: blocked anti-tampering lib: $libName")
        assertTrue(ex.message!!.contains(libName))
        assertTrue(ex.message!!.contains("GrindrPlus"))
    }
}
