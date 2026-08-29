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

class SPWApiClient(
    private var baseUrl: String = SPWCrypto.DEFAULT_NODE_URL
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

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

    suspend fun getExplorer(address: String): Result<SpwExplorerResponse> = withContext(Dispatchers.IO) {
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
}
