package youyeluna.lanipscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import youyeluna.lanipscanner.model.ScanHistory
import youyeluna.lanipscanner.model.ScanHistoryItem
import youyeluna.lanipscanner.model.SpeedTestHistory

@Database(
    entities = [ScanHistory::class, ScanHistoryItem::class, SpeedTestHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun speedTestHistoryDao(): SpeedTestHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lan_scanner_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}