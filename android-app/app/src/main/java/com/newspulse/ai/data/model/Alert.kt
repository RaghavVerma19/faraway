package com.newspulse.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class SeverityTier {
    CRITICAL,
    HIGH,
    WATCH,
    IGNORE
}

data class SignalDetail(
    val type: String,
    val source: String,
    val weight: Int,
    val detail: String,
    val eventTypeHash: String? = null
)

@Entity(tableName = "alerts")
@TypeConverters(AlertConverters::class)
data class Alert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tier: SeverityTier,
    val trustScore: Int,
    val symbol: String,
    val company: String,
    val headline: String,
    val action: String, // SELL, WATCH, BUY_DIP, HEDGE
    val signals: List<SignalDetail>,
    val reasoning: String,
    val impactPct: Double = 0.0,
    val source: String,
    
    // Multi-Agent Deliberation Committee Outputs
    val forensicsVerdict: String = "",
    val forensicsScore: Int = 0,
    val contagionPeers: List<String> = emptyList(),
    val contagionRationale: String = "",
    val quantAction: String = "HOLD",
    val circuitRisk: String = "LOW",
    val hedgingStrategy: String = ""
)

class AlertConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromTier(tier: SeverityTier): String = tier.name

    @TypeConverter
    fun toTier(value: String): SeverityTier = try {
        SeverityTier.valueOf(value)
    } catch (e: Exception) {
        SeverityTier.WATCH
    }

    @TypeConverter
    fun fromSignalList(signals: List<SignalDetail>): String = gson.toJson(signals)

    @TypeConverter
    fun toSignalList(value: String): List<SignalDetail> {
        val type = object : TypeToken<List<SignalDetail>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
