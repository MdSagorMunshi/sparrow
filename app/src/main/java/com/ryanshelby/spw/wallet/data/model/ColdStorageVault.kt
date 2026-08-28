package com.ryanshelby.spw.wallet.data.model

data class ColdStorageVault(
    val vaultId: String,
    val name: String,
    val address: String,
    val derivationPath: String = "m/44'/1926'/0'/0/0",
    val isAirGapped: Boolean = true,
    val totalBalanceSpw: Double = 0.0,
    val totalBalanceFeathers: Long = 0L,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val publicDescriptor: String = "wpkh([spw_vault/44h/1926h]xpub...)",
    val multiSigThreshold: String = "1-of-1 AirGapped"
)

data class UnsignedQrPayload(
    val version: Int = 1,
    val txHashPreview: String,
    val from: String,
    val to: String,
    val amountSpw: Double,
    val amountFeathers: Long,
    val feeFeathers: Long,
    val timestamp: Long,
    val rawPayload: String
)
