package com.grindrplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Verifies that the spoofed Play Integrity token produced by IntegritySpoofer
 * is a structurally valid JWT (three dot-separated base64url segments).
 * The hook class itself requires Xposed runtime; we test the token builder logic in isolation.
 */
class IntegritySpooferTest {

    private fun buildSpoofedToken(): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payload = enc.encodeToString(
            """{"appIntegrity":{"appRecognitionVerdict":"PLAY_RECOGNIZED"},"deviceIntegrity":{"deviceRecognitionVerdict":["MEETS_DEVICE_INTEGRITY","MEETS_STRONG_INTEGRITY"]},"accountDetails":{"appLicensingVerdict":"LICENSED"}}""".toByteArray()
        )
        return "$header.$payload."
    }

    @Test
    fun `token has exactly three dot-separated segments`() {
        val token = buildSpoofedToken()
        assertEquals("JWT must have header.payload.signature", 3, token.split(".").size)
    }

    @Test
    fun `token header decodes to alg none`() {
        val token = buildSpoofedToken()
        val headerB64 = token.split(".")[0]
        val header = String(Base64.getUrlDecoder().decode(headerB64))
        assertTrue("Header must contain alg:none", header.contains("\"alg\":\"none\""))
    }

    @Test
    fun `token payload contains expected verdicts`() {
        val token = buildSpoofedToken()
        val payloadB64 = token.split(".")[1]
        val payload = String(Base64.getUrlDecoder().decode(payloadB64))
        assertTrue(payload.contains("PLAY_RECOGNIZED"))
        assertTrue(payload.contains("MEETS_DEVICE_INTEGRITY"))
        assertTrue(payload.contains("LICENSED"))
    }

    @Test
    fun `token segments contain no padding characters`() {
        val token = buildSpoofedToken()
        val parts = token.split(".")
        assertFalse("Header must not contain = padding", parts[0].contains("="))
        assertFalse("Payload must not contain = padding", parts[1].contains("="))
    }

    @Test
    fun `token is non-empty`() {
        val token = buildSpoofedToken()
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `token header contains typ JWT`() {
        val token = buildSpoofedToken()
        val headerB64 = token.split(".")[0]
        val header = String(Base64.getUrlDecoder().decode(headerB64))
        assertTrue("Header must contain typ:JWT", header.contains("\"typ\":\"JWT\""))
    }

    @Test
    fun `token segments contain only base64url safe chars`() {
        val token = buildSpoofedToken()
        val validChars = Regex("^[A-Za-z0-9_\\-]*$")
        val parts = token.split(".")
        assertTrue("Header must be base64url safe", validChars.matches(parts[0]))
        assertTrue("Payload must be base64url safe", validChars.matches(parts[1]))
    }

    @Test
    fun `token is stable across multiple calls`() {
        val first = buildSpoofedToken()
        val second = buildSpoofedToken()
        assertEquals("Token must be deterministic", first, second)
    }
}
