package com.newspulse.ai.domain

import java.util.Locale
import kotlin.math.roundToInt

object HeadlineImpactEstimator {
    private val CRASH_IMPACT_MAP = mapOf(
        "collapse" to 15.0, "crash" to 12.0, "plunge" to 10.0, "tumble" to 10.0,
        "crater" to 15.0, "nosedive" to 12.0, "slump" to 8.0, "free fall" to 15.0,
        "plummet" to 10.0, "default" to 10.0, "bankruptcy" to 15.0, "insolvency" to 12.0,
        "resignation" to 8.0, "probe" to 6.0, "scam" to 12.0, "fraud" to 15.0,
        "sebi" to 6.0, "penalty" to 5.0, "fine" to 4.0, "arrest" to 10.0,
        "raids" to 8.0, "loss" to 5.0, "miss" to 5.0, "downgrade" to 5.0
    )

    private val SURGE_IMPACT_MAP = mapOf(
        "surge" to 10.0, "rally" to 8.0, "boom" to 10.0, "record" to 5.0,
        "beat" to 6.0, "upgrade" to 5.0, "outperform" to 5.0, "win" to 5.0,
        "dividend" to 3.0, "buyback" to 4.0, "expansion" to 5.0, "growth" to 4.0
    )

    private val PCT_REGEX = Regex("(\\d+(?:\\.\\d+)?)\\s*%")

    fun estimate(headline: String, sourceCredibility: Double = 1.0): Double {
        if (headline.isBlank()) return 0.0
        val text = headline.lowercase(Locale.ROOT)

        val explicitMatches = PCT_REGEX.findAll(text).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
        if (explicitMatches.isNotEmpty()) {
            val pct = explicitMatches.first()
            if (text.contains("loss") || text.contains("plunge") || text.contains("drop") || text.contains("fall")) {
                return ((-pct * sourceCredibility) * 10.0).roundToInt() / 10.0
            }
            if (text.contains("surge") || text.contains("gain") || text.contains("rally") || text.contains("jump")) {
                return ((pct * sourceCredibility) * 10.0).roundToInt() / 10.0
            }
        }

        for ((kw, impact) in CRASH_IMPACT_MAP) {
            if (text.contains(kw)) {
                return ((-impact * sourceCredibility) * 10.0).roundToInt() / 10.0
            }
        }

        for ((kw, impact) in SURGE_IMPACT_MAP) {
            if (text.contains(kw)) {
                return ((impact * sourceCredibility) * 10.0).roundToInt() / 10.0
            }
        }

        return 0.0
    }

    fun format(impactPct: Double): String {
        return when {
            impactPct > 0 -> "+$impactPct%"
            impactPct < 0 -> "$impactPct%"
            else -> "~"
        }
    }
}
