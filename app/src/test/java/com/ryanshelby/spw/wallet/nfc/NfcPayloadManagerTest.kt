package com.ryanshelby.spw.wallet.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.PrivateKey
import java.util.UUID

class NfcPayloadManagerTest {

    @Test
    fun testEcdhKeyExchange_success() {
        // Alice generates keypair
        val aliceKeyPair = NfcPayloadManager.generateEphemeralKeyPair()
        
        // Bob generates keypair
        val bobKeyPair = NfcPayloadManager.generateEphemeralKeyPair()

        // Alice computes shared secret using Bob's public key
        val aliceSharedSecret = NfcPayloadManager.deriveSharedSecret(
            aliceKeyPair.private as PrivateKey, 
            bobKeyPair.public.encoded
        )

        // Bob computes shared secret using Alice's public key
        val bobSharedSecret = NfcPayloadManager.deriveSharedSecret(
            bobKeyPair.private as PrivateKey, 
            aliceKeyPair.public.encoded
        )

        // The shared secrets must match exactly
        assertArrayEquals(aliceSharedSecret, bobSharedSecret)

        // Derive session keys
        val aliceSessionKey = NfcPayloadManager.deriveSessionKey(aliceSharedSecret)
        val bobSessionKey = NfcPayloadManager.deriveSessionKey(bobSharedSecret)

        // Session keys must match exactly
        assertArrayEquals(aliceSessionKey, bobSessionKey)
    }

    @Test
    fun testEncryptDecrypt_success() {
        val keyPair = NfcPayloadManager.generateEphemeralKeyPair()
        val sharedSecret = NfcPayloadManager.deriveSharedSecret(keyPair.private as PrivateKey, keyPair.public.encoded)
        val sessionKey = NfcPayloadManager.deriveSessionKey(sharedSecret)

        val invoice = NfcPaymentInvoice(
            address = "spw1testaddress123",
            name = "BraveTiger",
            amount = 100.5,
            token = "SPW",
            timestampMs = System.currentTimeMillis(),
            nonce = UUID.randomUUID().toString()
        )

        val encryptedPayload = NfcPayloadManager.encryptInvoice(invoice, sessionKey)
        val decryptedInvoice = NfcPayloadManager.decryptInvoice(encryptedPayload, sessionKey)

        assertEquals(invoice.address, decryptedInvoice.address)
        assertEquals(invoice.name, decryptedInvoice.name)
        assertEquals(invoice.amount, decryptedInvoice.amount)
        assertEquals(invoice.token, decryptedInvoice.token)
    }

    @Test
    fun testReplayProtection_sameNonce_fails() {
        val keyPair = NfcPayloadManager.generateEphemeralKeyPair()
        val sharedSecret = NfcPayloadManager.deriveSharedSecret(keyPair.private as PrivateKey, keyPair.public.encoded)
        val sessionKey = NfcPayloadManager.deriveSessionKey(sharedSecret)

        val invoice = NfcPaymentInvoice(
            address = "spw1testaddress123",
            name = "BraveTiger",
            timestampMs = System.currentTimeMillis(),
            nonce = UUID.randomUUID().toString()
        )

        val encryptedPayload = NfcPayloadManager.encryptInvoice(invoice, sessionKey)
        
        // First decryption should succeed
        NfcPayloadManager.decryptInvoice(encryptedPayload, sessionKey)

        // Second decryption of the same payload should throw SecurityException
        assertThrows(SecurityException::class.java) {
            NfcPayloadManager.decryptInvoice(encryptedPayload, sessionKey)
        }
    }

    @Test
    fun testReplayProtection_oldTimestamp_fails() {
        val keyPair = NfcPayloadManager.generateEphemeralKeyPair()
        val sharedSecret = NfcPayloadManager.deriveSharedSecret(keyPair.private as PrivateKey, keyPair.public.encoded)
        val sessionKey = NfcPayloadManager.deriveSessionKey(sharedSecret)

        val invoice = NfcPaymentInvoice(
            address = "spw1testaddress123",
            name = "BraveTiger",
            timestampMs = System.currentTimeMillis() - 35_000, // 35 seconds ago
            nonce = UUID.randomUUID().toString()
        )

        val encryptedPayload = NfcPayloadManager.encryptInvoice(invoice, sessionKey)

        // Decryption should throw SecurityException because it's older than 30s
        assertThrows(SecurityException::class.java) {
            NfcPayloadManager.decryptInvoice(encryptedPayload, sessionKey)
        }
    }
}
