package com.newspulse.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.newspulse.ai.data.model.SeenNews

@Dao
interface SeenNewsDao {
    @Query("SELECT EXISTS(SELECT 1 FROM seen_news WHERE fingerprint = :fingerprint)")
    suspend fun hasSeen(fingerprint: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(item: SeenNews)

    @Query("DELETE FROM seen_news WHERE seenAt < :cutoffTimestamp")
    suspend fun clearOldSeen(cutoffTimestamp: Long)
}
