package com.ryanshelby.spw.wallet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val spendKeyHex: String,
    val viewKeyHex: String,
    val spendPubHex: String,
    val viewPubHex: String,
    val mnemonic: String?,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val txHash: String,
    val type: String, // "SEND", "RECEIVE", "STEALTH"
    val fromAddress: String,
    val toAddress: String,
    val amountSpw: Double,
    val amountFeathers: Long,
    val tokenSymbol: String = "SPW",
    val timestamp: Long,
    val status: String,
    val feeSpw: Double,
    val memo: String,
    val blockNumber: Long,
    val txPubkey: String? = null,
    val merkleRoot: String = "",
    val bits: String = "",
    val confirmations: Int = 0,
    val nonce: Long = 0L
)

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey val symbol: String = "SPW",
    val name: String = "Sparrow",
    val balance: Double = 0.0,
    val feathers: Long = 0L,
    val decimals: Int = 8,
    val isNative: Boolean = true,
    val network: String = "SPW Mainnet",
    val iconHexColor: Long = 0xFF00E5FF
)



@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val address: String,
    val name: String,
    val network: String = "SPW Network",
    val addedTimestamp: Long = System.currentTimeMillis()
)
