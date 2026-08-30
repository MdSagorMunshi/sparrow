package com.ryanshelby.spw.wallet.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.ryanshelby.spw.wallet.SPWApplication
import java.security.PrivateKey
import java.util.UUID

class WalletApduService : HostApduService() {

    companion object {
        private const val TAG = "WalletApduService"
        private val AID = byteArrayOf(
            0xF0.toByte(), 0x39.toByte(), 0x41.toByte(), 0x48.toByte(), 
            0x14.toByte(), 0x81.toByte(), 0x00.toByte()
        )
        
        private val SELECT_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
            0x07.toByte()
        ) + AID

        private const val INS_EXCHANGE_KEY = 0x01.toByte()
        private const val INS_REQUEST_PAYLOAD = 0x02.toByte()
        
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        private val STATUS_NOT_ALLOWED = byteArrayOf(0x69.toByte(), 0x85.toByte())
    }

    private var sessionKey: ByteArray? = null
    
    // Allows foreground activity to toggle receive readiness when Always Active is off
    var isForegroundReceiveActive = false

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        val securityManager = SPWApplication.instance.securityManager
        val isAlwaysActive = securityManager.isNfcAlwaysActive()
        
        // In a real implementation, we'd also check a static variable or bound service state 
        // for isForegroundReceiveActive. We'll assume if the wallet is locked we don't share.
        if (!securityManager.hasWallet()) return STATUS_NOT_ALLOWED

        if (commandApdu.contentEquals(SELECT_APDU)) {
            Log.d(TAG, "Application selected")
            sessionKey = null // reset session
            return STATUS_SUCCESS
        }

        if (commandApdu.size >= 4 && commandApdu[0] == 0x80.toByte()) {
            val ins = commandApdu[1]
            
            when (ins) {
                INS_EXCHANGE_KEY -> {
                    // commandApdu structure: CLA(80) INS(01) P1(00) P2(00) Lc(XX) [Data]
                    val lc = commandApdu[4].toInt() and 0xFF
                    if (commandApdu.size < 5 + lc) return STATUS_FAILED
                    
                    val readerPublicKey = commandApdu.copyOfRange(5, 5 + lc)
                    
                    val keyPair = NfcPayloadManager.generateEphemeralKeyPair()
                    val sharedSecret = NfcPayloadManager.deriveSharedSecret(
                        keyPair.private as PrivateKey, 
                        readerPublicKey
                    )
                    sessionKey = NfcPayloadManager.deriveSessionKey(sharedSecret)
                    
                    val myPublicKey = keyPair.public.encoded
                    return myPublicKey + STATUS_SUCCESS
                }
                INS_REQUEST_PAYLOAD -> {
                    val currentSessionKey = sessionKey ?: return STATUS_FAILED
                    
                    val requestManager = com.ryanshelby.spw.wallet.payment.PaymentRequestManager.instance
                    val activeInvoice = requestManager.activeInvoice
                    
                    val invoice = activeInvoice ?: NfcPaymentInvoice(
                        address = securityManager.getWalletAddress(),
                        name = securityManager.getUserNickname(),
                        amount = null, // HCE typically acts as receiver just providing their address
                        token = null,
                        timestampMs = System.currentTimeMillis(),
                        nonce = java.util.UUID.randomUUID().toString()
                    )
                    
                    if (activeInvoice != null) {
                        // Let the manager know the payer has connected
                        requestManager.setConnected()
                    }
                    
                    val encryptedPayload = NfcPayloadManager.encryptInvoice(invoice, currentSessionKey)
                    sessionKey = null // single use
                    return encryptedPayload + STATUS_SUCCESS
                }
            }
        }

        return STATUS_FAILED
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
        sessionKey = null
    }
}
