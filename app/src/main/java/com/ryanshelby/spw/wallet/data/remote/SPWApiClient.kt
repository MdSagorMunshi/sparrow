package com.ryanshelby.spw.wallet.data.remote

import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SpwBalanceResponse(
    @Json(name = "address") val address: String = "",
    @Json(name = "balance_spw") val balanceSpw: Double = 0.0,
    @Json(name = "balance_feathers") val balanceFeathers: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SpwUtxo(
    @Json(name = "txid") val txid: String = "",
    @Json(name = "vout") val vout: Int = 0,
    @Json(name = "amount") val amount: Long = 0L, // in feathers
    @Json(name = "address") val address: String = "",
    @Json(name = "block_height") val blockHeight: Long? = null,
    @Json(name = "is_stealth") val isStealth: Boolean = false,
    @Json(name = "tx_pubkey") val txPubkey: String? = null
)

@JsonClass(generateAdapter = true)
data class SpwUtxosResponse(
    @Json(name = "utxos") val utxos: List<SpwUtxo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SpwTxOutput(
    @Json(name = "address") val address: String = "",
    @Json(name = "amount") val amount: Long = 0L,
    @Json(name = "data") val data: String? = null
)

@JsonClass(generateAdapter = true)
data class SpwTxInput(
    @Json(name = "prev_txid") val prevTxid: String = "",
    @Json(name = "prev_vout") val prevVout: Int = 0,
    @Json(name = "pubkey") val pubkey: String = "",
    @Json(name = "script_sig") val scriptSig: String = ""
)

@JsonClass(generateAdapter = true)
data class SpwTransaction(
    @Json(name = "txid") val txid: String = "",
    @Json(name = "inputs") val inputs: List<SpwTxInput> = emptyList(),
    @Json(name = "outputs") val outputs: List<SpwTxOutput> = emptyList(),
    @Json(name = "timestamp") val timestamp: Long = 0L,
    @Json(name = "block_height") val blockHeight: Long? = null,
    @Json(name = "coinbase_data") val coinbaseData: String? = null,
    @Json(name = "tx_pubkey") val txPubkey: String? = null,
    @Json(name = "color_issue") val colorIssue: String? = null
)

@JsonClass(generateAdapter = true)
data class SpwExplorerResponse(
    @Json(name = "address") val address: String = "",
    @Json(name = "balance_spw") val balanceSpw: Double = 0.0,
    @Json(name = "balance_feathers") val balanceFeathers: Long = 0L,
    @Json(name = "transactions") val transactions: List<SpwTransaction> = emptyList(),
    @Json(name = "utxos") val utxos: List<SpwUtxo> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SpwBroadcastRequest(
    @Json(name = "txid") val txid: String,
    @Json(name = "inputs") val inputs: List<SpwTxInput>,
    @Json(name = "outputs") val outputs: List<SpwTxOutput>,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "coinbase_data") val coinbaseData: String = "",
    @Json(name = "tx_pubkey") val txPubkey: String = "",
    @Json(name = "color_issue") val colorIssue: String = ""
)

@JsonClass(generateAdapter = true)
data class SpwBroadcastResponse(
    @Json(name = "txid") val txid: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SpwScanResponse(
    @Json(name = "utxos") val utxos: List<SpwUtxo> = emptyList(),
    @Json(name = "total_spw") val totalSpw: Double = 0.0,
    @Json(name = "total_feathers") val totalFeathers: Long = 0L,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SpwBlockResponse(
    @Json(name = "hash") val hash: String = "",
    @Json(name = "transactions") val transactions: List<SpwTransaction> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SpwBlockHeaderDto(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "height") val height: Long = 0L,
    @Json(name = "prev_hash") val prevHash: String = "",
    @Json(name = "merkle_root") val merkleRoot: String = "",
    @Json(name = "timestamp") val timestamp: Long = 0L,
    @Json(name = "bits") val bits: Long = 0x1f07ffffL,
    @Json(name = "nonce") val nonce: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SpwLatestBlockDto(
    @Json(name = "header") val header: SpwBlockHeaderDto? = null,
    @Json(name = "hash") val hash: String = "",
    @Json(name = "transactions") val transactions: List<Map<String, Any?>> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SpwChainInfoDto(
    @Json(name = "height") val height: Long = 0L,
    @Json(name = "next_reward") val nextReward: Double = 1.0,
    @Json(name = "network") val network: String? = null,
    @Json(name = "difficulty") val difficulty: Double? = null
)

@JsonClass(generateAdapter = true)
data class SpwMempoolDto(
    @Json(name = "transactions") val transactions: List<Map<String, Any?>> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SpwBlockSubmitResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "error") val error: String? = null
)

open class SPWApiClient(
    private var baseUrl: String = SPWCrypto.DEFAULT_NODE_URL
) {
    private var proxyConfig: com.ryanshelby.spw.wallet.data.model.ProxyConfig = com.ryanshelby.spw.wallet.data.model.ProxyConfig()

    @Volatile
    private var client: OkHttpClient = buildClient()

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })

        val javaProxy = proxyConfig.toJavaProxy()
        if (javaProxy != java.net.Proxy.NO_PROXY) {
            builder.proxy(javaProxy)
            if (proxyConfig.username.isNotBlank() && proxyConfig.password.isNotBlank()) {
                builder.proxyAuthenticator { _, response ->
                    val credential = okhttp3.Credentials.basic(proxyConfig.username, proxyConfig.password)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }
        }
        return builder.build()
    }

    fun setProxyConfig(config: com.ryanshelby.spw.wallet.data.model.ProxyConfig) {
        this.proxyConfig = config
        this.client = buildClient()
    }

    fun getProxyConfig(): com.ryanshelby.spw.wallet.data.model.ProxyConfig = proxyConfig

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun setNodeUrl(url: String) {
        baseUrl = url.trim().trimEnd('/')
    }

    fun getNodeUrl(): String = baseUrl

    suspend fun getBalance(address: String): Result<SpwBalanceResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/balance/${address.trim()}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwBalanceResponse::class.java)
            val result = adapter.fromJson(body) ?: SpwBalanceResponse(address = address)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUtxos(address: String): Result<List<SpwUtxo>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/utxos/${address.trim()}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwUtxosResponse::class.java)
            val result = adapter.fromJson(body)
            Result.success(result?.utxos ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun getExplorer(address: String): Result<SpwExplorerResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/explorer/${address.trim()}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwExplorerResponse::class.java)
            val result = adapter.fromJson(body)
            if (result?.error != null) {
                Result.failure(Exception(result.error))
            } else {
                Result.success(result ?: SpwExplorerResponse(address = address))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun broadcastTransaction(tx: SpwBroadcastRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/tx/broadcast"
            val adapter = moshi.adapter(SpwBroadcastRequest::class.java)
            val json = adapter.toJson(tx)
            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            val resAdapter = moshi.adapter(SpwBroadcastResponse::class.java)
            val parsed = try { resAdapter.fromJson(body) } catch (e: Exception) { null }

            if (!response.isSuccessful) {
                val errMsg = parsed?.error ?: body
                return@withContext Result.failure(Exception("Broadcast failed: $errMsg"))
            }

            val txid = parsed?.txid ?: tx.txid
            Result.success(txid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanStealthOutputs(viewPubHex: String, spendPubHex: String): Result<SpwScanResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/scan/${viewPubHex.trim()}/${spendPubHex.trim()}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwScanResponse::class.java)
            val result = adapter.fromJson(body)
            Result.success(result ?: SpwScanResponse())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlock(height: Long): Result<SpwBlockResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/block/$height"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwBlockResponse::class.java)
            val result = adapter.fromJson(body)
            Result.success(result ?: SpwBlockResponse())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestBlockHeight(): Long = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/chain/info"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext 0L
            val heightIdx = body.indexOf("\"height\":")
            if (heightIdx != -1) {
                val endIdx = body.indexOfAny(charArrayOf(',', '}'), heightIdx)
                body.substring(heightIdx + 9, endIdx).trim().toLongOrNull() ?: 0L
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun fetchLatestBlock(): Result<SpwLatestBlockDto?> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/block/latest"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.code == 404) {
                return@withContext Result.success(null) // Genesis
            }
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwLatestBlockDto::class.java)
            val result = adapter.fromJson(body)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchChainInfo(): Result<SpwChainInfoDto> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/chain/info"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val adapter = moshi.adapter(SpwChainInfoDto::class.java)
            val result = adapter.fromJson(body) ?: SpwChainInfoDto()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMempool(): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/mempool"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            if (!response.isSuccessful) {
                return@withContext Result.success(emptyList())
            }
            val adapter = moshi.adapter(SpwMempoolDto::class.java)
            val result = adapter.fromJson(body)
            Result.success(result?.transactions ?: emptyList())
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun submitBlock(blockJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/block/submit"
            val body = blockJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val adapter = moshi.adapter(SpwBlockSubmitResponse::class.java)
            val parsed = try { adapter.fromJson(responseBody) } catch (e: Exception) { null }

            if (!response.isSuccessful) {
                val errMsg = parsed?.error ?: "HTTP ${response.code}: $responseBody"
                return@withContext Result.failure(Exception(errMsg))
            }
            if (parsed?.status == "accepted" || response.isSuccessful) {
                Result.success(parsed?.status ?: "accepted")
            } else {
                Result.failure(Exception(parsed?.error ?: "Block rejected by node"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
