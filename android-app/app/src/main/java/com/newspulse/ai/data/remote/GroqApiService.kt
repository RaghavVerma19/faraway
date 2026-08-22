package com.newspulse.ai.data.remote

import com.google.gson.Gson
import com.newspulse.ai.data.model.GroqChatRequest
import com.newspulse.ai.data.model.GroqChatResponse
import com.newspulse.ai.data.model.GroqMessage
import com.newspulse.ai.data.model.MultiAgentCommitteeAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GroqApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeCrashHeadline(
        apiKey: String,
        headline: String,
        source: String
    ): MultiAgentCommitteeAnalysis? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val systemPrompt = """
            You are the Pulse Autonomous Risk Committee, a 3-agent intelligence swarm for Indian Equities (NSE/BSE).
            When given a breaking market headline, 3 specialized agents deliberate in consensus:

            1. FORENSICS AGENT (The Prosecutor): Evaluates SEBI/CBI/ED violations, auditor resignations, fraud, tax raids, forensic audit, debt defaults.
            2. CONTAGION AGENT (The Macro Strategist): Evaluates 2nd-order impact on lenders (e.g. SBIN, PNB), key suppliers, partners, or sector peers (e.g. ADANIENT probe -> ADANIPORTS, AWL; IT miss -> TCS, INFY).
            3. QUANT & DEFENSE OFFICER (The Risk Manager): Evaluates lower circuit lock probability (5%, 10%, 20%) and prescribes defense strategy: EXIT_LONG (panic dump), HEDGE_PUT (buy put), BUY_DIP (oversold reaction), or HOLD.

            Return strictly JSON with this schema:
            {
              "crash_probability": 0-100,
              "confidence": "HIGH" | "MEDIUM" | "LOW",
              "reasoning": "Consensus summary in max 2 sentences",
              "forensics_severity": 0-100,
              "forensics_verdict": "Clear, Suspicious, Severe, or Extreme",
              "contagion_peers": ["TICKER1", "TICKER2"],
              "contagion_rationale": "Why these peers are exposed in 1 sentence",
              "quant_action": "EXIT_LONG" | "HEDGE_PUT" | "BUY_DIP" | "HOLD",
              "circuit_risk": "HIGH (10-20% limit risk)" | "MODERATE" | "LOW",
              "hedging_strategy": "Concrete advice (e.g. Buy 1-strike OTM Put / Trail Stop Loss at -2.5%)"
            }
        """.trimIndent()

        val userPrompt = "Source: $source\nHeadline: ${headline.take(350)}\nDeliberate risk consensus across Forensics, Contagion, and Quant Risk."

        val requestPayload = GroqChatRequest(
            model = "openai/gpt-oss-120b",
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = userPrompt)
            )
        )

        try {
            val jsonBody = gson.toJson(requestPayload)
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val rawResponse = response.body?.string() ?: return@withContext null

            val chatResponse = gson.fromJson(rawResponse, GroqChatResponse::class.java)
            val assistantReply = chatResponse.choices?.firstOrNull()?.message?.content ?: return@withContext null

            gson.fromJson(assistantReply, MultiAgentCommitteeAnalysis::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
