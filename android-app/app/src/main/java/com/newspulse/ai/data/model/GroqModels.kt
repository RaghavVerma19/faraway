package com.newspulse.ai.data.model

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    val model: String = "openai/gpt-oss-120b",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.1,
    @SerializedName("max_tokens")
    val maxTokens: Int = 700,
    @SerializedName("response_format")
    val responseFormat: Map<String, String> = mapOf("type" to "json_object")
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatResponse(
    val id: String?,
    val choices: List<GroqChoice>?,
    val usage: GroqUsage?
)

data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class GroqUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

// Consensus Committee Output Structure
data class MultiAgentCommitteeAnalysis(
    @SerializedName("crash_probability")
    val crashProbability: Int = 0,
    val confidence: String = "MEDIUM",
    val reasoning: String = "",
    
    // Forensics Agent
    @SerializedName("forensics_severity")
    val forensicsSeverity: Int = 0,
    @SerializedName("forensics_verdict")
    val forensicsVerdict: String = "",
    
    // Contagion Agent
    @SerializedName("contagion_peers")
    val contagionPeers: List<String> = emptyList(),
    @SerializedName("contagion_rationale")
    val contagionRationale: String = "",
    
    // Quant & Defense Officer
    @SerializedName("quant_action")
    val quantAction: String = "HOLD",
    @SerializedName("circuit_risk")
    val circuitRisk: String = "LOW",
    @SerializedName("hedging_strategy")
    val hedgingStrategy: String = ""
)
