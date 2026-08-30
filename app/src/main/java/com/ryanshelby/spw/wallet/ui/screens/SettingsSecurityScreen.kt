package com.ryanshelby.spw.wallet.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.ryanshelby.spw.wallet.data.local.AccountEntity
import com.ryanshelby.spw.wallet.data.local.ContactEntity
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.KeyStoreTestResult
import com.ryanshelby.spw.wallet.data.local.NotificationPreferences
import com.ryanshelby.spw.wallet.security.KeystoreDiagnosticReport
import com.ryanshelby.spw.wallet.security.OledDisplayDetector
import com.ryanshelby.spw.wallet.security.SecureKeyStorage
import com.ryanshelby.spw.wallet.security.SecurityManager
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.components.StealthScanStatusCard
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.AppThemeState
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.DarkSurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.GreenEmerald
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticErrorMuted
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositiveMuted
import com.ryanshelby.spw.wallet.ui.theme.SemanticWarning
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsSecurityScreen(
    securityManager: SecurityManager,
    activeLanguage: AppLanguage,
    activeNetwork: NetworkConfig,
    contacts: List<ContactEntity>,
    seedPhrase: String,
    spendKeyHex: String,
    viewKeyHex: String,
    currentAddress: String,
    accounts: List<AccountEntity>,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    isScramblePin: Boolean,
    isPrivacyShieldEnabled: Boolean = securityManager.isPrivacyShieldEnabled(),
    onTogglePrivacyShield: (Boolean) -> Unit = { securityManager.setPrivacyShieldEnabled(it) },
    autoLockTimeoutMinutes: Int = securityManager.getAutoLockTimeoutMinutes(),
    onSetAutoLockTimeout: (Int) -> Unit = { securityManager.setAutoLockTimeoutMinutes(it) },
    currentTheme: ThemeMode = securityManager.getAppTheme(),
    onSelectTheme: (ThemeMode) -> Unit = {
        securityManager.setAppTheme(it)
        AppThemeState.setTheme(it)
    },
    onNavigateToAbout: () -> Unit = {},
    onNavigateToNfcSettings: () -> Unit = {},
    onBack: () -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetScramblePin: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectNetwork: (NetworkConfig) -> Unit,
    onAddContact: suspend (name: String, address: String) -> Unit,
    onDeleteContact: suspend (address: String) -> Unit,
    onChangePinRequest: () -> Unit,
    onCreateAccount: suspend (name: String) -> Unit,
    onImportMnemonic: suspend (mnemonic: String, name: String) -> Result<Unit>,
    onImportPrivateKey: suspend (spendKey: String, viewKey: String?, name: String) -> Result<Unit>,
    onSwitchAccount: suspend (account: AccountEntity) -> Unit,
    onVerifyPin: (String) -> Boolean,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit,
    onResetWallet: suspend () -> Unit = {},
    onScanStealthOutputs: suspend () -> Result<Pair<Int, Double>> = { Result.success(Pair(0, 0.0)) }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    val oledDetection = remember { OledDisplayDetector.detectDisplay(context) }
    var showThemeModal by remember { mutableStateOf(false) }
    var showOledUnsupportedDialog by remember { mutableStateOf(false) }

    var showNicknameDialog by remember { mutableStateOf(false) }
    var currentNickname by remember { mutableStateOf(securityManager.getUserNickname()) }
    var nicknameInput by remember { mutableStateOf(currentNickname) }

    var showSeedModal by remember { mutableStateOf(false) }
    var showKeysModal by remember { mutableStateOf(false) }
    var showAccountsModal by remember { mutableStateOf(false) }
    var showLanguageModal by remember { mutableStateOf(false) }
    var showNetworkModal by remember { mutableStateOf(false) }
    var showAddContactModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showDecoyModal by remember { mutableStateOf(false) }
    var showDebugDiagnosticsModal by remember { mutableStateOf(false) }
    var showClearWalletDialog by remember { mutableStateOf(false) }
    var showClearPinDialog by remember { mutableStateOf(false) }
    var enteredClearPin by remember { mutableStateOf("") }
    var clearPinError by remember { mutableStateOf<String?>(null) }

    // Authentication Guard State for Seed/Key Decryption
    var showAuthPinDialog by remember { mutableStateOf(false) }
    var pendingAuthTarget by remember { mutableStateOf<String?>(null) } // "seed" or "keys"
    var authPinInput by remember { mutableStateOf("") }
    var authPinError by remember { mutableStateOf<String?>(null) }
    var decryptedSeedPhrase by remember { mutableStateOf("") }
    var decryptedSpendKey by remember { mutableStateOf("") }
    var decryptedViewKey by remember { mutableStateOf("") }

    // Keystore Diagnostic Test State
    var isTestingKeystore by remember { mutableStateOf(false) }
    var keystoreTestResult by remember { mutableStateOf<KeyStoreTestResult?>(null) }
    
    val notificationPrefs = remember { NotificationPreferences(context) }
    var incTxEnabled by remember { mutableStateOf(notificationPrefs.incomingTransactionsEnabled) }
    var outTxEnabled by remember { mutableStateOf(notificationPrefs.outgoingTransactionsEnabled) }
    var miningEnabled by remember { mutableStateOf(notificationPrefs.miningRewardsEnabled) }

    fun triggerAuthForTarget(target: String) {
        val activity = context as? FragmentActivity
        if (isBiometricEnabled && isBiometricAvailable && activity != null) {
            val title = if (target == "seed") "Decrypt Recovery Seed" else "Decrypt Private Keys"
            val subtitle = "Hardware Keystore authentication required"
            securityManager.authenticateWithBiometrics(
                activity = activity,
                title = title,
                subtitle = subtitle,
                onSuccess = {
                    if (target == "seed") {
                        val seed = SecureKeyStorage.getMnemonicSeed(context) ?: seedPhrase
                        decryptedSeedPhrase = seed
                        showSeedModal = true
                    } else {
                        decryptedSpendKey = SecureKeyStorage.getSpendKey(context) ?: spendKeyHex
                        decryptedViewKey = SecureKeyStorage.getViewKey(context) ?: viewKeyHex
                        showKeysModal = true
                    }
                },
                onError = {
                    pendingAuthTarget = target
                    authPinInput = ""
                    authPinError = null
                    showAuthPinDialog = true
                }
            )
        } else {
            pendingAuthTarget = target
            authPinInput = ""
            authPinError = null
            showAuthPinDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with Back and Diagnostics Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        HapticUtil.performKeyClick(context)
                        onBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = strings.settings,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        HapticUtil.performKeyClick(context)
                        showDebugDiagnosticsModal = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug Diagnostics",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: ACCOUNTS & HARDWARE KEYSTORE KEYS
            SectionHeader(title = "SPW ACCOUNTS & HARDWARE KEYSTORE")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Manage Accounts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                showAccountsModal = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Manage SPW Accounts", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${accounts.size} active account(s) • Switch or generate", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Manage", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // View / Export Private Keys (Guarded by Keystore Auth)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                triggerAuthForTarget("keys")
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AmberGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Export Private Keys", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Protected", tint = AmberGold, modifier = Modifier.size(12.dp))
                                }
                                Text("Hardware AES-256 decrypted spend & view keys", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Export", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Secret Recovery Phrase Backup (Guarded by Keystore Auth)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                triggerAuthForTarget("seed")
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = RedCoral, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.backupSeed, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Shield, contentDescription = "Keystore Protected", tint = GreenEmerald, modifier = Modifier.size(12.dp))
                                }
                                Text("Android Keystore hardware-encrypted recovery seed", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Backup", color = RedCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: SECURITY & BIOMETRICS
            SectionHeader(title = "SECURITY & BIOMETRICS")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Biometric Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.biometricLogin, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(if (isBiometricAvailable) "Fingerprint / Face Keystore unlock" else "Biometrics not supported on device", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            enabled = isBiometricAvailable,
                            onCheckedChange = {
                                HapticUtil.performKeyClick(context)
                                onSetBiometricEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = CyanNeon,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }

                    // Scramble PIN Keypad Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Scramble PIN Keypad", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Randomize keypad digits on unlock", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isScramblePin,
                            onCheckedChange = {
                                HapticUtil.performKeyClick(context)
                                onSetScramblePin(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = PurpleNeon,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }

                    // App Switcher Privacy Shield Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Switcher Privacy Shield", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Mask balance & keys in Android recent apps", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isPrivacyShieldEnabled,
                            onCheckedChange = {
                                HapticUtil.performKeyClick(context)
                                onTogglePrivacyShield(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = TextPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }

                    // Auto-Lock Security Timer Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Auto-Lock Security Timer", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Lock wallet when minimized or in background", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0 to "Immediately",
                                1 to "1m",
                                5 to "5m",
                                15 to "15m",
                                -1 to "Never"
                            ).forEach { (timeout, label) ->
                                val isSelected = autoLockTimeoutMinutes == timeout
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) SurfaceSubtle else DarkSurfaceElevated)
                                        .border(0.8.dp, if (isSelected) TextPrimary else GlassCardBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            HapticUtil.performKeyClick(context)
                                            onSetAutoLockTimeout(timeout)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Change PIN Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                onChangePinRequest()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = AmberGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Change Security PIN", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Update master 6-digit security code", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Update", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    // Configure Decoy PIN Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                showDecoyModal = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = RedCoral, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Configure Decoy PIN", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Setup fake wallet or wipe on duress", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Configure", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: NETWORK & RPC
            SectionHeader(title = "SPW NETWORK & RPC NODE")

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticUtil.performKeyClick(context)
                        showNetworkModal = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(activeNetwork.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${activeNetwork.rpcUrl.take(32)}...", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Text("Switch", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Blockchain Explorer Link
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticUtil.performKeyClick(context)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeNetwork.explorerUrl))
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Open SPW Blockchain Explorer", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(activeNetwork.explorerUrl, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: PROFILE & NFC PAYMENTS
            SectionHeader(title = "PROFILE & NFC PAYMENTS")
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Nickname Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                nicknameInput = currentNickname
                                showNicknameDialog = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Public Nickname", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(currentNickname, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Edit", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderSubtle, thickness = 1.dp)

                    // NFC Settings Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                onNavigateToNfcSettings()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = GreenEmerald, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("NFC Tap-to-Pay Settings", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Configure offline payments and HCE", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: PREFERENCES & LOCALIZATION
            SectionHeader(title = "PREFERENCES & LOCALIZATION")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                showLanguageModal = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.language, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${activeLanguage.flag} ${activeLanguage.displayName}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Select", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = GlassCardBorder, thickness = 0.5.dp)

                    // Theme Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                showThemeModal = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Theme", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentTheme.displayName, color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (oledDetection.isOled) SemanticPositiveMuted else SurfaceSubtle)
                                            .border(0.6.dp, if (oledDetection.isOled) SemanticPositive else BorderSubtle, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (oledDetection.isOled) "OLED PANEL" else "LCD DISPLAY",
                                            color = if (oledDetection.isOled) SemanticPositive else TextMuted,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Text("Select", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: NOTIFICATIONS
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                showNotificationsModal = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = AmberGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Push Notifications", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Manage alerts for transactions and mining", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text("Manage", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: ADDRESS BOOK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "ADDRESS BOOK (${contacts.size})")
                Text(
                    text = "+ Add Contact",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        HapticUtil.performKeyClick(context)
                        showAddContactModal = true
                    }
                )
            }

            if (contacts.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No saved contacts yet.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contacts.forEach { contact ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Contacts, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(contact.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(contact.address.take(16) + "...", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { onDeleteContact(contact.address) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedCoral, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: ADVANCED / STEALTH ADDRESS MODE
            SectionHeader(title = "ADVANCED")

            var isStealthEnabled by remember { mutableStateOf(securityManager.isStealthModeEnabled()) }
            var isScanningStealth by remember { mutableStateOf(false) }
            var stealthScanStatus by remember { mutableStateOf<String?>(null) }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stealth Address Mode",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enable private transactions on home screen",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isStealthEnabled,
                            onCheckedChange = { enabled ->
                                HapticUtil.lightTap(context)
                                isStealthEnabled = enabled
                                securityManager.setStealthModeEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurpleNeon,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }

                    AnimatedVisibility(visible = isStealthEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            HorizontalDivider(
                                color = GlassCardBorder,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⚡ Privacy Mode Active",
                                    color = AmberGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "When sending, you can use stealth addresses to hide the recipient on-chain. The sender generates a one-time address using the recipient's public keys (ECDH). Only the recipient's view key can detect the payment.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            StealthScanStatusCard(
                                isScanning = isScanningStealth,
                                statusMessage = stealthScanStatus,
                                onScanClick = {
                                    HapticUtil.performSuccess(context)
                                    isScanningStealth = true
                                    stealthScanStatus = null
                                    scope.launch {
                                        val result = onScanStealthOutputs()
                                        isScanningStealth = false
                                        result.onSuccess { (count, spw) ->
                                            stealthScanStatus = if (count > 0) {
                                                "Found $count stealth output(s): ${String.format(java.util.Locale.US, "%.8f", spw)} SPW"
                                            } else {
                                                "Ledger synchronized • No new stealth outputs"
                                            }
                                        }.onFailure { err ->
                                            stealthScanStatus = "Scan error: ${err.message}"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: DEBUG & DIAGNOSTICS
            SectionHeader(title = "DEBUG & DIAGNOSTICS")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyanNeon.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Diagnostic & Keystore Panel", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Inspect Room, DataStore, Keystore & Clear Wallet", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { showDebugDiagnosticsModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: ABOUT & SYSTEM
            SectionHeader(title = "ABOUT & INFORMATION")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performKeyClick(context)
                                onNavigateToAbout()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("About SPARROW", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("v2.1.0-beta • Unofficial Client • Developer Info", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: DANGER ZONE
            SectionHeader(title = "DANGER ZONE")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = RedCoral.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = RedCoral, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Clear Wallet (Diagnostic Reset)", color = RedCoral, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Wipes Room DB, DataStore & Keystore keys", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { showClearWalletDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RedCoral, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ==========================================
        // MODALS & DIALOGS
        // ==========================================

        // 1. PIN AUTHENTICATION MODAL (Required to decrypt Keystore Seed / Keys)
        if (showAuthPinDialog) {
            Dialog(onDismissRequest = { showAuthPinDialog = false }) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyanNeon
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CyanNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (pendingAuthTarget == "seed") "Decrypt Recovery Seed" else "Decrypt Private Keys",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter your 6-digit Security PIN to decrypt hardware-backed Keystore secrets",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PIN Input Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            repeat(6) { idx ->
                                val filled = idx < authPinInput.length
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (filled) CyanNeon else DarkSurfaceElevated)
                                        .border(1.dp, if (filled) CyanNeon else GlassCardBorder, CircleShape)
                                )
                            }
                        }

                        if (authPinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(authPinError ?: "", color = RedCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Numeric Keypad
                        val keypadDigits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            keypadDigits.chunked(3).forEach { rowDigits ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowDigits.forEach { digit ->
                                        Button(
                                            onClick = {
                                                HapticUtil.performKeyClick(context)
                                                when (digit) {
                                                    "C" -> {
                                                        authPinInput = ""
                                                        authPinError = null
                                                    }
                                                    "OK" -> {
                                                        if (securityManager.verifyPin(authPinInput)) {
                                                            showAuthPinDialog = false
                                                            if (pendingAuthTarget == "seed") {
                                                                val seed = SecureKeyStorage.getMnemonicSeed(context) ?: seedPhrase
                                                                decryptedSeedPhrase = seed
                                                                showSeedModal = true
                                                            } else {
                                                                decryptedSpendKey = SecureKeyStorage.getSpendKey(context) ?: spendKeyHex
                                                                decryptedViewKey = SecureKeyStorage.getViewKey(context) ?: viewKeyHex
                                                                showKeysModal = true
                                                            }
                                                        } else {
                                                            HapticUtil.performError(context)
                                                            authPinError = "Incorrect PIN code"
                                                            authPinInput = ""
                                                        }
                                                    }
                                                    else -> {
                                                        if (authPinInput.length < 6) {
                                                            authPinInput += digit
                                                            authPinError = null
                                                            if (authPinInput.length == 6) {
                                                                if (securityManager.verifyPin(authPinInput)) {
                                                                    showAuthPinDialog = false
                                                                    if (pendingAuthTarget == "seed") {
                                                                        val seed = SecureKeyStorage.getMnemonicSeed(context) ?: seedPhrase
                                                                        decryptedSeedPhrase = seed
                                                                        showSeedModal = true
                                                                    } else {
                                                                        decryptedSpendKey = SecureKeyStorage.getSpendKey(context) ?: spendKeyHex
                                                                        decryptedViewKey = SecureKeyStorage.getViewKey(context) ?: viewKeyHex
                                                                        showKeysModal = true
                                                                    }
                                                                } else {
                                                                    HapticUtil.performError(context)
                                                                    authPinError = "Incorrect PIN code"
                                                                    authPinInput = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (digit == "OK") CyanNeon else DarkSurfaceElevated,
                                                contentColor = if (digit == "OK") DarkBackground else TextPrimary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = digit,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (digit == "OK") DarkBackground else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { showAuthPinDialog = false }) {
                            Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. DEBUG & DIAGNOSTICS MENU MODAL
        if (showDebugDiagnosticsModal) {
            val keystoreReport = remember { SecureKeyStorage.getDiagnosticInfo(context) }

            Dialog(onDismissRequest = { showDebugDiagnosticsModal = false }) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyanNeon
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Debug Diagnostics Menu", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { showDebugDiagnosticsModal = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Diagnostic Block 1: Android Keystore Hardware Security
                        Text("1. ANDROID KEYSTORE (HARDWARE-BACKED)", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DiagnosticItemRow(label = "Provider", value = keystoreReport.provider)
                                DiagnosticItemRow(label = "Key Alias", value = keystoreReport.alias)
                                DiagnosticItemRow(label = "Cipher Mode", value = "AES-256-GCM / NoPadding")
                                DiagnosticItemRow(label = "Hardware HSM Status", value = if (keystoreReport.isKeyPresent) "Active (Hardware Enclave)" else "Not Initialized")
                                DiagnosticItemRow(label = "Encrypted Seed Stored", value = if (keystoreReport.hasStoredEncryptedSeed) "YES (AES/GCM Encrypted)" else "None")
                                DiagnosticItemRow(label = "Encrypted Keys Stored", value = if (keystoreReport.hasStoredEncryptedKeys) "YES (Hardware Isolated)" else "None")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Benchmark Test Keystore Button
                        Button(
                            onClick = {
                                isTestingKeystore = true
                                scope.launch {
                                    delay(100)
                                    val res = SecureKeyStorage.testKeystoreRoundtrip(context)
                                    keystoreTestResult = res
                                    isTestingKeystore = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f), contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestingKeystore) {
                                CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing Hardware Keystore...")
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Keystore AES-256 Benchmark", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (keystoreTestResult != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val res = keystoreTestResult!!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (res.isSuccess) GreenEmerald.copy(alpha = 0.15f) else RedCoral.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (res.isSuccess) "✓ Keystore Verified: Roundtrip Latency: ${"%.2f".format(res.elapsedMs)} ms (AES-256-GCM)"
                                    else "✗ Keystore Error: ${res.error}",
                                    color = if (res.isSuccess) GreenEmerald else RedCoral,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Diagnostic Block 2: Jetpack DataStore
                        Text("2. JETPACK DATASTORE & PERSISTENCE", color = PurpleNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DiagnosticItemRow(label = "DataStore File", value = "spw_app_preferences.preferences_pb")
                                DiagnosticItemRow(label = "Status", value = "Synchronized (Active)")
                                DiagnosticItemRow(label = "Active Language", value = activeLanguage.displayName)
                                DiagnosticItemRow(label = "Active Network", value = activeNetwork.name)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Diagnostic Block 3: Room Database
                        Text("3. ROOM DATABASE (SQLite)", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DiagnosticItemRow(label = "Database Name", value = "spw_wallet_database")
                                DiagnosticItemRow(label = "Accounts Table", value = "${accounts.size} records")
                                DiagnosticItemRow(label = "Contacts Table", value = "${contacts.size} records")
                                DiagnosticItemRow(label = "Tables Count", value = "5 entities (Accounts, Txs, Tokens, Vaults, Contacts)")
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Diagnostic Clear Wallet Action
                        Text("4. DIAGNOSTIC TESTING ACTIONS", color = RedCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                showDebugDiagnosticsModal = false
                                showClearWalletDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedCoral, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Wallet (Diagnostic Reset)", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Nickname Edit Dialog
        // ─────────────────────────────────────────────────────────────────
        if (showNicknameDialog) {
            Dialog(onDismissRequest = { showNicknameDialog = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Edit Nickname",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Max 15 alphabetic characters.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { 
                                if (it.length <= 15 && it.all { char -> char.isLetter() }) {
                                    nicknameInput = it
                                }
                            },
                            label = { Text("Nickname", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { showNicknameDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (nicknameInput.isNotBlank()) {
                                        securityManager.setUserNickname(nicknameInput)
                                        currentNickname = nicknameInput
                                    }
                                    showNicknameDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. CLEAR WALLET CONFIRMATION DIALOG (DataStore + Room + Keystore Clear)
        if (showClearWalletDialog) {
            Dialog(onDismissRequest = { showClearWalletDialog = false }) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = RedCoral
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(RedCoral.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedCoral, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Execute Diagnostic Clear Wallet?", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This diagnostic operation tests clean app reset by performing atomic clear operations on:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• Room Database: Purge Accounts, Txs, Tokens, Vaults, Contacts", color = RedCoral, fontSize = 11.sp)
                                Text("• Jetpack DataStore: Clear spw_app_preferences", color = RedCoral, fontSize = 11.sp)
                                Text("• Android Keystore: Delete SPW_MASTER_HARDWARE_KEY", color = RedCoral, fontSize = 11.sp)
                                Text("• Session State: Return directly to Onboarding Setup", color = RedCoral, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClearWalletDialog = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = BorderStroke(1.dp, GlassCardBorder)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    showClearWalletDialog = false
                                    showClearPinDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RedCoral, contentColor = Color.White)
                            ) {
                                Text("Confirm Wipe", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (showClearPinDialog) {
            Dialog(onDismissRequest = { showClearPinDialog = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Authenticate to Clear Wallet",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PinPadView(
                            maxDigits = 6,
                            enteredPin = enteredClearPin,
                            errorMessage = clearPinError,
                            isScrambled = isScramblePin,
                            onDigitClick = { digit ->
                                if (enteredClearPin.length < 6) {
                                    enteredClearPin += digit
                                    if (enteredClearPin.length == 6) {
                                        if (onVerifyPin(enteredClearPin)) {
                                            HapticUtil.performSuccess(context)
                                            showClearPinDialog = false
                                            enteredClearPin = ""
                                            scope.launch {
                                                onResetWallet()
                                                Toast.makeText(context, "Wallet cleared via DataStore & Room operations", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            HapticUtil.performError(context)
                                            clearPinError = "Incorrect PIN"
                                            enteredClearPin = ""
                                        }
                                    }
                                }
                            },
                            onBackspaceClick = {
                                if (enteredClearPin.isNotEmpty()) enteredClearPin = enteredClearPin.dropLast(1)
                            },
                            onBiometricClick = if (isBiometricAvailable) {
                                {
                                    onTriggerBiometric {
                                        showClearPinDialog = false
                                        enteredClearPin = ""
                                        scope.launch {
                                            onResetWallet()
                                            Toast.makeText(context, "Wallet cleared via DataStore & Room operations", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // 4. ACCOUNTS MODAL
        if (showAccountsModal) {
            var isCreatingNew by remember { mutableStateOf(false) }
            var isImportingMnemonic by remember { mutableStateOf(false) }
            var newAccountName by remember { mutableStateOf("") }
            var importMnemonicText by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showAccountsModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("SPW Accounts", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        accounts.forEach { acc ->
                            val isSelected = acc.address == currentAddress
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                                    .border(if (isSelected) 1.dp else 0.dp, if (isSelected) CyanNeon else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable {
                                        scope.launch {
                                            onSwitchAccount(acc)
                                            showAccountsModal = false
                                        }
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(acc.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(acc.address.take(12) + "..." + acc.address.takeLast(6), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isCreatingNew) {
                            OutlinedTextField(
                                value = newAccountName,
                                onValueChange = { newAccountName = it },
                                placeholder = { Text("Account Name", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (newAccountName.isNotBlank()) {
                                        scope.launch {
                                            onCreateAccount(newAccountName.trim())
                                            showAccountsModal = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Account", fontWeight = FontWeight.Bold, color = DarkBackground)
                            }
                        } else if (isImportingMnemonic) {
                            OutlinedTextField(
                                value = importMnemonicText,
                                onValueChange = { importMnemonicText = it },
                                placeholder = { Text("Enter 12-word mnemonic phrase", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (importMnemonicText.isNotBlank()) {
                                        scope.launch {
                                            val res = onImportMnemonic(importMnemonicText.trim(), "Imported Account")
                                            if (res.isSuccess) {
                                                showAccountsModal = false
                                            } else {
                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error importing mnemonic", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Import from Seed", fontWeight = FontWeight.Bold, color = DarkBackground)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isCreatingNew = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+ New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                                }
                                Button(
                                    onClick = { isImportingMnemonic = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Import Seed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. PRIVATE KEYS MODAL (Decrypted via Keystore)
        if (showKeysModal) {
            Dialog(onDismissRequest = { showKeysModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Private Keys", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Decrypted from Android Keystore hardware vault. Keep strictly private!", color = RedCoral, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Spend Private Key
                        val effectiveSpendKey = if (decryptedSpendKey.isNotEmpty()) decryptedSpendKey else spendKeyHex
                        Text("Spend Private Key (ECDSA Secp256k1):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(10.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(effectiveSpendKey))
                                    Toast.makeText(context, "Spend Key Copied", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(effectiveSpendKey, color = CyanNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // View Private Key
                        val effectiveViewKey = if (decryptedViewKey.isNotEmpty()) decryptedViewKey else viewKeyHex
                        Text("View Private Key (ECDH Stealth):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(10.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(effectiveViewKey))
                                    Toast.makeText(context, "View Key Copied", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(effectiveViewKey, color = PurpleNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showKeysModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary, contentColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText)
                        }
                    }
                }
            }
        }

        // 6. SEED PHRASE MODAL (Decrypted via Keystore)
        if (showSeedModal) {
            val effectiveSeed = if (decryptedSeedPhrase.isNotEmpty()) decryptedSeedPhrase else seedPhrase
            Dialog(onDismissRequest = { showSeedModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SurfaceSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Secret Recovery Phrase", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Decrypted via Android Keystore Hardware Enclave", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Write these 12 words down in order. Never share them with anyone.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Seed words grid
                        val words = effectiveSeed.split(" ").filter { it.isNotBlank() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            words.chunked(3).forEachIndexed { rowIndex, rowWords ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowWords.forEachIndexed { colIndex, word ->
                                        val wordNum = rowIndex * 3 + colIndex + 1
                                        Text(
                                            text = "$wordNum. $word",
                                            color = CyanNeon,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(effectiveSeed))
                                Toast.makeText(context, "Seed Phrase Copied", Toast.LENGTH_SHORT).show()
                                showSeedModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Copy Seed & Close", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                }
            }
        }

        // 7. LANGUAGE MODAL
        if (showLanguageModal) {
            Dialog(onDismissRequest = { showLanguageModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("Select Language", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        AppLanguage.entries.forEach { lang ->
                            val isSelected = lang == activeLanguage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                                    .clickable {
                                        onSelectLanguage(lang)
                                        showLanguageModal = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lang.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(lang.displayName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // 7.1 THEME SELECTION MODAL
        if (showThemeModal) {
            Dialog(onDismissRequest = { showThemeModal = false }) {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select App Theme", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { showThemeModal = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }

                        HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

                        ThemeMode.entries.forEach { mode ->
                            val isSelected = mode == currentTheme
                            val isOledMode = mode == ThemeMode.OLED
                            val isOledSupported = oledDetection.isOled
                            val isEnabled = !isOledMode || isOledSupported

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (mode) {
                                            ThemeMode.DARK -> Color(0xFF0C0E12)
                                            ThemeMode.LIGHT -> Color(0xFFF8FAFC)
                                            ThemeMode.OLED -> Color(0xFF000000)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                        color = if (isSelected) (if (mode == ThemeMode.LIGHT) Color(0xFF0F172A) else Color.White) else BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isEnabled) {
                                            HapticUtil.performKeyClick(context)
                                            onSelectTheme(mode)
                                            showThemeModal = false
                                        } else {
                                            HapticUtil.performKeyClick(context)
                                            showOledUnsupportedDialog = true
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = mode.displayName,
                                                color = if (mode == ThemeMode.LIGHT) Color(0xFF0F172A) else Color(0xFFF9FAFB),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isOledMode) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isOledSupported) SemanticPositiveMuted else SemanticErrorMuted)
                                                        .border(0.6.dp, if (isOledSupported) SemanticPositive else SemanticError, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (isOledSupported) "OLED DETECTED" else "OLED REQUIRED",
                                                        color = if (isOledSupported) SemanticPositive else SemanticError,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mode.description,
                                            color = if (mode == ThemeMode.LIGHT) Color(0xFF475569) else Color(0xFF9CA3AF),
                                            fontSize = 11.sp
                                        )
                                        if (isOledMode && !isOledSupported) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Unavailable: Active display is LCD/IPS",
                                                color = SemanticError,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (mode == ThemeMode.LIGHT) Color(0xFF0F172A) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (isOledMode && !isOledSupported) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7.2 OLED UNSUPPORTED INFORMATIVE MODAL
        if (showOledUnsupportedDialog) {
            Dialog(onDismissRequest = { showOledUnsupportedDialog = false }) {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SemanticWarning, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OLED Display Required", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "OLED Pure Black mode physically switches off individual AMOLED/OLED subpixels for true zero power draw and infinite contrast.\n\nYour device was detected with a standard LCD/IPS display panel where the backlight remains active across the entire screen.\n\nDiagnostic: ${oledDetection.details}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Button(
                            onClick = { showOledUnsupportedDialog = false },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = ButtonPrimaryText
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Understand", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 8. NETWORK MODAL
        if (showNetworkModal) {
            Dialog(onDismissRequest = { showNetworkModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("Select SPW Network", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val networks = listOf(NetworkConfig.SPW_MAINNET, NetworkConfig.SPW_TESTNET, NetworkConfig.SPW_COMMUNITY)
                        networks.forEach { net ->
                            val isSelected = net.id == activeNetwork.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                                    .clickable {
                                        onSelectNetwork(net)
                                        showNetworkModal = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(net.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(net.rpcUrl, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // 9. ADD CONTACT MODAL
        if (showNotificationsModal) {
            Dialog(onDismissRequest = { showNotificationsModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = AmberGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Notification Settings",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose which alerts you want to receive.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Incoming Transactions", color = TextPrimary, fontSize = 14.sp)
                            Switch(
                                checked = incTxEnabled,
                                onCheckedChange = { 
                                    incTxEnabled = it
                                    notificationPrefs.incomingTransactionsEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.3f))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Outgoing Transactions", color = TextPrimary, fontSize = 14.sp)
                            Switch(
                                checked = outTxEnabled,
                                onCheckedChange = { 
                                    outTxEnabled = it
                                    notificationPrefs.outgoingTransactionsEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.3f))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mining Rewards", color = TextPrimary, fontSize = 14.sp)
                            Switch(
                                checked = miningEnabled,
                                onCheckedChange = { 
                                    miningEnabled = it
                                    notificationPrefs.miningRewardsEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.3f))
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showNotificationsModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GlassCardBorder)
                        ) {
                            Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showAddContactModal) {
            var contactName by remember { mutableStateOf("") }
            var contactAddr by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showAddContactModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("Add Contact", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            placeholder = { Text("Contact Name", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactAddr,
                            onValueChange = { contactAddr = it },
                            placeholder = { Text("SPW Address (spw1...)", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (contactName.isNotBlank() && contactAddr.isNotBlank()) {
                                    scope.launch {
                                        onAddContact(contactName.trim(), contactAddr.trim())
                                        showAddContactModal = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Contact", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                }
            }
        }
        if (showDecoyModal) {
            var decoyPin by remember { mutableStateOf("") }
            var decoyAction by remember { mutableStateOf(securityManager.getDecoyAction()) }
            var isSettingPin by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showDecoyModal = false }) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("Decoy PIN Configuration", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "A Decoy PIN allows you to either wipe the wallet or log into a fake wallet if you are forced to unlock your device.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        if (isSettingPin) {
                            Text("Enter 6-digit Decoy PIN", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            PinPadView(
                                maxDigits = 6,
                                enteredPin = decoyPin,
                                errorMessage = null,
                                isScrambled = isScramblePin,
                                onDigitClick = { digit ->
                                    if (decoyPin.length < 6) {
                                        decoyPin += digit
                                        if (decoyPin.length == 6) {
                                            securityManager.setDecoyPin(decoyPin)
                                            securityManager.setDecoyAction(decoyAction)
                                            Toast.makeText(context, "Decoy PIN Configured!", Toast.LENGTH_SHORT).show()
                                            showDecoyModal = false
                                        }
                                    }
                                },
                                onBackspaceClick = {
                                    if (decoyPin.isNotEmpty()) decoyPin = decoyPin.dropLast(1)
                                },
                                onBiometricClick = null
                            )
                        } else {
                            Text("Action on Decoy PIN Entry:", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(
                                    selected = decoyAction == "wipe",
                                    onClick = { decoyAction = "wipe" }
                                )
                                Text("Wipe Wallet Completely", color = TextPrimary, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(
                                    selected = decoyAction == "fake_wallet",
                                    onClick = { decoyAction = "fake_wallet" }
                                )
                                Text("Load Fake Wallet", color = TextPrimary, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { isSettingPin = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground)
                            ) {
                                Text("Set Decoy PIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DiagnosticItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}
