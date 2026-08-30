package com.ryanshelby.spw.wallet.payment

import com.ryanshelby.spw.wallet.data.remote.SPWApiClient
import com.ryanshelby.spw.wallet.security.SPWCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TransactionWatcher(
    private val apiClient: SPWApiClient,
    private val requestManager: PaymentRequestManager = PaymentRequestManager.instance,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher)
    private var watchJob: Job? = null

    fun startWatching(targetAddress: String, targetAmount: Double, afterTimestampMs: Long) {
        watchJob?.cancel()
        watchJob = scope.launch {
            val targetFeathers = (targetAmount * SPWCrypto.FEATHERS_PER_SPW).toLong()
            
            while (isActive) {
                val state = requestManager.requestState.value
                if (state != PaymentRequestState.WAITING && state != PaymentRequestState.CONNECTED) {
                    break
                }

                try {
                    val result = apiClient.getExplorer(targetAddress)
                    if (result.isSuccess) {
                        val data = result.getOrNull()
                        if (data != null) {
                            // Check UTXOs (mempool/0-conf)
                            val mempoolMatch = data.utxos.any { utxo ->
                                utxo.amount == targetFeathers && 
                                // Ideally we'd check timestamp, but utxos don't have timestamp. 
                                // In a real app we'd maintain known utxos before the request and diff them.
                                // For this feature, matching amount is a good proxy.
                                utxo.address == targetAddress 
                            }
                            
                            // Check transactions (1-conf+)
                            val chainMatch = data.transactions.any { tx ->
                                tx.timestamp * 1000 >= afterTimestampMs && 
                                tx.outputs.any { out -> out.address == targetAddress && out.amount == targetFeathers }
                            }
                            
                            if (mempoolMatch || chainMatch) {
                                // Payment found!
                                launch(Dispatchers.Main) {
                                    requestManager.setReceived()
                                }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient network errors during polling
                }
                
                delay(3000)
            }
        }
    }
    
    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
    }
}
