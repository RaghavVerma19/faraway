package com.newspulse.ai.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DhanOrderRequest(
    @SerializedName("dhanClientId") val dhanClientId: String,
    @SerializedName("transactionType") val transactionType: String, // BUY, SELL
    @SerializedName("exchangeSegment") val exchangeSegment: String = "NSE_EQ",
    @SerializedName("productType") val productType: String = "CNC", // CNC (Delivery), INTRADAY
    @SerializedName("orderType") val orderType: String = "MARKET", // MARKET, LIMIT
    @SerializedName("validity") val validity: String = "DAY",
    @SerializedName("securityId") val securityId: String,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("disclosedQuantity") val disclosedQuantity: Int = 0,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("triggerPrice") val triggerPrice: Double = 0.0,
    @SerializedName("afterMarketOrder") val afterMarketOrder: Boolean = false
)

data class DhanOrderResponse(
    @SerializedName("orderId") val orderId: String?,
    @SerializedName("orderStatus") val orderStatus: String?,
    @SerializedName("remarks") val remarks: String?,
    @SerializedName("errorCode") val errorCode: String?,
    @SerializedName("errorMessage") val errorMessage: String?
)

data class DhanPosition(
    @SerializedName("tradingSymbol") val tradingSymbol: String,
    @SerializedName("securityId") val securityId: String,
    @SerializedName("positionType") val positionType: String,
    @SerializedName("netQty") val netQty: Int,
    @SerializedName("costPrice") val costPrice: Double,
    @SerializedName("buyAvg") val buyAvg: Double,
    @SerializedName("sellAvg") val sellAvg: Double,
    @SerializedName("realizedProfit") val realizedProfit: Double,
    @SerializedName("unrealizedProfit") val unrealizedProfit: Double
)

class DhanBrokerService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Map common Indian equities to Dhan Security IDs (NSE)
    val securityIdMap = mapOf(
        "RELIANCE" to "2885",
        "TCS" to "11536",
        "HDFCBANK" to "1333",
        "INFY" to "1594",
        "TATAMOTORS" to "3456",
        "ADANIENT" to "25",
        "ADANIPORTS" to "15083",
        "SBIN" to "3045",
        "ICICIBANK" to "4963",
        "BHARTIARTL" to "10604",
        "HCLTECH" to "7229",
        "WIPRO" to "3787",
        "ZOMATO" to "5097",
        "ITC" to "1660",
        "LT" to "11483"
    )

    suspend fun placeRealOrder(
        clientId: String,
        accessToken: String,
        symbol: String,
        transactionType: String, // SELL or BUY
        quantity: Int = 1
    ): DhanOrderResponse = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || accessToken.isBlank()) {
            return@withContext DhanOrderResponse(
                orderId = null,
                orderStatus = "FAILED",
                remarks = "Missing Dhan Client ID or Access Token",
                errorCode = "DH_AUTH_MISSING",
                errorMessage = "Configure Dhan credentials in Settings or Setup"
            )
        }

        val upper = symbol.uppercase().trim()
        val secId = securityIdMap[upper] ?: "2885" // Fallback to Reliance security ID

        val payload = DhanOrderRequest(
            dhanClientId = clientId.trim(),
            transactionType = transactionType.uppercase().trim(),
            securityId = secId,
            quantity = quantity
        )

        try {
            val jsonBody = gson.toJson(payload)
            val request = Request.Builder()
                .url("https://api.dhan.co/v2/orders")
                .header("access-token", accessToken.trim())
                .header("client-id", clientId.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (response.isSuccessful && respBody.isNotBlank()) {
                gson.fromJson(respBody, DhanOrderResponse::class.java)
            } else {
                val errorObj = try { gson.fromJson(respBody, JsonObject::class.java) } catch (e: Exception) { null }
                val errorMsg = errorObj?.get("errorMessage")?.asString ?: "HTTP ${response.code}: $respBody"
                DhanOrderResponse(
                    orderId = null,
                    orderStatus = "REJECTED",
                    remarks = "Broker Error",
                    errorCode = "DH_${response.code}",
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            DhanOrderResponse(
                orderId = null,
                orderStatus = "NETWORK_ERROR",
                remarks = e.message ?: "Failed to connect to Dhan API",
                errorCode = "DH_NET_ERR",
                errorMessage = e.localizedMessage
            )
        }
    }

    suspend fun fetchRealPositions(clientId: String, accessToken: String): List<DhanPosition> = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || accessToken.isBlank()) return@withContext emptyList()

        try {
            val request = Request.Builder()
                .url("https://api.dhan.co/v2/positions")
                .header("access-token", accessToken.trim())
                .header("client-id", clientId.trim())
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: return@withContext emptyList()

            if (response.isSuccessful) {
                val positions = gson.fromJson(respBody, Array<DhanPosition>::class.java)
                positions?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun testConnection(clientId: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || accessToken.isBlank()) return@withContext false
        try {
            val request = Request.Builder()
                .url("https://api.dhan.co/v2/fundlimit")
                .header("access-token", accessToken.trim())
                .header("client-id", clientId.trim())
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
