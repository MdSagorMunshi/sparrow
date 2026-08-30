package com.ryanshelby.spw.wallet.payment

import com.ryanshelby.spw.wallet.nfc.NfcPaymentInvoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentRequestState {
    IDLE,
    WAITING,
    CONNECTED,
    RECEIVED,
    CANCELLED,
    EXPIRED
}

class PaymentRequestManager private constructor() {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _requestState = MutableStateFlow(PaymentRequestState.IDLE)
    val requestState: StateFlow<PaymentRequestState> = _requestState.asStateFlow()
    
    private var _activeInvoice: NfcPaymentInvoice? = null
    val activeInvoice: NfcPaymentInvoice? get() = _activeInvoice
    
    private var expiryJob: Job? = null
    
    fun createRequest(invoice: NfcPaymentInvoice) {
        _activeInvoice = invoice
        _requestState.value = PaymentRequestState.WAITING
        
        // 2-minute expiration
        expiryJob?.cancel()
        expiryJob = scope.launch {
            delay(2 * 60 * 1000L) // 2 minutes
            if (_requestState.value == PaymentRequestState.WAITING || _requestState.value == PaymentRequestState.CONNECTED) {
                _requestState.value = PaymentRequestState.EXPIRED
                _activeInvoice = null
            }
        }
    }
    
    fun setConnected() {
        if (_requestState.value == PaymentRequestState.WAITING) {
            _requestState.value = PaymentRequestState.CONNECTED
        }
    }
    
    fun setReceived() {
        if (_requestState.value == PaymentRequestState.WAITING || _requestState.value == PaymentRequestState.CONNECTED) {
            _requestState.value = PaymentRequestState.RECEIVED
            expiryJob?.cancel()
            _activeInvoice = null
        }
    }
    
    fun cancelRequest() {
        if (_requestState.value == PaymentRequestState.WAITING || _requestState.value == PaymentRequestState.CONNECTED) {
            _requestState.value = PaymentRequestState.CANCELLED
            expiryJob?.cancel()
            _activeInvoice = null
        }
    }
    
    fun reset() {
        _requestState.value = PaymentRequestState.IDLE
        expiryJob?.cancel()
        _activeInvoice = null
    }
    
    companion object {
        val instance by lazy { PaymentRequestManager() }
    }
}
