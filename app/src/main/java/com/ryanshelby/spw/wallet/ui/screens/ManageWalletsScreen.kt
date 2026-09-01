package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.ryanshelby.spw.wallet.SPWApplication
import com.ryanshelby.spw.wallet.data.local.AccountEntity
import com.ryanshelby.spw.wallet.security.BiometricHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Dedicated professional multi-wallet management screen with creation,
 * multi-tab import, account switching, secret key backup, and high-security deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWalletsScreen(
    onBack: () -> Unit,
    onAllAccountsRemoved: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val repository = remember { SPWApplication.instance.walletRepository }
    val securityManager = remember { SPWApplication.instance.securityManager }

    val accounts by repository.accountsFlow.collectAsState(initial = emptyList())
    val activeAccount = accounts.find { it.isPrimary } ?: accounts.firstOrNull()

    // Dialog & Modal States
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var accountToRename by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToExportKey by remember { mutableStateOf<AccountEntity?>(null) }

    // Security Gate State for Deletion or Key Export
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showSecurityAuthModal by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Manage Wallets",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${accounts.size} Active Account${if (accounts.size == 1) "" else "s"}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        HapticUtil.lightTap(context)
                        showCreateDialog = true
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .border(1.dp, CyanNeon.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Wallet",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = FinanceBackground)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. ACTIVE WALLET BANNER ──────────────────────────────────────────
            if (activeAccount != null) {
                item {
                    Text(
                        text = "CURRENT ACTIVE WALLET",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    ActiveWalletCard(
                        account = activeAccount,
                        onCopyAddress = {
                            clipboardManager.setText(AnnotatedString(activeAccount.address))
                            HapticUtil.performSuccess(context)
                            Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onRename = { accountToRename = activeAccount },
                        onExportKey = {
                            authError = null
                            pendingAction = {
                                accountToExportKey = activeAccount
                            }
                            showSecurityAuthModal = true
                        }
                    )
                }
            }

            // ── 2. QUICK ACTION CARDS (CREATE & IMPORT) ──────────────────────────
            item {
                Text(
                    text = "ADDITIONAL WALLET ACTIONS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Create New Wallet Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfacePrimary)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable {
                                HapticUtil.lightTap(context)
                                showCreateDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanNeon.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Create Account", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Generate fresh BIP-44 keypair", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    // Import Wallet Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfacePrimary)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable {
                                HapticUtil.lightTap(context)
                                showImportDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PurpleNeon.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Import Wallet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Phrase or Private Key", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // ── 3. ALL CONFIGURED WALLETS LIST ──────────────────────────────────
            item {
                Text(
                    text = "ALL CONFIGURED WALLETS (${accounts.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(accounts, key = { it.id }) { account ->
                WalletItemCard(
                    account = account,
                    isActive = account.id == activeAccount?.id,
                    onSwitch = {
                        scope.launch {
                            HapticUtil.performSuccess(context)
                            repository.switchActiveAccount(account)
                            Toast.makeText(context, "Switched to ${account.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopyAddress = {
                        clipboardManager.setText(AnnotatedString(account.address))
                        HapticUtil.lightTap(context)
                        Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                    },
                    onRename = { accountToRename = account },
                    onExportKey = {
                        authError = null
                        pendingAction = {
                            accountToExportKey = account
                        }
                        showSecurityAuthModal = true
                    },
                    onDelete = {
                        accountToDelete = account
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // ── DIALOG: CREATE NEW ACCOUNT ───────────────────────────────────────────
    if (showCreateDialog) {
        var newName by remember { mutableStateOf("Account ${accounts.size + 1}") }
        var selectedWordCount by remember { mutableIntStateOf(12) }
        var isGenerating by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isGenerating) showCreateDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Create New Account", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Generates a fresh SPW wallet address with BIP-44 key derivation.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Account Name", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "RECOVERY PHRASE LENGTH",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 12 Words Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedWordCount == 12) CyanNeon.copy(alpha = 0.15f) else SurfacePrimary)
                                .border(
                                    1.2.dp,
                                    if (selectedWordCount == 12) CyanNeon else BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    HapticUtil.lightTap(context)
                                    selectedWordCount = 12
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "12 Words",
                                    color = if (selectedWordCount == 12) CyanNeon else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "128-bit Standard",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // 24 Words Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedWordCount == 24) CyanNeon.copy(alpha = 0.15f) else SurfacePrimary)
                                .border(
                                    1.2.dp,
                                    if (selectedWordCount == 24) CyanNeon else BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    HapticUtil.lightTap(context)
                                    selectedWordCount = 24
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "24 Words",
                                    color = if (selectedWordCount == 24) CyanNeon else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "256-bit Maximum",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showCreateDialog = false },
                            enabled = !isGenerating
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    isGenerating = true
                                    scope.launch {
                                        try {
                                            val keys = repository.createNewAccount(newName.trim(), selectedWordCount)
                                            HapticUtil.performSuccess(context)
                                            Toast.makeText(context, "$selectedWordCount-Word Account Created: ${keys.address.take(10)}...", Toast.LENGTH_SHORT).show()
                                            showCreateDialog = false
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                }
                            },
                            enabled = newName.isNotBlank() && !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Text("Create Account", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── DIALOG: IMPORT WALLET (PHRASE / PRIVATE KEY) ─────────────────────────
    if (showImportDialog) {
        var importTab by remember { mutableIntStateOf(0) } // 0 = Seed, 1 = Private Key
        var importName by remember { mutableStateOf("Imported Wallet") }
        var importMnemonic by remember { mutableStateOf("") }
        var importSpendKey by remember { mutableStateOf("") }
        var importViewKey by remember { mutableStateOf("") }
        var isImporting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isImporting) showImportDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurpleNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Import SPW Wallet", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TabRow(
                        selectedTabIndex = importTab,
                        containerColor = SurfacePrimary,
                        contentColor = CyanNeon,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[importTab]),
                                color = CyanNeon
                            )
                        }
                    ) {
                        Tab(
                            selected = importTab == 0,
                            onClick = { importTab = 0 },
                            text = { Text("Phrase", fontSize = 11.sp, fontWeight = if (importTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = importTab == 1,
                            onClick = { importTab = 1 },
                            text = { Text("Private Key", fontSize = 11.sp, fontWeight = if (importTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = importTab == 2,
                            onClick = { importTab = 2 },
                            text = { Text("Watch-Only", fontSize = 11.sp, fontWeight = if (importTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Wallet Name", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (importTab == 0) {
                        // Mnemonic Phrase Input
                        OutlinedTextField(
                            value = importMnemonic,
                            onValueChange = { importMnemonic = it },
                            label = { Text("12 or 24-Word Recovery Phrase", color = TextMuted) },
                            minLines = 3,
                            maxLines = 4,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            importMnemonic = clip.trim()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyanNeon)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        val wordCount = importMnemonic.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                        Text(
                            text = "$wordCount words entered (Expected 12 or 24)",
                            color = if (wordCount in listOf(12, 24)) SemanticPositive else TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    } else if (importTab == 1) {
                        // Private Key Input
                        OutlinedTextField(
                            value = importSpendKey,
                            onValueChange = { importSpendKey = it },
                            label = { Text("Spend Private Key (64-Hex or WIF)", color = TextMuted) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            importSpendKey = clip.trim()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyanNeon)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = importViewKey,
                            onValueChange = { importViewKey = it },
                            label = { Text("View Key (Optional for Dual-Key)", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    } else {
                        // Watch-Only (Cold Storage) Input
                        OutlinedTextField(
                            value = importSpendKey,
                            onValueChange = { importSpendKey = it },
                            label = { Text("SPW Public Address (Base58)", color = TextMuted) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            importSpendKey = clip.trim()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyanNeon)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = importViewKey,
                            onValueChange = { importViewKey = it },
                            label = { Text("Stealth View Key (Optional)", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔒 Safe for tracking cold storage hardware vaults. Spend keys never touch this device.",
                            color = CyanNeon,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showImportDialog = false },
                            enabled = !isImporting
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isImporting = true
                                scope.launch {
                                    try {
                                        if (importTab == 0) {
                                            val res = repository.importAccountByMnemonic(importMnemonic.trim(), importName.trim())
                                            if (res.isSuccess) {
                                                HapticUtil.performSuccess(context)
                                                Toast.makeText(context, "Wallet Imported Successfully!", Toast.LENGTH_SHORT).show()
                                                showImportDialog = false
                                            } else {
                                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } else if (importTab == 1) {
                                            val res = repository.importAccountByPrivateKey(
                                                spendKeyHex = importSpendKey.trim(),
                                                viewKeyHex = importViewKey.trim(),
                                                name = importName.trim()
                                            )
                                            if (res.isSuccess) {
                                                HapticUtil.performSuccess(context)
                                                Toast.makeText(context, "Private Key Imported!", Toast.LENGTH_SHORT).show()
                                                showImportDialog = false
                                            } else {
                                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            val res = repository.importWatchOnlyAccount(
                                                address = importSpendKey.trim(),
                                                viewKeyHex = importViewKey.trim(),
                                                name = importName.trim().ifEmpty { "Cold Vault" }
                                            )
                                            if (res.isSuccess) {
                                                HapticUtil.performSuccess(context)
                                                Toast.makeText(context, "Watch-Only Vault Added!", Toast.LENGTH_SHORT).show()
                                                showImportDialog = false
                                            } else {
                                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } finally {
                                        isImporting = false
                                    }
                                }
                            },
                            enabled = !isImporting && (
                                (importTab == 0 && importMnemonic.isNotBlank()) ||
                                (importTab == 1 && importSpendKey.isNotBlank()) ||
                                (importTab == 2 && importSpendKey.isNotBlank())
                            ),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Text(if (importTab == 2) "Add Watch-Only" else "Import & Activate", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── DIALOG: RENAME WALLET ────────────────────────────────────────────────
    if (accountToRename != null) {
        val target = accountToRename!!
        var renameText by remember { mutableStateOf(target.name) }

        Dialog(onDismissRequest = { accountToRename = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Rename Wallet", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Account Label", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { accountToRename = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (renameText.isNotBlank()) {
                                    scope.launch {
                                        repository.renameAccount(target.id, renameText.trim())
                                        accountToRename = null
                                        Toast.makeText(context, "Wallet renamed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── DIALOG: CAUTION & CRITICAL WARNING BEFORE REMOVAL ────────────────────
    if (accountToDelete != null) {
        val target = accountToDelete!!
        var hasConfirmedBackup by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { accountToDelete = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, RedCoral.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RedCoral.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedCoral, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("REMOVE WALLET", color = RedCoral, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Permanent local deletion", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSubtle)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(target.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = target.address,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "⚠️ CAUTION: Deleting this wallet will remove all cryptographic keys from this device.\n\n" +
                                "Funds can ONLY be restored with your Secret Recovery Phrase or Private Key. If you have not backed it up, your funds will be permanently lost.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasConfirmedBackup = !hasConfirmedBackup }
                    ) {
                        Checkbox(
                            checked = hasConfirmedBackup,
                            onCheckedChange = { hasConfirmedBackup = it },
                            colors = CheckboxDefaults.colors(checkedColor = RedCoral)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I have securely backed up my recovery phrase / key.",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { accountToDelete = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val toDeleteId = target.id
                                accountToDelete = null
                                // Require PIN / Biometric verification before executing deletion
                                pendingAction = {
                                    scope.launch {
                                        val success = repository.removeAccount(toDeleteId)
                                        if (success) {
                                            HapticUtil.performSuccess(context)
                                            Toast.makeText(context, "Wallet removed", Toast.LENGTH_SHORT).show()
                                            if (accounts.size <= 1) {
                                                onAllAccountsRemoved()
                                            }
                                        }
                                    }
                                }
                                showSecurityAuthModal = true
                            },
                            enabled = hasConfirmedBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = RedCoral)
                        ) {
                            Text("Verify & Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── DIALOG: EXPORT SECRET RECOVERY PHRASE / PRIVATE KEY ──────────────────
    if (accountToExportKey != null) {
        val target = accountToExportKey!!
        var isVisible by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { accountToExportKey = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Wallet Credentials", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(target.name, color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (target.mnemonic != null) {
                        Text("SECRET RECOVERY PHRASE", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfacePrimary)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isVisible) target.mnemonic else "•••• •••• •••• •••• •••• •••• •••• •••• •••• •••• •••• ••••",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        Text("PRIVATE KEY (HEX)", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfacePrimary)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isVisible) target.spendKeyHex else "••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { isVisible = !isVisible }
                        ) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isVisible) "Hide" else "Reveal", color = CyanNeon, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val textToCopy = target.mnemonic ?: target.spendKeyHex
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                HapticUtil.performSuccess(context)
                                Toast.makeText(context, "Copied to clipboard! Clear clipboard after pasting.", Toast.LENGTH_LONG).show()
                                accountToExportKey = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // ── SECURITY GATE MODAL: PIN / BIOMETRICS AUTH ───────────────────────────
    if (showSecurityAuthModal) {
        var pinInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = {
            showSecurityAuthModal = false
            pendingAction = null
            authError = null
            accountToExportKey = null
            accountToDelete = null
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Security Verification", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Enter PIN or use Biometrics to proceed", color = TextSecondary, fontSize = 11.sp)

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(authError!!, color = RedCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PinPadView(
                        enteredPin = pinInput,
                        onDigitClick = { digit ->
                            if (pinInput.length < 6) {
                                val newPin = pinInput + digit
                                pinInput = newPin
                                if (newPin.length == 6) {
                                    if (securityManager.verifyPin(newPin)) {
                                        showSecurityAuthModal = false
                                        val action = pendingAction
                                        pendingAction = null
                                        action?.invoke()
                                    } else {
                                        authError = "Incorrect PIN. Try again."
                                        HapticUtil.performError(context)
                                        pinInput = ""
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (pinInput.isNotEmpty()) {
                                pinInput = pinInput.dropLast(1)
                            }
                        },
                        onBiometricClick = if (activity != null && securityManager.canAuthenticateWithBiometrics()) {
                            {
                                BiometricHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = "SPW Wallet Security",
                                    subtitle = "Authenticate to confirm wallet action",
                                    onSuccess = {
                                        showSecurityAuthModal = false
                                        val action = pendingAction
                                        pendingAction = null
                                        action?.invoke()
                                    }
                                )
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            showSecurityAuthModal = false
                            pendingAction = null
                            authError = null
                            accountToExportKey = null
                            accountToDelete = null
                        }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Large highlighted card for currently active wallet.
 */
@Composable
private fun ActiveWalletCard(
    account: AccountEntity,
    onCopyAddress: () -> Unit,
    onRename: () -> Unit,
    onExportKey: () -> Unit
) {
    val securityManager = remember { SPWApplication.instance.securityManager }
    val cached = remember(account.address) { securityManager.getCachedBalance(account.address) }
    val balanceSpw = cached?.first ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfacePrimary,
                        SurfaceElevated
                    )
                )
            )
            .border(1.2.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SemanticPositive)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACTIVE WALLET", color = SemanticPositive, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row {
                    IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onExportKey, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Key, contentDescription = "Export Credentials", tint = AmberGold, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = account.name,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceSubtle)
                    .clickable(onClick = onCopyAddress)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (account.address.length > 20) "${account.address.take(10)}...${account.address.takeLast(8)}" else account.address,
                    color = CyanNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(13.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BALANCE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format(Locale.US, "%.4f", balanceSpw)} SPW",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (account.isWatchOnly) AmberGold.copy(alpha = 0.15f) else SurfaceSubtle)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            account.isWatchOnly -> "Watch-Only (Cold Storage)"
                            account.mnemonic != null -> "12/24-Word Mnemonic"
                            else -> "Private Key Hex"
                        },
                        color = if (account.isWatchOnly) AmberGold else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Standard item card for non-active or configured wallets.
 */
@Composable
private fun WalletItemCard(
    account: AccountEntity,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onCopyAddress: () -> Unit,
    onRename: () -> Unit,
    onExportKey: () -> Unit,
    onDelete: () -> Unit
) {
    val securityManager = remember { SPWApplication.instance.securityManager }
    val cached = remember(account.address) { securityManager.getCachedBalance(account.address) }
    val balanceSpw = cached?.first ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfacePrimary)
            .border(1.dp, if (isActive) CyanNeon.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) CyanNeon.copy(alpha = 0.15f) else SurfaceSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.name.take(1).uppercase(),
                            color = if (isActive) CyanNeon else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SemanticPositive.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", color = SemanticPositive, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (account.isWatchOnly) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AmberGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("COLD VAULT", color = AmberGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = if (account.address.length > 16) "${account.address.take(8)}...${account.address.takeLast(6)}" else account.address,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.4f", balanceSpw)} SPW",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopyAddress, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onExportKey, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Key, contentDescription = "Keys", tint = AmberGold, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedCoral, modifier = Modifier.size(15.dp))
                    }
                }

                if (!isActive) {
                    Button(
                        onClick = onSwitch,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceSubtle),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Make Active", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
