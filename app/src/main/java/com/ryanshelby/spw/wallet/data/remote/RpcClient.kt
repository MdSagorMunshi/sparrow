package com.ryanshelby.spw.wallet.data.remote

import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RpcClient(
    val apiClient: SPWApiClient = SPWApiClient()
) {

    suspend fun getAddressBalance(address: String, network: NetworkConfig): Double = withContext(Dispatchers.IO) {
        apiClient.setNodeUrl(network.rpcUrl)
        val res = apiClient.getBalance(address)
        if (res.isSuccess) {
            val data = res.getOrNull()
            return@withContext data?.balanceSpw ?: 0.0
        }
        return@withContext 0.0
    }

    suspend fun getUtxos(address: String, network: NetworkConfig): List<SpwUtxo> = withContext(Dispatchers.IO) {
        apiClient.setNodeUrl(network.rpcUrl)
        val res = apiClient.getUtxos(address)
        return@withContext res.getOrNull() ?: emptyList()
    }

    suspend fun getExplorerHistory(address: String, network: NetworkConfig): SpwExplorerResponse? = withContext(Dispatchers.IO) {
        apiClient.setNodeUrl(network.rpcUrl)
        val res = apiClient.getExplorer(address)
        return@withContext res.getOrNull()
    }

    suspend fun broadcastTransaction(request: SpwBroadcastRequest, network: NetworkConfig): Result<String> = withContext(Dispatchers.IO) {
        apiClient.setNodeUrl(network.rpcUrl)
        return@withContext apiClient.broadcastTransaction(request)
    }
}
