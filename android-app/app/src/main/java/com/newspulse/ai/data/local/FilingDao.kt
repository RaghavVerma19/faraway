package com.newspulse.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.newspulse.ai.data.model.Filing
import kotlinx.coroutines.flow.Flow

@Dao
interface FilingDao {
    @Query("SELECT * FROM filings ORDER BY fetchedAt DESC LIMIT :limit")
    fun getRecentFilings(limit: Int = 50): Flow<List<Filing>>

    @Query("SELECT * FROM filings WHERE symbol = :symbol ORDER BY fetchedAt DESC LIMIT :limit")
    fun getFilingsForSymbol(symbol: String, limit: Int = 50): Flow<List<Filing>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFiling(filing: Filing)

    @Query("DELETE FROM filings WHERE fetchedAt < :cutoffTimestamp")
    suspend fun clearOldFilings(cutoffTimestamp: Long)
}
