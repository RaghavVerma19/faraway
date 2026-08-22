package com.newspulse.ai.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.data.remote.UpstoxPosition
import com.newspulse.ai.ui.MainViewModel
import com.newspulse.ai.ui.theme.CrashContainer
import com.newspulse.ai.ui.theme.CrashContent
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.SurgeContainer
import com.newspulse.ai.ui.theme.SurgeContent
import com.newspulse.ai.ui.theme.TwitterBlue
import com.newspulse.ai.ui.theme.Typography

@Composable
fun PortfolioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val upstoxPositions by viewModel.upstoxPositions.collectAsState()
    val isFetchingPositions by viewModel.isFetchingPositions.collectAsState()
    val upstoxToken by viewModel.upstoxAccessToken.collectAsState()
    val lastExecutionResult by viewModel.lastOrderExecutionResult.collectAsState()
    val liveQuotes by viewModel.liveQuotes.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBase)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upstox Live Defense",
                style = Typography.headlineSmall,
                color = OnSurfacePrimary
            )

            IconButton(
                onClick = { viewModel.refreshUpstoxData() },
                enabled = !isFetchingPositions
            ) {
                if (isFetchingPositions) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TwitterBlue, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Upstox Data",
                        tint = OnSurfaceSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

        // Broker Status Hero Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineDivider, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UPSTOX API v2 STATUS",
                        style = Typography.labelSmall,
                        color = OnSurfaceSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (upstoxToken.isNotBlank()) "Bearer Token Configured" else "No Upstox Token",
                        style = Typography.titleMedium,
                        color = OnSurfacePrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(if (upstoxToken.isNotBlank()) SurgeContainer else CrashContainer, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (upstoxToken.isNotBlank()) "LIVE STREAMING" else "NOT CONNECTED",
                        color = if (upstoxToken.isNotBlank()) SurgeContent else CrashContent,
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (upstoxToken.isBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Configure your Upstox Access Token in Settings to stream 100% free live quotes & place instant real market orders.",
                    style = Typography.bodySmall,
                    color = OnSurfaceSecondary
                )
            }
        }

        // Execution Feedback Toast Banner
        lastExecutionResult?.let { res ->
            val isSuccess = res.status.equals("success", ignoreCase = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSuccess) SurgeContainer else CrashContainer)
                    .border(1.dp, if (isSuccess) SurgeContent.copy(alpha = 0.4f) else CrashContent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Upstox Order Status: ${res.status.uppercase()}",
                        style = Typography.titleSmall,
                        color = if (isSuccess) SurgeContent else CrashContent,
                        fontWeight = FontWeight.Bold
                    )
                    if (res.data?.orderId != null) {
                        Text(
                            text = "Upstox Order ID: ${res.data.orderId}",
                            style = Typography.bodySmall,
                            color = OnSurfacePrimary
                        )
                    }
                    if (!res.errors.isNullOrEmpty()) {
                        Text(
                            text = "Error: ${res.errors.firstOrNull()?.message ?: "Order error"}",
                            style = Typography.bodySmall,
                            color = CrashContent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

        // Live Positions List
        if (upstoxPositions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (upstoxToken.isBlank()) "Connect Upstox to view live Demat positions." else "No open positions on Upstox account.",
                        style = Typography.bodyMedium,
                        color = OnSurfaceSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Market Sell orders execute instantly from the Feed upon crash alerts.",
                        style = Typography.bodySmall,
                        color = OnSurfaceDisabled
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(upstoxPositions, key = { it.instrumentToken }) { pos ->
                    val isProfit = pos.pnl >= 0
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceBase)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = pos.tradingSymbol,
                                        style = Typography.titleSmall,
                                        color = OnSurfacePrimary
                                    )
                                    Text(
                                        text = "${pos.quantity} Qty • Buy Avg ₹${String.format("%.2f", pos.buyPrice)}",
                                        style = Typography.bodySmall,
                                        color = OnSurfaceSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isProfit) "+" else ""}₹${String.format("%.2f", pos.pnl)}",
                                        style = Typography.titleSmall,
                                        color = if (isProfit) SurgeContent else CrashContent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "LTP ₹${String.format("%.2f", pos.lastPrice)}",
                                        style = Typography.bodySmall,
                                        color = OnSurfaceDisabled
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.executeRealUpstoxOrder(
                                            symbol = pos.tradingSymbol,
                                            transactionType = "SELL",
                                            quantity = Math.abs(pos.quantity)
                                        )
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrashContent),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = "Exit", modifier = Modifier.size(14.dp), tint = CrashContent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Market Exit on Upstox", style = Typography.labelSmall, color = CrashContent)
                                }
                            }
                        }

                        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}
