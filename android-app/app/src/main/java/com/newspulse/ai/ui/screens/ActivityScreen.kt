package com.newspulse.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newspulse.ai.ui.MainViewModel
import com.newspulse.ai.ui.theme.CrashContent
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.SurgeContent
import com.newspulse.ai.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trades by viewModel.executedTrades.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBase)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Activity",
                style = Typography.headlineSmall,
                color = OnSurfacePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Executed Trades • Tap Feed to place new orders",
                style = Typography.bodySmall,
                color = OnSurfaceSecondary
            )
        }
        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

        if (trades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "No trades executed yet",
                        style = Typography.titleMedium,
                        color = OnSurfacePrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When you tap \"Market Sell on Dhan\" from Feed, the trade appears here with symbol, qty, price and broker status.",
                        style = Typography.bodySmall,
                        color = OnSurfaceSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(trades, key = { it.id }) { order ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${order.symbol} • ${order.action}",
                                    style = Typography.titleSmall,
                                    color = OnSurfacePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = order.companyName,
                                    style = Typography.bodySmall,
                                    color = OnSurfaceSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = order.brokerStatus,
                                    style = Typography.labelSmall,
                                    color = if (order.action == "SELL") CrashContent else SurgeContent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${order.quantity} Qty • ₹${String.format("%.2f", order.executionPrice)} = ₹${String.format("%.2f", order.totalAmount)}",
                                style = Typography.bodySmall,
                                color = OnSurfacePrimary
                            )
                            Text(
                                text = dateFormat.format(Date(order.timestamp)),
                                style = Typography.bodySmall,
                                color = OnSurfaceDisabled
                            )
                        }
                        if (order.triggerAlertHeadline.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = order.triggerAlertHeadline,
                                style = Typography.bodySmall,
                                color = OnSurfaceSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
                }
            }
        }
    }
}
