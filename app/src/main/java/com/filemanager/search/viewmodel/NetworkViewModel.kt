package com.filemanager.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.search.data.monitor.AppFilter
import com.filemanager.search.data.monitor.AppNetworkUsage
import com.filemanager.search.data.monitor.NetworkData
import com.filemanager.search.data.monitor.NetworkRepository
import com.filemanager.search.data.monitor.SystemNetworkStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NetworkUiState(
    val isLoading: Boolean = true,
    val systemStats: SystemNetworkStats = SystemNetworkStats.EMPTY,
    val allApps: List<AppNetworkUsage> = emptyList(),
    val filteredApps: List<AppNetworkUsage> = emptyList(),
    val topApps: List<AppNetworkUsage> = emptyList(),
    val filter: AppFilter = AppFilter.ALL,
    val searchQuery: String = "",
    val hasUsageAccess: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val error: String? = null
)

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NetworkRepository(application)

    private val _data = MutableStateFlow(NetworkData.EMPTY)
    private val _filter = MutableStateFlow(AppFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NetworkUiState> = combine(
        _data, _filter, _searchQuery
    ) { data, filter, query ->
        val topApps = data.appUsageList.take(10)

        val filtered = applyFilters(data.appUsageList, filter, query)

        NetworkUiState(
            isLoading = false,
            systemStats = data.systemStats,
            allApps = data.appUsageList,
            filteredApps = filtered,
            topApps = topApps,
            filter = filter,
            searchQuery = query,
            hasUsageAccess = data.hasUsageAccess,
            isNetworkAvailable = repository.isNetworkAvailable()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkUiState()
    )

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getPerAppUsage()
                _data.value = data
            } catch (_: Exception) {}
        }
    }

    fun onFilterSelected(filter: AppFilter) {
        _filter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun hasUsageStatsPermission(): Boolean {
        return repository.hasUsageStatsPermission()
    }

    private fun applyFilters(
        apps: List<AppNetworkUsage>,
        filter: AppFilter,
        query: String
    ): List<AppNetworkUsage> {
        val filtered = when (filter) {
            AppFilter.ALL -> apps
            AppFilter.SYSTEM -> apps.filter { it.isSystemApp }
            AppFilter.USER -> apps.filter { !it.isSystemApp }
            AppFilter.TOP_MEMORY -> apps // في سياق الشبكة = الأكثر استهلاكاً
            AppFilter.RUNNING -> apps.filter { it.totalBytes > 0 }
        }

        return if (query.isBlank()) filtered
        else filtered.filter {
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }
}
