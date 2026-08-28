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
}
