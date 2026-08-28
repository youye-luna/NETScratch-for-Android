package youyeluna.lanipscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import youyeluna.lanipscanner.App
import youyeluna.lanipscanner.model.ScanHistory
import youyeluna.lanipscanner.model.ScanHistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as App).repository
    private val historyItemsCache = mutableMapOf<Long, StateFlow<List<ScanHistoryItem>>>()

    val allHistory: StateFlow<List<ScanHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getHistoryItems(historyId: Long): StateFlow<List<ScanHistoryItem>> {
        return historyItemsCache.getOrPut(historyId) {
            repository.getHistoryItems(historyId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }
    }

    suspend fun getHistoryById(historyId: Long): ScanHistory? {
        return repository.getHistoryById(historyId)
    }

    fun deleteHistory(historyId: Long) {
        viewModelScope.launch {
            repository.deleteHistory(historyId)
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
        }
    }
}