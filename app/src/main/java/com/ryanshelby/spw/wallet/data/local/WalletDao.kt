package com.ryanshelby.spw.wallet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    // ── Accounts ────────────────────────────────────────────────────────
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE address = :address LIMIT 1")
    suspend fun getAccountByAddress(address: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("UPDATE accounts SET isPrimary = (id = :activeId)")
    suspend fun setActiveAccount(activeId: String)

    @Query("UPDATE accounts SET name = :newName WHERE id = :id")
    suspend fun renameAccount(id: String, newName: String)

    // ── Transactions ───────────────────────────────────────────────────
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE fromAddress = :address OR toAddress = :address ORDER BY timestamp DESC")
    fun getTransactionsForAddress(address: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // ── Tokens ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM tokens WHERE symbol = 'SPW' LIMIT 1")
    fun getNativeToken(): Flow<TokenEntity?>

    @Query("SELECT * FROM tokens WHERE symbol = 'SPW' LIMIT 1")
    suspend fun getNativeTokenSync(): TokenEntity?

    @Query("SELECT * FROM tokens")
    fun getAllTokens(): Flow<List<TokenEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<TokenEntity>)

    @Query("UPDATE tokens SET balance = :balance, feathers = :feathers WHERE symbol = 'SPW'")
    suspend fun updateNativeBalance(balance: Double, feathers: Long)

    @Query("DELETE FROM tokens")
    suspend fun clearTokens()

    // ── Contacts ────────────────────────────────────────────────────────
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE address = :address")
    suspend fun deleteContact(address: String)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()
}
