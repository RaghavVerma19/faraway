package com.newspulse.ai.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "filings")
data class Filing(
    @PrimaryKey
    val hash: String,
    val source: String,
    val symbol: String,
    val company: String,
    val filingType: String,
    val title: String,
    val url: String,
    val publishedAt: String,
    val severity: Int,
    val fetchedAt: Long = System.currentTimeMillis()
)
