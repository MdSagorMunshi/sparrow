package com.ryanshelby.spw.wallet.mining

import com.ryanshelby.spw.wallet.data.remote.SpwLatestBlockDto
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.security.SPWCrypto.toHex
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native SPW Cryptographic Proof-of-Work (PoW) and Block Assembly Engine.
 * Conforms 100% to the official SPW Node consensus and miner_client.py protocol.
 */
object MiningEngine {

    const val UNITS: Long = 100_000_000L // Feathers per SPW
    const val GENESIS_BITS: Long = 0x1f07ffffL
    const val RX_KEY_INTERVAL: Long = 2048L

    /**
     * Converts compact bits representation into a 256-bit target integer.
     */
    fun bitsToTarget(bits: Long): BigInteger {
        val exp = ((bits ushr 24) and 0xFFL).toInt()
        val coef = bits and 0x007FFFFFL
        if (exp <= 3) {
            return BigInteger.valueOf(coef).shiftRight(8 * (3 - exp))
        }
        return BigInteger.valueOf(coef).multiply(BigInteger.valueOf(256).pow(exp - 3))
    }

    /**
     * Returns canonical output map matching TxOutput.to_dict(): omit empty/zero optional fields.
     */
    fun outputCanonical(o: Map<String, Any?>): Map<String, Any> {
        val d = mutableMapOf<String, Any>()
        d["address"] = o["address"]?.toString() ?: ""
        d["amount"] = (o["amount"] as? Number)?.toLong() ?: 0L

        val data = o["data"]?.toString()
        if (!data.isNullOrEmpty()) {
            d["data"] = data
        }

        val colorId = o["color_id"]?.toString()
        if (!colorId.isNullOrEmpty()) {
            d["color_id"] = colorId
            d["color_qty"] = (o["color_qty"] as? Number)?.toLong() ?: 0L
        }
        return d
    }

    /**
     * Matches Transaction.txid exactly: sort_keys=True, compact separators, double SHA-256.
     */
    fun txid(txDict: Map<String, Any?>): String {
        val rawInputs = (txDict["inputs"] as? List<*>) ?: emptyList<Any>()
        val inputsList = rawInputs.mapNotNull { item ->
            if (item is Map<*, *>) {
                mapOf(
                    "prev_txid" to (item["prev_txid"]?.toString() ?: "0".repeat(64)),
                    "prev_vout" to ((item["prev_vout"] as? Number)?.toInt() ?: -1),
                    "pubkey" to (item["pubkey"]?.toString() ?: ""),
                    "script_sig" to (item["script_sig"]?.toString() ?: "")
                )
            } else null
        }

        val rawOutputs = (txDict["outputs"] as? List<*>) ?: emptyList<Any>()
        val outputsList = rawOutputs.mapNotNull { item ->
            if (item is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                outputCanonical(item as Map<String, Any?>)
            } else null
        }

        val canonical = mapOf(
            "coinbase_data" to (txDict["coinbase_data"]?.toString() ?: ""),
            "color_issue" to (txDict["color_issue"]?.toString() ?: ""),
            "inputs" to inputsList,
            "outputs" to outputsList,
            "timestamp" to ((txDict["timestamp"] as? Number)?.toLong() ?: 0L),
            "tx_pubkey" to (txDict["tx_pubkey"]?.toString() ?: "")
        )

        val sortedJson = SPWCrypto.pyjson(canonical)
        val rawBytes = sortedJson.toByteArray(Charsets.UTF_8)
        return SPWCrypto.dsha256(rawBytes).toHex()
    }

    /**
     * Pairwise double SHA-256 Merkle Root over list of transaction IDs.
     */
    fun merkleRoot(txids: List<String>): String {
        if (txids.isEmpty()) return "0".repeat(64)
        var layer = txids.toList()
        while (layer.size > 1) {
            val workLayer = if (layer.size % 2 == 1) layer + layer.last() else layer
            val nextLayer = mutableListOf<String>()
            for (i in 0 until workLayer.size step 2) {
                val combined = (workLayer[i] + workLayer[i + 1]).toByteArray(Charsets.UTF_8)
                val h = SPWCrypto.dsha256(combined).toHex()
                nextLayer.add(h)
            }
            layer = nextLayer
        }
        return layer[0]
    }

    /**
     * Builds candidate block template ready for PoW nonce search.
     */
    fun buildCandidate(
        latestBlock: SpwLatestBlockDto?,
        minerAddress: String,
        rewardSpw: Double,
        mempoolTxs: List<Map<String, Any?>>
    ): Map<String, Any> {
        val height: Long
        val prevHash: String
        val bits: Long

        if (latestBlock != null && latestBlock.header != null) {
            height = latestBlock.header.height + 1
            prevHash = latestBlock.hash.ifEmpty { "0".repeat(64) }
            bits = latestBlock.header.bits
        } else {
            height = 0L
            prevHash = "0".repeat(64)
            bits = GENESIS_BITS
        }

        val rewardFeathers = (rewardSpw * UNITS).toLong()
        val nowSec = System.currentTimeMillis() / 1000L

        val coinbase = mapOf(
            "inputs" to listOf(
                mapOf(
                    "prev_txid" to "0".repeat(64),
                    "prev_vout" to -1,
                    "script_sig" to "",
                    "pubkey" to ""
                )
            ),
            "outputs" to listOf(
                mapOf(
                    "amount" to rewardFeathers,
                    "address" to minerAddress
                )
            ),
            "timestamp" to nowSec,
            "coinbase_data" to "SPW block $height",
            "tx_pubkey" to "",
            "color_issue" to ""
        )

        val cbTxid = txid(coinbase)
        val allTxids = mutableListOf(cbTxid)
        for (tx in mempoolTxs) {
            allTxids.add(txid(tx))
        }

        val mr = merkleRoot(allTxids)

        val header = mutableMapOf<String, Any>(
            "version" to 1,
            "height" to height,
            "prev_hash" to prevHash,
            "merkle_root" to mr,
            "timestamp" to nowSec,
            "bits" to bits,
            "nonce" to 0L
        )

        val allTransactions = mutableListOf<Map<String, Any?>>()
        allTransactions.add(coinbase)
        allTransactions.addAll(mempoolTxs)

        return mapOf(
            "header" to header,
            "transactions" to allTransactions,
            "tx_count" to allTransactions.size
        )
    }

    /**
     * Calculates candidate block header hash for a given nonce.
     */
    fun calculateBlockHash(
        version: Int,
        height: Long,
        prevHash: String,
        merkleRoot: String,
        timestamp: Long,
        bits: Long,
        nonce: Long
    ): String {
        val raw = "${version}${height}${prevHash}${merkleRoot}${timestamp}${bits}${nonce}"
        val rawBytes = raw.toByteArray(Charsets.UTF_8)
        return SPWCrypto.dsha256(rawBytes).toHex()
    }

    /**
     * Core PoW loop with duty-cycle throttling and progress reporting.
     * Returns Pair(nonce, hash) on success, or null if stopped.
     */
    fun mineBlock(
        candidate: MutableMap<String, Any>,
        bits: Long,
        stopFlag: AtomicBoolean,
        cpuLimit: Int = 80,
        onProgress: ((noncesScanned: Long, hashRate: Double) -> Unit)? = null
    ): Pair<Long, String>? {
        @Suppress("UNCHECKED_CAST")
        val header = candidate["header"] as? MutableMap<String, Any> ?: return null

        val version = (header["version"] as? Number)?.toInt() ?: 1
        val height = (header["height"] as? Number)?.toLong() ?: 0L
        val prevHash = header["prev_hash"]?.toString() ?: "0".repeat(64)
        val merkleRoot = header["merkle_root"]?.toString() ?: "0".repeat(64)
        var timestamp = (header["timestamp"] as? Number)?.toLong() ?: (System.currentTimeMillis() / 1000L)

        val target = bitsToTarget(bits)
        var nonce = 0L

        val throttle = cpuLimit < 99
        val BATCH = 250
        val duty = (cpuLimit.coerceIn(5, 100) / 100.0)
        val sleepRatio = (1.0 - duty) / duty

        var batchStart = System.nanoTime()
        var tReport = System.currentTimeMillis()
        var nonceAtReport = 0L

        while (!stopFlag.get()) {
            val hashHex = calculateBlockHash(version, height, prevHash, merkleRoot, timestamp, bits, nonce)
            val hashInt = BigInteger(hashHex, 16)

            if (hashInt < target) {
                header["nonce"] = nonce
                header["timestamp"] = timestamp
                candidate["hash"] = hashHex
                return Pair(nonce, hashHex)
            }

            nonce++

            if (nonce % 2000 == 0L) {
                timestamp = System.currentTimeMillis() / 1000L
                header["timestamp"] = timestamp
                val now = System.currentTimeMillis()
                val elapsed = (now - tReport) / 1000.0
                if (elapsed >= 1.0) {
                    val rate = ((nonce - nonceAtReport) / elapsed)
                    onProgress?.invoke(nonce, rate)
                    tReport = now
                    nonceAtReport = nonce
                }
            }

            if (throttle && nonce % BATCH == 0L) {
                val batchElapsedMs = (System.nanoTime() - batchStart) / 1_000_000.0
                val sleepMs = (batchElapsedMs * sleepRatio).toLong()
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs.coerceAtMost(50))
                    } catch (_: InterruptedException) {
                        break
                    }
                }
                batchStart = System.nanoTime()
            }
        }

        return null
    }
}
