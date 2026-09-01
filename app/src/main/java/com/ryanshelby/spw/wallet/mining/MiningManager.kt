package com.ryanshelby.spw.wallet.mining

import android.content.Context
import com.ryanshelby.spw.wallet.SPWApplication
import com.ryanshelby.spw.wallet.data.remote.SPWApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Observable UI state of the on-chain node mining engine.
 */
data class MiningState(
    val isActive: Boolean = false,
    val hashRate: Double = 0.0,
    val cpuAllocation: Int = 50,
    val sessionMinedSpw: Double = 0.0,
    val totalMinedSpw: Double = 0.0,
    val acceptedShares: Int = 0,
    val rejectedShares: Int = 0,
    val previousBlockHeight: Long = 0L,
    val previousBlockHash: String = "0".repeat(12),
    val currentBlockHeight: Long = 1L,
    val currentBlockHash: String = "0".repeat(12),
    val nextBlockHeight: Long = 2L,
    val nextBlockName: String = "Block #2 (Pending)",
    val logs: List<String> = emptyList()
)

/**
 * Singleton manager coordinating the real Proof-of-Work node mining engine,
 * blockchain RPC polling, block assembly, submission, and UI telemetry.
 */
class MiningManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("spw_mining_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var miningJob: Job? = null
    private val stopFlag = AtomicBoolean(false)

    private val apiClient: SPWApiClient
        get() = SPWApplication.instance.rpcClient.apiClient

    private val _state = MutableStateFlow(
        MiningState(
            totalMinedSpw = prefs.getFloat("total_mined_spw", 0.0f).toDouble(),
            logs = listOf(
                "> SPW Mining Subsystem initialized.",
                "> Standby mode active. Ready to connect to node."
            )
        )
    )
    val state: StateFlow<MiningState> = _state.asStateFlow()

    fun startMining(payoutAddress: String, cpuAllocation: Int = 50) {
        try {
            com.ryanshelby.spw.wallet.service.MiningForegroundService.startService(context, payoutAddress, cpuAllocation)
        } catch (e: Exception) {
            // Fallback for direct testing or environment restrictions
            startMiningInternal(payoutAddress, cpuAllocation)
        }
    }

    fun startMiningInternal(payoutAddress: String, cpuAllocation: Int = 50) {
        if (_state.value.isActive && miningJob?.isActive == true) return
        stopFlag.set(false)

        _state.value = _state.value.copy(
            isActive = true,
            cpuAllocation = cpuAllocation,
            sessionMinedSpw = 0.0,
            acceptedShares = 0,
            rejectedShares = 0,
            hashRate = 0.0
        )

        appendLog("> Connecting to node: ${apiClient.getNodeUrl()}")
        appendLog("> Payout address: ${if (payoutAddress.length > 14) "${payoutAddress.take(8)}...${payoutAddress.takeLast(6)}" else payoutAddress}")
        appendLog("> Initializing PoW candidate engine at ${cpuAllocation}% CPU limit...")

        miningJob?.cancel()
        miningJob = scope.launch(Dispatchers.Default) {
            while (isActive && !stopFlag.get()) {
                try {
                    // 1. Fetch latest blockchain info & mempool from node
                    val latestBlock = apiClient.fetchLatestBlock().getOrNull()
                    val chainInfo = apiClient.fetchChainInfo().getOrNull()
                    val mempoolTxs = apiClient.fetchMempool().getOrNull() ?: emptyList()

                    val targetHeight = if (latestBlock?.header != null) latestBlock.header.height + 1 else (chainInfo?.height?.plus(1) ?: 1L)
                    val prevHash = latestBlock?.hash ?: "0".repeat(64)
                    val rewardSpw = chainInfo?.nextReward ?: 1.0
                    val bits = latestBlock?.header?.bits ?: MiningEngine.GENESIS_BITS

                    _state.value = _state.value.copy(
                        previousBlockHeight = (targetHeight - 1).coerceAtLeast(0),
                        previousBlockHash = prevHash.take(12),
                        currentBlockHeight = targetHeight,
                        currentBlockHash = "mining...",
                        nextBlockHeight = targetHeight + 1,
                        nextBlockName = "Block #${targetHeight + 1} (Pending)"
                    )

                    appendLog("> Mining block #$targetHeight (reward: ${String.format(Locale.US, "%.2f", rewardSpw)} SPW, mempool txs: ${mempoolTxs.size})...")

                    // 2. Build candidate block
                    val candidate = MiningEngine.buildCandidate(
                        latestBlock = latestBlock,
                        minerAddress = payoutAddress,
                        rewardSpw = rewardSpw,
                        mempoolTxs = mempoolTxs
                    ).toMutableMap()

                    val tStart = System.currentTimeMillis()

                    // 3. Run PoW nonce search
                    val result = MiningEngine.mineBlock(
                        candidate = candidate,
                        bits = bits,
                        stopFlag = stopFlag,
                        cpuLimit = _state.value.cpuAllocation,
                        onProgress = { nonces, rate ->
                            _state.value = _state.value.copy(hashRate = rate)
                        }
                    )

                    if (result != null && !stopFlag.get()) {
                        val (nonce, blockHash) = result
                        val elapsedSec = (System.currentTimeMillis() - tStart) / 1000.0
                        val rate = if (elapsedSec > 0) (nonce / elapsedSec) else 0.0

                        appendLog(">>> Nonce found! Nonce: $nonce, Hash: ${blockHash.take(14)}...")
                        appendLog("> Submitting block #$targetHeight to node...")

                        // 4. Submit candidate block to node
                        val blockJson = mapToJsonString(candidate)
                        val submitResult = apiClient.submitBlock(blockJson)

                        if (submitResult.isSuccess) {
                            val newAccepted = _state.value.acceptedShares + 1
                            val newSessionMined = _state.value.sessionMinedSpw + rewardSpw
                            val newTotalMined = _state.value.totalMinedSpw + rewardSpw
                            prefs.edit().putFloat("total_mined_spw", newTotalMined.toFloat()).apply()

                            _state.value = _state.value.copy(
                                acceptedShares = newAccepted,
                                sessionMinedSpw = newSessionMined,
                                totalMinedSpw = newTotalMined,
                                currentBlockHash = blockHash.take(12),
                                hashRate = rate
                            )

                            appendLog("✓ Block #$targetHeight ACCEPTED by node! (+${String.format(Locale.US, "%.4f", rewardSpw)} SPW)")

                            // Trigger wallet repository refresh to update confirmed balance
                            try {
                                SPWApplication.instance.walletRepository.refreshOnChainData()
                            } catch (_: Exception) {}
                        } else {
                            val errMsg = submitResult.exceptionOrNull()?.message ?: "Unknown error"
                            _state.value = _state.value.copy(
                                rejectedShares = _state.value.rejectedShares + 1
                            )
                            appendLog("✗ Block #$targetHeight Rejected by node: $errMsg. Rebuilding candidate...")
                            delay(600)
                        }
                    }
                } catch (e: Exception) {
                    if (!stopFlag.get()) {
                        appendLog("! Node communication notice: ${e.localizedMessage ?: "retrying"}. Retrying in 2s...")
                        delay(2000)
                    }
                }
            }
        }
    }

    fun stopMining() {
        try {
            com.ryanshelby.spw.wallet.service.MiningForegroundService.stopService(context)
        } catch (e: Exception) {
            stopMiningInternal()
        }
    }

    fun stopMiningInternal() {
        stopFlag.set(true)
        miningJob?.cancel()
        miningJob = null
        _state.value = _state.value.copy(
            isActive = false,
            hashRate = 0.0
        )
        appendLog("> Mining Engine stopped by user.")
    }

    fun updateCpuAllocation(newCpu: Int) {
        _state.value = _state.value.copy(cpuAllocation = newCpu)
    }

    private fun appendLog(msg: String) {
        val currentLogs = _state.value.logs.toMutableList()
        currentLogs.add(msg)
        if (currentLogs.size > 50) {
            currentLogs.removeAt(0)
        }
        _state.value = _state.value.copy(logs = currentLogs)
    }

    private fun mapToJsonString(map: Map<*, *>): String {
        val json = JSONObject()
        for ((k, v) in map) {
            when (v) {
                is Map<*, *> -> json.put(k.toString(), JSONObject(mapToJsonString(v)))
                is List<*> -> json.put(k.toString(), listToJsonArray(v))
                else -> json.put(k.toString(), v)
            }
        }
        return json.toString()
    }

    private fun listToJsonArray(list: List<*>): JSONArray {
        val arr = JSONArray()
        for (item in list) {
            when (item) {
                is Map<*, *> -> arr.put(JSONObject(mapToJsonString(item)))
                is List<*> -> arr.put(listToJsonArray(item))
                else -> arr.put(item)
            }
        }
        return arr
    }
}
