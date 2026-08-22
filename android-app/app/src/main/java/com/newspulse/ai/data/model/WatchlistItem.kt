package com.newspulse.ai.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey
    val symbol: String,
    val companyName: String,
    val addedAt: Long = System.currentTimeMillis()
)
