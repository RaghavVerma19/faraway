package com.newspulse.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.newspulse.ai.data.model.Alert
import com.newspulse.ai.data.model.AlertConverters
import com.newspulse.ai.data.model.Filing
import com.newspulse.ai.data.model.PaperTradeOrder
import com.newspulse.ai.data.model.PortfolioHolding
import com.newspulse.ai.data.model.SeenNews
import com.newspulse.ai.data.model.WatchlistItem

@Database(
    entities = [
        Alert::class,
        WatchlistItem::class,
        Filing::class,
        SeenNews::class,
        PortfolioHolding::class,
        PaperTradeOrder::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AlertConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun filingDao(): FilingDao
    abstract fun seenNewsDao(): SeenNewsDao
    abstract fun portfolioDao(): PortfolioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "newspulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
