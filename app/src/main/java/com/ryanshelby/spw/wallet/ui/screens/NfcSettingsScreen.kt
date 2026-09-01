package com.ryanshelby.spw.wallet.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SecurityManager
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.components.NfcRippleAnimation
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable

enum class WriteTagState {
    READY,
    WRITING,
    SUCCESS,
    ERROR
}

@Composable
fun NfcSettingsScreen(
    securityManager: SecurityManager,
    isNfcSupported: Boolean = false,
    isNfcEnabled: Boolean = false,
    onStartWriteTag: (address: String) -> Unit = {},
    onStopWriteTag: () -> Unit = {},
    tagWriteSuccessEvent: Long = 0L,
    tagWriteErrorEvent: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var alwaysActive by remember { mutableStateOf(securityManager.isNfcAlwaysActive()) }
    var requireAuth by remember { mutableStateOf(securityManager.isNfcRequireAuth()) }
    var sendToggleEnabled by remember { mutableStateOf(securityManager.isNfcSendToggleEnabled()) }

    var showWriteTagPopup by remember { mutableStateOf(false) }
    var writeTagState by remember { mutableStateOf(WriteTagState.READY) }
    var writeErrorMessage by remember { mutableStateOf<String?>(null) }

    // Listen to write events from NfcManager
    LaunchedEffect(tagWriteSuccessEvent) {
        if (tagWriteSuccessEvent > 0L && showWriteTagPopup) {
            writeTagState = WriteTagState.SUCCESS
            HapticUtil.performSuccess(context)
        }
    }

    LaunchedEffect(tagWriteErrorEvent) {
        if (tagWriteErrorEvent != null && showWriteTagPopup) {
            writeTagState = WriteTagState.ERROR
            writeErrorMessage = tagWriteErrorEvent
            HapticUtil.performError(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (showWriteTagPopup) {
                onStopWriteTag()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfacePrimary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .bouncyClickable {
                        HapticUtil.lightTap(context)
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("NFC Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isNfcSupported) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SemanticError, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No NFC Support", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your device does not have NFC hardware, or it is unavailable.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            return@Column
        }

        if (!isNfcEnabled) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NFC is Disabled", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please enable NFC in your system settings to use Tap-to-Pay and write physical tags.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        Text("TAP-TO-PAY & RECEIVE", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Always Active NFC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Nfc, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Always Active Receive", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Receive funds in the background without opening the app", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = alwaysActive,
                        onCheckedChange = {
                            HapticUtil.performKeyClick(context)
                            alwaysActive = it
                            securityManager.setNfcAlwaysActive(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanNeon.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceSubtle
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderSubtle, thickness = 1.dp)

                // Require Auth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Require Biometrics/PIN", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Require authentication before sending any payment via NFC", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = requireAuth,
                        onCheckedChange = {
                            HapticUtil.performKeyClick(context)
                            requireAuth = it
                            securityManager.setNfcRequireAuth(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanNeon.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceSubtle
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderSubtle, thickness = 1.dp)

                // Auto-Enable in Send Page
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Auto-Scan in Send Page", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Automatically search for NFC receivers when on the Send screen", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = sendToggleEnabled,
                        onCheckedChange = {
                            HapticUtil.performKeyClick(context)
                            sendToggleEnabled = it
                            securityManager.setNfcSendToggleEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanNeon.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceSubtle
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Physical NFC Tag Tools Section
        Text("PHYSICAL NFC TAG TOOLS", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Write Wallet Address to Tag", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Program a physical NFC sticker or card with your public SPW address", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        HapticUtil.lightTap(context)
                        writeTagState = WriteTagState.READY
                        writeErrorMessage = null
                        showWriteTagPopup = true
                        onStartWriteTag(securityManager.getWalletAddress())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Write NFC Tag", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap-to-Pay uses ECDH key exchange with AES-GCM encryption, ensuring your identity remains private during NFC transactions.", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }

    // Write NFC Tag Popup
    if (showWriteTagPopup) {
        WriteNfcTagPopup(
            state = writeTagState,
            walletAddress = securityManager.getWalletAddress(),
            errorMessage = writeErrorMessage,
            onDismiss = {
                showWriteTagPopup = false
                writeTagState = WriteTagState.READY
                writeErrorMessage = null
                onStopWriteTag()
            },
            onRetry = {
                writeTagState = WriteTagState.READY
                writeErrorMessage = null
                onStartWriteTag(securityManager.getWalletAddress())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteNfcTagPopup(
    state: WriteTagState,
    walletAddress: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WRITE NFC TAG",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith fadeOut()
                },
                label = "WriteTagState"
            ) { targetState ->
                when (targetState) {
                    WriteTagState.READY -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NfcRippleAnimation(modifier = Modifier.size(120.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Ready to Write",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hold a blank or rewritable NFC tag firmly against the back of your phone",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Address Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Writing Address", color = TextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = walletAddress,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    WriteTagState.WRITING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CyanNeon, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Writing NFC Tag...", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Keep tag close to your phone", color = TextSecondary, fontSize = 13.sp)
                        }
                    }

                    WriteTagState.SUCCESS -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AccentPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = AccentPrimary, modifier = Modifier.size(38.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("NFC Tag Written!", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your SPW wallet address was successfully programmed to the tag.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    WriteTagState.ERROR -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SemanticError.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = SemanticError, modifier = Modifier.size(38.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Tag Write Failed", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "Could not write to NFC tag. Ensure the tag is rewritable and not locked.",
                                color = SemanticError,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Close", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
