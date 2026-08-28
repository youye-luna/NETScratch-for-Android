package youyeluna.lanipscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import youyeluna.lanipscanner.model.ScanHistory
import youyeluna.lanipscanner.model.ScanHistoryItem

@Dao
interface ScanHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ScanHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItems(items: List<ScanHistoryItem>)

    @Transaction
    suspend fun insertScanWithItems(history: ScanHistory, items: List<ScanHistoryItem>) {
        val historyId = insertHistory(history)
        val itemsWithId = items.map { it.copy(historyId = historyId) }
        insertHistoryItems(itemsWithId)
    }

    @Query("SELECT * FROM scan_history ORDER BY scanTime DESC")
    fun getAllHistory(): Flow<List<ScanHistory>>

    @Query("SELECT * FROM scan_history_items WHERE historyId = :historyId")
    fun getHistoryItems(historyId: Long): Flow<List<ScanHistoryItem>>

    @Query("SELECT * FROM scan_history WHERE id = :historyId LIMIT 1")
    suspend fun getHistoryById(historyId: Long): ScanHistory?

    @Query("DELETE FROM scan_history WHERE id = :historyId")
    suspend fun deleteHistory(historyId: Long)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllHistory()

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getHistoryCount(): Flow<Int>

    @Query("DELETE FROM scan_history WHERE scanTime < :timestamp")
    suspend fun deleteHistoryOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM scan_history ORDER BY scanTime DESC")
    suspend fun getHistoryCountValue(): Int

    @Query("SELECT id FROM scan_history ORDER BY scanTime DESC LIMIT 1 OFFSET :offset")
    suspend fun getHistoryIdAtOffset(offset: Int): Long?
}