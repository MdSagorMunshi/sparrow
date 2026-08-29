package com.ryanshelby.spw.wallet.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    var showExplanationDialog by remember { mutableStateOf(false) }
    var hasAskedBefore by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!isGranted && !hasAskedBefore) {
                showExplanationDialog = true
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = {
                showExplanationDialog = false
                hasAskedBefore = true
            },
            containerColor = SurfacePrimary,
            title = {
                Text("Allow Notifications", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    text = "We need notification permission to alert you when your wallet receives or sends SPW tokens, and for future background mining status updates.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExplanationDialog = false
                        hasAskedBefore = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary,
                        contentColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Allow", color = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExplanationDialog = false
                        hasAskedBefore = true
                    }
                ) {
                    Text("Not now", color = TextMuted)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
