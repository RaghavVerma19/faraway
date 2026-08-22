package com.newspulse.ai.service

import java.util.Calendar
import java.util.TimeZone

object MarketSchedule {
    private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")

    fun isMarketHours(): Boolean {
        val calendar = Calendar.getInstance(IST_TIMEZONE)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Saturday (7) and Sunday (1) are closed
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        // Indian Market Trading Session: 09:15 AM (555 mins) to 03:30 PM (930 mins) IST
        return totalMinutes in 555..930
    }

    fun getMarketStatusDescription(): String {
        val calendar = Calendar.getInstance(IST_TIMEZONE)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return "Market Closed (Weekend)"
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        return when {
            totalMinutes < 540 -> "Market Closed (Pre-market at 9:00 AM)"
            totalMinutes in 540..554 -> "Pre-Market Session"
            totalMinutes in 555..930 -> "Market Live (Active Surveillance)"
            totalMinutes in 931..1080 -> "Post-Market / Filings Window"
            else -> "Market Closed"
        }
    }
}
