package com.ryanshelby.spw.wallet

import com.ryanshelby.spw.wallet.security.BiometricHelper
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColor
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.ryanshelby.spw.wallet.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ryanshelby.spw.wallet.security.HapticUtil

import com.ryanshelby.spw.wallet.ui.screens.ChangePinScreen
import com.ryanshelby.spw.wallet.ui.screens.DashboardScreen
import com.ryanshelby.spw.wallet.ui.screens.HistoryScreen
import com.ryanshelby.spw.wallet.ui.screens.OnboardingScreen
import com.ryanshelby.spw.wallet.ui.screens.PinLockScreen
import com.ryanshelby.spw.wallet.ui.screens.ReceiveScreen
import com.ryanshelby.spw.wallet.ui.screens.SendTransferScreen
import com.ryanshelby.spw.wallet.ui.screens.SettingsSecurityScreen
import com.ryanshelby.spw.wallet.ui.screens.MiningScreen
import com.ryanshelby.spw.wallet.ui.components.NotificationPermissionHandler
import com.ryanshelby.spw.wallet.ui.theme.AccentMuted
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBackground
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.MyApplicationTheme
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    fun updatePrivacyShield(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onStop() {
        super.onStop()
        SPWApplication.instance.securityManager.recordBackgroundTimestamp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updatePrivacyShield(SPWApplication.instance.securityManager.isPrivacyShieldEnabled())

        setContent {
            MyApplicationTheme {
                val app = SPWApplication.instance
                val repository = app.walletRepository
                val securityManager = app.securityManager
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val clipboardManager = LocalClipboardManager.current

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // App State
                val accounts by repository.accountsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val tokens by repository.tokensFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val transactions by repository.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val contacts by repository.contactsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val activeNetwork by repository.activeNetwork.collectAsStateWithLifecycle()
                val activeLanguage by repository.activeLanguage.collectAsStateWithLifecycle()
                val isInitialSyncing by repository.isInitialSyncing.collectAsStateWithLifecycle()

                var hideBalance by remember { mutableStateOf(securityManager.isHideBalance()) }
                var isBiometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
                var isScramblePin by remember { mutableStateOf(securityManager.isScramblePin()) }
                var isWalletUnlocked by remember { mutableStateOf(!securityManager.isPinSet()) }
                var isPrivacyShieldEnabled by remember { mutableStateOf(securityManager.isPrivacyShieldEnabled()) }
                var autoLockTimeoutMinutes by remember { mutableIntStateOf(securityManager.getAutoLockTimeoutMinutes()) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (securityManager.hasWallet() && securityManager.isPinSet() && securityManager.shouldAutoLock()) {
                                isWalletUnlocked = false
                                navController.navigate("pin_lock") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            securityManager.resetBackgroundTimer()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val hasWallet = securityManager.hasWallet()
                val isPinSet = securityManager.isPinSet()
                val startDestination = if (!hasWallet) "onboarding" else if (isPinSet) "pin_lock" else "dashboard"

                val isBottomNavVisible = currentRoute in listOf(
                    "dashboard", "send", "receive", "cold_vault", "history", "settings"
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        AnimatedVisibility(
                            visible = isBottomNavVisible && isWalletUnlocked && securityManager.hasWallet(),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            FinanceBottomNavigationBar(
                                currentRoute = currentRoute ?: "dashboard",
                                onNavigate = { targetRoute ->
                                    HapticUtil.performKeyClick(context)
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NotificationPermissionHandler()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                            .padding(bottom = if (isBottomNavVisible && securityManager.hasWallet()) 76.dp else 0.dp)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 0. Onboarding & First-time Setup Screen
                            composable("onboarding") {
                                OnboardingScreen(
                                    activeLanguage = activeLanguage,
                                    isBiometricSupported = securityManager.canAuthenticateWithBiometrics(),
                                    onCreateWallet = { mnemonic, walletName, pin ->
                                        repository.createAndInitializeWallet(mnemonic, walletName, pin).map { Unit }
                                    },
                                    onImportMnemonic = { mnemonic, walletName, pin ->
                                        repository.importWalletFromMnemonic(mnemonic, walletName, pin).map { Unit }
                                    },
                                    onImportPrivateKey = { privateKeyHex, walletName, pin ->
                                        repository.importWalletFromPrivateKey(privateKeyHex, walletName, pin).map { Unit }
                                    },
                                    onLanguageSelected = { lang ->
                                        repository.setActiveLanguage(lang)
                                    },
                                    onOnboardingComplete = {
                                        isWalletUnlocked = true
                                        navController.navigate("dashboard") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // 1. PIN Lock Screen
                            composable("pin_lock") {
                                PinLockScreen(
                                    isFirstTimeSetup = !securityManager.isPinSet(),
                                    activeLanguage = activeLanguage,
                                    isBiometricAvailable = securityManager.canAuthenticateWithBiometrics() && isBiometricEnabled,
                                    isScramblePin = securityManager.isScramblePin(),
                                    onPinSuccess = {
                                        isWalletUnlocked = true
                                        navController.navigate("dashboard") {
                                            popUpTo("pin_lock") { inclusive = true }
                                        }
                                    },
                                    onBiometricRequest = { onAuthSuccess ->
                                        securityManager.authenticateWithBiometrics(
                                            activity = this@MainActivity,
                                            title = "Sparrow Biometric Unlock",
                                            subtitle = "Touch sensor to unlock your SPW Wallet",
                                            onSuccess = onAuthSuccess,
                                            onError = { errMsg ->
                                                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onSaveNewPin = { newPin ->
                                        securityManager.setPin(newPin)
                                    },
                                    onVerifyPin = { pin ->
                                        if (securityManager.isDecoyPin(pin)) {
                                            val action = securityManager.getDecoyAction()
                                            if (action == "wipe") {
                                                scope.launch {
                                                    repository.resetWalletData()
                                                    Toast.makeText(context, "Wallet wiped (Decoy PIN)", Toast.LENGTH_LONG).show()
                                                    isWalletUnlocked = false
                                                    navController.navigate("onboarding") {
                                                        popUpTo(0) { inclusive = true }
                                                    }
                                                }
                                                return@PinLockScreen false
                                            } else if (action == "fake_wallet") {
                                                scope.launch {
                                                    val fakeMnemonic = securityManager.getFakeWalletMnemonic()
                                                    if (fakeMnemonic != null) {
                                                        val accounts = repository.accountsFlow.first()
                                                        val fakeAccount = accounts.find { it.mnemonic == fakeMnemonic }
                                                        if (fakeAccount != null) {
                                                            repository.switchActiveAccount(fakeAccount)
                                                        } else {
                                                            repository.importWalletFromMnemonic(fakeMnemonic, "Sparrow Fake", pin)
                                                        }
                                                    } else {
                                                        val newFakeMnemonic = com.ryanshelby.spw.wallet.security.SPWCrypto.generateMnemonic(128)
                                                        securityManager.setFakeWalletMnemonic(newFakeMnemonic)
                                                        repository.importWalletFromMnemonic(newFakeMnemonic, "Sparrow Fake", pin)
                                                    }
                                                    Toast.makeText(context, "Decoy wallet activated", Toast.LENGTH_SHORT).show()
                                                }
                                                return@PinLockScreen true
                                            }
                                        }
                                        securityManager.verifyPin(pin)
                                    }
                                )
                            }

                            // Change PIN Screen
                            composable("change_pin") {
                                ChangePinScreen(
                                    isScramblePin = securityManager.isScramblePin(),
                                    onVerifyCurrentPin = { pin -> securityManager.verifyPin(pin) },
                                    onSaveNewPin = { newPin -> securityManager.setPin(newPin) },
                                    onSuccess = {
                                        Toast.makeText(context, "PIN Changed Successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // 2. Dashboard Screen
                            composable("dashboard") {
                                DashboardScreen(
                                    isSyncing = isInitialSyncing,
                                    walletName = securityManager.getWalletName(),
                                    walletAddress = securityManager.getWalletAddress(),
                                    viewKeyHex = securityManager.getViewKeyHex(),
                                    tokens = tokens,
                                    transactions = transactions,
                                    network = activeNetwork,
                                    activeLanguage = activeLanguage,
                                    hideBalance = hideBalance,
                                    onToggleHideBalance = {
                                        hideBalance = !hideBalance
                                        securityManager.setHideBalance(hideBalance)
                                    },
                                    onNavigateToSend = { tokenSymbol ->
                                        val route = if (tokenSymbol != null) "send?token=$tokenSymbol" else "send"
                                        navController.navigate(route)
                                    },
                                    onNavigateToSendWithRecipient = { tokenSymbol, recipient ->
                                        val route = when {
                                            recipient != null && tokenSymbol != null -> "send?token=$tokenSymbol&recipient=$recipient"
                                            recipient != null -> "send?recipient=$recipient"
                                            tokenSymbol != null -> "send?token=$tokenSymbol"
                                            else -> "send"
                                        }
                                        navController.navigate(route)
                                    },
                                    onNavigateToReceive = {
                                        navController.navigate("receive")
                                    },

                                    onNavigateToHistory = {
                                        navController.navigate("history")
                                    },
                                    onNavigateToMining = {
                                        navController.navigate("mining")
                                    },
                                    onRefresh = {
                                        scope.launch {
                                            repository.refreshOnChainData()
                                        }
                                    },
                                    onCopyAddress = { address ->
                                        clipboardManager.setText(AnnotatedString(address))
                                        HapticUtil.performSuccess(context)
                                        Toast.makeText(context, "Wallet Address Copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            // 3. Send Screen
                            composable(
                                route = "send?token={token}&recipient={recipient}&amount={amount}",
                                arguments = listOf(
                                    navArgument("token") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("recipient") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("amount") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                val initialToken = backStackEntry.arguments?.getString("token")
                                val initialRecipient = backStackEntry.arguments?.getString("recipient")
                                val initialAmount = backStackEntry.arguments?.getString("amount")
                                SendTransferScreen(
                                    initialTokenSymbol = initialToken,
                                    initialRecipientAddress = initialRecipient,
                                    initialAmount = initialAmount,
                                    tokens = tokens,
                                    contacts = contacts,
                                    network = activeNetwork,
                                    activeLanguage = activeLanguage,
                                    walletAddress = securityManager.getWalletAddress(),
                                    onBack = { navController.popBackStack() },
                                    onConfirmSend = { tokenSymbol, toAddress, amount, gasFee, memo, isStealth, recipientViewPubHex ->
                                        repository.sendTransfer(
                                            tokenSymbol = tokenSymbol,
                                            toAddress = toAddress,
                                            amount = amount,
                                            gasFee = gasFee,
                                            memo = memo,
                                            isStealth = isStealth,
                                            recipientViewPubHex = recipientViewPubHex
                                        )
                                    },
                                    onVerifyPin = { pin ->
                                        securityManager.verifyPin(pin)
                                    },
                                    onTriggerBiometric = { onSuccess ->
                                        securityManager.authenticateWithBiometrics(
                                            activity = this@MainActivity,
                                            title = "Authorize SPW Transfer",
                                            subtitle = "Biometric confirmation required to sign transfer",
                                            onSuccess = onSuccess,
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                )
                            }

                            // 4. Receive Screen
                            composable("receive") {
                                ReceiveScreen(
                                    walletAddress = securityManager.getWalletAddress(),
                                    walletName = securityManager.getWalletName(),
                                    spendPubHex = securityManager.getSpendPubHex(),
                                    viewPubHex = securityManager.getViewPubHex(),
                                    network = activeNetwork,
                                    activeLanguage = activeLanguage,
                                    onBack = { navController.popBackStack() }
                                )
                            }


                            // 6. History Screen
                            composable("history") {
                                HistoryScreen(
                                    transactions = transactions,
                                    activeLanguage = activeLanguage,
                                    network = activeNetwork,
                                    walletAddress = securityManager.getWalletAddress(),
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("mining") {
                                MiningScreen(
                                    walletAddress = securityManager.getWalletAddress(),
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // 7. Settings Screen
                            composable("settings") {
                                SettingsSecurityScreen(
                                    securityManager = securityManager,
                                    activeLanguage = activeLanguage,
                                    activeNetwork = activeNetwork,
                                    contacts = contacts,
                                    seedPhrase = securityManager.getSeedPhrase(),
                                    spendKeyHex = securityManager.getSpendKeyHex(),
                                    viewKeyHex = securityManager.getViewKeyHex(),
                                    currentAddress = securityManager.getWalletAddress(),
                                    accounts = accounts,
                                    isBiometricAvailable = securityManager.canAuthenticateWithBiometrics(),
                                    isBiometricEnabled = isBiometricEnabled,
                                    isScramblePin = isScramblePin,
                                    isPrivacyShieldEnabled = isPrivacyShieldEnabled,
                                    onTogglePrivacyShield = { enabled ->
                                        isPrivacyShieldEnabled = enabled
                                        securityManager.setPrivacyShieldEnabled(enabled)
                                        updatePrivacyShield(enabled)
                                    },
                                    autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                                    onSetAutoLockTimeout = { timeout ->
                                        autoLockTimeoutMinutes = timeout
                                        securityManager.setAutoLockTimeoutMinutes(timeout)
                                    },
                                    onBack = { navController.popBackStack() },
                                    onSetBiometricEnabled = { enabled ->
                                        isBiometricEnabled = enabled
                                        securityManager.setBiometricEnabled(enabled)
                                    },
                                    onSetScramblePin = { scramble ->
                                        isScramblePin = scramble
                                        securityManager.setScramblePin(scramble)
                                    },
                                    onSelectLanguage = { lang ->
                                        repository.setActiveLanguage(lang)
                                    },
                                    onSelectNetwork = { net ->
                                        repository.setActiveNetwork(net)
                                    },
                                    onVerifyPin = { securityManager.verifyPin(it) },
                                    onTriggerBiometric = { onSuccess ->
                                        BiometricHelper.showBiometricPrompt(
                                            activity = this@MainActivity,
                                            title = "Authenticate",
                                            subtitle = "Verify identity to clear wallet",
                                            onSuccess = onSuccess
                                        )
                                    },
                                    onAddContact = { name, address ->
                                        repository.saveContact(name, address)
                                    },
                                    onDeleteContact = { address ->
                                        repository.deleteContact(address)
                                    },
                                    onChangePinRequest = {
                                        navController.navigate("change_pin")
                                    },
                                    onCreateAccount = { name ->
                                        repository.createNewAccount(name)
                                    },
                                    onImportMnemonic = { mnemonic, name ->
                                        repository.importAccountByMnemonic(mnemonic, name).map { }
                                    },
                                    onImportPrivateKey = { spendKey, viewKey, name ->
                                        repository.importAccountByPrivateKey(spendKey, viewKey, name).map { }
                                    },
                                    onSwitchAccount = { account ->
                                        repository.switchActiveAccount(account)
                                    },
                                    onResetWallet = {
                                        repository.resetWalletData()
                                        isWalletUnlocked = false
                                        navController.navigate("onboarding") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onScanStealthOutputs = {
                                        repository.scanStealthOutputs()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfacePrimary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FinanceNavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Wallet",
                    isSelected = currentRoute == "dashboard",
                    onClick = { onNavigate("dashboard") }
                )
                FinanceNavItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Send",
                    isSelected = currentRoute.startsWith("send"),
                    onClick = { onNavigate("send") }
                )
                FinanceNavItem(
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    label = "Receive",
                    isSelected = currentRoute == "receive",
                    onClick = { onNavigate("receive") }
                )
                FinanceNavItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_mining),
                    label = "Mining",
                    isSelected = currentRoute == "mining",
                    onClick = { onNavigate("mining") }
                )
                FinanceNavItem(
                    icon = Icons.Default.History,
                    label = "Explorer",
                    isSelected = currentRoute == "history",
                    onClick = { onNavigate("history") }
                )
                FinanceNavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isSelected = currentRoute == "settings",
                    onClick = { onNavigate("settings") }
                )
            }
        }
    }
}

@Composable
private fun FinanceNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) SurfaceElevated else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) TextPrimary else TextMuted,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
