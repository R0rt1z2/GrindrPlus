package com.grindrplus.manager.utils

import android.annotation.SuppressLint
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale
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

/**
 * Creates a new keystore file with a self-signed certificate for APK signing
 *
 * @param out The output keystore file
 * @throws Exception If keystore creation fails
 */
@SuppressLint("NewApi")
fun newKeystore(out: File) {
    try {
        val key = createKey()

        KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, "password".toCharArray())
            setKeyEntry(
                "alias",
                key.privateKey,
                "password".toCharArray(),
                arrayOf<Certificate>(key.publicKey)
            )
            store(out.outputStream(), "password".toCharArray())
        }
    } catch (e: Exception) {
        if (out.exists()) out.delete()
        throw IOException("Failed to create keystore: ${e.localizedMessage}", e)
    }
}

/**
 * Creates a key pair for signing APKs
 */
private fun createKey(): KeySet {
    try {
        var serialNumber: BigInteger

        do serialNumber = SecureRandom().nextInt().toBigInteger()
        while (serialNumber < BigInteger.ZERO)

        val x500Name = X500Name("CN=GrindrPlus")
        val pair = KeyPairGenerator.getInstance("RSA").run {
            initialize(2048)
            generateKeyPair()
        }

        // Valid for 30 years
        val notBefore = Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L)
        val notAfter = Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 366L * 30L)

        val builder = X509v3CertificateBuilder(
            x500Name,
            serialNumber,
            notBefore,
            notAfter,
            Locale.ENGLISH,
            x500Name,
            SubjectPublicKeyInfo.getInstance(pair.public.encoded)
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)

        return KeySet(
            JcaX509CertificateConverter().getCertificate(builder.build(signer)),
            pair.private
        )
    } catch (e: Exception) {
        throw IOException("Failed to create signing key: ${e.localizedMessage}", e)
    }
}

/**
 * Data class to hold a key pair for APK signing
 */
class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)