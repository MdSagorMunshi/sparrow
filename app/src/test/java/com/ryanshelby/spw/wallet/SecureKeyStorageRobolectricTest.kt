package com.ryanshelby.spw.wallet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.security.SecureKeyStorage
import com.ryanshelby.spw.wallet.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureKeyStorageRobolectricTest {

    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityManager = SecurityManager(context)
        securityManager.deleteWallet()
    }

    @Test
    fun testSecureKeyStorageRoundtrip() {
        val testSeed = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        SecureKeyStorage.storeMnemonicSeed(context, testSeed)

        assertTrue(SecureKeyStorage.hasEncryptedSeed(context))

        val decrypted = SecureKeyStorage.getMnemonicSeed(context)
        assertEquals(testSeed, decrypted)

        val report = SecureKeyStorage.getDiagnosticInfo(context)
        assertEquals("AndroidKeyStore", report.provider)
        assertTrue(report.hasStoredEncryptedSeed)
    }

    @Test
    fun testAuthenticationGuardedSeedRetrieval() {
        val testSeed = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val keys = securityManager.importMnemonic(testSeed, "Test Account")
        securityManager.setPin("123456")

        // Valid PIN retrieval
        val authResult = securityManager.getSeedPhraseAuthenticated("123456")
        assertTrue(authResult.isSuccess)
        assertEquals(testSeed, authResult.getOrNull())

        // Invalid PIN rejection
        val badAuthResult = securityManager.getSeedPhraseAuthenticated("999999")
        assertTrue(badAuthResult.isFailure)

        // Biometric bypass simulation
        val bioResult = securityManager.getSeedPhraseAuthenticated(pin = null, isBiometricAuthenticated = true)
        assertTrue(bioResult.isSuccess)
        assertEquals(testSeed, bioResult.getOrNull())
    }

    @Test
    fun testWipeKeysClearsStorage() {
        val testSeed = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        securityManager.importMnemonic(testSeed)
        assertTrue(SecureKeyStorage.hasEncryptedSeed(context))

        SecureKeyStorage.clearAllSecureKeys(context)
        assertFalse(SecureKeyStorage.hasEncryptedSeed(context))
    }

    @Test
    fun testSingleSaltImmutabilityAcrossLogins() {
        val prefs = context.getSharedPreferences("spw_wallet_security_prefs", Context.MODE_PRIVATE)
        securityManager.setPin("654321")
        val initialSalt = prefs.getString("key_pin_salt", null)
        val initialHash = prefs.getString("key_pin_hash", null)

        assertNotNull(initialSalt)
        assertNotNull(initialHash)

        // Verify PIN multiple times
        assertTrue(securityManager.verifyPin("654321"))
        assertTrue(securityManager.verifyPin("654321"))

        // Salt and hash must remain static across logins (no rotating salt)
        val secondSalt = prefs.getString("key_pin_salt", null)
        val secondHash = prefs.getString("key_pin_hash", null)
        assertEquals(initialSalt, secondSalt)
        assertEquals(initialHash, secondHash)
    }

    @Test
    fun testStorageTamperDetectionEnforcesLockout() {
        val prefs = context.getSharedPreferences("spw_wallet_security_prefs", Context.MODE_PRIVATE)
        securityManager.setPin("111111")

        // Fail once
        assertFalse(securityManager.verifyPin("000000"))
        val state1 = securityManager.getRateLimitState()
        assertEquals(1, state1.attempts)
        assertFalse(state1.isTampered)

        // Root attacker tampers with shared_prefs XML to reset attempts to 0 without valid MAC
        prefs.edit().putInt("key_pin_failed_attempts", 0).commit()

        // SecurityManager reads state -> detects tamper -> enforces 15-minute lockout
        val tamperedState = securityManager.getRateLimitState()
        assertTrue(tamperedState.isTampered)
        assertTrue(securityManager.isLockedOut())
        assertTrue(securityManager.getRemainingLockoutSeconds() > 800L) // ~15 minutes

        // Even with correct PIN, verification is rejected while locked out
        assertFalse(securityManager.verifyPin("111111"))
    }

    @Test
    fun testInterruptedWriteSafety() {
        val prefs = context.getSharedPreferences("spw_wallet_security_prefs", Context.MODE_PRIVATE)
        securityManager.setPin("123456")

        // Simulate an interrupted write where salt was removed / corrupted
        prefs.edit().remove("key_pin_salt").commit()

        // App should safely reject without unhandled crash
        assertFalse(securityManager.verifyPin("123456"))

        // Re-setting PIN restores valid state
        securityManager.setPin("123456")
        assertTrue(securityManager.verifyPin("123456"))
    }

    @Test
    fun testMemoryZeroization() {
        securityManager.setPin("222222")

        val pinBuffer = charArrayOf('2', '2', '2', '2', '2', '2')
        val success = securityManager.verifyPin(pinBuffer)
        assertTrue(success)

        // Verify that pinBuffer was zeroized in memory
        for (c in pinBuffer) {
            assertEquals('\u0000', c)
        }
    }
}
