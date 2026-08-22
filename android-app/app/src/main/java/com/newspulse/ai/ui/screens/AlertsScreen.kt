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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.data.model.Alert
import com.newspulse.ai.data.model.SeverityTier
import com.newspulse.ai.domain.HeadlineImpactEstimator
import com.newspulse.ai.ui.MainViewModel
import com.newspulse.ai.ui.components.FeedRow
import com.newspulse.ai.ui.components.TierBadge
import com.newspulse.ai.ui.theme.CrashContainer
import com.newspulse.ai.ui.theme.CrashContent
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.SurfaceContainerHigh
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.SurgeContent
import com.newspulse.ai.ui.theme.TwitterBlue
import com.newspulse.ai.ui.theme.Typography
import com.newspulse.ai.ui.theme.WarningContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alerts by viewModel.alerts.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lastOrderResult by viewModel.lastOrderExecutionResult.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedAlertDetail by remember { mutableStateOf<Alert?>(null) }

    val watchlistedSymbols = remember(watchlist) {
        watchlist.map { it.symbol }.toSet()
    }

    val filteredAlerts = remember(alerts, selectedFilter, searchQuery) {
        alerts.filter { alert ->
            val matchesFilter = when (selectedFilter) {
                "CRITICAL" -> alert.tier == SeverityTier.CRITICAL
                "HIGH" -> alert.tier == SeverityTier.HIGH
                "WATCH" -> alert.tier == SeverityTier.WATCH
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                alert.symbol.contains(searchQuery, ignoreCase = true) ||
                        alert.company.contains(searchQuery, ignoreCase = true) ||
                        alert.headline.contains(searchQuery, ignoreCase = true) ||
                        alert.contagionPeers.any { it.contains(searchQuery, ignoreCase = true) }
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBase)
    ) {
        // Top App Bar
        Column(modifier = Modifier.background(SurfaceBase)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pulse",
                    style = Typography.headlineSmall,
                    color = OnSurfacePrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Alerts",
                            tint = OnSurfaceSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.triggerManualScan() },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TwitterBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Scan",
                                tint = OnSurfaceSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (isSearchActive) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search tickers, contagion peers, or keywords...", style = Typography.bodyMedium, color = OnSurfaceDisabled) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurfacePrimary,
                            unfocusedTextColor = OnSurfacePrimary,
                            focusedContainerColor = SurfaceContainerLow,
                            unfocusedContainerColor = SurfaceContainerLow,
                            focusedBorderColor = TwitterBlue,
                            unfocusedBorderColor = OutlineDivider
                        )
                    )
                }
            }

            // Real Order Status Banner
            lastOrderResult?.let { res ->
                val isSuccess = res.status.equals("success", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSuccess) SurfaceContainerHigh else CrashContainer)
                        .border(1.dp, if (isSuccess) TwitterBlue else CrashContent, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (res.data?.orderId != null) "Upstox Order ID: ${res.data.orderId} (SUCCESS)" else "Order: ${res.errors?.firstOrNull()?.message ?: res.status}",
                            style = Typography.bodySmall,
                            color = OnSurfacePrimary
                        )
                    }
                }
            }

            val filterOptions = listOf("ALL", "CRITICAL", "HIGH", "WATCH")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                style = Typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (filter == "ALL") OnSurfacePrimary else when (filter) {
                                "CRITICAL" -> CrashContent
                                "HIGH" -> WarningContent
                                else -> TwitterBlue
                            },
                            selectedLabelColor = if (filter == "ALL") SurfaceBase else Color.White,
                            containerColor = SurfaceContainerLow,
                            labelColor = OnSurfaceSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = OutlineDivider,
                            selectedBorderColor = OutlineDivider,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
        }

        // Continuous Timeline Feed with Real Upstox Market Sell
        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isScanning) "Scanning market feeds with Agent Swarm..." else "No $selectedFilter alerts in feed.",
                        style = Typography.bodyMedium,
                        color = OnSurfaceSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap refresh to run live autonomous scan.",
                        style = Typography.bodySmall,
                        color = OnSurfaceDisabled
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    val isWatchlisted = watchlistedSymbols.contains(alert.symbol)
                    FeedRow(
                        alert = alert,
                        isWatchlisted = isWatchlisted,
                        onToggleWatchlist = {
                            viewModel.toggleWatchlist(alert.symbol, alert.company)
                        },
                        onExecuteDhanSell = {
                            viewModel.executeRealUpstoxOrder(alert.symbol, "SELL", 1)
                        },
                        onDismiss = {
                            viewModel.deleteAlert(alert.id)
                        },
                        onClick = { selectedAlertDetail = alert }
                    )
                }
            }
        }
    }

    // Multi-Agent Deliberation Modal with Direct Upstox Broker Order
    selectedAlertDetail?.let { alert ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(Date(alert.timestamp))
        val impactFormatted = HeadlineImpactEstimator.format(alert.impactPct)
        val isWatchlisted = watchlistedSymbols.contains(alert.symbol)

        AlertDialog(
            onDismissRequest = { selectedAlertDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${alert.symbol} • ${alert.action}",
                        style = Typography.titleMedium,
                        color = OnSurfacePrimary
                    )
                    TierBadge(tier = alert.tier)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = alert.company,
                        style = Typography.bodySmall,
                        color = OnSurfaceSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Panic Score: ${alert.trustScore}/100 • Est. Impact: $impactFormatted",
                        style = Typography.titleSmall,
                        color = if (alert.impactPct < 0) CrashContent else SurgeContent
                    )
                    Text(
                        text = formattedTime,
                        style = Typography.bodySmall,
                        color = OnSurfaceDisabled
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = alert.headline,
                        style = Typography.bodyMedium,
                        color = OnSurfacePrimary
                    )

                    // 1. Forensics Agent Breakdown
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Forensics",
                                tint = WarningContent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FORENSICS AGENT (SEBI & Regulatory)",
                                style = Typography.labelSmall,
                                color = WarningContent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verdict: ${alert.forensicsVerdict.ifBlank { "Standard Disclosure" }} (Severity: ${alert.forensicsScore}/100)",
                            style = Typography.bodySmall,
                            color = OnSurfacePrimary
                        )
                    }

                    // 2. Contagion & Ecosystem Agent Breakdown
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Contagion",
                                tint = TwitterBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONTAGION AGENT (Macro & Peers)",
                                style = Typography.labelSmall,
                                color = TwitterBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (alert.contagionPeers.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                alert.contagionPeers.forEach { peer ->
                                    Box(
                                        modifier = Modifier
                                            .background(SurfaceBase, RoundedCornerShape(999.dp))
                                            .border(1.dp, OutlineDivider, RoundedCornerShape(999.dp))
                                            .clickable {
                                                searchQuery = peer
                                                isSearchActive = true
                                                selectedAlertDetail = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(peer, style = Typography.labelSmall, color = TwitterBlue)
                                    }
                                }
                            }
                        }
                        if (alert.contagionRationale.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.contagionRationale,
                                style = Typography.bodySmall,
                                color = OnSurfaceSecondary
                            )
                        }
                    }

                    // 3. Quant & Defense Officer Breakdown
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Quant",
                                tint = CrashContent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QUANT RISK & DEFENSE OFFICER",
                                style = Typography.labelSmall,
                                color = CrashContent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prescribed Action: ${alert.quantAction} • Limit Risk: ${alert.circuitRisk}",
                            style = Typography.bodySmall,
                            color = OnSurfacePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (alert.hedgingStrategy.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Strategy: ${alert.hedgingStrategy}",
                                style = Typography.bodySmall,
                                color = OnSurfaceSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct Market Order on Upstox
                    Button(
                        onClick = {
                            viewModel.executeRealUpstoxOrder(alert.symbol, "SELL", 1)
                            selectedAlertDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrashContent, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Upstox Order", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Place Real Market Sell on Upstox", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("https://www.google.com/finance/quote/${alert.symbol}:NSE")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Chart",
                                tint = TwitterBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chart", color = OnSurfacePrimary, style = Typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.toggleWatchlist(alert.symbol, alert.company)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(
                                imageVector = if (isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (isWatchlisted) TwitterBlue else OnSurfaceSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isWatchlisted) "Saved" else "Track", color = OnSurfacePrimary, style = Typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "[${alert.tier.name}] ${alert.symbol}: ${alert.headline}")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "[${alert.tier.name}] ${alert.symbol} Alert\n\n${alert.headline}\n\nForensics: ${alert.forensicsVerdict}\nQuant Action: ${alert.quantAction}\nContagion: ${alert.contagionPeers.joinToString(", ")}\n\nTracked via Pulse"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Market Alert"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = OnSurfaceSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = OnSurfacePrimary, style = Typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAlertDetail = null }) {
                    Text("Close", color = TwitterBlue, style = Typography.labelSmall)
                }
            },
            containerColor = SurfaceContainerHigh,
            textContentColor = OnSurfacePrimary,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
