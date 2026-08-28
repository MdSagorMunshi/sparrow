package com.ryanshelby.spw.wallet.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    var showExplanationDialog by remember { mutableStateOf(false) }
    var hasAskedBefore by remember { mutableStateOf(false) } // In a real app this would be in DataStore/SharedPreferences

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // We do nothing else; if granted, the OS allows it. If denied, we respect it.
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            // For this version we simulate checking if we asked. We just ask once per app launch if not granted.
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
            containerColor = DarkBackground,
            title = {
                Text("Allow Notifications", color = TextPrimary, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                ) {
                    Text("Allow", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExplanationDialog = false
                        hasAskedBefore = true
                    }
                ) {
                    Text("Deny", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
