package com.ryanshelby.spw.wallet.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.CyanGlow
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.DarkSurface
import com.ryanshelby.spw.wallet.ui.theme.DarkSurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.GreenEmerald
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class OnboardingStage {
    LANGUAGE_SELECTION,
    WELCOME,
    CREATE_BACKUP_SEED,
    VERIFY_SEED_CHALLENGE,
    IMPORT_WALLET,
    SET_SECURITY_PIN,
    SETUP_SUCCESS
}

@Composable
fun OnboardingScreen(
    activeLanguage: AppLanguage,
    isBiometricSupported: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCreateWallet: suspend (mnemonic: String, walletName: String, pin: String) -> Result<Unit>,
    onImportMnemonic: suspend (mnemonic: String, walletName: String, pin: String) -> Result<Unit>,
    onImportPrivateKey: suspend (privateKeyHex: String, walletName: String, pin: String) -> Result<Unit>,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var currentStage by remember { mutableStateOf(OnboardingStage.LANGUAGE_SELECTION) }

    // Seed generation state
    var seedWordCount by remember { mutableIntStateOf(12) } // 12 or 24
    var generatedMnemonic by remember { mutableStateOf("") }
    var hasUserSavedSeedCheckbox by remember { mutableStateOf(false) }
    var isSeedHidden by remember { mutableStateOf(false) }

    // Verification challenge state
    var challengeIndices by remember { mutableStateOf<List<Int>>(emptyList()) } // 0-based indices e.g. [2, 6, 10]
    var selectedChallengeAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var challengeOptionsByPos by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    var challengeError by remember { mutableStateOf<String?>(null) }

    // Import state
    var importTypeTab by remember { mutableIntStateOf(0) } // 0: Mnemonic, 1: Private Key
    var importMnemonicInput by remember { mutableStateOf("") }
    var importKeyInput by remember { mutableStateOf("") }
    var importWalletName by remember { mutableStateOf("Sparrow Wallet") }
    var importError by remember { mutableStateOf<String?>(null) }

    // PIN Setup State
    var pinSetupStep by remember { mutableIntStateOf(1) } // 1 = Enter PIN, 2 = Confirm PIN
    var enteredFirstPin by remember { mutableStateOf("") }
    var enteredConfirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isBiometricEnabled by remember { mutableStateOf(isBiometricSupported) }
    var isFinalizing by remember { mutableStateOf(false) }
    var isImportFlow by remember { mutableStateOf(false) }

    // Helper to generate a new seed phrase
    fun regenerateMnemonic(count: Int) {
        val bits = if (count == 24) 256 else 128
        generatedMnemonic = SPWCrypto.generateMnemonic(bits)
        hasUserSavedSeedCheckbox = false
    }

    // Helper to initialize verification challenge
    fun setupVerificationChallenge() {
        val words = generatedMnemonic.split(" ").filter { it.isNotBlank() }
        if (words.size < 12) return

        // Pick 3 distinct random indices
        val total = words.size
        val indices = mutableSetOf<Int>()
        while (indices.size < 3) {
            indices.add(Random.nextInt(total))
        }
        val sortedIndices = indices.sorted()
        challengeIndices = sortedIndices
        selectedChallengeAnswers = emptyMap()
        challengeError = null

        // Generate 4 multiple-choice options for each requested word position
        val optionsMap = mutableMapOf<Int, List<String>>()
        val commonBip39Pool = listOf(
            "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
            "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
            "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
            "adapt", "add", "addict", "address", "adjust", "admit", "adult", "advance",
            "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
            "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol",
            "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already"
        )

        for (idx in sortedIndices) {
            val correctWord = words[idx]
            val distractors = (words.filter { it != correctWord } + commonBip39Pool)
                .shuffled()
                .distinct()
                .filter { it != correctWord }
                .take(3)
            optionsMap[idx] = (distractors + correctWord).shuffled()
        }
        challengeOptionsByPos = optionsMap
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {

        AnimatedContent(
            targetState = currentStage,
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { 300 }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { -300 }) + fadeOut()
            },
            label = "onboarding_screen_animation"
        ) { stage ->
            when (stage) {
                OnboardingStage.LANGUAGE_SELECTION -> {
                    LanguageSelectionStep(
                        activeLanguage = activeLanguage,
                        onLanguageSelected = onLanguageSelected,
                        onContinue = { currentStage = OnboardingStage.WELCOME }
                    )
                }

                OnboardingStage.WELCOME -> {
                    WelcomeLandingStep(
                        onStartCreateWallet = {
                            HapticUtil.performSuccess(context)
                            isImportFlow = false
                            regenerateMnemonic(seedWordCount)
                            currentStage = OnboardingStage.CREATE_BACKUP_SEED
                        },
                        onStartImportWallet = {
                            HapticUtil.performKeyClick(context)
                            isImportFlow = true
                            importError = null
                            currentStage = OnboardingStage.IMPORT_WALLET
                        }
                    )
                }

                OnboardingStage.CREATE_BACKUP_SEED -> {
                    CreateBackupSeedStep(
                        mnemonic = generatedMnemonic,
                        wordCount = seedWordCount,
                        isSeedHidden = isSeedHidden,
                        hasUserSaved = hasUserSavedSeedCheckbox,
                        onWordCountChange = { count ->
                            seedWordCount = count
                            regenerateMnemonic(count)
                        },
                        onToggleHide = { isSeedHidden = !isSeedHidden },
                        onCheckboxChange = { hasUserSavedSeedCheckbox = it },
                        onCopySeed = {
                            clipboardManager.setText(AnnotatedString(generatedMnemonic))
                            HapticUtil.performSuccess(context)
                            Toast.makeText(context, "Recovery phrase copied (store securely offline!)", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { currentStage = OnboardingStage.WELCOME },
                        onContinue = {
                            if (hasUserSavedSeedCheckbox) {
                                HapticUtil.performSuccess(context)
                                setupVerificationChallenge()
                                currentStage = OnboardingStage.VERIFY_SEED_CHALLENGE
                            }
                        }
                    )
                }

                OnboardingStage.VERIFY_SEED_CHALLENGE -> {
                    val words = remember(generatedMnemonic) { generatedMnemonic.split(" ").filter { it.isNotBlank() } }
                    VerifySeedChallengeStep(
                        challengeIndices = challengeIndices,
                        challengeOptions = challengeOptionsByPos,
                        selectedAnswers = selectedChallengeAnswers,
                        errorMessage = challengeError,
                        onSelectWord = { index, selectedWord ->
                            HapticUtil.performKeyClick(context)
                            challengeError = null
                            selectedChallengeAnswers = selectedChallengeAnswers + (index to selectedWord)
                        },
                        onBack = { currentStage = OnboardingStage.CREATE_BACKUP_SEED },
                        onVerifyAndProceed = {
                            var allCorrect = true
                            for (idx in challengeIndices) {
                                val expected = words.getOrNull(idx)
                                val actual = selectedChallengeAnswers[idx]
                                if (expected == null || actual != expected) {
                                    allCorrect = false
                                    break
                                }
                            }

                            if (!allCorrect) {
                                HapticUtil.performError(context)
                                challengeError = "Incorrect words selected. Please check your backup and try again."
                            } else {
                                HapticUtil.performSuccess(context)
                                pinSetupStep = 1
                                enteredFirstPin = ""
                                enteredConfirmPin = ""
                                pinError = null
                                currentStage = OnboardingStage.SET_SECURITY_PIN
                            }
                        }
                    )
                }

                OnboardingStage.IMPORT_WALLET -> {
                    ImportWalletStep(
                        activeTab = importTypeTab,
                        mnemonicText = importMnemonicInput,
                        privateKeyText = importKeyInput,
                        walletName = importWalletName,
                        errorMessage = importError,
                        onTabChange = {
                            importTypeTab = it
                            importError = null
                        },
                        onMnemonicChange = {
                            importMnemonicInput = it
                            importError = null
                        },
                        onKeyChange = {
                            importKeyInput = it
                            importError = null
                        },
                        onWalletNameChange = { importWalletName = it },
                        onPasteMnemonic = {
                            val clip = clipboardManager.getText()?.text ?: ""
                            if (clip.isNotBlank()) {
                                importMnemonicInput = clip.trim()
                                HapticUtil.performSuccess(context)
                            }
                        },
                        onPastePrivateKey = {
                            val clip = clipboardManager.getText()?.text ?: ""
                            if (clip.isNotBlank()) {
                                importKeyInput = clip.trim()
                                HapticUtil.performSuccess(context)
                            }
                        },
                        onBack = { currentStage = OnboardingStage.WELCOME },
                        onProceed = {
                            if (importTypeTab == 0) {
                                val cleanWords = importMnemonicInput.trim().lowercase()
                                val wordList = cleanWords.split(Regex("\\s+")).filter { it.isNotBlank() }
                                if (wordList.size != 12 && wordList.size != 24) {
                                    HapticUtil.performError(context)
                                    importError = "Please enter exactly 12 or 24 recovery words (currently ${wordList.size})"
                                    return@ImportWalletStep
                                }
                                if (!SPWCrypto.validateMnemonic(cleanWords)) {
                                    HapticUtil.performError(context)
                                    importError = "Invalid recovery phrase checksum. Please check spelling."
                                    return@ImportWalletStep
                                }
                            } else {
                                val cleanKey = importKeyInput.trim()
                                if (cleanKey.length != 64 || cleanKey.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) {
                                    HapticUtil.performError(context)
                                    importError = "Invalid private key. Must be 64 hexadecimal characters."
                                    return@ImportWalletStep
                                }
                            }

                            HapticUtil.performSuccess(context)
                            pinSetupStep = 1
                            enteredFirstPin = ""
                            enteredConfirmPin = ""
                            pinError = null
                            currentStage = OnboardingStage.SET_SECURITY_PIN
                        }
                    )
                }

                OnboardingStage.SET_SECURITY_PIN -> {
                    SetSecurityPinStep(
                        pinStep = pinSetupStep,
                        firstPin = enteredFirstPin,
                        confirmPin = enteredConfirmPin,
                        errorMessage = pinError,
                        isBiometricSupported = isBiometricSupported,
                        isBiometricEnabled = isBiometricEnabled,
                        isFinalizing = isFinalizing,
                        onToggleBiometric = { isBiometricEnabled = it },
                        onPinDigit = { digit ->
                            if (pinSetupStep == 1) {
                                if (enteredFirstPin.length < 6) {
                                    enteredFirstPin += digit
                                    if (enteredFirstPin.length == 6) {
                                        HapticUtil.performSuccess(context)
                                        pinSetupStep = 2
                                        pinError = null
                                    }
                                }
                            } else {
                                if (enteredConfirmPin.length < 6) {
                                    enteredConfirmPin += digit
                                    if (enteredConfirmPin.length == 6) {
                                        if (enteredConfirmPin == enteredFirstPin) {
                                            // Finalize Setup!
                                            HapticUtil.performSuccess(context)
                                            isFinalizing = true
                                            scope.launch {
                                                val finalPin = enteredFirstPin
                                                val res = if (!isImportFlow) {
                                                    onCreateWallet(generatedMnemonic, "Sparrow Main", finalPin)
                                                } else {
                                                    if (importTypeTab == 0) {
                                                        onImportMnemonic(importMnemonicInput.trim().lowercase(), importWalletName.ifBlank { "Sparrow Account" }, finalPin)
                                                    } else {
                                                        onImportPrivateKey(importKeyInput.trim(), importWalletName.ifBlank { "Sparrow Account" }, finalPin)
                                                    }
                                                }

                                                if (res.isSuccess) {
                                                    currentStage = OnboardingStage.SETUP_SUCCESS
                                                } else {
                                                    isFinalizing = false
                                                    pinError = res.exceptionOrNull()?.message ?: "Failed to initialize wallet"
                                                    HapticUtil.performError(context)
                                                }
                                            }
                                        } else {
                                            HapticUtil.performError(context)
                                            pinError = "PINs do not match. Please try again."
                                            enteredConfirmPin = ""
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (pinSetupStep == 1) {
                                if (enteredFirstPin.isNotEmpty()) enteredFirstPin = enteredFirstPin.dropLast(1)
                            } else {
                                if (enteredConfirmPin.isNotEmpty()) {
                                    enteredConfirmPin = enteredConfirmPin.dropLast(1)
                                } else {
                                    // Go back to step 1
                                    pinSetupStep = 1
                                    enteredFirstPin = ""
                                }
                            }
                        },
                        onBack = {
                            if (pinSetupStep == 2) {
                                pinSetupStep = 1
                                enteredConfirmPin = ""
                                enteredFirstPin = ""
                                pinError = null
                            } else {
                                currentStage = if (isImportFlow) OnboardingStage.IMPORT_WALLET else OnboardingStage.VERIFY_SEED_CHALLENGE
                            }
                        }
                    )
                }

                OnboardingStage.SETUP_SUCCESS -> {
                    SetupSuccessStep(
                        isImported = isImportFlow,
                        onEnterWallet = {
                            HapticUtil.performSuccess(context)
                            onOnboardingComplete()
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Welcome Landing Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeLandingStep(
    onStartCreateWallet: () -> Unit,
    onStartImportWallet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Logo Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyanNeon.copy(alpha = 0.25f), PurpleNeon.copy(alpha = 0.2f))
                        )
                    )
                    .border(2.dp, CyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "SPW Logo",
                    tint = CyanNeon,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SPW WALLET",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Text(
                text = "Next-Gen Stealth & Cold Storage Protocol",
                style = MaterialTheme.typography.bodyMedium,
                color = CyanNeon,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Value Prop Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeaturePill(
                    icon = Icons.Default.VpnKey,
                    title = "Dual ECDSA/ECDH Stealth Transfers",
                    desc = "Cryptographic one-time address shielding for total privacy",
                    tint = PurpleNeon
                )
                FeaturePill(
                    icon = Icons.Default.QrCode,
                    title = "Air-Gapped Cold QR Vaults",
                    desc = "Zero network exposure offline signing via dynamic QR codes",
                    tint = CyanNeon
                )
                FeaturePill(
                    icon = Icons.Default.Lock,
                    title = "Zero-Knowledge Local Storage",
                    desc = "Private keys encrypted on-device with Android Keystore",
                    tint = GreenEmerald
                )
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartCreateWallet,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Create New Wallet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            OutlinedButton(
                onClick = onStartImportWallet,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Import Existing Wallet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    title: String,
    desc: String,
    tint: Color
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f))
                    .border(1.dp, tint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Backup Seed Phrase Step
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateBackupSeedStep(
    mnemonic: String,
    wordCount: Int,
    isSeedHidden: Boolean,
    hasUserSaved: Boolean,
    onWordCountChange: (Int) -> Unit,
    onToggleHide: () -> Unit,
    onCheckboxChange: (Boolean) -> Unit,
    onCopySeed: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val words = remember(mnemonic) { mnemonic.split(" ").filter { it.isNotBlank() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Text(
                text = "Step 1 of 3: Secret Phrase",
                color = CyanNeon,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Length Switch (12 vs 24 words)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = wordCount == 12,
                    onClick = { onWordCountChange(12) },
                    label = { Text("12w", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeon,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = wordCount == 24,
                    onClick = { onWordCountChange(24) },
                    label = { Text("24w", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeon,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Write Down Recovery Phrase",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "These words are the ONLY way to recover your wallet. Write them down on paper and keep them in a secure, offline place.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Warning Alert Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = AmberGold.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGold, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Do not take screenshots or share these words with anyone. Anyone with this phrase has full access to your funds.",
                    color = AmberGold,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Words Grid Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowing = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount-Word Seed Phrase",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onToggleHide,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSeedHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopySeed,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Seed", tint = CyanNeon, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Words in 2-column or 3-column flow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = if (wordCount == 24) 3 else 3
                ) {
                    words.forEachIndexed { index, word ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%02d.", index + 1),
                                    color = CyanNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSeedHidden) "••••" else word,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Verification Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .clickable { onCheckboxChange(!hasUserSaved) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = hasUserSaved,
                onCheckedChange = { onCheckboxChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanNeon,
                    checkmarkColor = DarkBackground,
                    uncheckedColor = TextSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I have written down all $wordCount words in order and stored them in a safe offline location.",
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            enabled = hasUserSaved,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanNeon,
                contentColor = DarkBackground,
                disabledContainerColor = DarkSurfaceElevated,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Verify Recovery Phrase",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasUserSaved) DarkBackground else TextMuted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (hasUserSaved) DarkBackground else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Verify Seed Challenge Step
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VerifySeedChallengeStep(
    challengeIndices: List<Int>,
    challengeOptions: Map<Int, List<String>>,
    selectedAnswers: Map<Int, String>,
    errorMessage: String?,
    onSelectWord: (index: Int, word: String) -> Unit,
    onBack: () -> Unit,
    onVerifyAndProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Step 2 of 3: Verify Seed",
                    color = CyanNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Confirm Secret Words",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select the correct word for each requested position to ensure your backup is accurate.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Challenge Quiz Cards for the 3 positions
        challengeIndices.forEach { posIndex ->
            val positionNumber = posIndex + 1
            val options = challengeOptions[posIndex] ?: emptyList()
            val selected = selectedAnswers[posIndex]

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Word #$positionNumber",
                            color = CyanNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selected != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selected, color = GreenEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Options Grid (2x2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.take(2).forEach { optionWord ->
                            val isChoiceSelected = selected == optionWord
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChoiceSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isChoiceSelected) CyanNeon else GlassCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectWord(posIndex, optionWord) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionWord,
                                    color = if (isChoiceSelected) CyanNeon else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChoiceSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.drop(2).take(2).forEach { optionWord ->
                            val isChoiceSelected = selected == optionWord
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChoiceSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isChoiceSelected) CyanNeon else GlassCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectWord(posIndex, optionWord) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionWord,
                                    color = if (isChoiceSelected) CyanNeon else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChoiceSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = RedCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        val allAnswered = challengeIndices.all { selectedAnswers.containsKey(it) }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onVerifyAndProceed,
            enabled = allAnswered,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanNeon,
                contentColor = DarkBackground,
                disabledContainerColor = DarkSurfaceElevated,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Continue to PIN Setup",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (allAnswered) DarkBackground else TextMuted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (allAnswered) DarkBackground else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Import Wallet Step
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ImportWalletStep(
    activeTab: Int,
    mnemonicText: String,
    privateKeyText: String,
    walletName: String,
    errorMessage: String?,
    onTabChange: (Int) -> Unit,
    onMnemonicChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onWalletNameChange: (String) -> Unit,
    onPasteMnemonic: () -> Unit,
    onPastePrivateKey: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Restore Wallet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tabs (Recovery Phrase vs Private Key)
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkSurfaceElevated,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = CyanNeon
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { onTabChange(0) },
                text = { Text("Recovery Words", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { onTabChange(1) },
                text = { Text("Private Key", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab Content
        if (activeTab == 0) {
            val wordCount = remember(mnemonicText) {
                mnemonicText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enter 12 or 24 Recovery Words", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onPasteMnemonic,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mnemonicText,
                        onValueChange = onMnemonicChange,
                        placeholder = { Text("e.g. apple banana chair dragon eagle flame ...", color = TextMuted, fontSize = 13.sp) },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Words detected: $wordCount (Requires 12 or 24)",
                        color = if (wordCount == 12 || wordCount == 24) GreenEmerald else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enter Spend Private Key (Hex)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onPastePrivateKey,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = privateKeyText,
                        onValueChange = onKeyChange,
                        placeholder = { Text("64-char hex private key", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Length: ${privateKeyText.length} / 64 hex characters",
                        color = if (privateKeyText.length == 64) GreenEmerald else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wallet Name
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Wallet Nickname (Optional)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = walletName,
                    onValueChange = onWalletNameChange,
                    placeholder = { Text("e.g. Main Sparrow Account", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = GlassCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage, color = RedCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onProceed,
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Proceed to PIN Setup", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Set Security PIN Step
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SetSecurityPinStep(
    pinStep: Int,
    firstPin: String,
    confirmPin: String,
    errorMessage: String?,
    isBiometricSupported: Boolean,
    isBiometricEnabled: Boolean,
    isFinalizing: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onPinDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBack: () -> Unit
) {
    val activePin = if (pinStep == 1) firstPin else confirmPin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Step 3 of 3: Device Security",
                    color = CyanNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CyanNeon.copy(alpha = 0.15f))
                    .border(1.dp, CyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (pinStep == 1) "Create a 6-Digit PIN" else "Confirm Your 6-Digit PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = if (pinStep == 1) "This PIN will encrypt your wallet keys on this device." else "Re-enter your 6-digit PIN to confirm.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < activePin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) CyanNeon else DarkSurfaceElevated)
                            .border(1.5.dp, if (isFilled) CyanGlow else GlassCardBorder, CircleShape)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage, color = RedCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (isFinalizing) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Deriving cryptographic keys & initializing on-chain state...", color = CyanNeon, fontSize = 12.sp)
                }
            }
        }

        // Bottom PIN Pad & Biometric Toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isBiometricSupported && pinStep == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Enable Biometric Unlock", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = CyanNeon
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Interactive PinPadView
            PinPadView(
                enteredPin = activePin,
                isScrambled = false,
                errorMessage = null,
                onDigitClick = onPinDigit,
                onBackspaceClick = onBackspace
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Setup Success Step
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SetupSuccessStep(
    isImported: Boolean,
    onEnterWallet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(GreenEmerald.copy(alpha = 0.2f))
                .border(2.dp, GreenEmerald, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = GreenEmerald, modifier = Modifier.size(50.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isImported) "Wallet Restored!" else "Wallet Created Successfully!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your SPW stealth accounts and encrypted local vault are ready for live on-chain operations.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onEnterWallet,
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Open Sparrow Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Language Selection Step
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LanguageSelectionStep(
    activeLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Select Language",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = activeLanguage == lang
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageSelected(lang) }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) CyanNeon else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    glowing = isSelected
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = lang.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = lang.displayName,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyanNeon else TextPrimary
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
        }
    }
}
