package com.ryanshelby.spw.wallet

import com.ryanshelby.spw.wallet.data.model.ProxyConfig
import com.ryanshelby.spw.wallet.data.model.ProxyType
import com.ryanshelby.spw.wallet.data.remote.SpwUtxo
import com.ryanshelby.spw.wallet.security.SPWCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Proxy

class AdvancedFeaturesUnitTest {

    @Test
    fun testProxyConfig_DirectVsTor() {
        val directConfig = ProxyConfig(enabled = false, type = ProxyType.NONE)
        assertEquals(Proxy.NO_PROXY, directConfig.toJavaProxy())

        val torConfig = ProxyConfig(enabled = true, type = ProxyType.TOR_ORBOT, host = "127.0.0.1", port = 9050)
        val javaProxy = torConfig.toJavaProxy()
        assertEquals(Proxy.Type.SOCKS, javaProxy.type())
        assertNotNull(javaProxy.address())
    }

    @Test
    fun testProxyConfig_CustomSocks5AndHttp() {
        val socks5Config = ProxyConfig(enabled = true, type = ProxyType.SOCKS5, host = "192.168.1.100", port = 1080)
        assertEquals(Proxy.Type.SOCKS, socks5Config.toJavaProxy().type())

        val httpConfig = ProxyConfig(enabled = true, type = ProxyType.HTTP, host = "proxy.example.com", port = 8080)
        assertEquals(Proxy.Type.HTTP, httpConfig.toJavaProxy().type())
    }

    @Test
    fun testDisposableBurner_MnemonicAndAddressGeneration() {
        val mnemonic12 = SPWCrypto.generateMnemonic(128)
        assertEquals(12, mnemonic12.trim().split("\\s+".toRegex()).size)

        val keys = SPWCrypto.createAccountFromMnemonic(mnemonic12)
        assertTrue(SPWCrypto.isValidSpwAddress(keys.address))
        assertTrue(keys.address.startsWith("D"))
        assertNotNull(keys.spendKeyHex)
        assertNotNull(keys.viewKeyHex)
    }

    @Test
    fun testCoinControl_UtxoCalculations() {
        val utxos = listOf(
            SpwUtxo(txid = "a1b2c3d4e5f6", vout = 0, amount = 100_000_000L, address = "DTest1", isStealth = false),
            SpwUtxo(txid = "b2c3d4e5f6a1", vout = 1, amount = 50_000_000L, address = "DTest2", isStealth = true),
            SpwUtxo(txid = "c3d4e5f6a1b2", vout = 0, amount = 250_000_000L, address = "DTest3", isStealth = false)
        )

        val totalFeathers = utxos.sumOf { it.amount }
        assertEquals(400_000_000L, totalFeathers)
        assertEquals(4.0, totalFeathers.toDouble() / 100_000_000.0, 0.0001)

        val stealthUtxos = utxos.filter { it.isStealth }
        assertEquals(1, stealthUtxos.size)
        assertEquals(50_000_000L, stealthUtxos.first().amount)
    }

    @Test
    fun testSpwAddressValidation() {
        assertTrue(SPWCrypto.isValidSpwAddress(SPWCrypto.pubkeyToAddress(ByteArray(33) { 0x02.toByte() })))
        assertFalse(SPWCrypto.isValidSpwAddress("InvalidAddress123"))
        assertFalse(SPWCrypto.isValidSpwAddress(""))
    }

    @Test
    fun testBip47Paynym_GenerationAndParsing() {
        val mnemonic = SPWCrypto.generateMnemonic(128)
        val acc = SPWCrypto.createAccountFromMnemonic(mnemonic)

        // 1. Generate BIP-47 Paynym code
        val paynymCode = SPWCrypto.generatePaynymCode(acc.spendPubHex, acc.viewPubHex)
        assertTrue(paynymCode.startsWith("PM8T"))
        assertTrue(SPWCrypto.isPaynymCode(paynymCode))

        // 2. Parse BIP-47 Paynym code
        val parsed = SPWCrypto.parsePaynymCode(paynymCode)
        assertNotNull(parsed)
        assertEquals(acc.spendPubHex.lowercase(), parsed!!.spendPubHex.lowercase())
        assertEquals(acc.viewPubHex.lowercase(), parsed.viewPubHex.lowercase())
        assertTrue(parsed.alias.startsWith("+sparrow/"))

        // 3. Test with URI scheme prefix
        val uriParsed = SPWCrypto.parsePaynymCode("paynym:$paynymCode?label=Alice")
        assertNotNull(uriParsed)
        assertEquals(acc.spendPubHex.lowercase(), uriParsed!!.spendPubHex.lowercase())

        // 4. Test invalid paynym codes
        assertFalse(SPWCrypto.isPaynymCode("PM8TInvalidPayload12345"))
        assertFalse(SPWCrypto.isPaynymCode("DAdg5ZAM8pa8sw1YccFp95mU8szyGJ5C95"))
    }

    @Test
    fun testBip47Paynym_StealthOutputRoundtrip() {
        // Bob's Paynym
        val bobMnemonic = SPWCrypto.generateMnemonic(128)
        val bobKeys = SPWCrypto.createAccountFromMnemonic(bobMnemonic)
        val bobPaynym = SPWCrypto.generatePaynymCode(bobKeys.spendPubHex, bobKeys.viewPubHex)

        // Alice receives Bob's Paynym code and creates a stealth output
        val parsedPaynym = SPWCrypto.parsePaynymCode(bobPaynym)
        assertNotNull(parsedPaynym)

        val stealthOutput = SPWCrypto.makeStealthOutput(parsedPaynym!!.spendPubHex, parsedPaynym.viewPubHex)
        assertTrue(SPWCrypto.isValidSpwAddress(stealthOutput.oneTimeAddress))
        assertNotNull(stealthOutput.txPubkeyHex)

        // Bob scans the blockchain and discovers the stealth payment meant for him
        val isForBob = SPWCrypto.scanStealthOutput(
            outputAddress = stealthOutput.oneTimeAddress,
            txPubkeyHex = stealthOutput.txPubkeyHex,
            viewKeyHex = bobKeys.viewKeyHex,
            spendPubHex = bobKeys.spendPubHex
        )
        assertTrue(isForBob)

        // Bob derives the one-time private key to spend the UTXO
        val oneTimePrivKeyHex = SPWCrypto.deriveStealthPrivKey(
            txPubkeyHex = stealthOutput.txPubkeyHex,
            viewKeyHex = bobKeys.viewKeyHex,
            spendKeyHex = bobKeys.spendKeyHex
        )
        val derivedSpendPub = SPWCrypto.getCompressedPublicKey(SPWCrypto.hexToBytes(oneTimePrivKeyHex))
        val derivedAddress = SPWCrypto.pubkeyToAddress(derivedSpendPub)

        assertEquals(stealthOutput.oneTimeAddress, derivedAddress)
    }
}
