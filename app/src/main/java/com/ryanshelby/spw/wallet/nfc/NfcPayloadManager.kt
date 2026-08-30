package com.ryanshelby.spw.wallet.nfc

import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.ConcurrentHashMap

object NfcPayloadManager {

    private const val CURVE_NAME = "secp256r1"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val REPLAY_WINDOW_MS = 30_000L

    // In-memory cache to track seen nonces and prevent replay attacks
    private val seenNonces = ConcurrentHashMap<String, Long>()

    // Generate ephemeral EC KeyPair for ECDH
    fun generateEphemeralKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec(CURVE_NAME)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    // Derive a shared secret using ECDH
    fun deriveSharedSecret(privateKey: PrivateKey, otherPublicKeyBytes: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance("EC")
        val publicKeySpec = X509EncodedKeySpec(otherPublicKeyBytes)
        val otherPublicKey = keyFactory.generatePublic(publicKeySpec)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(otherPublicKey, true)
        
        return keyAgreement.generateSecret()
    }

    // Derive a session key from the shared secret using HKDF (simplified via HMAC-SHA256)
    fun deriveSessionKey(sharedSecret: ByteArray, salt: ByteArray? = null, info: ByteArray? = null): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val prkKey = SecretKeySpec(salt ?: ByteArray(32), "HmacSHA256")
        mac.init(prkKey)
        val prk = mac.doFinal(sharedSecret)

        val sessionMac = Mac.getInstance("HmacSHA256")
        sessionMac.init(SecretKeySpec(prk, "HmacSHA256"))
        
        val infoBytes = info ?: "NfcSessionKey".toByteArray(Charsets.UTF_8)
        val input = ByteBuffer.allocate(infoBytes.size + 1)
        input.put(infoBytes)
        input.put(1.toByte()) // counter

        return sessionMac.doFinal(input.array())
    }

    // Encrypt the NfcPaymentInvoice using AES-GCM
    fun encryptInvoice(invoice: NfcPaymentInvoice, sessionKeyBytes: ByteArray): ByteArray {
        val json = invoice.toJson()
        val plainText = json.toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val secretKey = SecretKeySpec(sessionKeyBytes, "AES")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(plainText)

        // Prepend IV to ciphertext
        val result = ByteBuffer.allocate(iv.size + cipherText.size)
        result.put(iv)
        result.put(cipherText)
        return result.array()
    }

    // Decrypt the payload and validate against replay attacks
    fun decryptInvoice(encryptedPayload: ByteArray, sessionKeyBytes: ByteArray): NfcPaymentInvoice {
        if (encryptedPayload.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Payload too short")
        }

        val iv = encryptedPayload.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encryptedPayload.copyOfRange(GCM_IV_LENGTH, encryptedPayload.size)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val secretKey = SecretKeySpec(sessionKeyBytes, "AES")

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val plainText = cipher.doFinal(cipherText)
        val json = String(plainText, Charsets.UTF_8)

        val invoice = NfcPaymentInvoice.fromJson(json)
        validateInvoice(invoice)
        
        return invoice
    }

    private fun validateInvoice(invoice: NfcPaymentInvoice) {
        val now = System.currentTimeMillis()
        
        // 1. Check if timestamp is within the valid window (30 seconds)
        if (now - invoice.timestampMs > REPLAY_WINDOW_MS || invoice.timestampMs > now + 5000) {
            throw SecurityException("Invoice timestamp is outside the valid 30s window (Replay Attack detected)")
        }

        // 2. Check if nonce has been seen recently
        cleanUpSeenNonces(now)
        if (seenNonces.putIfAbsent(invoice.nonce, invoice.timestampMs) != null) {
            throw SecurityException("Invoice nonce has already been seen (Replay Attack detected)")
        }
    }

    private fun cleanUpSeenNonces(now: Long) {
        // Remove nonces older than the replay window to free memory
        val iterator = seenNonces.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > REPLAY_WINDOW_MS) {
                iterator.remove()
            }
        }
    }
}
