package com.newspulse.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.newspulse.ai.data.model.PaperTradeOrder
import com.newspulse.ai.data.model.PortfolioHolding
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_holdings ORDER BY symbol ASC")
    fun getAllHoldings(): Flow<List<PortfolioHolding>>

    @Query("SELECT * FROM portfolio_holdings")
    suspend fun getAllHoldingsSync(): List<PortfolioHolding>

    @Query("SELECT * FROM portfolio_holdings WHERE symbol = :symbol LIMIT 1")
    suspend fun getHoldingBySymbol(symbol: String): PortfolioHolding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: PortfolioHolding)

    @Update
    suspend fun updateHolding(holding: PortfolioHolding)

    @Query("DELETE FROM portfolio_holdings WHERE symbol = :symbol")
    suspend fun deleteHolding(symbol: String)

    @Query("SELECT * FROM paper_trade_orders ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentOrders(limit: Int = 50): Flow<List<PaperTradeOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PaperTradeOrder): Long

    @Query("SELECT SUM(capitalProtected) FROM paper_trade_orders")
    fun getTotalCapitalProtected(): Flow<Double?>
}
