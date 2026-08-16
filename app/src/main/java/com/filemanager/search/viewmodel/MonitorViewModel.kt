package com.filemanager.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.search.data.monitor.AppFilter
import com.filemanager.search.data.monitor.AppProcessInfo
import com.filemanager.search.data.monitor.MemoryRepository
import com.filemanager.search.data.monitor.MonitorData
import com.filemanager.search.data.monitor.SystemMemoryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonitorUiState(
    val isLoading: Boolean = true,
    val systemMemory: SystemMemoryInfo = SystemMemoryInfo.EMPTY,
    val allApps: List<AppProcessInfo> = emptyList(),
    val filteredApps: List<AppProcessInfo> = emptyList(),
    val topMemoryApps: List<AppProcessInfo> = emptyList(),
    val filter: AppFilter = AppFilter.ALL,
    val searchQuery: String = "",
    val refreshIntervalMs: Long = 5000L,
    val isMonitoring: Boolean = false,
    val hasProcessAccess: Boolean = false,
    val hasUsageStatsAccess: Boolean = false,
    val dataNote: String? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MemoryRepository(application)

    private val _filter = MutableStateFlow(AppFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _refreshIntervalMs = MutableStateFlow(5000L)
    private val _isMonitoring = MutableStateFlow(false)

    // تدفق المراقبة — يُحدّث تلقائياً بالفترة المحددة
    private val pollingFlow = combine(
        _refreshIntervalMs,
        _isMonitoring
    ) { interval, monitoring ->
        Pair(interval, monitoring)
    }.flatMapLatest { (interval, monitoring) ->
        flow {
            while (true) {
                if (monitoring) {
                    try {
                        val data = repository.getMonitorData()
                        emit(data)
                    } catch (_: Exception) {
                        emit(MonitorData.EMPTY)
                    }
                }
                kotlinx.coroutines.delay(interval)
            }
        }.flowOn(Dispatchers.IO)
    }.onStart {
        // تحميل أولي حتى بدون مراقبة
        try {
            emit(repository.getMonitorData())
        } catch (_: Exception) {
            emit(MonitorData.EMPTY)
        }
    }

    val uiState: StateFlow<MonitorUiState> = combine(
        pollingFlow,
        _filter,
        _searchQuery,
        _refreshIntervalMs,
        _isMonitoring
    ) { data, filter, query, interval, monitoring ->
        val topMemory = data.appList
            .filter { it.memoryKb > 0 }
            .sortedByDescending { it.memoryKb }
            .take(10)

        val filtered = applyFilters(data.appList, filter, query)

        MonitorUiState(
            isLoading = false,
            systemMemory = data.systemMemory,
            allApps = data.appList,
            filteredApps = filtered,
            topMemoryApps = topMemory,
            filter = filter,
            searchQuery = query,
            refreshIntervalMs = interval,
            isMonitoring = monitoring,
            hasProcessAccess = data.hasProcessAccess,
            hasUsageStatsAccess = data.hasUsageStatsAccess,
            dataNote = data.dataNote
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonitorUiState()
    )

    private fun applyFilters(
        apps: List<AppProcessInfo>,
        filter: AppFilter,
        query: String
    ): List<AppProcessInfo> {
        val filtered = when (filter) {
            AppFilter.ALL -> apps
            AppFilter.SYSTEM -> apps.filter { it.isSystemApp }
            AppFilter.USER -> apps.filter { !it.isSystemApp }
            AppFilter.TOP_MEMORY -> apps.filter { it.memoryKb > 0 }
                .sortedByDescending { it.memoryKb }
            AppFilter.RUNNING -> apps.filter { it.isRunning }
        }

        return if (query.isBlank()) filtered
        else filtered.filter {
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    // ═══════════════════════════════════════════
    // الأحداث
    // ═══════════════════════════════════════════

    fun onFilterSelected(filter: AppFilter) {
        _filter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onRefreshIntervalChanged(intervalMs: Long) {
        _refreshIntervalMs.value = intervalMs
    }

    fun onToggleMonitoring(enabled: Boolean) {
        _isMonitoring.value = enabled
    }

    fun onRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getMonitorData()
                // التدفق سيُحدّث تلقائياً
            } catch (_: Exception) {}
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        return repository.hasUsageStatsPermission()
    }
}
