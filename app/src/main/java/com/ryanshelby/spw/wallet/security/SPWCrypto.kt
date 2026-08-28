package com.ryanshelby.spw.wallet.security

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.math.ec.ECPoint
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SPWAccountKeys(
    val address: String,
    val spendKeyHex: String,
    val viewKeyHex: String,
    val spendPubHex: String,
    val viewPubHex: String,
    val mnemonic: String?
)

data class StealthOutput(
    val oneTimeAddress: String,
    val txPubkeyHex: String
)

data class TxInputData(
    val prevTxid: String,
    val prevVout: Int,
    val pubkey: String = "",
    val scriptSig: String = ""
)

data class TxOutputData(
    val address: String,
    val amount: Long,
    val data: String? = null
)

object SPWCrypto {
    const val ADDRESS_VERSION: Byte = 0x1e // Version byte 30 -> 'D' prefix Base58Check
    const val COIN_TYPE: Int = 1926
    const val FEATHERS_PER_SPW: Long = 100_000_000L // 1 SPW = 10^8 feathers
    const val DEFAULT_NODE_URL = "https://wallet.spw.network/api"

    private val ecParams = SECNamedCurves.getByName("secp256k1")
    private val domainParams = ECDomainParameters(ecParams.curve, ecParams.g, ecParams.n, ecParams.h)
    private val CURVE_N: BigInteger = ecParams.n
    private val SECURE_RANDOM = SecureRandom()

    private const val B58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    // ── Base58 & Base58Check ──────────────────────────────────────────────

    fun b58Encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        var num = BigInteger(1, data)
        val sb = StringBuilder()
        val base = BigInteger.valueOf(58)
        while (num > BigInteger.ZERO) {
            val divRem = num.divideAndRemainder(base)
            num = divRem[0]
            val rem = divRem[1].toInt()
            sb.append(B58_CHARS[rem])
        }
        var leadingZeros = 0
        for (b in data) {
            if (b == 0.toByte()) leadingZeros++ else break
        }
        for (i in 0 until leadingZeros) {
            sb.append('1')
        }
        return sb.reverse().toString()
    }

    fun b58Decode(input: String): ByteArray {
        var num = BigInteger.ZERO
        val base = BigInteger.valueOf(58)
        for (c in input) {
            val idx = B58_CHARS.indexOf(c)
            if (idx < 0) throw IllegalArgumentException("Invalid Base58 character: $c")
            num = num.multiply(base).add(BigInteger.valueOf(idx.toLong()))
        }
        val bytes = num.toByteArray()
        val stripLeadingZero = bytes.isNotEmpty() && bytes[0] == 0.toByte()
        val raw = if (stripLeadingZero) bytes.copyOfRange(1, bytes.size) else bytes

        var leadingOnes = 0
        for (c in input) {
            if (c == '1') leadingOnes++ else break
        }
        val result = ByteArray(leadingOnes + raw.size)
        System.arraycopy(raw, 0, result, leadingOnes, raw.size)
        return result
    }

    fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    fun dsha256(data: ByteArray): ByteArray {
        return sha256(sha256(data))
    }

    fun ripemd160(data: ByteArray): ByteArray {
        val digest = RIPEMD160Digest()
        digest.update(data, 0, data.size)
        val out = ByteArray(20)
        digest.doFinal(out, 0)
        return out
    }

    fun pubkeyToAddress(compressedPubkey: ByteArray): String {
        val h = ripemd160(sha256(compressedPubkey))
        val payload = ByteArray(1 + h.size)
        payload[0] = ADDRESS_VERSION
        System.arraycopy(h, 0, payload, 1, h.size)
        val checksum = dsha256(payload).copyOfRange(0, 4)
        val full = ByteArray(payload.size + 4)
        System.arraycopy(payload, 0, full, 0, payload.size)
        System.arraycopy(checksum, 0, full, payload.size, 4)
        return b58Encode(full)
    }

    fun isValidSpwAddress(address: String): Boolean {
        return try {
            val raw = b58Decode(address.trim())
            if (raw.size != 25) return false
            if (raw[0] != ADDRESS_VERSION) return false
            val payload = raw.copyOfRange(0, 21)
            val checksum = raw.copyOfRange(21, 25)
            val expected = dsha256(payload).copyOfRange(0, 4)
            checksum.contentEquals(expected)
        } catch (e: Exception) {
            false
        }
    }

    // ── SECP256k1 EC Point Helpers ────────────────────────────────────────

    fun getCompressedPublicKey(privateKeyBytes: ByteArray): ByteArray {
        val privInt = BigInteger(1, privateKeyBytes)
        val point = ecParams.g.multiply(privInt).normalize()
        return point.getEncoded(true)
    }

    fun decodePoint(compressedPubkey: ByteArray): ECPoint {
        return ecParams.curve.decodePoint(compressedPubkey).normalize()
    }

    // ── BIP39 Mnemonic & Seed Derivation ───────────────────────────────────

    fun generateMnemonic(strengthBits: Int = 128): String {
        require(strengthBits in listOf(128, 160, 192, 224, 256)) { "Strength must be 128, 160, 192, 224, or 256" }
        val entropy = ByteArray(strengthBits / 8)
        SECURE_RANDOM.nextBytes(entropy)

        val checksumBits = strengthBits / 32
        val hash = sha256(entropy)

        val totalBits = strengthBits + checksumBits
        val bitBuffer = StringBuilder()
        for (b in entropy) {
            val s = Integer.toBinaryString(b.toInt() and 0xFF)
            bitBuffer.append("0".repeat(8 - s.length)).append(s)
        }
        for (b in hash) {
            val s = Integer.toBinaryString(b.toInt() and 0xFF)
            bitBuffer.append("0".repeat(8 - s.length)).append(s)
        }

        val totalWords = totalBits / 11
        val words = mutableListOf<String>()
        for (i in 0 until totalWords) {
            val chunk = bitBuffer.substring(i * 11, (i + 1) * 11)
            val index = chunk.toInt(2)
            words.add(Bip39Wordlist.WORDS[index])
        }
        return words.joinToString(" ")
    }

    fun validateMnemonic(phrase: String): Boolean {
        val words = phrase.trim().lowercase().split("\\s+".toRegex())
        if (words.size !in listOf(12, 15, 18, 21, 24)) return false
        val indices = words.map { Bip39Wordlist.indexOf(it) ?: return false }

        val totalBits = words.size * 11
        val checksumBits = words.size / 3
        val entropyBits = totalBits - checksumBits

        val bitBuffer = StringBuilder()
        for (idx in indices) {
            val s = Integer.toBinaryString(idx)
            bitBuffer.append("0".repeat(11 - s.length)).append(s)
        }

        val entropyBinary = bitBuffer.substring(0, entropyBits)
        val checksumBinary = bitBuffer.substring(entropyBits, totalBits)

        val entropyBytes = ByteArray(entropyBits / 8)
        for (i in entropyBytes.indices) {
            val byteStr = entropyBinary.substring(i * 8, (i + 1) * 8)
            entropyBytes[i] = byteStr.toInt(2).toByte()
        }

        val hash = sha256(entropyBytes)
        val hashBinary = StringBuilder()
        for (b in hash) {
            val s = Integer.toBinaryString(b.toInt() and 0xFF)
            hashBinary.append("0".repeat(8 - s.length)).append(s)
        }
        val expectedChecksum = hashBinary.substring(0, checksumBits)
        return checksumBinary == expectedChecksum
    }

    fun mnemonicToSeed(phrase: String, passphrase: String = ""): ByteArray {
        val gen = PKCS5S2ParametersGenerator(SHA512Digest())
        val pwdBytes = phrase.trim().toByteArray(StandardCharsets.UTF_8)
        val saltBytes = ("mnemonic$passphrase").toByteArray(StandardCharsets.UTF_8)
        gen.init(pwdBytes, saltBytes, 2048)
        val param = gen.generateDerivedParameters(512) as KeyParameter
        return param.key
    }

    // ── BIP32 HD Derivation ────────────────────────────────────────────────

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }

    private data class Bip32Key(val key: ByteArray, val chainCode: ByteArray)

    private fun bip32Master(seed: ByteArray): Bip32Key {
        val i = hmacSha512("Bitcoin seed".toByteArray(StandardCharsets.UTF_8), seed)
        return Bip32Key(i.copyOfRange(0, 32), i.copyOfRange(32, 64))
    }

    private fun bip32ChildHardened(parent: Bip32Key, index: Int): Bip32Key {
        val buffer = ByteBuffer.allocate(37)
        buffer.put(0.toByte())
        buffer.put(parent.key)
        buffer.putInt(0x80000000.toInt() or index)
        val i = hmacSha512(parent.chainCode, buffer.array())
        val il = BigInteger(1, i.copyOfRange(0, 32))
        val pk = BigInteger(1, parent.key)
        val child = (il.add(pk)).mod(CURVE_N)
        return Bip32Key(bigIntTo32Bytes(child), i.copyOfRange(32, 64))
    }

    private fun bip32ChildNormal(parent: Bip32Key, index: Int): Bip32Key {
        val pub = getCompressedPublicKey(parent.key)
        val buffer = ByteBuffer.allocate(37)
        buffer.put(pub)
        buffer.putInt(index)
        val i = hmacSha512(parent.chainCode, buffer.array())
        val il = BigInteger(1, i.copyOfRange(0, 32))
        val pk = BigInteger(1, parent.key)
        val child = (il.add(pk)).mod(CURVE_N)
        return Bip32Key(bigIntTo32Bytes(child), i.copyOfRange(32, 64))
    }

    private fun bip32DerivePath(seed: ByteArray, path: String): ByteArray {
        var current = bip32Master(seed)
        val parts = path.trim().split("/").drop(1)
        for (part in parts) {
            val hardened = part.endsWith("'")
            val idx = part.trimEnd('\'').toInt()
            current = if (hardened) {
                bip32ChildHardened(current, idx)
            } else {
                bip32ChildNormal(current, idx)
            }
        }
        return current.key
    }

    fun mnemonicToSpendKey(phrase: String, passphrase: String = "", account: Int = 0, index: Int = 0): ByteArray {
        val seed = mnemonicToSeed(phrase, passphrase)
        return bip32DerivePath(seed, "m/44'/$COIN_TYPE'/$account'/0/$index")
    }

    fun mnemonicToViewKey(phrase: String, passphrase: String = "", account: Int = 0, index: Int = 0): ByteArray {
        val seed = mnemonicToSeed(phrase, passphrase)
        return bip32DerivePath(seed, "m/44'/$COIN_TYPE'/$account'/1/$index")
    }

    fun createAccountFromMnemonic(phrase: String, passphrase: String = "", account: Int = 0): SPWAccountKeys {
        val spendBytes = mnemonicToSpendKey(phrase, passphrase, account, 0)
        val viewBytes = mnemonicToViewKey(phrase, passphrase, account, 0)
        val spendPub = getCompressedPublicKey(spendBytes)
        val viewPub = getCompressedPublicKey(viewBytes)
        val address = pubkeyToAddress(spendPub)

        return SPWAccountKeys(
            address = address,
            spendKeyHex = spendBytes.toHex(),
            viewKeyHex = viewBytes.toHex(),
            spendPubHex = spendPub.toHex(),
            viewPubHex = viewPub.toHex(),
            mnemonic = phrase
        )
    }

    fun createAccountFromPrivateKey(spendKeyHex: String, viewKeyHex: String? = null): SPWAccountKeys {
        val spendBytes = hexToBytes(spendKeyHex.trim())
        val spendPub = getCompressedPublicKey(spendBytes)
        val viewBytes = if (viewKeyHex != null && viewKeyHex.isNotBlank()) {
            hexToBytes(viewKeyHex.trim())
        } else {
            // derive default view key deterministically if omitted
            sha256(spendBytes)
        }
        val viewPub = getCompressedPublicKey(viewBytes)
        val address = pubkeyToAddress(spendPub)

        return SPWAccountKeys(
            address = address,
            spendKeyHex = spendBytes.toHex(),
            viewKeyHex = viewBytes.toHex(),
            spendPubHex = spendPub.toHex(),
            viewPubHex = viewPub.toHex(),
            mnemonic = null
        )
    }

    // ── Canonical Python JSON Serialization ────────────────────────────────

    fun pyjson(v: Any?): String {
        return when (v) {
            null -> "null"
            is Boolean -> v.toString()
            is Number -> v.toString()
            is String -> {
                "\"" + v.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t") + "\""
            }
            is List<*> -> "[" + v.joinToString(", ") { pyjson(it) } + "]"
            is Map<*, *> -> {
                val sortedKeys = v.keys.map { it.toString() }.sorted()
                "{" + sortedKeys.joinToString(", ") { k ->
                    "\"$k\": " + pyjson(v[k])
                } + "}"
            }
            else -> "\"" + v.toString() + "\""
        }
    }

    // ── Transaction Signing & TXID ─────────────────────────────────────────

    fun computeSigningDigest(
        inputs: List<TxInputData>,
        outputs: List<TxOutputData>,
        timestamp: Long,
        txPubkey: String = "",
        colorIssue: String = ""
    ): ByteArray {
        val inputsList = inputs.map {
            mapOf("prev_txid" to it.prevTxid, "prev_vout" to it.prevVout)
        }
        val outputsList = outputs.map {
            val m = mutableMapOf<String, Any>("address" to it.address, "amount" to it.amount)
            if (it.data != null) m["data"] = it.data
            m
        }
        val dataMap = mapOf(
            "color_issue" to colorIssue,
            "inputs" to inputsList,
            "outputs" to outputsList,
            "timestamp" to timestamp,
            "tx_pubkey" to txPubkey
        )
        val canonicalJson = pyjson(dataMap)
        return dsha256(canonicalJson.toByteArray(StandardCharsets.UTF_8))
    }

    fun computeTxid(
        signedInputs: List<TxInputData>,
        outputs: List<TxOutputData>,
        timestamp: Long,
        coinbaseData: String = "",
        txPubkey: String = "",
        colorIssue: String = ""
    ): String {
        val inputsList = signedInputs.map {
            mapOf(
                "prev_txid" to it.prevTxid,
                "prev_vout" to it.prevVout,
                "pubkey" to it.pubkey,
                "script_sig" to it.scriptSig
            )
        }
        val outputsList = outputs.map {
            val m = mutableMapOf<String, Any>("address" to it.address, "amount" to it.amount)
            if (it.data != null) m["data"] = it.data
            m
        }
        val dataMap = mapOf(
            "coinbase_data" to coinbaseData,
            "color_issue" to colorIssue,
            "inputs" to inputsList,
            "outputs" to outputsList,
            "timestamp" to timestamp,
            "tx_pubkey" to txPubkey
        )
        val canonicalJson = pyjson(dataMap)
        return dsha256(canonicalJson.toByteArray(StandardCharsets.UTF_8)).toHex()
    }

    fun signDigest(digest: ByteArray, privateKeyBytes: ByteArray): String {
        val signer = ECDSASigner(HMacDSAKCalculator(org.bouncycastle.crypto.digests.SHA256Digest()))
        val privKeyParams = ECPrivateKeyParameters(BigInteger(1, privateKeyBytes), domainParams)
        signer.init(true, privKeyParams)
        val components = signer.generateSignature(digest)

        var r = components[0]
        var s = components[1]

        // Low-S canonical enforcement
        val halfN = CURVE_N.shiftRight(1)
        if (s > halfN) {
            s = CURVE_N.subtract(s)
        }

        val baos = ByteArrayOutputStream()
        val derGen = DERSequenceGenerator(baos)
        derGen.addObject(ASN1Integer(r))
        derGen.addObject(ASN1Integer(s))
        derGen.close()

        return baos.toByteArray().toHex()
    }

    // ── Stealth Addresses (ECDH Dual-Key Scheme) ───────────────────────────

    fun makeStealthOutput(recipientSpendPubHex: String, recipientViewPubHex: String): StealthOutput {
        val rBytes = ByteArray(32)
        SECURE_RANDOM.nextBytes(rBytes)
        val r = BigInteger(1, rBytes).mod(CURVE_N)

        val R = ecParams.g.multiply(r).normalize()
        val viewPoint = decodePoint(hexToBytes(recipientViewPubHex))
        val sharedPoint = viewPoint.multiply(r).normalize()

        val xCoord = bigIntTo32Bytes(sharedPoint.affineXCoord.toBigInteger())
        val h = BigInteger(1, sha256(xCoord))

        val spendPoint = decodePoint(hexToBytes(recipientSpendPubHex))
        val oneTimePoint = ecParams.g.multiply(h).add(spendPoint).normalize()

        val oneTimeAddress = pubkeyToAddress(oneTimePoint.getEncoded(true))
        val txPubkeyHex = R.getEncoded(true).toHex()

        return StealthOutput(oneTimeAddress, txPubkeyHex)
    }

    fun scanStealthOutput(
        outputAddress: String,
        txPubkeyHex: String,
        viewKeyHex: String,
        spendPubHex: String
    ): Boolean {
        return try {
            val R = decodePoint(hexToBytes(txPubkeyHex))
            val viewKeyInt = BigInteger(1, hexToBytes(viewKeyHex))
            val sharedPoint = R.multiply(viewKeyInt).normalize()

            val xCoord = bigIntTo32Bytes(sharedPoint.affineXCoord.toBigInteger())
            val h = BigInteger(1, sha256(xCoord))

            val spendPoint = decodePoint(hexToBytes(spendPubHex))
            val expectedPoint = ecParams.g.multiply(h).add(spendPoint).normalize()

            pubkeyToAddress(expectedPoint.getEncoded(true)) == outputAddress
        } catch (e: Exception) {
            false
        }
    }

    fun deriveStealthPrivKey(txPubkeyHex: String, viewKeyHex: String, spendKeyHex: String): String {
        val R = decodePoint(hexToBytes(txPubkeyHex))
        val viewKeyInt = BigInteger(1, hexToBytes(viewKeyHex))
        val sharedPoint = R.multiply(viewKeyInt).normalize()

        val xCoord = bigIntTo32Bytes(sharedPoint.affineXCoord.toBigInteger())
        val h = BigInteger(1, sha256(xCoord))

        val spendInt = BigInteger(1, hexToBytes(spendKeyHex))
        val oneTimeInt = spendInt.add(h).mod(CURVE_N)
        return bigIntTo32Bytes(oneTimeInt).toHex()
    }

    // ── SPW Connect Sign-In Protocol ───────────────────────────────────────

    fun signConnectMessage(app: String, address: String, nonce: String, spendKeyHex: String): String {
        val message = "SPW Wallet Sign-In v1\napp: $app\naddress: $address\nnonce: $nonce"
        val digest = sha256(message.toByteArray(StandardCharsets.UTF_8))
        return signDigest(digest, hexToBytes(spendKeyHex))
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun bigIntTo32Bytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        if (raw.size == 32) return raw
        val out = ByteArray(32)
        if (raw.size > 32) {
            System.arraycopy(raw, raw.size - 32, out, 0, 32)
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.size, raw.size)
        }
        return out
    }

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun sha256Hex(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).toHex()
    }
}
