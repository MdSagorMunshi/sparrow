package com.ryanshelby.spw.wallet.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.PrivateKey
import android.content.ComponentName
import android.nfc.cardemulation.CardEmulation

class NfcManager(private val activity: Activity) : NfcAdapter.ReaderCallback {

    companion object {
        private const val TAG = "NfcManager"
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
    }

    private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    
    var onInvoiceReceived: ((NfcPaymentInvoice) -> Unit)? = null
    var onTagWriteSuccess: (() -> Unit)? = null
    var onTagWriteError: ((String) -> Unit)? = null
    
    var writeModeAddress: String? = null

    fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }

    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    fun enableReaderMode() {
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        nfcAdapter?.enableReaderMode(activity, this, flags, null)
    }

    fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    fun setPreferredWalletService() {
        if (nfcAdapter == null) return
        try {
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            val component = ComponentName(activity, WalletApduService::class.java)
            cardEmulation.setPreferredService(activity, component)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set preferred NFC service", e)
        }
    }

    fun unsetPreferredWalletService() {
        if (nfcAdapter == null) return
        try {
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            cardEmulation.unsetPreferredService(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unset preferred NFC service", e)
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return

        if (writeModeAddress != null) {
            handleTagWriting(tag)
            return
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            handleIsoDep(isoDep)
            return
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            handleNdef(ndef)
            return
        }
    }

    private fun handleIsoDep(isoDep: IsoDep) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                isoDep.connect()
                isoDep.timeout = 5000

                // 1. Select AID
                val selectResponse = isoDep.transceive(SELECT_APDU)
                if (!isSuccess(selectResponse)) throw Exception("App not selected")

                // 2. Generate Ephemeral KeyPair
                val keyPair = NfcPayloadManager.generateEphemeralKeyPair()
                val myPublicKey = keyPair.public.encoded
                
                // 3. Send Round 1: Public Key
                val round1Apdu = byteArrayOf(
                    0x80.toByte(), INS_EXCHANGE_KEY, 0x00.toByte(), 0x00.toByte(),
                    myPublicKey.size.toByte()
                ) + myPublicKey
                
                val round1Response = isoDep.transceive(round1Apdu)
                if (!isSuccess(round1Response)) throw Exception("Key exchange failed")
                
                val servicePublicKey = round1Response.copyOfRange(0, round1Response.size - 2)
                
                // 4. Derive Session Key
                val sharedSecret = NfcPayloadManager.deriveSharedSecret(
                    keyPair.private as PrivateKey, 
                    servicePublicKey
                )
                val sessionKey = NfcPayloadManager.deriveSessionKey(sharedSecret)

                // 5. Send Round 2: Request Payload
                val round2Apdu = byteArrayOf(
                    0x80.toByte(), INS_REQUEST_PAYLOAD, 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
                )
                
                val round2Response = isoDep.transceive(round2Apdu)
                if (!isSuccess(round2Response)) throw Exception("Payload request failed")
                
                val encryptedPayload = round2Response.copyOfRange(0, round2Response.size - 2)
                
                // 6. Decrypt Invoice
                val invoice = NfcPayloadManager.decryptInvoice(encryptedPayload, sessionKey)
                
                launch(Dispatchers.Main) {
                    onInvoiceReceived?.invoke(invoice)
                }

            } catch (e: Exception) {
                Log.e(TAG, "IsoDep communication failed", e)
            } finally {
                try { isoDep.close() } catch (e: Exception) {}
            }
        }
    }

    private fun handleNdef(ndef: Ndef) {
        try {
            ndef.connect()
            val message = ndef.ndefMessage
            if (message != null && message.records.isNotEmpty()) {
                val record = message.records[0]
                val payload = String(record.payload)
                if (payload.startsWith("spw://pay?address=")) {
                    val address = payload.substringAfter("address=")
                    val invoice = NfcPaymentInvoice(
                        address = address,
                        name = "NFC Tag",
                        timestampMs = System.currentTimeMillis(),
                        nonce = "tag-static-nonce" // Static tag bypasses strict replay since it's unencrypted
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        onInvoiceReceived?.invoke(invoice)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "NDEF read failed", e)
        } finally {
            try { ndef.close() } catch (e: Exception) {}
        }
    }

    private fun handleTagWriting(tag: Tag) {
        val address = writeModeAddress ?: return
        val uri = "spw://pay?address=$address"
        val record = NdefRecord.createUri(uri)
        val message = NdefMessage(arrayOf(record))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) throw Exception("Tag is read-only")
                ndef.writeNdefMessage(message)
                CoroutineScope(Dispatchers.Main).launch { onTagWriteSuccess?.invoke() }
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    CoroutineScope(Dispatchers.Main).launch { onTagWriteSuccess?.invoke() }
                } else {
                    throw Exception("Tag is not NDEF formatable")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write tag", e)
            CoroutineScope(Dispatchers.Main).launch { onTagWriteError?.invoke(e.message ?: "Write failed") }
        } finally {
            writeModeAddress = null
        }
    }

    private fun isSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        return response[response.size - 2] == 0x90.toByte() && response[response.size - 1] == 0x00.toByte()
    }
}
