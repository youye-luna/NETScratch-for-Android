package youyeluna.lanipscanner.data

import kotlinx.coroutines.flow.Flow
import youyeluna.lanipscanner.model.SpeedTestHistory

class SpeedTestHistoryRepository(private val dao: SpeedTestHistoryDao) {
    val allHistory: Flow<List<SpeedTestHistory>> = dao.getAll()

    suspend fun save(record: SpeedTestHistory) {
        dao.insert(record)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun cleanByCount(maxCount: Int) {
        val totalCount = dao.getCount()
        if (totalCount <= maxCount) return
        val toDelete = totalCount - maxCount
        for (i in 0 until toDelete) {
            val oldestId = dao.getIdAtOffset(totalCount - 1 - i)
            if (oldestId != null) {
                dao.deleteById(oldestId)
            }
        }
    }
}
