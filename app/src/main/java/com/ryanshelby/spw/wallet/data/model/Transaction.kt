package com.ryanshelby.spw.wallet.data.model

enum class TransactionType {
    SEND,
    RECEIVE,
    STEALTH
}

enum class TransactionStatus {
    CONFIRMED,
    PENDING,
    FAILED
}

data class TransactionItem(
    val txHash: String,
    val type: TransactionType,
    val fromAddress: String,
    val toAddress: String,
    val amountSpw: Double,
    val amountFeathers: Long,
    val tokenSymbol: String = "SPW",
    val timestamp: Long,
    val status: TransactionStatus,
    val feeSpw: Double,
    val memo: String = "",
    val blockNumber: Long = 0L,
    val txPubkey: String? = null,
    val merkleRoot: String = "",
    val bits: String = "",
    val confirmations: Int = 0,
    val nonce: Long = 0L
)
