package com.grindrplus.manager.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.android.apksig.ApkSigner
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
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

class KeyStoreUtils(context: Context) {
    val keyStore by lazy {
        File(context.cacheDir, "keystore.jks").also {
            if (!it.exists()) {
                try {
                    newKeystore(it)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to create keystore: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    throw e
                }
            }
        }
    }

    private val signerConfig: ApkSigner.SignerConfig by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        this.keyStore.inputStream().use { stream ->
            keyStore.load(stream, null)
        }

        val alias = keyStore.aliases().nextElement()
        val certificate = keyStore.getCertificate(alias) as X509Certificate

        ApkSigner.SignerConfig.Builder(
            "GrindrPlus",
            keyStore.getKey(alias, "password".toCharArray()) as PrivateKey,
            listOf(certificate)
        ).build()
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

    fun signApk(apkFile: File, output: File) {
        ApkSigner.Builder(listOf(signerConfig))
            .setV1SigningEnabled(false) // TODO: enable so api <24 devices can work, however zip-alignment breaks
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setInputApk(apkFile)
            .setOutputApk(output)
            .build()
            .sign()
    }
}

/**
 * Data class to hold a key pair for APK signing
 */
class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)