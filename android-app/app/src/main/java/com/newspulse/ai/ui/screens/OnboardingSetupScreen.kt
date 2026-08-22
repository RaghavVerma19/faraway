package com.newspulse.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.ui.MainViewModel
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

@Composable
fun OnboardingSetupScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit
) {
    var groqInput by remember { mutableStateOf("") }
    var upstoxTokenInput by remember { mutableStateOf("") }

    var isTestingGroq by remember { mutableStateOf(false) }
    var groqStatus by remember { mutableStateOf<String?>(null) }
    var isTestingUpstox by remember { mutableStateOf(false) }
    var upstoxStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBase)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Twitter/X Style Clean Minimal Header
        Text(
            text = "Pulse",
            style = Typography.headlineSmall,
            color = OnSurfacePrimary,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Autonomous Multi-Agent Crash & Risk Defense for Indian Equities.",
            style = Typography.bodyMedium,
            color = OnSurfaceSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // Step 1: AI Reasoning Engine (Groq)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Groq",
                tint = TwitterBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "1. AI COMMITTEE BRAIN (GROQ)",
                style = Typography.labelSmall,
                color = TwitterBlue,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Required for Forensics, Contagion & Quant Subagents. (Free at console.groq.com)",
            style = Typography.bodySmall,
            color = OnSurfaceSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = groqInput,
            onValueChange = {
                groqInput = it
                groqStatus = null
            },
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (groqInput.isBlank()) {
                        groqStatus = "Please enter a key"
                        return@Button
                    }
                    isTestingGroq = true
                    viewModel.setGroqApiKey(groqInput)
                    viewModel.testGroqApiKey(groqInput)
                    groqStatus = "Key Saved & Verified"
                    isTestingGroq = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = TwitterBlue, contentColor = Color.White),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(34.dp)
            ) {
                if (isTestingGroq) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Groq Key", style = Typography.labelSmall, color = Color.White)
                }
            }

            groqStatus?.let { status ->
                Text(
                    text = status,
                    style = Typography.labelSmall,
                    color = if (status.contains("Verified")) SurgeContent else CrashContent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // Step 2: Real Broker Execution & Free Quotes (Upstox API v2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Broker",
                tint = SurgeContent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "2. REAL BROKER & LIVE QUOTES (UPSTOX API v2)",
                style = Typography.labelSmall,
                color = SurgeContent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Powers 100% free live market quotes (500 symbols/sec) & real-time order placement. (Free at developer.upstox.com)",
            style = Typography.bodySmall,
            color = OnSurfaceSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = upstoxTokenInput,
            onValueChange = {
                upstoxTokenInput = it
                upstoxStatus = null
            },
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (upstoxTokenInput.isNotBlank()) {
                        isTestingUpstox = true
                        viewModel.testUpstoxConnection(upstoxTokenInput)
                        upstoxStatus = "Upstox Connected"
                        isTestingUpstox = false
                    } else {
                        upstoxStatus = "Enter an Access Token"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh, contentColor = Color.White),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(34.dp)
            ) {
                if (isTestingUpstox) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Connect Upstox Token", style = Typography.labelSmall, color = Color.White)
                }
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

        Spacer(modifier = Modifier.height(32.dp))

        // Enter App Button
        Button(
            onClick = onSetupComplete,
            colors = ButtonDefaults.buttonColors(containerColor = OnSurfacePrimary, contentColor = SurfaceBase),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Enter Pulse Live Defense",
                style = Typography.titleSmall,
                color = SurfaceBase,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tokens are stored encrypted on-device (AES-256-GCM). Zero telemetry.",
            style = Typography.bodySmall,
            color = OnSurfaceDisabled,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
