package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiningScreen(
    walletAddress: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var cpuLimit by remember { mutableFloatStateOf(50f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
    ) {
        TopAppBar(
            title = { Text("Node Mining (Preview)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .bouncyClickable {
                            HapticUtil.performKeyClick(context)
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Node Configuration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        
                        Text("Node Endpoint", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("https://spw.network/api", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Reward Payout Address", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(walletAddress, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CPU Allocation Limit", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("${cpuLimit.toInt()}%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Slider(
                            value = cpuLimit,
                            onValueChange = { cpuLimit = it },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = TextPrimary,
                                activeTrackColor = TextPrimary,
                                inactiveTrackColor = SurfaceSubtle
                            )
                        )
                        
                        Button(
                            onClick = {
                                Toast.makeText(context, "Native RandomX Engine integration required for mobile hash computation.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary, contentColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText)
                        ) {
                            Text("START MINING ENGINE", color = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Hash Rate", "0 H/s", Modifier.weight(1f))
                    StatCard("Block Height", "0", Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Accepted Hashes", "0", Modifier.weight(1f), AccentPrimary)
                    StatCard("Rejected", "0", Modifier.weight(1f), SemanticError)
                }
            }

            item {
                Text("Process Logs", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "> Node daemon connected.\n> RandomX cryptographic subsystem loaded.\n> Standby mode active...",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    FinanceCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
        }
    }
}
