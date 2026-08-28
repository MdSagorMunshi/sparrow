package com.ryanshelby.spw.wallet.data.model

data class NetworkConfig(
    val id: String,
    val name: String,
    val rpcUrl: String,
    val coinType: Int = 1926,
    val chainId: Long = 1926L,
    val symbol: String = "SPW",
    val explorerUrl: String = "https://wallet.spw.network",
    val isTestnet: Boolean = false,
    val isDefault: Boolean = false
) {
    companion object {
        val SPW_MAINNET = NetworkConfig(
            id = "spw_mainnet",
            name = "SPW Official Mainnet",
            rpcUrl = "https://wallet.spw.network/api",
            coinType = 1926,
            chainId = 1926L,
            symbol = "SPW",
            explorerUrl = "https://wallet.spw.network",
            isTestnet = false,
            isDefault = true
        )

        val SPW_COMMUNITY = NetworkConfig(
            id = "spw_community",
            name = "SPW Global Node",
            rpcUrl = "https://spw.network/api",
            coinType = 1926,
            chainId = 1926L,
            symbol = "SPW",
            explorerUrl = "https://spw.network",
            isTestnet = false,
            isDefault = false
        )

        val SPW_TESTNET = NetworkConfig(
            id = "spw_testnet",
            name = "SPW Testnet Node",
            rpcUrl = "https://testnet.spw.network/api",
            coinType = 1926,
            chainId = 1927L,
            symbol = "tSPW",
            explorerUrl = "https://testnet.spw.network",
            isTestnet = true,
            isDefault = false
        )

        val LOCAL_NODE = NetworkConfig(
            id = "spw_local",
            name = "SPW Local Dev Node (8333)",
            rpcUrl = "http://10.0.2.2:8333",
            coinType = 1926,
            chainId = 1337L,
            symbol = "SPW",
            explorerUrl = "http://localhost:8333",
            isTestnet = true,
            isDefault = false
        )

        val DEFAULT_NETWORKS = listOf(SPW_MAINNET, SPW_COMMUNITY, SPW_TESTNET, LOCAL_NODE)
    }
}
