package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.ryanshelby.spw.wallet.SPWApplication
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiningScreen(
    walletAddress: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val miningManager = remember { SPWApplication.instance.miningManager }
    val miningState by miningManager.state.collectAsState()

    var cpuLimit by remember { mutableFloatStateOf(miningState.cpuAllocation.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SPW Node Mining", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    if (miningState.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SemanticPositive.copy(alpha = 0.15f))
                                .border(0.8.dp, SemanticPositive.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = SemanticPositive,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
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
            // Node Configuration
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
                        Text(
                            text = if (walletAddress.length > 18) "${walletAddress.take(8)}...${walletAddress.takeLast(8)}" else walletAddress,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Engine Control Card
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
                            onValueChange = {
                                cpuLimit = it
                                miningManager.updateCpuAllocation(it.toInt())
                            },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = TextPrimary,
                                activeTrackColor = TextPrimary,
                                inactiveTrackColor = SurfaceSubtle
                            )
                        )

                        if (!miningState.isActive) {
                            Button(
                                onClick = {
                                    HapticUtil.performSuccess(context)
                                    miningManager.startMining(walletAddress, cpuLimit.toInt())
                                    Toast.makeText(context, "Mining Engine Started", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonPrimary,
                                    contentColor = ButtonPrimaryText
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("START MINING ENGINE", color = ButtonPrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    HapticUtil.performError(context)
                                    miningManager.stopMining()
                                    Toast.makeText(context, "Mining Engine Stopped", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SemanticError,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("STOP MINING ENGINE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Stat Cards Row 1: Hashrate & Block Height
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Hash Rate",
                        value = if (miningState.isActive) String.format(Locale.US, "%.1f H/s", miningState.hashRate) else "0.0 H/s",
                        modifier = Modifier.weight(1f),
                        valueColor = if (miningState.isActive) SemanticPositive else TextMuted
                    )
                    StatCard(
                        title = "Block Height",
                        value = "#${miningState.currentBlockHeight}",
                        modifier = Modifier.weight(1f),
                        valueColor = TextPrimary
                    )
                }
            }

            // Stat Cards Row 2: Total Mined & Session Mined
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Mined",
                        value = String.format(Locale.US, "%.4f SPW", miningState.totalMinedSpw),
                        modifier = Modifier.weight(1f),
                        valueColor = SemanticPositive
                    )
                    StatCard(
                        title = "Session Mined",
                        value = String.format(Locale.US, "%.4f SPW", miningState.sessionMinedSpw),
                        modifier = Modifier.weight(1f),
                        valueColor = AccentPrimary
                    )
                }
            }

            // Stat Cards Row 3: Accepted Hashes & Rejected
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Accepted Shares",
                        value = "${miningState.acceptedShares}",
                        modifier = Modifier.weight(1f),
                        valueColor = SemanticPositive
                    )
                    StatCard(
                        title = "Rejected Shares",
                        value = "${miningState.rejectedShares}",
                        modifier = Modifier.weight(1f),
                        valueColor = if (miningState.rejectedShares > 0) SemanticError else TextMuted
                    )
                }
            }

            // Block Telemetry Card (Previous, Current, Next Block)
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "BLOCK TELEMETRY",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Previous Block", color = TextSecondary, fontSize = 11.sp)
                            Text("#${miningState.previousBlockHeight} (${miningState.previousBlockHash})", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Block", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("#${miningState.currentBlockHeight} (${miningState.currentBlockHash})", color = SemanticPositive, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Next Block", color = TextSecondary, fontSize = 11.sp)
                            Text("#${miningState.nextBlockHeight} (Pending)", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Process Logs
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        reverseLayout = true
                    ) {
                        items(miningState.logs.size) { index ->
                            val log = miningState.logs[miningState.logs.size - 1 - index]
                            Text(
                                text = log,
                                color = when {
                                    log.contains("NEW BLOCK") -> SemanticPositive
                                    log.contains("accepted") -> SemanticPositive
                                    log.contains("rejected") -> SemanticError
                                    else -> TextSecondary
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
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
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
        }
    }
}
