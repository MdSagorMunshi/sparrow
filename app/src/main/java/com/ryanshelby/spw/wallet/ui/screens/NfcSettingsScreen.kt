package com.ryanshelby.spw.wallet.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SecurityManager
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary

@Composable
fun NfcSettingsScreen(
    securityManager: SecurityManager,
    isNfcSupported: Boolean = false,
    isNfcEnabled: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var alwaysActive by remember { mutableStateOf(securityManager.isNfcAlwaysActive()) }
    var requireAuth by remember { mutableStateOf(securityManager.isNfcRequireAuth()) }
    var sendToggleEnabled by remember { mutableStateOf(securityManager.isNfcSendToggleEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("NFC Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isNfcSupported) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SemanticError, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No NFC Support", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your device does not have NFC hardware, or it is unavailable.", color = TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            return@Column
        }

        if (!isNfcEnabled) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NFC is Disabled", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please enable NFC in your system settings to use Tap-to-Pay and other NFC features.", color = TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = DarkBackground),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        Text("TAP-TO-PAY", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

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
                        Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
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

                // Toggle inside Send Page
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Auto-Enable in Send Page", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap-to-Pay uses ECDH key exchange with AES-GCM encryption, ensuring your identity remains private during NFC transactions.", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
