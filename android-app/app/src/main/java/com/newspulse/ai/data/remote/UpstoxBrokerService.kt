package com.newspulse.ai.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.newspulse.ai.data.model.LiveQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class UpstoxOrderRequest(
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("product") val product: String = "D", // D = Delivery (CNC), I = Intraday
    @SerializedName("validity") val validity: String = "DAY",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("tag") val tag: String = "pulse_defense",
    @SerializedName("instrument_token") val instrumentToken: String,
    @SerializedName("order_type") val orderType: String = "MARKET",
    @SerializedName("transaction_type") val transactionType: String, // BUY, SELL
    @SerializedName("disclosed_quantity") val disclosedQuantity: Int = 0,
    @SerializedName("trigger_price") val triggerPrice: Double = 0.0,
    @SerializedName("is_amo") val isAmo: Boolean = false
)

data class UpstoxOrderResponse(
    val status: String,
    val data: UpstoxOrderData?,
    val errors: List<UpstoxApiError>?
)

data class UpstoxOrderData(
    @SerializedName("order_id") val orderId: String?
)

data class UpstoxApiError(
    val errorCode: String?,
    val message: String?,
    val propertyPath: String?
)

data class UpstoxPosition(
    @SerializedName("tradingsymbol") val tradingSymbol: String,
    @SerializedName("instrument_token") val instrumentToken: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("buy_price") val buyPrice: Double,
    @SerializedName("sell_price") val sellPrice: Double,
    @SerializedName("last_price") val lastPrice: Double,
    @SerializedName("pnl") val pnl: Double,
    @SerializedName("product") val product: String
)

class UpstoxBrokerService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Upstox Instrument Tokens for Top Indian Stocks (NSE_EQ)
    val instrumentMap = mapOf(
        "RELIANCE" to "NSE_EQ|INE002A01018",
        "TCS" to "NSE_EQ|INE467B01029",
        "HDFCBANK" to "NSE_EQ|INE040A01034",
        "INFY" to "NSE_EQ|INE009A01021",
        "TATAMOTORS" to "NSE_EQ|INE155A01022",
        "ADANIENT" to "NSE_EQ|INE423A01024",
        "ADANIPORTS" to "NSE_EQ|INE742F01042",
        "SBIN" to "NSE_EQ|INE062A01020",
        "ICICIBANK" to "NSE_EQ|INE090A01021",
        "BHARTIARTL" to "NSE_EQ|INE397D01024",
        "HCLTECH" to "NSE_EQ|INE860A01027",
        "WIPRO" to "NSE_EQ|INE075A01022",
        "ZOMATO" to "NSE_EQ|INE758T01015",
        "ITC" to "NSE_EQ|INE154A01025",
        "LT" to "NSE_EQ|INE018A01030",
        "MARUTI" to "NSE_EQ|INE585B01010",
        "BAJFINANCE" to "NSE_EQ|INE296A01024"
    )

    suspend fun fetchLiveQuotes(
        token: String,
        symbols: List<String>
    ): Map<String, LiveQuote> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext emptyMap()

        val tokensList = symbols.mapNotNull { instrumentMap[it.uppercase()] }.distinct()
        if (tokensList.isEmpty()) return@withContext emptyMap()

        val instrumentsQuery = tokensList.joinToString(",")
        val result = mutableMapOf<String, LiveQuote>()

        try {
            val request = Request.Builder()
                .url("https://api.upstox.com/v2/market-quote/quotes?instrument_key=$instrumentsQuery")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: return@withContext emptyMap()

            if (response.isSuccessful) {
                val jsonObject = gson.fromJson(respBody, JsonObject::class.java)
                val dataObj = jsonObject.getAsJsonObject("data") ?: return@withContext emptyMap()

                for ((sym, instToken) in instrumentMap) {
                    val formattedKey = instToken.replace("|", ":")
                    val quoteObj = dataObj.getAsJsonObject(formattedKey) ?: dataObj.getAsJsonObject(instToken)
                    if (quoteObj != null) {
                        val lastPrice = quoteObj.get("last_price")?.asDouble ?: 0.0
                        val ohlc = quoteObj.getAsJsonObject("ohlc")
                        val closePrice = ohlc?.get("close")?.asDouble ?: lastPrice
                        val dayHigh = ohlc?.get("high")?.asDouble ?: lastPrice
                        val dayLow = ohlc?.get("low")?.asDouble ?: lastPrice
                        val volume = quoteObj.get("volume")?.asLong ?: 0L

                        val change = lastPrice - closePrice
                        val changePct = if (closePrice > 0) (change / closePrice) * 100.0 else 0.0

                        result[sym] = LiveQuote(
                            symbol = sym,
                            ltp = lastPrice,
                            change = change,
                            changePct = changePct,
                            dayHigh = dayHigh,
                            dayLow = dayLow,
                            previousClose = closePrice,
                            volume = volume,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }
        result
    }

    suspend fun placeRealOrder(
        token: String,
        symbol: String,
        transactionType: String = "SELL",
        quantity: Int = 1
    ): UpstoxOrderResponse = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext UpstoxOrderResponse(
                status = "error",
                data = null,
                errors = listOf(UpstoxApiError("AUTH_MISSING", "Missing Upstox Access Token", "token"))
            )
        }

        val upper = symbol.uppercase().trim()
        val instrumentToken = instrumentMap[upper] ?: "NSE_EQ|INE002A01018"

        val payload = UpstoxOrderRequest(
            quantity = quantity,
            product = "D",
            validity = "DAY",
            instrumentToken = instrumentToken,
            orderType = "MARKET",
            transactionType = transactionType.uppercase().trim()
        )

        try {
            val jsonBody = gson.toJson(payload)
            val request = Request.Builder()
                .url("https://api.upstox.com/v2/order/place")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (respBody.isNotBlank()) {
                gson.fromJson(respBody, UpstoxOrderResponse::class.java)
            } else {
                UpstoxOrderResponse(
                    status = "error",
                    data = null,
                    errors = listOf(UpstoxApiError("HTTP_${response.code}", "HTTP error ${response.code}", null))
                )
            }
        } catch (e: Exception) {
            UpstoxOrderResponse(
                status = "error",
                data = null,
                errors = listOf(UpstoxApiError("NETWORK_ERR", e.message ?: "Failed to connect to Upstox API", null))
            )
        }
    }

    suspend fun fetchPositions(token: String): List<UpstoxPosition> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext emptyList()

        try {
            val request = Request.Builder()
                .url("https://api.upstox.com/v2/portfolio/short-term-positions")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: return@withContext emptyList()

            if (response.isSuccessful) {
                val jsonObject = gson.fromJson(respBody, JsonObject::class.java)
                val dataArray = jsonObject.getAsJsonArray("data")
                if (dataArray != null) {
                    val list = mutableListOf<UpstoxPosition>()
                    dataArray.forEach { elem ->
                        list.add(gson.fromJson(elem, UpstoxPosition::class.java))
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }
        emptyList()
    }

    suspend fun testConnection(token: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        try {
            // Test with market quotes endpoint which works with both Analytics & Interactive tokens!
            val request = Request.Builder()
                .url("https://api.upstox.com/v2/market-quote/quotes?instrument_key=NSE_EQ|INE002A01018")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
