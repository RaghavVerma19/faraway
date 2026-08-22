package com.newspulse.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class LiveQuote(
    val symbol: String,
    val ltp: Double,
    val change: Double = 0.0,
    val changePct: Double = 0.0,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val previousClose: Double = 0.0,
    val volume: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "portfolio_holdings")
data class PortfolioHolding(
    @PrimaryKey
    val symbol: String,
    val companyName: String,
    val quantity: Int,
    val avgBuyPrice: Double,
    val currentLtp: Double = avgBuyPrice,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val investedValue: Double get() = quantity * avgBuyPrice
    val currentValue: Double get() = quantity * currentLtp
    val unrealizedPnL: Double get() = currentValue - investedValue
    val unrealizedPnLPct: Double get() = if (investedValue > 0) (unrealizedPnL / investedValue) * 100.0 else 0.0
}

@Entity(tableName = "paper_trade_orders")
data class PaperTradeOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String,
    val companyName: String,
    val action: String, // SELL, BUY, HEDGE_PUT
    val quantity: Int,
    val executionPrice: Double,
    val totalAmount: Double = quantity * executionPrice,
    val triggerAlertHeadline: String = "",
    val capitalProtected: Double = 0.0, // Amount saved vs holding through drop
    val brokerStatus: String = "EXECUTED (SANDBOX)"
)
