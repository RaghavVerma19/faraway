package com.newspulse.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.ui.MainViewModel
import com.newspulse.ai.ui.theme.CrashContent
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.SurgeContent
import com.newspulse.ai.ui.theme.TwitterBlue
import com.newspulse.ai.ui.theme.Typography

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val groqKey by viewModel.groqApiKey.collectAsState()
    val upstoxToken by viewModel.upstoxAccessToken.collectAsState()
    val marketHoursOnly by viewModel.marketHoursOnly.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val isMonitoringActive by viewModel.isMonitoringActive.collectAsState()
    val testStatus by viewModel.groqTestStatus.collectAsState()
    val upstoxStatus by viewModel.upstoxConnectionStatus.collectAsState()

    var groqInput by remember(groqKey) { mutableStateOf(groqKey) }
    var upstoxTokenInput by remember(upstoxToken) { mutableStateOf(upstoxToken) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBase)
            .verticalScroll(rememberScrollState())
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
                text = "Settings & Broker API",
                style = Typography.headlineSmall,
                color = OnSurfacePrimary
            )
        }

        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

        // Section 1: Upstox API v2 Integration
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "UPSTOX BROKER & LIVE QUOTES",
                style = Typography.labelSmall,
                color = OnSurfaceDisabled,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Upstox Access Token (Bearer Token)",
                style = Typography.titleMedium,
                color = OnSurfacePrimary
            )
            Text(
                text = "Powers 100% free real-time NSE market quotes & live market order placement. (Free at developer.upstox.com)",
                style = Typography.bodySmall,
                color = OnSurfaceSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = upstoxTokenInput,
                onValueChange = { upstoxTokenInput = it },
                placeholder = { Text("Upstox Access Token (JWT)", style = Typography.bodyMedium, color = OnSurfaceDisabled) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurfacePrimary,
                    unfocusedTextColor = OnSurfacePrimary,
                    focusedBorderColor = TwitterBlue,
                    unfocusedBorderColor = OutlineDivider,
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.testUpstoxConnection(upstoxTokenInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TwitterBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Connect Upstox API", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                upstoxStatus?.let { status ->
                    Text(
                        text = status,
                        style = Typography.labelSmall,
                        color = if (status.contains("Connected")) SurgeContent else CrashContent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

        // Section 2: AI Brain (Groq Multi-Agent)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "AGENTIC AI COMMITTEE (GROQ)",
                style = Typography.labelSmall,
                color = OnSurfaceDisabled,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Groq API Key (OpenAI GPT-OSS 120B)",
                style = Typography.titleMedium,
                color = OnSurfacePrimary
            )
            Text(
                text = "Powers Forensics, Contagion & Quant Subagents. (Free at console.groq.com)",
                style = Typography.bodySmall,
                color = OnSurfaceSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = groqInput,
                onValueChange = { groqInput = it },
                placeholder = { Text("gsk_...", style = Typography.bodyMedium, color = OnSurfaceDisabled) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurfacePrimary,
                    unfocusedTextColor = OnSurfacePrimary,
                    focusedBorderColor = TwitterBlue,
                    unfocusedBorderColor = OutlineDivider,
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.setGroqApiKey(groqInput)
                        viewModel.testGroqApiKey(groqInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TwitterBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Save & Test Connection", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                testStatus?.let { status ->
                    Text(
                        text = status,
                        style = Typography.labelSmall,
                        color = if (status.contains("Success", ignoreCase = true)) SurgeContent else CrashContent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)

        // Section 3: Surveillance Engine
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "MONITORING ENGINE",
                style = Typography.labelSmall,
                color = OnSurfaceDisabled,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Surveillance Service",
                        style = Typography.titleMedium,
                        color = OnSurfacePrimary
                    )
                    Text(
                        text = if (isMonitoringActive) "Foreground service running • Continuous scan" else "Engine stopped",
                        style = Typography.bodySmall,
                        color = if (isMonitoringActive) SurgeContent else OnSurfaceDisabled
                    )
                }

                Switch(
                    checked = isMonitoringActive,
                    onCheckedChange = { viewModel.toggleMonitoring(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceBase,
                        checkedTrackColor = SurgeContent,
                        uncheckedThumbColor = OnSurfaceDisabled,
                        uncheckedTrackColor = SurfaceContainerLow
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Market Hours Adaptive Mode",
                        style = Typography.titleMedium,
                        color = OnSurfacePrimary
                    )
                    Text(
                        text = "Auto-sleeps outside 9:15 AM - 3:30 PM IST (Saves 90% battery)",
                        style = Typography.bodySmall,
                        color = OnSurfaceSecondary
                    )
                }

                Switch(
                    checked = marketHoursOnly,
                    onCheckedChange = { viewModel.setMarketHoursOnly(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceBase,
                        checkedTrackColor = TwitterBlue
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "High-Priority Notifications",
                        style = Typography.titleMedium,
                        color = OnSurfacePrimary
                    )
                    Text(
                        text = "Instant alert sound and vibration on Critical & High risks",
                        style = Typography.bodySmall,
                        color = OnSurfaceSecondary
                    )
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceBase,
                        checkedTrackColor = TwitterBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
