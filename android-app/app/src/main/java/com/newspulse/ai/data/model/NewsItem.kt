package com.newspulse.ai.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Immutable
data class NewsItem(
    val source: String,
    val headline: String,
    val url: String = "",
    val publishedAt: String = "",
    val fingerprint: String = computeFingerprint(headline)
) {
    companion object {
        fun computeFingerprint(text: String): String {
            val normalized = text.lowercase().trim().replace(Regex("\\s+"), " ")
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(normalized.toByteArray(Charsets.UTF_8))
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }
}

@Entity(tableName = "seen_news")
data class SeenNews(
    @PrimaryKey
    val fingerprint: String,
    val seenAt: Long = System.currentTimeMillis()
)
