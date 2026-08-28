package youyeluna.lanipscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import youyeluna.lanipscanner.model.SpeedTestHistory

@Dao
interface SpeedTestHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SpeedTestHistory): Long

    @Query("SELECT * FROM speed_test_history ORDER BY testTime DESC")
    fun getAll(): Flow<List<SpeedTestHistory>>

    @Query("DELETE FROM speed_test_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM speed_test_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM speed_test_history")
    suspend fun getCount(): Int

    @Query("SELECT id FROM speed_test_history ORDER BY testTime DESC LIMIT 1 OFFSET :offset")
    suspend fun getIdAtOffset(offset: Int): Long?
}
