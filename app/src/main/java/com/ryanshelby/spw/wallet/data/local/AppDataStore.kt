package com.ryanshelby.spw.wallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "spw_app_preferences")

/**
 * Jetpack DataStore manager for app diagnostic flags, preferences, and session state.
 */
class AppDataStore(private val context: Context) {

    companion object {
        val KEY_DEBUG_MODE = booleanPreferencesKey("debug_mode_enabled")
        val KEY_SIMULATE_RPC_LATENCY = booleanPreferencesKey("simulate_rpc_latency")
        val KEY_ENFORCE_KEYSTORE_HARDWARE = booleanPreferencesKey("enforce_keystore_hardware")
        val KEY_LAST_DIAGNOSTIC_TIMESTAMP = longPreferencesKey("last_diagnostic_timestamp")
        val KEY_ENCRYPTED_SEED_BACKUP = stringPreferencesKey("encrypted_seed_backup_blob")
        val KEY_FAUCET_TESTNET_ENABLED = booleanPreferencesKey("faucet_testnet_enabled")
        val KEY_TELEMETRY_LOGS_ENABLED = booleanPreferencesKey("telemetry_logs_enabled")
    }

    val isDebugMode: Flow<Boolean> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_DEBUG_MODE] ?: true }

    val isSimulateLatency: Flow<Boolean> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_SIMULATE_RPC_LATENCY] ?: false }

    val isFaucetEnabled: Flow<Boolean> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_FAUCET_TESTNET_ENABLED] ?: true }

    val lastDiagnosticTimestamp: Flow<Long> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_LAST_DIAGNOSTIC_TIMESTAMP] ?: 0L }

    suspend fun setDebugMode(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[KEY_DEBUG_MODE] = enabled
        }
    }

    suspend fun setSimulateLatency(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[KEY_SIMULATE_RPC_LATENCY] = enabled
        }
    }

    suspend fun setFaucetEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[KEY_FAUCET_TESTNET_ENABLED] = enabled
        }
    }

    suspend fun recordDiagnosticTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.appDataStore.edit { preferences ->
            preferences[KEY_LAST_DIAGNOSTIC_TIMESTAMP] = timestamp
        }
    }

    suspend fun saveEncryptedSeedBlob(blob: String) {
        context.appDataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_SEED_BACKUP] = blob
        }
    }

    /**
     * Clears all Jetpack DataStore preferences completely.
     */
    suspend fun clearDataStore() {
        context.appDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
