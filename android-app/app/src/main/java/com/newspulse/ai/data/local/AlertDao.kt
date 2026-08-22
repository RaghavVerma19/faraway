package com.newspulse.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.newspulse.ai.data.model.Alert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT :limit")
    fun getAllAlerts(limit: Int = 100): Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE tier = :tier ORDER BY timestamp DESC LIMIT :limit")
    fun getAlertsByTier(tier: String, limit: Int = 100): Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE symbol = :symbol AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentAlertsForSymbol(symbol: String, sinceTimestamp: Long): List<Alert>

    @Query("SELECT COUNT(*) FROM alerts WHERE timestamp >= :sinceTimestamp")
    fun getAlertCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM alerts WHERE tier = 'CRITICAL' AND timestamp >= :sinceTimestamp")
    fun getCriticalCountSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert): Long

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlert(id: Long)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()
}
