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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.domain.CompanyRegistry
import com.newspulse.ai.ui.MainViewModel
import com.newspulse.ai.ui.theme.CrashContent
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.SurfaceContainerHigh
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.TwitterBlue
import com.newspulse.ai.ui.theme.Typography

@Composable
fun WatchlistScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val watchlist by viewModel.watchlist.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var symbolInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    symbolInput = ""
                    inputError = null
                    showAddDialog = true
                },
                containerColor = TwitterBlue,
                contentColor = SurfaceBase,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Symbol")
            }
        },
        containerColor = SurfaceBase
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBase)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watchlist",
                    style = Typography.headlineSmall,
                    color = OnSurfacePrimary
                )
            }

            HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

            if (watchlist.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No symbols in your watchlist.\nTap the add button to track Indian equities.",
                        style = Typography.bodyMedium,
                        color = OnSurfaceSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(watchlist, key = { it.symbol }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceBase)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.symbol,
                                        style = Typography.titleMedium,
                                        color = OnSurfacePrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.companyName,
                                        style = Typography.bodySmall,
                                        color = OnSurfaceSecondary
                                    )
                                }

                                IconButton(onClick = { viewModel.removeFromWatchlist(item.symbol) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Symbol",
                                        tint = OnSurfaceDisabled,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Ticker to Watchlist", style = Typography.titleMedium, color = OnSurfacePrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter stock symbol (e.g., RELIANCE, TCS, INFY, TATAMOTORS, ADANIENT):",
                        style = Typography.bodySmall,
                        color = OnSurfaceSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = symbolInput,
                        onValueChange = {
                            symbolInput = it.uppercase()
                            inputError = null
                        },
                        singleLine = true,
                        placeholder = { Text("RELIANCE", style = Typography.bodyMedium, color = OnSurfaceDisabled) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurfacePrimary,
                            unfocusedTextColor = OnSurfacePrimary,
                            focusedBorderColor = TwitterBlue,
                            unfocusedBorderColor = OutlineDivider,
                            focusedContainerColor = SurfaceContainerLow,
                            unfocusedContainerColor = SurfaceContainerLow
                        ),
                        isError = inputError != null
                    )
                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = inputError ?: "",
                            color = CrashContent,
                            style = Typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sym = symbolInput.trim().uppercase()
                        if (sym.isBlank()) {
                            inputError = "Symbol cannot be empty"
                            return@Button
                        }
                        val match = CompanyRegistry.resolve(sym)
                        val companyName = match?.companyName ?: sym
                        viewModel.addToWatchlist(sym, companyName)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TwitterBlue, contentColor = SurfaceBase),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Add Ticker", style = Typography.labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = OnSurfaceSecondary, style = Typography.labelSmall)
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
