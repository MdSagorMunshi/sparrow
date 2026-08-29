package com.ryanshelby.spw.wallet.data.repository

import android.content.Context
import com.ryanshelby.spw.wallet.data.local.AccountEntity
import com.ryanshelby.spw.wallet.data.local.AppDatabase
import com.ryanshelby.spw.wallet.data.local.ContactEntity
import com.ryanshelby.spw.wallet.data.local.TokenEntity
import com.ryanshelby.spw.wallet.data.local.TransactionEntity
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.CryptoAsset
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionStatus
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.data.remote.RpcClient
import com.ryanshelby.spw.wallet.data.remote.SPWApiClient
import com.ryanshelby.spw.wallet.data.remote.SpwBroadcastRequest
import com.ryanshelby.spw.wallet.data.remote.SpwTxInput
import com.ryanshelby.spw.wallet.data.remote.SpwTxOutput
import com.ryanshelby.spw.wallet.data.remote.SpwUtxo
import com.ryanshelby.spw.wallet.security.SPWAccountKeys
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.security.SPWCrypto.toHex
import com.ryanshelby.spw.wallet.security.SecurityManager
import com.ryanshelby.spw.wallet.service.PushNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.ryanshelby.spw.wallet.data.local.AppDataStore
import com.ryanshelby.spw.wallet.security.SecureKeyStorage
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.sync.Mutex

class WalletRepository(
    private val context: Context,
    private val database: AppDatabase,
    val securityManager: SecurityManager,
    val notificationService: PushNotificationService,
    private val rpcClient: RpcClient
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val walletDao = database.walletDao()
    private val apiClient: SPWApiClient = rpcClient.apiClient
    val appDataStore = AppDataStore(context)

    private val _activeNetwork = MutableStateFlow(NetworkConfig.SPW_MAINNET)
    val activeNetwork: StateFlow<NetworkConfig> = _activeNetwork.asStateFlow()

    private val _activeLanguage = MutableStateFlow(
        AppLanguage.entries.find { it.code == securityManager.getSelectedLanguageCode() } ?: AppLanguage.ENGLISH
    )
    val activeLanguage: StateFlow<AppLanguage> = _activeLanguage.asStateFlow()

    private val _liveUtxos = MutableStateFlow<List<SpwUtxo>>(emptyList())
    val liveUtxos: StateFlow<List<SpwUtxo>> = _liveUtxos.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val refreshMutex = Mutex()

    val accountsFlow: Flow<List<AccountEntity>> = walletDao.getAllAccounts()

    val nativeTokenFlow: Flow<CryptoAsset> = walletDao.getNativeToken().map { entity ->
        if (entity == null) {
            CryptoAsset(
                symbol = "SPW",
                name = "Sparrow",
                balance = 0.0,
                feathers = 0L,
                decimals = 8,
                isNative = true,
                network = _activeNetwork.value.name
            )
        } else {
            CryptoAsset(
                symbol = entity.symbol,
                name = entity.name,
                balance = entity.balance,
                feathers = entity.feathers,
                decimals = entity.decimals,
                isNative = entity.isNative,
                network = entity.network,
                iconHexColor = entity.iconHexColor
            )
        }
    }

    val tokensFlow: Flow<List<CryptoAsset>> = nativeTokenFlow.map { listOf(it) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactionsFlow: Flow<List<TransactionItem>> = accountsFlow
        .map { accounts -> accounts.find { it.isPrimary } }
        .flatMapLatest { activeAccount ->
            if (activeAccount != null) {
                walletDao.getTransactionsForAddress(activeAccount.address).map { entities ->
                    entities.map { it.toTransactionItem() }
                }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }



    val contactsFlow: Flow<List<ContactEntity>> = walletDao.getAllContacts()

    init {
        repositoryScope.launch {
            if (securityManager.hasWallet()) {
                initWalletState()
                refreshOnChainData()
                startPeriodicSync()
            }
        }
    }

    suspend fun createAndInitializeWallet(mnemonic: String, walletName: String, pin: String): Result<SPWAccountKeys> {
        return try {
            val account = SPWCrypto.createAccountFromMnemonic(mnemonic)
            securityManager.importMnemonic(mnemonic, walletName)
            securityManager.setPin(pin)

            val dbAccount = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = walletName,
                address = account.address,
                spendKeyHex = account.spendKeyHex,
                viewKeyHex = account.viewKeyHex,
                spendPubHex = account.spendPubHex,
                viewPubHex = account.viewPubHex,
                mnemonic = mnemonic,
                isPrimary = true
            )
            walletDao.insertAccount(dbAccount)

            val nativeToken = TokenEntity(
                symbol = "SPW",
                name = "Sparrow",
                balance = 0.0,
                feathers = 0L,
                decimals = 8,
                isNative = true,
                network = _activeNetwork.value.name,
                iconHexColor = 0xFF00E5FF
            )
            walletDao.insertTokens(listOf(nativeToken))

            refreshOnChainData()
            startPeriodicSync()
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importWalletFromMnemonic(mnemonic: String, walletName: String, pin: String): Result<SPWAccountKeys> {
        return try {
            val cleanMnemonic = mnemonic.trim().lowercase()
            if (!SPWCrypto.validateMnemonic(cleanMnemonic)) {
                return Result.failure(IllegalArgumentException("Invalid recovery phrase. Please check the words."))
            }
            createAndInitializeWallet(cleanMnemonic, walletName, pin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importWalletFromPrivateKey(privateKeyHex: String, walletName: String, pin: String): Result<SPWAccountKeys> {
        return try {
            val cleanKey = privateKeyHex.trim()
            val account = SPWCrypto.createAccountFromPrivateKey(cleanKey)
            securityManager.importPrivateKey(cleanKey, name = walletName)
            securityManager.setPin(pin)

            val dbAccount = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = walletName,
                address = account.address,
                spendKeyHex = account.spendKeyHex,
                viewKeyHex = account.viewKeyHex,
                spendPubHex = account.spendPubHex,
                viewPubHex = account.viewPubHex,
                mnemonic = null,
                isPrimary = true
            )
            walletDao.insertAccount(dbAccount)

            val nativeToken = TokenEntity(
                symbol = "SPW",
                name = "Sparrow",
                balance = 0.0,
                feathers = 0L,
                decimals = 8,
                isNative = true,
                network = _activeNetwork.value.name,
                iconHexColor = 0xFF00E5FF
            )
            walletDao.insertTokens(listOf(nativeToken))

            refreshOnChainData()
            startPeriodicSync()
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetWalletData() {
        // Clear Room database
        walletDao.clearAccounts()
        walletDao.clearTransactions()
        walletDao.clearTokens()
        walletDao.clearContacts()

        // Clear DataStore preferences
        appDataStore.clearDataStore()

        // Clear Keystore hardware encryption keys
        SecureKeyStorage.clearAllSecureKeys(context)

        // Clear SharedPreferences
        securityManager.deleteWallet()

        _liveUtxos.value = emptyList()
    }

    /**
     * Diagnostic clear wallet action for testing:
     * Cleans DataStore, Room database tables, and Android Keystore hardware-backed keys.
     */
    suspend fun clearWalletDiagnostic(): Result<Unit> {
        return try {
            resetWalletData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun initWalletState() {
        val myAddress = securityManager.getWalletAddress()
        val mySpendKey = securityManager.getSpendKeyHex()
        val myViewKey = securityManager.getViewKeyHex()
        val mySpendPub = securityManager.getSpendPubHex()
        val myViewPub = securityManager.getViewPubHex()
        val myMnemonic = securityManager.getSeedPhrase().ifEmpty { null }

        // Ensure default account in Room DB
        val existingAccount = walletDao.getAccountByAddress(myAddress)
        if (existingAccount == null) {
            val account = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = securityManager.getWalletName(),
                address = myAddress,
                spendKeyHex = mySpendKey,
                viewKeyHex = myViewKey,
                spendPubHex = mySpendPub,
                viewPubHex = myViewPub,
                mnemonic = myMnemonic,
                isPrimary = true
            )
            walletDao.insertAccount(account)
        }

        // Initialize single native SPW token entity with 0 initial balance until on-chain sync
        val nativeToken = TokenEntity(
            symbol = "SPW",
            name = "Sparrow",
            balance = 0.0,
            feathers = 0L,
            decimals = 8,
            isNative = true,
            network = _activeNetwork.value.name,
            iconHexColor = 0xFF00E5FF
        )
        walletDao.insertTokens(listOf(nativeToken))
    }

    suspend fun refreshOnChainData() {
        if (!refreshMutex.tryLock()) return
        try {
            _isRefreshing.value = true
            val address = securityManager.getWalletAddress()
        if (address.isEmpty() || !SPWCrypto.isValidSpwAddress(address)) {
            _isRefreshing.value = false
            return
        }

        apiClient.setNodeUrl(_activeNetwork.value.rpcUrl)

        // 1. Fetch live balance from SPW node
        val balanceResult = apiClient.getBalance(address)
        if (balanceResult.isSuccess) {
            val bal = balanceResult.getOrNull()
            if (bal != null) {
                // Prevent node's outdated balance from overwriting our locally deducted balance
                val pendingSends = walletDao.getAllTransactionsSync().filter {
                    (it.type == "SEND" || it.type == "STEALTH") && it.status == "PENDING"
                }
                
                if (pendingSends.isEmpty()) {
                    walletDao.updateNativeBalance(bal.balanceSpw, bal.balanceFeathers)
                }
            }
        }

        // 2. Fetch live UTXOs
        val utxosResult = apiClient.getUtxos(address)
        if (utxosResult.isSuccess) {
            _liveUtxos.value = utxosResult.getOrNull() ?: emptyList()
        }

        // 3. Fetch on-chain Explorer history
        val explorerResult = apiClient.getExplorer(address)
        if (explorerResult.isSuccess) {
            val exp = explorerResult.getOrNull()
            if (exp != null && exp.transactions.isNotEmpty()) {
                val existingTxHashes = walletDao.getAllTransactionsSync().map { it.txHash }.toSet()

                val dbTxs = exp.transactions.map { tx ->
                    val isIncoming = tx.outputs.any { it.address == address }
                    val outputForMe = tx.outputs.firstOrNull { it.address == address }
                    val amountFeathers = if (isIncoming) {
                        outputForMe?.amount ?: 0L
                    } else {
                        tx.outputs.filter { it.address != address }.sumOf { it.amount }
                    }
                    val amountSpw = amountFeathers.toDouble() / SPWCrypto.FEATHERS_PER_SPW

                    TransactionEntity(
                        txHash = tx.txid,
                        type = if (tx.txPubkey != null && tx.txPubkey.isNotEmpty()) "STEALTH" else if (isIncoming) "RECEIVE" else "SEND",
                        fromAddress = tx.inputs.firstOrNull()?.pubkey?.let { pk ->
                            try { SPWCrypto.pubkeyToAddress(SPWCrypto.hexToBytes(pk)) } catch (e: Exception) { pk }
                        } ?: "Network",
                        toAddress = tx.outputs.firstOrNull()?.address ?: address,
                        amountSpw = amountSpw,
                        amountFeathers = amountFeathers,
                        tokenSymbol = "SPW",
                        timestamp = if (tx.timestamp > 0) tx.timestamp * 1000L else System.currentTimeMillis(),
                        status = "CONFIRMED",
                        feeSpw = 0.0001,
                        memo = if (tx.txPubkey != null && tx.txPubkey.isNotEmpty()) "Stealth Shielded Transfer" else "SPW On-Chain Transfer",
                        blockNumber = tx.blockHeight ?: 1L,
                        txPubkey = tx.txPubkey,
                        merkleRoot = SPWCrypto.sha256Hex("merkle:${tx.txid}"),
                        bits = "1a0${tx.txid.take(5)}",
                        confirmations = ((tx.blockHeight ?: 1L) % 500 + 6).toInt(),
                        nonce = tx.txid.take(8).toLongOrNull(16) ?: 0L
                    )
                }
                walletDao.insertTransactions(dbTxs)
                
                val activeAccount = walletDao.getAccountByAddress(address)
                val cutoffTime = (activeAccount?.createdAt ?: System.currentTimeMillis()) - 60000L // 1 minute buffer for edge cases

                dbTxs.forEach { tx ->
                    if (!existingTxHashes.contains(tx.txHash) && tx.toAddress == address && tx.fromAddress != address) {
                        if (tx.timestamp >= cutoffTime) {
                            notificationService.showIncomingTransferNotification(
                                amount = tx.amountSpw,
                                symbol = tx.tokenSymbol,
                                fromAddress = tx.fromAddress
                            )
                        }
                    }
                }
            }
        }

        } finally {
            _isRefreshing.value = false
            refreshMutex.unlock()
        }
    }

    private fun startPeriodicSync() {
        repositoryScope.launch {
            while (true) {
                delay(2000) // Poll blockchain every 2s
                refreshOnChainData()
            }
        }
    }

    fun setActiveNetwork(network: NetworkConfig) {
        _activeNetwork.value = network
        securityManager.setActiveNetworkId(network.id)
        apiClient.setNodeUrl(network.rpcUrl)
        repositoryScope.launch {
            refreshOnChainData()
        }
    }

    fun setActiveLanguage(language: AppLanguage) {
        _activeLanguage.value = language
        securityManager.setSelectedLanguageCode(language.code)
    }

    // ── Real SPW Transaction Building, Signing & Broadcasting ─────────────

    suspend fun sendTransfer(
        tokenSymbol: String,
        toAddress: String,
        amount: Double,
        gasFee: Double = 0.0001,
        memo: String = "",
        isStealth: Boolean = false,
        recipientViewPubHex: String? = null
    ): Result<String> {
        val myAddress = securityManager.getWalletAddress()
        val spendKeyHex = securityManager.getSpendKeyHex()
        val spendPubHex = securityManager.getSpendPubHex()

        if (spendKeyHex.isEmpty()) {
            return Result.failure(Exception("Wallet private key not available. Please unlock or import wallet."))
        }

        val amountFeathers = (amount * SPWCrypto.FEATHERS_PER_SPW).toLong()
        val feeFeathers = (gasFee * SPWCrypto.FEATHERS_PER_SPW).toLong().coerceAtLeast(10000L)
        val neededFeathers = amountFeathers + feeFeathers

        // Use cached UTXOs first (updated every 2s by periodic sync), try fresh fetch as enhancement
        apiClient.setNodeUrl(_activeNetwork.value.rpcUrl)
        val availableUtxos = if (_liveUtxos.value.isNotEmpty()) {
            // We already have cached UTXOs from periodic sync — use them immediately
            _liveUtxos.value
        } else {
            // No cached UTXOs yet, must fetch
            val utxoResult = apiClient.getUtxos(myAddress)
            utxoResult.getOrNull() ?: emptyList()
        }

        var selectedTotal = 0L
        val selectedUtxos = mutableListOf<SpwUtxo>()

        for (u in availableUtxos) {
            selectedUtxos.add(u)
            selectedTotal += u.amount
            if (selectedTotal >= neededFeathers) break
        }

        if (selectedTotal < neededFeathers) {
            return Result.failure(Exception("Insufficient balance. Need ${(neededFeathers.toDouble() / 1e8)} SPW, available: ${(selectedTotal.toDouble() / 1e8)} SPW"))
        }

        // Build Inputs
        val inputs = if (selectedUtxos.isNotEmpty()) {
            selectedUtxos.map { u ->
                com.ryanshelby.spw.wallet.security.TxInputData(
                    prevTxid = u.txid,
                    prevVout = u.vout,
                    pubkey = spendPubHex,
                    scriptSig = ""
                )
            }
        } else {
            listOf(
                com.ryanshelby.spw.wallet.security.TxInputData(
                    prevTxid = "0".repeat(64),
                    prevVout = 0,
                    pubkey = spendPubHex,
                    scriptSig = ""
                )
            )
        }

        // Handle Stealth / Standard Outputs
        val outputs = mutableListOf<com.ryanshelby.spw.wallet.security.TxOutputData>()
        var txPubkeyHex = ""

        if (isStealth && recipientViewPubHex != null && recipientViewPubHex.isNotBlank()) {
            val stealthOutput = SPWCrypto.makeStealthOutput(
                recipientSpendPubHex = toAddress,
                recipientViewPubHex = recipientViewPubHex
            )
            outputs.add(com.ryanshelby.spw.wallet.security.TxOutputData(address = stealthOutput.oneTimeAddress, amount = amountFeathers))
            txPubkeyHex = stealthOutput.txPubkeyHex
        } else {
            outputs.add(com.ryanshelby.spw.wallet.security.TxOutputData(address = toAddress, amount = amountFeathers))
        }

        // Memo output if present (OP_RETURN standard)
        if (memo.isNotBlank()) {
            val memoBytes = memo.toByteArray(StandardCharsets.UTF_8)
            if (memoBytes.size <= 80) {
                outputs.add(com.ryanshelby.spw.wallet.security.TxOutputData(address = "", amount = 0L, data = memoBytes.toHex()))
            }
        }

        // Change output
        val changeFeathers = if (selectedTotal >= neededFeathers) selectedTotal - neededFeathers else 0L
        if (changeFeathers > 0) {
            outputs.add(com.ryanshelby.spw.wallet.security.TxOutputData(address = myAddress, amount = changeFeathers))
        }

        val timestamp = System.currentTimeMillis() / 1000L

        // 1. Calculate Canonical Signing Digest
        val digest = SPWCrypto.computeSigningDigest(
            inputs = inputs,
            outputs = outputs,
            timestamp = timestamp,
            txPubkey = txPubkeyHex
        )

        // 2. Sign with SECP256k1 spend private key (DER ECDSA)
        val spendKeyBytes = SPWCrypto.hexToBytes(spendKeyHex)
        val sigHex = SPWCrypto.signDigest(digest, spendKeyBytes)

        // 3. Attach DER Signature to inputs
        val signedInputs = inputs.map { it.copy(scriptSig = sigHex) }

        // 4. Compute exact on-chain TXID
        val txid = SPWCrypto.computeTxid(
            signedInputs = signedInputs,
            outputs = outputs,
            timestamp = timestamp,
            coinbaseData = "",
            txPubkey = txPubkeyHex
        )

        // 5. Broadcast to SPW Network Node
        val broadcastRequest = SpwBroadcastRequest(
            txid = txid,
            inputs = signedInputs.map { SpwTxInput(it.prevTxid, it.prevVout, it.pubkey, it.scriptSig) },
            outputs = outputs.map { SpwTxOutput(it.address, it.amount, it.data) },
            timestamp = timestamp,
            coinbaseData = "",
            txPubkey = txPubkeyHex,
            colorIssue = ""
        )

        val broadcastResult = apiClient.broadcastTransaction(broadcastRequest)
        val isSuccess = broadcastResult.isSuccess
        val finalTxHash = if (isSuccess) broadcastResult.getOrNull() ?: txid else txid

        val txEntity = TransactionEntity(
            txHash = finalTxHash,
            type = if (isStealth) "STEALTH" else "SEND",
            fromAddress = myAddress,
            toAddress = toAddress,
            amountSpw = amount,
            amountFeathers = amountFeathers,
            tokenSymbol = "SPW",
            timestamp = System.currentTimeMillis(),
            status = if (isSuccess) "PENDING" else "FAILED",
            feeSpw = gasFee,
            memo = memo.ifEmpty { if (isStealth) "Stealth Shielded Transfer" else "SPW Direct Transfer" },
            blockNumber = 0L,
            txPubkey = txPubkeyHex.ifEmpty { null },
            merkleRoot = SPWCrypto.sha256Hex("merkle:$finalTxHash"),
            bits = "1a0${finalTxHash.take(5)}",
            confirmations = 0,
            nonce = finalTxHash.take(8).toLongOrNull(16) ?: 0L
        )
        walletDao.insertTransaction(txEntity)

        if (isSuccess) {
            val currentToken = walletDao.getNativeTokenSync()
            if (currentToken != null) {
                val newFeathers = currentToken.feathers - neededFeathers
                val newSpw = newFeathers.toDouble() / SPWCrypto.FEATHERS_PER_SPW
                walletDao.updateNativeBalance(newSpw, newFeathers)
            }

            // Trigger notification
            notificationService.showOutgoingTransferNotification(
                amount = amount,
                symbol = "SPW",
                toAddress = toAddress,
                txHash = finalTxHash
            )

            // Refresh balance quickly
            repositoryScope.launch {
                delay(500)
                refreshOnChainData()
            }
            return Result.success(finalTxHash)
        } else {
            val errorMsg = broadcastResult.exceptionOrNull()?.message ?: "Unknown error"
            return Result.failure(Exception("Transaction rejected: $errorMsg"))
        }
    }

    suspend fun createNewAccount(name: String): SPWAccountKeys {
        val keys = securityManager.generateNewWallet(name)
        val entity = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            address = keys.address,
            spendKeyHex = keys.spendKeyHex,
            viewKeyHex = keys.viewKeyHex,
            spendPubHex = keys.spendPubHex,
            viewPubHex = keys.viewPubHex,
            mnemonic = keys.mnemonic,
            isPrimary = false
        )
        walletDao.insertAccount(entity)
        return keys
    }

    suspend fun importAccountByMnemonic(mnemonic: String, name: String): Result<SPWAccountKeys> {
        return try {
            if (!SPWCrypto.validateMnemonic(mnemonic)) {
                return Result.failure(Exception("Invalid BIP39 mnemonic phrase."))
            }
            val keys = securityManager.importMnemonic(mnemonic, name)
            val entity = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                address = keys.address,
                spendKeyHex = keys.spendKeyHex,
                viewKeyHex = keys.viewKeyHex,
                spendPubHex = keys.spendPubHex,
                viewPubHex = keys.viewPubHex,
                mnemonic = keys.mnemonic,
                isPrimary = true
            )
            walletDao.insertAccount(entity)
            switchActiveAccount(entity)
            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importAccountByPrivateKey(spendKeyHex: String, viewKeyHex: String? = null, name: String): Result<SPWAccountKeys> {
        return try {
            val keys = securityManager.importPrivateKey(spendKeyHex, viewKeyHex, name)
            val entity = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                address = keys.address,
                spendKeyHex = keys.spendKeyHex,
                viewKeyHex = keys.viewKeyHex,
                spendPubHex = keys.spendPubHex,
                viewPubHex = keys.viewPubHex,
                mnemonic = null,
                isPrimary = true
            )
            walletDao.insertAccount(entity)
            switchActiveAccount(entity)
            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun switchActiveAccount(account: AccountEntity) {
        // Clear previous account state from DB
        walletDao.clearTransactions()
        walletDao.clearTokens()

        securityManager.setWalletName(account.name)
        if (account.mnemonic != null) {
            securityManager.importMnemonic(account.mnemonic, account.name)
        } else {
            securityManager.importPrivateKey(account.spendKeyHex, account.viewKeyHex, account.name)
        }
        walletDao.setActiveAccount(account.id)
        
        // Re-initialize a blank native token for the new active account
        val nativeToken = TokenEntity(
            symbol = "SPW",
            name = "Sparrow",
            balance = 0.0,
            feathers = 0L,
            decimals = 8,
            isNative = true,
            network = _activeNetwork.value.name,
            iconHexColor = 0xFF00E5FF
        )
        walletDao.insertTokens(listOf(nativeToken))
        
        refreshOnChainData()
    }



    suspend fun saveContact(name: String, address: String) {
        walletDao.insertContact(ContactEntity(address, name, _activeNetwork.value.name))
    }

    suspend fun deleteContact(address: String) {
        walletDao.deleteContact(address)
    }

    fun signDappMessage(app: String, nonce: String): String {
        val address = securityManager.getWalletAddress()
        val spendKey = securityManager.getSpendKeyHex()
        return SPWCrypto.signConnectMessage(app, address, nonce, spendKey)
    }

    private fun TransactionEntity.toTransactionItem(): TransactionItem = TransactionItem(
        txHash = txHash,
        type = when (type) {
            "SEND" -> TransactionType.SEND
            "RECEIVE" -> TransactionType.RECEIVE
            "STEALTH" -> TransactionType.STEALTH
            else -> TransactionType.SEND // Fallback
        },
        fromAddress = fromAddress,
        toAddress = toAddress,
        amountSpw = amountSpw,
        amountFeathers = amountFeathers,
        tokenSymbol = "SPW",
        timestamp = timestamp,
        status = when (status) {
            "CONFIRMED" -> TransactionStatus.CONFIRMED
            "PENDING" -> TransactionStatus.PENDING
            else -> TransactionStatus.FAILED
        },
        feeSpw = feeSpw,
        memo = memo,
        blockNumber = blockNumber,
        txPubkey = txPubkey,
        merkleRoot = merkleRoot,
        bits = bits,
        confirmations = confirmations,
        nonce = nonce
    )


}
