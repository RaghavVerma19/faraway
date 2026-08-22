package com.newspulse.ai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.newspulse.ai.data.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY symbol ASC")
    fun getWatchlist(): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM watchlist")
    suspend fun getWatchlistSync(): List<WatchlistItem>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    suspend fun isSymbolOnWatchlist(symbol: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToWatchlist(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    @Query("SELECT COUNT(*) FROM watchlist")
    fun getWatchlistCount(): Flow<Int>
}
