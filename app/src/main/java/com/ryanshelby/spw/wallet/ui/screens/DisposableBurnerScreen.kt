package com.ryanshelby.spw.wallet.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.SPWApplication
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.GlowingQrCodeView
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisposableBurnerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repository = remember { SPWApplication.instance.walletRepository }
    val securityManager = remember { SPWApplication.instance.securityManager }

    var amountText by remember { mutableStateOf("") }
    var memoText by remember { mutableStateOf("") }
    var selectedDurationMinutes by remember { mutableIntStateOf(15) } // 15, 60, 1440

    // Ephemeral Burner Keypair
    var burnerKeys by remember {
        mutableStateOf(
            SPWCrypto.createAccountFromMnemonic(SPWCrypto.generateMnemonic(128))
        )
    }
    val burnerAddress = remember(burnerKeys) {
        burnerKeys.address
    }

    var isCreated by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableLongStateOf(15 * 60L) }
    var isPaid by remember { mutableStateOf(false) }
    var receivedAmountSpw by remember { mutableStateOf(0.0) }

    // Expiration Countdown & Settlement Polling
    LaunchedEffect(isCreated, isPaid) {
        if (isCreated && !isPaid) {
            remainingSeconds = selectedDurationMinutes * 60L
            while (isActive && remainingSeconds > 0 && !isPaid) {
                delay(1000)
                remainingSeconds -= 1

                // Poll node for settlement
                if (remainingSeconds % 3 == 0L) {
                    try {
                        val balRes = repository.apiClient.getBalance(burnerAddress)
                        if (balRes.isSuccess) {
                            val bal = balRes.getOrNull()?.balanceSpw ?: 0.0
                            if (bal > 0.0) {
                                isPaid = true
                                receivedAmountSpw = bal
                                HapticUtil.performSuccess(context)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val uriPayload = remember(burnerAddress, amountText, memoText) {
        val amt = amountText.toDoubleOrNull() ?: 0.0
        val base = "spw:$burnerAddress"
        val params = mutableListOf<String>()
        if (amt > 0) params.add("amount=$amt")
        if (memoText.isNotBlank()) params.add("memo=${android.net.Uri.encode(memoText)}")
        if (params.isNotEmpty()) "$base?${params.joinToString("&")}" else base
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Disposable Burner Invoice", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Single-use ephemeral stealth request", color = RedCoral, fontSize = 11.sp)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = FinanceBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isCreated) {
                // STEP 1: CONFIGURATION
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RedCoral.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = RedCoral, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Burner Payment Parameters", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Invoice auto-expires after duration", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("REQUESTED AMOUNT (SPW)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    amountText = filtered
                                }
                            },
                            placeholder = { Text("0.00 (Optional for open amount)", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("INVOICE MEMO (OPTIONAL)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = memoText,
                            onValueChange = { memoText = it },
                            placeholder = { Text("e.g. Coffee #42, Concert Ticket", color = TextMuted, fontSize = 13.sp) },
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

                        Text("EXPIRATION TIME", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                15 to "15 Mins",
                                60 to "1 Hour",
                                1440 to "24 Hours"
                            ).forEach { (mins, label) ->
                                val isSelected = selectedDurationMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) RedCoral.copy(alpha = 0.15f) else SurfacePrimary)
                                        .border(1.dp, if (isSelected) RedCoral else BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable {
                                            HapticUtil.lightTap(context)
                                            selectedDurationMinutes = mins
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) RedCoral else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                HapticUtil.performSuccess(context)
                                isCreated = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedCoral),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Burner Invoice", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // STEP 2: ACTIVE INVOICE WITH LIVE SETTLEMENT TRACKER
                Spacer(modifier = Modifier.height(8.dp))

                // Settlement Banner
                if (isPaid) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SemanticPositive.copy(alpha = 0.15f))
                            .border(1.5.dp, SemanticPositive, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SemanticPositive, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PAYMENT SETTLED & RECEIVED!", color = SemanticPositive, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Received ${String.format(Locale.US, "%.4f", receivedAmountSpw)} SPW on-chain", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Countdown Banner
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    val timeStr = String.format(Locale.US, "%02d:%02d", mins, secs)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (remainingSeconds > 60) SurfaceElevated else RedCoral.copy(alpha = 0.15f))
                            .border(1.dp, if (remainingSeconds > 60) BorderSubtle else RedCoral, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = if (remainingSeconds > 60) CyanNeon else RedCoral, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Awaiting Payment...", color = TextSecondary, fontSize = 12.sp)
                            }
                            Text(
                                text = timeStr,
                                color = if (remainingSeconds > 60) CyanNeon else RedCoral,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GlowingQrCodeView(
                            data = uriPayload,
                            sizeDp = 220.dp,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = burnerAddress,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceSubtle)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(burnerAddress))
                                    HapticUtil.performSuccess(context)
                                    Toast.makeText(context, "Burner address copied", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )

                        if (amountText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Amount: $amountText SPW", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(uriPayload))
                            HapticUtil.performSuccess(context)
                            Toast.makeText(context, "Invoice Link Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link", color = TextPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uriPayload)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Burner Invoice"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedCoral)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        isCreated = false
                        isPaid = false
                        burnerKeys = SPWCrypto.createAccountFromMnemonic(SPWCrypto.generateMnemonic(128))
                    }
                ) {
                    Text("Create Another Burner Invoice", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
