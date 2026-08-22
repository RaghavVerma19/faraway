package com.newspulse.ai.domain

import android.content.Context
import com.newspulse.ai.data.local.AppDatabase
import com.newspulse.ai.data.model.Alert
import com.newspulse.ai.data.model.NewsItem
import com.newspulse.ai.data.model.SeenNews
import com.newspulse.ai.data.model.SeverityTier
import com.newspulse.ai.data.model.SignalDetail
import com.newspulse.ai.data.preferences.UserPreferences
import com.newspulse.ai.data.remote.GroqApiService
import com.newspulse.ai.data.remote.NewsApiService
import com.newspulse.ai.data.remote.RssParser
import com.newspulse.ai.service.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class CrashEngine(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val preferences: UserPreferences = UserPreferences(context),
    private val rssParser: RssParser = RssParser(),
    private val newsApiService: NewsApiService = NewsApiService(),
    private val groqApiService: GroqApiService = GroqApiService(),
    private val notificationHelper: NotificationHelper = NotificationHelper(context)
) {

    data class ScanResult(
        val totalFetched: Int,
        val newHeadlines: Int,
        val alertsGenerated: Int
    )

    suspend fun runScanCycle(): ScanResult = withContext(Dispatchers.IO) {
        val groqKey = preferences.groqApiKey.value
        val newsApiKey = preferences.newsApiKey.value
        val finnhubKey = preferences.finnhubApiKey.value
        val minThreshold = preferences.minPanicThreshold.value
        val notificationsEnabled = preferences.notificationsEnabled.value

        // 1. Ingest across 18+ sources in parallel
        val allNews = coroutineScope {
            val rssDeferred = rssParser.defaultRssSources.map { (sourceName, url) ->
                async { rssParser.fetchRssFeed(sourceName, url) }
            }
            val newsApiDeferred = async { newsApiService.fetchNewsApiHeadlines(newsApiKey) }
            val finnhubDeferred = async { newsApiService.fetchFinnhubHeadlines(finnhubKey) }

            val combined = mutableListOf<NewsItem>()
            rssDeferred.awaitAll().forEach { combined.addAll(it) }
            combined.addAll(newsApiDeferred.await())
            combined.addAll(finnhubDeferred.await())
            combined
        }

        val uniqueItems = allNews.distinctBy { it.fingerprint }
        val freshItems = mutableListOf<NewsItem>()
        val seenNewsDao = database.seenNewsDao()

        for (item in uniqueItems) {
            if (!seenNewsDao.hasSeen(item.fingerprint)) {
                seenNewsDao.markSeen(SeenNews(item.fingerprint))
                freshItems.add(item)
            }
        }

        val candidateItems = if (freshItems.isNotEmpty()) freshItems else uniqueItems.take(40)

        data class MatchedItem(
            val news: NewsItem,
            val company: CompanyMatch,
            val crashHits: List<String>,
            val surgeHits: List<String>,
            val score: Int
        )

        val symbolGroups = mutableMapOf<String, MutableList<MatchedItem>>()

        for (item in candidateItems) {
            val matchedCompany = CompanyRegistry.resolve(item.headline) ?: continue
            val lowerText = item.headline.lowercase(Locale.ROOT)
            val crashHits = KeywordWeights.CRASH_KEYWORDS.filter { lowerText.contains(it) }
            val surgeHits = KeywordWeights.SURGE_KEYWORDS.filter { lowerText.contains(it) }

            val cred = KeywordWeights.SOURCE_CREDIBILITY[item.source] ?: 0.8
            var score = 0
            if (crashHits.isNotEmpty()) {
                score += if (cred >= 0.9) 35 else 20
                if (crashHits.any { it in listOf("sebi", "fraud", "probe", "arrest", "resignation", "penalty", "loss", "crash", "downgrade") }) {
                    score += 25
                }
            } else if (surgeHits.isNotEmpty()) {
                score += 15
            } else {
                score += 10
            }

            val matched = MatchedItem(item, matchedCompany, crashHits, surgeHits, score)
            symbolGroups.getOrPut(matchedCompany.symbol) { mutableListOf() }.add(matched)
        }

        var alertsCount = 0
        val alertDao = database.alertDao()

        // 4. Autonomous Agent Swarm Consensus
        for ((symbol, matches) in symbolGroups) {
            val primaryMatch = matches.first()
            val companyName = primaryMatch.company.companyName
            val combinedHeadline = matches.maxByOrNull { it.news.headline.length }?.news?.headline ?: primaryMatch.news.headline

            var compositeScore = min(100, matches.sumOf { it.score })
            val signals = mutableListOf<SignalDetail>()

            for (m in matches) {
                if (m.crashHits.isNotEmpty()) {
                    signals.add(
                        SignalDetail(
                            type = "news",
                            source = m.news.source,
                            weight = m.score,
                            detail = "Keywords: ${m.crashHits.take(3).joinToString(", ")}",
                            eventTypeHash = m.news.fingerprint
                        )
                    )
                } else if (m.surgeHits.isNotEmpty()) {
                    signals.add(
                        SignalDetail(
                            type = "surge",
                            source = m.news.source,
                            weight = m.score,
                            detail = "Keywords: ${m.surgeHits.take(3).joinToString(", ")}",
                            eventTypeHash = m.news.fingerprint
                        )
                    )
                }
            }

            var aiReasoning = ""
            var forensicsVerdict = "Standard corporate event"
            var forensicsScore = 15
            var contagionPeers = emptyList<String>()
            var contagionRationale = ""
            var quantAction = "HOLD"
            var circuitRisk = "LOW"
            var hedgingStrategy = ""

            // Multi-Agent Deliberation Swarm via OpenAI GPT-OSS 120B on Groq
            if (groqKey.isNotBlank() && (compositeScore >= 20 || matches.any { it.crashHits.isNotEmpty() })) {
                val committee = groqApiService.analyzeCrashHeadline(
                    apiKey = groqKey,
                    headline = combinedHeadline,
                    source = matches.first().news.source
                )
                if (committee != null) {
                    if (committee.crashProbability >= 30) {
                        compositeScore = min(100, max(compositeScore, committee.crashProbability))
                    }
                    aiReasoning = committee.reasoning
                    forensicsVerdict = committee.forensicsVerdict
                    forensicsScore = committee.forensicsSeverity
                    contagionPeers = committee.contagionPeers
                    contagionRationale = committee.contagionRationale
                    quantAction = committee.quantAction
                    circuitRisk = committee.circuitRisk
                    hedgingStrategy = committee.hedgingStrategy

                    signals.add(
                        SignalDetail(
                            type = "multi_agent_swarm",
                            source = "Pulse Agent Committee",
                            weight = committee.crashProbability,
                            detail = "Forensics: $forensicsVerdict ($forensicsScore/100) • Quant: $quantAction"
                        )
                    )
                }
            }

            val tier = when {
                compositeScore >= 70 -> SeverityTier.CRITICAL
                compositeScore >= 50 -> SeverityTier.HIGH
                compositeScore >= minThreshold -> SeverityTier.WATCH
                else -> SeverityTier.IGNORE
            }

            if (tier != SeverityTier.IGNORE) {
                val recentAlerts = alertDao.getRecentAlertsForSymbol(symbol, System.currentTimeMillis() - (2 * 3600 * 1000L))
                val isDuplicate = recentAlerts.any { it.headline == combinedHeadline || it.tier == tier }

                if (!isDuplicate) {
                    val impactPct = HeadlineImpactEstimator.estimate(combinedHeadline)
                    val alert = Alert(
                        tier = tier,
                        trustScore = compositeScore,
                        symbol = symbol,
                        company = companyName,
                        headline = combinedHeadline,
                        action = if (quantAction != "HOLD") quantAction else if (tier == SeverityTier.CRITICAL) "EXIT_LONG" else "WATCH",
                        signals = signals,
                        reasoning = aiReasoning.ifBlank { signals.joinToString("; ") { it.detail } },
                        impactPct = impactPct,
                        source = matches.map { it.news.source }.distinct().joinToString(", "),
                        forensicsVerdict = forensicsVerdict,
                        forensicsScore = forensicsScore,
                        contagionPeers = contagionPeers,
                        contagionRationale = contagionRationale,
                        quantAction = quantAction,
                        circuitRisk = circuitRisk,
                        hedgingStrategy = hedgingStrategy
                    )

                    val alertId = alertDao.insertAlert(alert)
                    alertsCount++

                    if (notificationsEnabled && (tier == SeverityTier.CRITICAL || tier == SeverityTier.HIGH)) {
                        notificationHelper.showCrashAlert(alert.copy(id = alertId))
                    }
                }
            }
        }

        ScanResult(allNews.size, freshItems.size, alertsCount)
    }
}
