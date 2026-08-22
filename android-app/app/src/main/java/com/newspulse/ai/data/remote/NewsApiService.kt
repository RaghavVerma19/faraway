package com.newspulse.ai.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.newspulse.ai.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class NewsApiArticle(
    val title: String?,
    val url: String?,
    val publishedAt: String?
)

data class NewsApiResponse(
    val status: String?,
    val articles: List<NewsApiArticle>?
)

data class FinnhubNewsItem(
    val headline: String?,
    val url: String?,
    val datetime: Long?
)

class NewsApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    suspend fun fetchNewsApiHeadlines(apiKey: String, query: String = "Nifty OR Sensex OR SEBI OR crash OR probe OR earnings"): List<NewsItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://newsapi.org/v2/everything?q=$encodedQuery&language=en&sortBy=publishedAt&pageSize=15&apiKey=$apiKey"

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val data = gson.fromJson(body, NewsApiResponse::class.java)

            data.articles?.mapNotNull { art ->
                val title = art.title ?: return@mapNotNull null
                NewsItem(
                    source = "newsapi",
                    headline = title,
                    url = art.url ?: "",
                    publishedAt = art.publishedAt ?: ""
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchFinnhubHeadlines(apiKey: String): List<NewsItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val url = "https://finnhub.io/api/v1/news?category=general&token=$apiKey"

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val items = gson.fromJson(body, Array<FinnhubNewsItem>::class.java)

            items.take(15).mapNotNull { item ->
                val headline = item.headline ?: return@mapNotNull null
                NewsItem(
                    source = "finnhub",
                    headline = headline,
                    url = item.url ?: "",
                    publishedAt = item.datetime?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
