package com.newspulse.ai.data.remote

import android.util.Xml
import com.newspulse.ai.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

class RssParser(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    val defaultRssSources = listOf(
        Pair("et_markets", "https://economictimes.indiatimes.com/markets/rssfeeds/21727745.cms"),
        Pair("moneycontrol_latest", "https://www.moneycontrol.com/rss/latestnews.xml"),
        Pair("moneycontrol_market", "https://www.moneycontrol.com/rss/marketreports.xml"),
        Pair("moneycontrol_buzzing", "https://www.moneycontrol.com/rss/buzzingstocks.xml"),
        Pair("mint_markets", "https://www.livemint.com/rss/markets"),
        Pair("mint_companies", "https://www.livemint.com/rss/companies"),
        Pair("cnbc_tv18", "https://www.cnbctv18.com/commonfeeds/v1/cne/rss/market.xml"),
        Pair("business_standard", "https://www.business-standard.com/rss/markets-106.rss"),
        Pair("business_standard_companies", "https://www.business-standard.com/rss/companies-101.rss"),
        Pair("financial_express", "https://www.financialexpress.com/market/feed/"),
        Pair("ndtv_profit", "https://news.google.com/rss/search?q=site:ndtvprofit.com+stocks&hl=en-IN&gl=IN&ceid=IN:en"),
        Pair("the_hindu_businessline", "https://www.thehindubusinessline.com/markets/feeder/default.rss"),
        Pair("zee_business", "https://news.google.com/rss/search?q=site:zeebiz.com+markets+stocks&hl=en-IN&gl=IN&ceid=IN:en"),
        Pair("reuters_in", "https://news.google.com/rss/search?q=site:reuters.com+stocks+india&hl=en-IN&gl=IN&ceid=IN:en"),
        Pair("google_market_crash", "https://news.google.com/rss/search?q=NSE+OR+BSE+OR+Nifty+crash+OR+plunge+OR+probe+OR+fraud+OR+penalty&hl=en-IN&gl=IN&ceid=IN:en"),
        Pair("google_sebi_action", "https://news.google.com/rss/search?q=SEBI+probe+OR+SEBI+order+OR+auditor+resignation+OR+penalty&hl=en-IN&gl=IN&ceid=IN:en"),
        Pair("google_corporate_actions", "https://news.google.com/rss/search?q=Tata+OR+Reliance+OR+Adani+OR+Infosys+OR+HDFC+probe+OR+results+OR+loss&hl=en-IN&gl=IN&ceid=IN:en"),
        Pair("google_earnings_miss", "https://news.google.com/rss/search?q=quarterly+results+loss+OR+downgrade+OR+target+cut+stocks+NSE&hl=en-IN&gl=IN&ceid=IN:en")
    )

    suspend fun fetchRssFeed(sourceName: String, url: String, limit: Int = 20): List<NewsItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<NewsItem>()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()

            val response = client.newCall(request).execute()
            val xmlData = response.body?.string() ?: return@withContext emptyList()

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlData))

            var eventType = parser.eventType
            var insideItem = false
            var currentTitle = ""
            var currentLink = ""
            var currentPubDate = ""

            while (eventType != XmlPullParser.END_DOCUMENT && items.size < limit) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentPubDate = ""
                        } else if (insideItem) {
                            if (tagName.equals("title", ignoreCase = true)) {
                                currentTitle = parser.nextText()?.trim() ?: ""
                            } else if (tagName.equals("link", ignoreCase = true)) {
                                currentLink = parser.nextText()?.trim() ?: ""
                            } else if (tagName.equals("pubDate", ignoreCase = true) || tagName.equals("published", ignoreCase = true)) {
                                currentPubDate = parser.nextText()?.trim() ?: ""
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) && insideItem) {
                            insideItem = false
                            if (currentTitle.isNotBlank()) {
                                items.add(
                                    NewsItem(
                                        source = sourceName,
                                        headline = currentTitle,
                                        url = currentLink,
                                        publishedAt = currentPubDate
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Silently ignore individual feed timeouts to let other feeds succeed
        }
        items
    }
}
