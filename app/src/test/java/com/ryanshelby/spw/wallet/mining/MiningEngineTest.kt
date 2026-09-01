package com.ryanshelby.spw.wallet.mining

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class MiningEngineTest {

    @Test
    fun testBitsToTarget() {
        val genesisBits = 0x1f07ffffL
        val target = MiningEngine.bitsToTarget(genesisBits)
        assertTrue("Genesis target must be positive", target > BigInteger.ZERO)

        // Test with standard difficulty bits: 0x1d00ffff
        val btcBits = 0x1d00ffffL
        val btcTarget = MiningEngine.bitsToTarget(btcBits)
        assertTrue("BTC target must be positive", btcTarget > BigInteger.ZERO)
        assertTrue("Higher difficulty bits must have smaller target", btcTarget < target)
    }

    @Test
    fun testOutputCanonical() {
        val input = mapOf<String, Any?>(
            "address" to "DTestAddress123",
            "amount" to 100000000L,
            "data" to "",
            "color_id" to null,
            "color_qty" to 0L
        )
        val canonical = MiningEngine.outputCanonical(input)
        assertEquals("DTestAddress123", canonical["address"])
        assertEquals(100000000L, canonical["amount"])
        assertTrue("Empty data should be omitted", !canonical.containsKey("data"))
        assertTrue("Empty color_id should be omitted", !canonical.containsKey("color_id"))
    }

    @Test
    fun testTxidGeneration() {
        val coinbaseTx = mapOf(
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
                    "amount" to 100000000L,
                    "address" to "D5n8Q7Z..."
                )
            ),
            "timestamp" to 1700000000L,
            "coinbase_data" to "SPW block 1",
            "tx_pubkey" to "",
            "color_issue" to ""
        )
        val txid = MiningEngine.txid(coinbaseTx)
        assertEquals("TxID must be 64-character SHA-256 hex string", 64, txid.length)
    }

    @Test
    fun testMerkleRootCalculation() {
        val tx1 = "a".repeat(64)
        val tx2 = "b".repeat(64)
        val tx3 = "c".repeat(64)

        // Single txid Merkle root is the txid itself
        val root1 = MiningEngine.merkleRoot(listOf(tx1))
        assertEquals(tx1, root1)

        // Pair txids Merkle root
        val root2 = MiningEngine.merkleRoot(listOf(tx1, tx2))
        assertEquals(64, root2.length)

        // Odd number of txids (3 txids with padding)
        val root3 = MiningEngine.merkleRoot(listOf(tx1, tx2, tx3))
        assertEquals(64, root3.length)
    }

    @Test
    fun testBuildCandidate() {
        val candidate = MiningEngine.buildCandidate(
            latestBlock = null,
            minerAddress = "DTestMinerAddress123456789",
            rewardSpw = 1.0,
            mempoolTxs = emptyList()
        )
        assertNotNull(candidate["header"])
        assertNotNull(candidate["transactions"])
        assertEquals(1, candidate["tx_count"])

        @Suppress("UNCHECKED_CAST")
        val header = candidate["header"] as Map<String, Any>
        assertEquals(0L, header["height"])
        assertEquals(MiningEngine.GENESIS_BITS, header["bits"])
    }

    @Test
    fun testMineBlockWithEasyTarget() {
        val candidate = MiningEngine.buildCandidate(
            latestBlock = null,
            minerAddress = "DTestMinerAddress123456789",
            rewardSpw = 1.0,
            mempoolTxs = emptyList()
        ).toMutableMap()

        val stopFlag = AtomicBoolean(false)
        // High target (easy difficulty) for quick test completion
        val easyBits = 0x207fffffL
        val result = MiningEngine.mineBlock(
            candidate = candidate,
            bits = easyBits,
            stopFlag = stopFlag,
            cpuLimit = 100
        )
        assertNotNull("Should find nonce for easy target", result)
        assertTrue("Nonce must be >= 0", result!!.first >= 0)
        assertEquals(64, result.second.length)
    }
}
