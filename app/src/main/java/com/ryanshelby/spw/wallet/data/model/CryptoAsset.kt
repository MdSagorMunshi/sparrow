package com.ryanshelby.spw.wallet.data.model

data class CryptoAsset(
    val symbol: String = "SPW",
    val name: String = "Sparrow",
    val balance: Double = 0.0,
    val feathers: Long = 0L,
    val decimals: Int = 8,
    val isNative: Boolean = true,
    val network: String = "SPW Mainnet",
    val iconHexColor: Long = 0xFF00E5FF
)
