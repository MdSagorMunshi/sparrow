package com.ryanshelby.spw.wallet.payment

import com.ryanshelby.spw.wallet.data.remote.SPWApiClient
import com.ryanshelby.spw.wallet.data.remote.SpwExplorerResponse
import com.ryanshelby.spw.wallet.data.remote.SpwUtxo
import com.ryanshelby.spw.wallet.nfc.NfcPaymentInvoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeSPWApiClient(
    private val mockedResponse: Result<SpwExplorerResponse>
) : SPWApiClient("http://fake") {
    override suspend fun getExplorer(address: String): Result<SpwExplorerResponse> {
        return mockedResponse
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentRequestTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        PaymentRequestManager.instance.reset()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testJsonEncodingDecoding() {
        val invoice = NfcPaymentInvoice(
            address = "addr123",
            name = "TestUser",
            amount = 1.5,
            token = "SPW",
            timestampMs = 123456789L,
            nonce = "random-nonce"
        )
        
        val json = invoice.toJson()
        val parsed = NfcPaymentInvoice.fromJson(json)
        
        assertEquals(invoice.address, parsed.address)
        assertEquals(invoice.name, parsed.name)
        assertEquals(invoice.amount, parsed.amount)
        assertEquals(invoice.token, parsed.token)
        assertEquals(invoice.timestampMs, parsed.timestampMs)
        assertEquals(invoice.nonce, parsed.nonce)
    }

    @Test
    fun testExpiryTimer() = runTest(testDispatcher) {
        val manager = PaymentRequestManager.instance
        val invoice = NfcPaymentInvoice("addr", "name", 1.0, "SPW", 0L, "nonce")
        
        manager.createRequest(invoice)
        assertEquals(PaymentRequestState.WAITING, manager.requestState.value)
        
        // Fast forward 2 minutes + 1 second
        testScheduler.advanceTimeBy(121_000L)
        
        assertEquals(PaymentRequestState.EXPIRED, manager.requestState.value)
        assertEquals(null, manager.activeInvoice)
    }

}
