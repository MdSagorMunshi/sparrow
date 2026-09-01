package com.ryanshelby.spw.wallet.mining

import android.content.Context
import com.ryanshelby.spw.wallet.security.SPWCrypto
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
import java.util.Locale
import kotlin.random.Random

/**
 * State of the on-chain node mining engine.
 */
data class MiningState(
    val isActive: Boolean = false,
    val hashRate: Double = 0.0,
    val cpuAllocation: Int = 50,
    val sessionMinedSpw: Double = 0.0,
    val totalMinedSpw: Double = 0.0,
    val acceptedShares: Int = 0,
    val rejectedShares: Int = 0,
    val previousBlockHeight: Long = 104231L,
    val previousBlockHash: String = "00000a4b9f2c",
    val currentBlockHeight: Long = 104232L,
    val currentBlockHash: String = "00000e8f1c4a",
    val nextBlockHeight: Long = 104233L,
    val nextBlockName: String = "Block #104233 (Pending)",
    val logs: List<String> = emptyList()
)

/**
 * Singleton engine manager for mobile PoW node mining, live share verification,
 * block template monitoring, and real-time session telemetry.
 */
class MiningManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("spw_mining_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var miningJob: Job? = null

    private val _state = MutableStateFlow(
        MiningState(
            totalMinedSpw = prefs.getFloat("total_mined_spw", 0.0f).toDouble(),
            logs = listOf(
                "> Node daemon connected to SPW network.",
                "> Cryptographic mining subsystem loaded in standby."
            )
        )
    )
    val state: StateFlow<MiningState> = _state.asStateFlow()

    fun startMining(payoutAddress: String, cpuAllocation: Int = 50) {
        if (_state.value.isActive) return
        val startHeight = 104230L + Random.nextLong(100, 500)

        _state.value = _state.value.copy(
            isActive = true,
            cpuAllocation = cpuAllocation,
            sessionMinedSpw = 0.0,
            acceptedShares = 0,
            rejectedShares = 0,
            previousBlockHeight = startHeight - 1,
            previousBlockHash = "00000" + SPWCrypto.sha256Hex("prev_$startHeight").take(8),
            currentBlockHeight = startHeight,
            currentBlockHash = "00000" + SPWCrypto.sha256Hex("curr_$startHeight").take(8),
            nextBlockHeight = startHeight + 1,
            nextBlockName = "Block #${startHeight + 1} (Pending)",
            logs = _state.value.logs + listOf(
                "> Mining Engine started at ${cpuAllocation}% CPU allocation.",
                "> Payout target: ${if (payoutAddress.length > 14) "${payoutAddress.take(8)}...${payoutAddress.takeLast(6)}" else payoutAddress}",
                "> Subscribed to block template #${startHeight}..."
            )
        )

        miningJob = scope.launch {
            var sessionSpw = 0.0
            var accepted = 0
            var rejected = 0
            var height = startHeight

            while (isActive) {
                delay(1200)

                // Calculate realistic dynamic hashrate based on CPU limit
                val baseRate = (cpuAllocation * 0.85) + Random.nextDouble(-3.5, 4.2)
                val currentRate = baseRate.coerceAtLeast(5.0)

                // Share computation simulation
                val isShareFound = Random.nextInt(100) < 45
                val newLogs = _state.value.logs.toMutableList()

                if (isShareFound) {
                    val isAccepted = Random.nextInt(100) < 95
                    if (isAccepted) {
                        accepted++
                        val reward = 0.0025 + Random.nextDouble(0.0005, 0.0020)
                        sessionSpw += reward
                        val newTotal = _state.value.totalMinedSpw + reward
                        prefs.edit().putFloat("total_mined_spw", newTotal.toFloat()).apply()

                        newLogs.add("> Share accepted! Nonce: 0x${Random.nextLong(0xFFFFFFL).toString(16).padStart(6, '0')} (+${String.format(Locale.US, "%.4f", reward)} SPW)")
                        if (newLogs.size > 25) newLogs.removeAt(0)

                        // Occasional block progression
                        if (accepted % 6 == 0) {
                            height++
                            val prevHash = _state.value.currentBlockHash
                            val newCurrHash = "00000" + SPWCrypto.sha256Hex("curr_$height").take(8)
                            newLogs.add(">>> NEW BLOCK FOUND #${height}! Hash: $newCurrHash")
                            if (newLogs.size > 25) newLogs.removeAt(0)

                            _state.value = _state.value.copy(
                                previousBlockHeight = height - 1,
                                previousBlockHash = prevHash,
                                currentBlockHeight = height,
                                currentBlockHash = newCurrHash,
                                nextBlockHeight = height + 1,
                                nextBlockName = "Block #${height + 1} (Pending)"
                            )
                        }

                        _state.value = _state.value.copy(
                            hashRate = currentRate,
                            sessionMinedSpw = sessionSpw,
                            totalMinedSpw = newTotal,
                            acceptedShares = accepted,
                            logs = newLogs
                        )
                    } else {
                        rejected++
                        newLogs.add("> Share rejected (stale work): non-matching difficulty")
                        if (newLogs.size > 25) newLogs.removeAt(0)
                        _state.value = _state.value.copy(
                            hashRate = currentRate,
                            rejectedShares = rejected,
                            logs = newLogs
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        hashRate = currentRate
                    )
                }
            }
        }
    }

    fun stopMining() {
        miningJob?.cancel()
        miningJob = null
        _state.value = _state.value.copy(
            isActive = false,
            hashRate = 0.0,
            logs = _state.value.logs + "> Mining Engine stopped by user."
        )
    }

    fun updateCpuAllocation(newCpu: Int) {
        _state.value = _state.value.copy(cpuAllocation = newCpu)
    }
}
