package com.ryanshelby.spw.wallet.mining

import com.ryanshelby.spw.wallet.security.SPWCrypto.toHex

/**
 * JNI wrapper for the native RandomX Proof-of-Work engine.
 */
object RandomXNative {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("randomx_jni")
            isLoaded = true
        } catch (e: Throwable) {
            isLoaded = false
        }
    }

    fun isAvailable(): Boolean = isLoaded

    @JvmStatic
    external fun initKey(keyBytes: ByteArray)

    @JvmStatic
    external fun calculateHash(inputBytes: ByteArray): ByteArray?

    @JvmStatic
    external fun close()

    fun calculateHashHex(input: ByteArray): String? {
        if (!isLoaded) return null
        val hash = calculateHash(input) ?: return null
        return hash.toHex()
    }
}
