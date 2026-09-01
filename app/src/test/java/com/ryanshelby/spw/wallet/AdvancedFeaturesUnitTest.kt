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
}
