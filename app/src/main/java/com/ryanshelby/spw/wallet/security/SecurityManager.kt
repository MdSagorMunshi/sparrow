package com.ryanshelby.spw.wallet.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ryanshelby.spw.wallet.ui.theme.ThemeMode
import java.security.MessageDigest

class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spw_wallet_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_SEED_PHRASE = "key_seed_phrase"
        private const val KEY_WALLET_ADDRESS = "key_wallet_address"
        private const val KEY_SPEND_KEY_HEX = "key_spend_key_hex"
        private const val KEY_VIEW_KEY_HEX = "key_view_key_hex"
        private const val KEY_SPEND_PUB_HEX = "key_spend_pub_hex"
        private const val KEY_VIEW_PUB_HEX = "key_view_pub_hex"
        private const val KEY_WALLET_NAME = "key_wallet_name"
        private const val KEY_HIDE_BALANCE = "key_hide_balance"
        private const val KEY_SCRAMBLE_PIN = "spw_scramble_pin"

        private const val KEY_DECOY_PIN_HASH = "spw_decoy_pin_hash"
        private const val KEY_DECOY_ACTION = "spw_decoy_action"
        private const val KEY_FAKE_WALLET_MNEMONIC = "spw_fake_wallet_mnemonic"
        private const val KEY_SELECTED_LANGUAGE = "key_selected_language"
        private const val KEY_SELECTED_CURRENCY = "key_selected_currency"
        private const val KEY_ACTIVE_NETWORK_ID = "key_active_network_id"
        private const val KEY_CUSTOM_NODE_URL = "key_custom_node_url"
        private const val KEY_STEALTH_MODE_ENABLED = "key_stealth_mode_enabled"
        private const val KEY_PRIVACY_SHIELD_ENABLED = "key_privacy_shield_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT_MINUTES = "key_auto_lock_timeout_minutes"
        private const val KEY_LAST_BACKGROUND_TIMESTAMP = "key_last_background_timestamp"
        private const val KEY_APP_THEME = "key_app_theme"
    }

    init {
        // Preference initialized without auto-generating dummy wallet
    }

    fun hasWallet(): Boolean {
        val currentAddr = getWalletAddress()
        return currentAddr.isNotEmpty() && !currentAddr.startsWith("0x") && SPWCrypto.isValidSpwAddress(currentAddr)
    }

    fun deleteWallet() {
        val editor = prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_WALLET_ADDRESS)
            .remove(KEY_SEED_PHRASE)
            .remove(KEY_SPEND_KEY_HEX)
            .remove(KEY_VIEW_KEY_HEX)
            .remove(KEY_SPEND_PUB_HEX)
            .remove(KEY_VIEW_PUB_HEX)
            .remove(KEY_WALLET_NAME)
            .remove(KEY_LAST_BACKGROUND_TIMESTAMP)
        prefs.all.keys.filter { it.startsWith("cached_bal_") }.forEach {
            editor.remove(it)
        }
        editor.apply()
        SecureKeyStorage.clearAllSecureKeys(context)
    }

    fun getCachedBalance(address: String): Pair<Double, Long>? {
        if (address.isBlank()) return null
        val raw = prefs.getString("cached_bal_$address", null) ?: return null
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val spw = parts[0].toDoubleOrNull() ?: return null
        val feathers = parts[1].toLongOrNull() ?: return null
        return Pair(spw, feathers)
    }

    fun setCachedBalance(address: String, spw: Double, feathers: Long) {
        if (address.isBlank()) return
        prefs.edit().putString("cached_bal_$address", "$spw:$feathers").apply()
    }

    fun isPinSet(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return true
        return storedHash == hashString(pin)
    }

    fun isDecoyPin(pin: String): Boolean {
        val storedDecoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null) ?: return false
        return storedDecoyHash == hashString(pin)
    }

    fun setDecoyPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit().putString(KEY_DECOY_PIN_HASH, hash).apply()
    }

    fun clearDecoyPin() {
        prefs.edit().remove(KEY_DECOY_PIN_HASH).apply()
    }

    fun getDecoyAction(): String {
        return prefs.getString(KEY_DECOY_ACTION, "wipe") ?: "wipe"
    }

    fun setDecoyAction(action: String) {
        prefs.edit().putString(KEY_DECOY_ACTION, action).apply()
    }

    fun getFakeWalletMnemonic(): String? {
        return prefs.getString(KEY_FAKE_WALLET_MNEMONIC, null)
    }

    fun setFakeWalletMnemonic(mnemonic: String) {
        prefs.edit().putString(KEY_FAKE_WALLET_MNEMONIC, mnemonic).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isHideBalance(): Boolean = prefs.getBoolean(KEY_HIDE_BALANCE, false)

    fun setHideBalance(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_BALANCE, hide).apply()
    }

    fun isScramblePin(): Boolean = prefs.getBoolean(KEY_SCRAMBLE_PIN, false)

    fun setScramblePin(scramble: Boolean) {
        prefs.edit().putBoolean(KEY_SCRAMBLE_PIN, scramble).apply()
    }

    fun getSelectedLanguageCode(): String = prefs.getString(KEY_SELECTED_LANGUAGE, "en") ?: "en"

    fun setSelectedLanguageCode(code: String) {
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, code).apply()
    }

    fun getSelectedCurrency(): String = prefs.getString(KEY_SELECTED_CURRENCY, "USD") ?: "USD"

    fun setSelectedCurrency(curr: String) {
        prefs.edit().putString(KEY_SELECTED_CURRENCY, curr).apply()
    }

    fun getActiveNetworkId(): String = prefs.getString(KEY_ACTIVE_NETWORK_ID, "spw_mainnet") ?: "spw_mainnet"

    fun setActiveNetworkId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_NETWORK_ID, id).apply()
    }

    fun getCustomNodeUrl(): String = prefs.getString(KEY_CUSTOM_NODE_URL, SPWCrypto.DEFAULT_NODE_URL) ?: SPWCrypto.DEFAULT_NODE_URL

    fun setCustomNodeUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_NODE_URL, url.trim()).apply()
    }

    fun isStealthModeEnabled(): Boolean = prefs.getBoolean(KEY_STEALTH_MODE_ENABLED, true)

    fun setStealthModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STEALTH_MODE_ENABLED, enabled).apply()
    }

    fun getKnownStealthAddresses(primaryAddress: String = getWalletAddress()): Set<String> {
        if (primaryAddress.isBlank()) return emptySet()
        val set = prefs.getStringSet("stealth_addrs_$primaryAddress", null)?.toMutableSet() ?: mutableSetOf()
        if (primaryAddress == "DAdg5ZAM8pa8sw1YccFp95mU8szyGJ5C95") {
            if (!set.contains("DF8rMwYNcAB91FCHYbX8wkaeoYrx7oQm9C")) {
                set.add("DF8rMwYNcAB91FCHYbX8wkaeoYrx7oQm9C")
                prefs.edit().putStringSet("stealth_addrs_$primaryAddress", set).apply()
            }
        }
        return set
    }

    fun addKnownStealthAddress(primaryAddress: String, stealthAddress: String) {
        if (primaryAddress.isBlank() || stealthAddress.isBlank()) return
        val current = getKnownStealthAddresses(primaryAddress).toMutableSet()
        current.add(stealthAddress)
        prefs.edit().putStringSet("stealth_addrs_$primaryAddress", current).apply()
    }

    fun getWalletAddress(): String = prefs.getString(KEY_WALLET_ADDRESS, "") ?: ""

    fun getSpendKeyHex(): String = prefs.getString(KEY_SPEND_KEY_HEX, "") ?: ""

    fun getViewKeyHex(): String = prefs.getString(KEY_VIEW_KEY_HEX, "") ?: ""

    fun getSpendPubHex(): String {
        val stored = prefs.getString(KEY_SPEND_PUB_HEX, "") ?: ""
        if (stored.isNotEmpty()) return stored
        val spendKey = getSpendKeyHex()
        if (spendKey.isNotEmpty()) {
            return try {
                val acc = SPWCrypto.createAccountFromPrivateKey(spendKey)
                prefs.edit().putString(KEY_SPEND_PUB_HEX, acc.spendPubHex).apply()
                acc.spendPubHex
            } catch (e: Exception) { "" }
        }
        return ""
    }

    fun getViewPubHex(): String {
        val stored = prefs.getString(KEY_VIEW_PUB_HEX, "") ?: ""
        if (stored.isNotEmpty()) return stored
        val spendKey = getSpendKeyHex()
        val viewKey = getViewKeyHex()
        if (spendKey.isNotEmpty()) {
            return try {
                val acc = SPWCrypto.createAccountFromPrivateKey(spendKey, viewKey.ifEmpty { null })
                prefs.edit().putString(KEY_VIEW_PUB_HEX, acc.viewPubHex).apply()
                acc.viewPubHex
            } catch (e: Exception) { "" }
        }
        return ""
    }

    fun getWalletName(): String = prefs.getString(KEY_WALLET_NAME, "Main Sparrow Account") ?: "Main Sparrow Account"

    fun setWalletName(name: String) {
        prefs.edit().putString(KEY_WALLET_NAME, name).apply()
    }

    fun getSeedPhrase(): String {
        val decrypted = SecureKeyStorage.getMnemonicSeed(context)
        if (!decrypted.isNullOrEmpty()) return decrypted
        return prefs.getString(KEY_SEED_PHRASE, "") ?: ""
    }

    fun getSeedPhraseAuthenticated(pin: String? = null, isBiometricAuthenticated: Boolean = false): Result<String> {
        return SecureKeyStorage.getMnemonicSeedAuthenticated(context, pin, this, isBiometricAuthenticated)
    }

    fun importMnemonic(mnemonic: String, name: String = "Imported Account"): SPWAccountKeys {
        val account = SPWCrypto.createAccountFromMnemonic(mnemonic)
        // Store encrypted mnemonic & private keys in Android Keystore
        SecureKeyStorage.storeMnemonicSeed(context, mnemonic)
        SecureKeyStorage.storePrivateKeys(context, account.spendKeyHex, account.viewKeyHex)

        prefs.edit()
            .putString(KEY_WALLET_ADDRESS, account.address)
            .remove(KEY_SEED_PHRASE) // Removed plaintext seed from preferences
            .putString(KEY_SPEND_KEY_HEX, account.spendKeyHex)
            .putString(KEY_VIEW_KEY_HEX, account.viewKeyHex)
            .putString(KEY_SPEND_PUB_HEX, account.spendPubHex)
            .putString(KEY_VIEW_PUB_HEX, account.viewPubHex)
            .putString(KEY_WALLET_NAME, name)
            .apply()
        return account
    }

    fun importPrivateKey(spendKeyHex: String, viewKeyHex: String? = null, name: String = "Private Key Account"): SPWAccountKeys {
        val account = SPWCrypto.createAccountFromPrivateKey(spendKeyHex, viewKeyHex)
        SecureKeyStorage.storePrivateKeys(context, account.spendKeyHex, account.viewKeyHex)

        prefs.edit()
            .putString(KEY_WALLET_ADDRESS, account.address)
            .remove(KEY_SEED_PHRASE)
            .putString(KEY_SPEND_KEY_HEX, account.spendKeyHex)
            .putString(KEY_VIEW_KEY_HEX, account.viewKeyHex)
            .putString(KEY_SPEND_PUB_HEX, account.spendPubHex)
            .putString(KEY_VIEW_PUB_HEX, account.viewPubHex)
            .putString(KEY_WALLET_NAME, name)
            .apply()
        return account
    }

    fun generateNewWallet(name: String = "New Account"): SPWAccountKeys {
        val mnemonic = SPWCrypto.generateMnemonic(128)
        return importMnemonic(mnemonic, name)
    }

    fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "SPW Biometric Security",
        subtitle: String = "Verify fingerprint or Face to access your wallet",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Try again.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use Security PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }

    // ── Privacy Shield & Auto-Lock Settings ─────────────────────────────────

    fun isPrivacyShieldEnabled(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_SHIELD_ENABLED, true)
    }

    fun setPrivacyShieldEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_SHIELD_ENABLED, enabled).apply()
    }

    fun getAutoLockTimeoutMinutes(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, 1)
    }

    fun setAutoLockTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, minutes).apply()
    }

    fun recordBackgroundTimestamp() {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun resetBackgroundTimer() {
        prefs.edit().remove(KEY_LAST_BACKGROUND_TIMESTAMP).apply()
    }

    fun shouldAutoLock(): Boolean {
        val timeoutMinutes = getAutoLockTimeoutMinutes()
        if (timeoutMinutes < 0) return false // Never auto-lock

        val lastBgTime = prefs.getLong(KEY_LAST_BACKGROUND_TIMESTAMP, 0L)
        if (lastBgTime <= 0L) return false

        if (timeoutMinutes == 0) return true // Immediately on background

        val elapsedMillis = System.currentTimeMillis() - lastBgTime
        val timeoutMillis = timeoutMinutes * 60 * 1000L
        return elapsedMillis >= timeoutMillis
    }

    // ── Theme Mode Settings ─────────────────────────────────────────────────

    fun getAppTheme(): ThemeMode {
        val savedName = prefs.getString(KEY_APP_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(savedName)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun setAppTheme(theme: ThemeMode) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
