package com.ryanshelby.spw.wallet

import com.ryanshelby.spw.wallet.security.SPWCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SPWCryptoTest {

    @Test
    fun testFeathersConversion() {
        val spw = 1.5
        val feathers = (spw * SPWCrypto.FEATHERS_PER_SPW).toLong()
        assertEquals(150_000_000L, feathers)

        val backToSpw = feathers.toDouble() / SPWCrypto.FEATHERS_PER_SPW
        assertEquals(1.5, backToSpw, 0.00000001)
    }

    @Test
    fun testMnemonicGeneration() {
        val mnemonic12 = SPWCrypto.generateMnemonic(128)
        val words12 = mnemonic12.split(" ")
        assertEquals(12, words12.size)
        assertTrue(SPWCrypto.validateMnemonic(mnemonic12))

        val mnemonic24 = SPWCrypto.generateMnemonic(256)
        val words24 = mnemonic24.split(" ")
        assertEquals(24, words24.size)
        assertTrue(SPWCrypto.validateMnemonic(mnemonic24))
    }

    @Test
    fun testKeyPairDerivation() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val keyPair = SPWCrypto.createAccountFromMnemonic(mnemonic)
        assertNotNull(keyPair)
        assertTrue(keyPair.address.isNotEmpty())
        assertTrue(keyPair.spendKeyHex.isNotEmpty())
        assertTrue(keyPair.viewKeyHex.isNotEmpty())
        assertTrue(keyPair.spendPubHex.isNotEmpty())
        assertTrue(keyPair.viewPubHex.isNotEmpty())
    }
}
