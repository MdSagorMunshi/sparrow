package com.ryanshelby.spw.wallet.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ryanshelby.spw.wallet.ui.theme.ThemeMode
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spw_wallet_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_PIN_FAILED_ATTEMPTS = "key_pin_failed_attempts"
        private const val KEY_PIN_LOCKOUT_UNTIL = "key_pin_lockout_until"
        private const val KEY_PIN_RATE_LIMIT_MAC = "key_pin_rate_limit_mac"
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
        private const val KEY_DECOY_PIN_SALT = "spw_decoy_pin_salt"
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

        private const val PIN_PEPPER_KEY_ALIAS = "spw_pin_pepper_v2"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val HASH_KEY_LENGTH_BITS = 256
        private const val TAG = "SecurityManager"
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
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_FAILED_ATTEMPTS)
            .remove(KEY_PIN_LOCKOUT_UNTIL)
            .remove(KEY_PIN_RATE_LIMIT_MAC)
            .remove(KEY_DECOY_PIN_HASH)
            .remove(KEY_DECOY_PIN_SALT)
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
        editor.commit()
        SecureKeyStorage.clearAllSecureKeys(context)
        resetFailedPinAttempts()
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

    fun setPin(pin: CharArray) {
        val salt = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val hashBytes: ByteArray
        try {
            hashBytes = derivePinHash(pin, salt)
        } finally {
            pin.fill('\u0000')
        }

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hashBytes, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hashBase64)
            .commit()
        resetFailedPinAttempts()
    }

    fun setPin(pin: String) {
        val chars = pin.toCharArray()
        try {
            setPin(chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    fun verifyPin(pin: CharArray): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: run {
            pin.fill('\u0000')
            return true
        }

        if (isLockedOut()) {
            pin.fill('\u0000')
            return false
        }

        val storedSalt = prefs.getString(KEY_PIN_SALT, null)
        if (storedSalt == null && storedHash.length == 64 && storedHash.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            // Legacy migration: verify against old SHA-256 and upgrade automatically
            val pinClone = pin.clone()
            val legacyMatch = verifyLegacySha256(pin, storedHash)
            if (legacyMatch) {
                setPin(pinClone)
                pin.fill('\u0000')
                resetFailedPinAttempts()
                return true
            } else {
                pinClone.fill('\u0000')
                pin.fill('\u0000')
                recordFailedPinAttempt()
                return false
            }
        }

        if (storedSalt == null) {
            pin.fill('\u0000')
            recordFailedPinAttempt()
            return false
        }

        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val expectedHash = Base64.decode(storedHash, Base64.NO_WRAP)
        val derivedHash: ByteArray
        try {
            derivedHash = derivePinHash(pin, salt)
        } finally {
            pin.fill('\u0000')
        }

        val isValid = MessageDigest.isEqual(expectedHash, derivedHash)
        if (isValid) {
            resetFailedPinAttempts()
            return true
        } else {
            recordFailedPinAttempt()
            return false
        }
    }

    fun verifyPin(pin: String): Boolean {
        val chars = pin.toCharArray()
        try {
            return verifyPin(chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    fun isDecoyPin(pin: CharArray): Boolean {
        val storedDecoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null) ?: return false
        val storedDecoySalt = prefs.getString(KEY_DECOY_PIN_SALT, null)
        if (storedDecoySalt == null) {
            return verifyLegacySha256(pin, storedDecoyHash)
        }
        val salt = Base64.decode(storedDecoySalt, Base64.NO_WRAP)
        val expectedHash = Base64.decode(storedDecoyHash, Base64.NO_WRAP)
        val derivedHash = derivePinHash(pin, salt)
        return MessageDigest.isEqual(expectedHash, derivedHash)
    }

    fun isDecoyPin(pin: String): Boolean {
        val chars = pin.toCharArray()
        try {
            return isDecoyPin(chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    fun setDecoyPin(pin: CharArray) {
        val salt = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val hashBytes: ByteArray
        try {
            hashBytes = derivePinHash(pin, salt)
        } finally {
            pin.fill('\u0000')
        }
        prefs.edit()
            .putString(KEY_DECOY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_DECOY_PIN_HASH, Base64.encodeToString(hashBytes, Base64.NO_WRAP))
            .commit()
    }

    fun setDecoyPin(pin: String) {
        val chars = pin.toCharArray()
        try {
            setDecoyPin(chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    fun clearDecoyPin() {
        prefs.edit()
            .remove(KEY_DECOY_PIN_HASH)
            .remove(KEY_DECOY_PIN_SALT)
            .commit()
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

    // ── Hardware KeyStore Pepper & Cryptographic Derivation ────────────────

    private var inMemoryFallbackKey: SecretKey? = null

    fun getOrCreateHardwarePepperKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(PIN_PEPPER_KEY_ALIAS)) {
                val entry = keyStore.getEntry(PIN_PEPPER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val strongBoxSpec = KeyGenParameterSpec.Builder(
                        PIN_PEPPER_KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN
                    ).setIsStrongBoxBacked(true).build()
                    keyGenerator.init(strongBoxSpec)
                    val key = keyGenerator.generateKey()
                    Log.i(TAG, "PIN Hardware Pepper key successfully created in StrongBox security tier")
                    return key
                } catch (e: Exception) {
                    Log.w(TAG, "StrongBox tier unavailable for HMAC-SHA256 (${e.javaClass.simpleName}: ${e.message}). Falling back to standard TEE security tier.")
                }
            }

            val teeSpec = KeyGenParameterSpec.Builder(
                PIN_PEPPER_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN
            ).setIsStrongBoxBacked(false).build()
            keyGenerator.init(teeSpec)
            val key = keyGenerator.generateKey()
            Log.i(TAG, "PIN Hardware Pepper key created in standard TEE security tier")
            return key
        } catch (e: Exception) {
            Log.w(TAG, "AndroidKeyStore HMAC-SHA256 unavailable in current environment (${e.javaClass.simpleName}: ${e.message}). Using software fallback key.")
            val existing = inMemoryFallbackKey
            if (existing != null) return existing
            val fallbackKey = KeyGenerator.getInstance("HmacSHA256").generateKey()
            inMemoryFallbackKey = fallbackKey
            return fallbackKey
        }
    }

    private fun charArrayToUtf8Bytes(chars: CharArray): ByteArray {
        val charBuffer = CharBuffer.wrap(chars)
        val byteBuffer = StandardCharsets.UTF_8.encode(charBuffer)
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        if (byteBuffer.hasArray()) {
            byteBuffer.array().fill(0.toByte())
        }
        return bytes
    }

    private fun bytesToHexChars(bytes: ByteArray): CharArray {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return hexChars
    }

    private fun derivePinHash(pin: CharArray, salt: ByteArray): ByteArray {
        val hardwareKey = getOrCreateHardwarePepperKey()
        val pinBytes = charArrayToUtf8Bytes(pin)
        val pepperedBytes: ByteArray
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(hardwareKey)
            pepperedBytes = mac.doFinal(pinBytes)
        } finally {
            pinBytes.fill(0.toByte())
        }

        val pepperHexChars = bytesToHexChars(pepperedBytes)
        val derived: ByteArray
        try {
            val spec = PBEKeySpec(pepperHexChars, salt, PBKDF2_ITERATIONS, HASH_KEY_LENGTH_BITS)
            val skf = try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            } catch (_: Exception) {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            }
            derived = skf.generateSecret(spec).encoded
        } finally {
            pepperedBytes.fill(0.toByte())
            pepperHexChars.fill('\u0000')
        }
        return derived
    }

    private fun verifyLegacySha256(pin: CharArray, expectedHexHash: String): Boolean {
        val pinBytes = charArrayToUtf8Bytes(pin)
        val hashBytes: ByteArray
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            hashBytes = digest.digest(pinBytes)
        } finally {
            pinBytes.fill(0.toByte())
        }

        val hexChars = bytesToHexChars(hashBytes)
        hashBytes.fill(0.toByte())
        try {
            if (hexChars.size != expectedHexHash.length) return false
            var matches = true
            for (i in hexChars.indices) {
                if (hexChars[i].lowercaseChar() != expectedHexHash[i].lowercaseChar()) {
                    matches = false
                }
            }
            return matches
        } finally {
            hexChars.fill('\u0000')
        }
    }

    // ── Hardware-Signed Anti-Tamper Rate Limiting ───────────────────────────

    private fun signRateLimit(attempts: Int, lockoutUntil: Long): String {
        val hardwareKey = getOrCreateHardwarePepperKey()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hardwareKey)
        val payload = "$attempts:$lockoutUntil".toByteArray(StandardCharsets.UTF_8)
        return Base64.encodeToString(mac.doFinal(payload), Base64.NO_WRAP)
    }

    data class RateLimitState(
        val attempts: Int,
        val lockoutUntil: Long,
        val isTampered: Boolean
    )

    fun getRateLimitState(): RateLimitState {
        val attempts = prefs.getInt(KEY_PIN_FAILED_ATTEMPTS, 0)
        val lockoutUntil = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        val storedMac = prefs.getString(KEY_PIN_RATE_LIMIT_MAC, null)

        if (storedMac == null) {
            if (isPinSet() && (attempts > 0 || lockoutUntil > 0L)) {
                // Tamper: attempts or lockout exist without MAC signature
                Log.e(TAG, "Rate limit state missing MAC signature! Enforcing 15-minute security lockout.")
                val maxLockout = System.currentTimeMillis() + (15 * 60 * 1000L)
                val maxAttempts = maxOf(attempts, 10)
                try {
                    val newMac = signRateLimit(maxAttempts, maxLockout)
                    prefs.edit()
                        .putInt(KEY_PIN_FAILED_ATTEMPTS, maxAttempts)
                        .putLong(KEY_PIN_LOCKOUT_UNTIL, maxLockout)
                        .putString(KEY_PIN_RATE_LIMIT_MAC, newMac)
                        .commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist tampered lockout state", e)
                }
                return RateLimitState(attempts = maxAttempts, lockoutUntil = maxLockout, isTampered = true)
            }
            return RateLimitState(attempts = 0, lockoutUntil = 0L, isTampered = false)
        }

        val expectedMac = try {
            signRateLimit(attempts, lockoutUntil)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute rate limit MAC", e)
            null
        }

        val isValid = expectedMac != null && MessageDigest.isEqual(
            Base64.decode(storedMac, Base64.NO_WRAP),
            Base64.decode(expectedMac, Base64.NO_WRAP)
        )

        if (!isValid) {
            Log.e(TAG, "Rate limit state tampering detected! Enforcing 15-minute security lockout.")
            val maxLockout = System.currentTimeMillis() + (15 * 60 * 1000L)
            val maxAttempts = maxOf(attempts, 10)
            try {
                val newMac = signRateLimit(maxAttempts, maxLockout)
                prefs.edit()
                    .putInt(KEY_PIN_FAILED_ATTEMPTS, maxAttempts)
                    .putLong(KEY_PIN_LOCKOUT_UNTIL, maxLockout)
                    .putString(KEY_PIN_RATE_LIMIT_MAC, newMac)
                    .commit()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist tampered lockout state", e)
            }
            return RateLimitState(attempts = maxAttempts, lockoutUntil = maxLockout, isTampered = true)
        }

        return RateLimitState(attempts = attempts, lockoutUntil = lockoutUntil, isTampered = false)
    }

    fun isLockedOut(): Boolean {
        val state = getRateLimitState()
        return System.currentTimeMillis() < state.lockoutUntil
    }

    fun getRemainingLockoutSeconds(): Long {
        val state = getRateLimitState()
        val diff = state.lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) (diff + 999L) / 1000L else 0L
    }

    fun recordFailedPinAttempt() {
        val state = getRateLimitState()
        val newAttempts = state.attempts + 1
        val now = System.currentTimeMillis()
        val cooldownMillis = when {
            newAttempts in 1..2 -> 0L
            newAttempts in 3..4 -> 30_000L      // 30 seconds
            newAttempts in 5..6 -> 120_000L     // 2 minutes
            newAttempts in 7..9 -> 300_000L     // 5 minutes
            else -> 900_000L                    // 15 minutes
        }
        val lockoutUntil = if (cooldownMillis > 0) now + cooldownMillis else 0L
        val mac = signRateLimit(newAttempts, lockoutUntil)
        prefs.edit()
            .putInt(KEY_PIN_FAILED_ATTEMPTS, newAttempts)
            .putLong(KEY_PIN_LOCKOUT_UNTIL, lockoutUntil)
            .putString(KEY_PIN_RATE_LIMIT_MAC, mac)
            .commit()
    }

    fun resetFailedPinAttempts() {
        prefs.edit()
            .remove(KEY_PIN_FAILED_ATTEMPTS)
            .remove(KEY_PIN_LOCKOUT_UNTIL)
            .remove(KEY_PIN_RATE_LIMIT_MAC)
            .commit()
    }
}
