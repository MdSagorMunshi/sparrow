package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.nfc.NfcPaymentInvoice
import com.ryanshelby.spw.wallet.payment.PaymentRequestManager
import com.ryanshelby.spw.wallet.payment.PaymentRequestState
import com.ryanshelby.spw.wallet.payment.TransactionWatcher
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SecurityManager
import com.ryanshelby.spw.wallet.ui.components.GlowingQrCodeView
import com.ryanshelby.spw.wallet.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPaymentScreen(
    securityManager: SecurityManager,
    transactionWatcher: TransactionWatcher,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val requestManager = PaymentRequestManager.instance
    val requestState by requestManager.requestState.collectAsState()
    val activeInvoice = requestManager.activeInvoice
    
    var amountInput by remember { mutableStateOf("") }
    
    DisposableEffect(Unit) {
        onDispose {
            if (requestManager.requestState.value == PaymentRequestState.WAITING || 
                requestManager.requestState.value == PaymentRequestState.CONNECTED) {
                requestManager.cancelRequest()
                transactionWatcher.stopWatching()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
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
            Text(
                text = "Request Payment",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (requestState) {
            PaymentRequestState.IDLE, PaymentRequestState.CANCELLED, PaymentRequestState.EXPIRED -> {
                if (requestState == PaymentRequestState.EXPIRED) {
                    Text("Request Expired", color = SemanticWarning, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (SPW)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val invoice = NfcPaymentInvoice(
                                address = securityManager.getWalletAddress(),
                                name = securityManager.getUserNickname(),
                                amount = amount,
                                token = "SPW",
                                timestampMs = System.currentTimeMillis(),
                                nonce = java.util.UUID.randomUUID().toString()
                            )
                            requestManager.createRequest(invoice)
                            transactionWatcher.startWatching(invoice.address, amount, invoice.timestampMs)
                        } else {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text("Generate Request", color = ButtonPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            PaymentRequestState.WAITING -> {
                Text("Waiting for Payment", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap NFC phone or scan QR Code to pay", color = TextSecondary, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                if (activeInvoice != null) {
                    val qrData = activeInvoice.toJson()
                    GlowingQrCodeView(
                        data = qrData,
                        sizeDp = 270.dp,
                        showCenterBadge = false
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "${activeInvoice.amount} SPW Requested",
                        color = CyanNeon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Listening for NFC & on-chain tx...", color = TextMuted, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        requestManager.cancelRequest()
                        transactionWatcher.stopWatching()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel Request", color = SemanticError, fontWeight = FontWeight.SemiBold)
                }
            }
            
            PaymentRequestState.CONNECTED -> {
                Text("Payer Connected via NFC", color = AccentPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Waiting for payer to confirm transaction...", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(color = AccentPrimary, modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        requestManager.cancelRequest()
                        transactionWatcher.stopWatching()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                ) {
                    Text("Cancel Request", color = SemanticError)
                }
            }
            
            PaymentRequestState.RECEIVED -> {
                Text("Payment Received!", color = SemanticPositive, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = SemanticPositive,
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        requestManager.reset()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text("Done", color = ButtonPrimaryText)
                }
            }
        }
    }
}
