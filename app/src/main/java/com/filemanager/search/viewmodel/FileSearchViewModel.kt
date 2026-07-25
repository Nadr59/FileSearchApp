package com.filemanager.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.search.data.FileItem
import com.filemanager.search.data.FileRepository
import com.filemanager.search.data.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FileSearchUiState(
    val hasPermission: Boolean = false,
    val hasDeletePermission: Boolean = false,
    val selectedFileType: FileType = FileType.ALL,
    val isSearching: Boolean = false,
    val showResults: Boolean = false,
    val allResults: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val filteredResults: List<FileItem> = emptyList(),
    val selectedFileIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDeletePermissionDialog: Boolean = false,
    val selectedFileInfo: FileItem? = null,
    val deleteMessage: String? = null,
    val error: String? = null
)

class FileSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)

    private val _uiState = MutableStateFlow(FileSearchUiState())
    val uiState: StateFlow<FileSearchUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    fun onDeletePermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasDeletePermission = granted,
            showDeletePermissionDialog = false
        )
        // إذا منح الإذن: نفذ الحذف مباشرة
        if (granted) {
            doDelete()
        }
    }

    fun onFileTypeSelected(type: FileType) {
        _uiState.value = _uiState.value.copy(selectedFileType = type)
    }

    fun searchFiles() {
        val type = _uiState.value.selectedFileType
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = repository.searchFiles(type)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        showResults = true,
                        allResults = files,
                        filteredResults = files,
                        searchQuery = "",
                        selectedFileIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val all = _uiState.value.allResults
        val filtered = if (query.isBlank()) all
        else all.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.path.contains(query, ignoreCase = true)
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredResults = filtered)
    }

    fun onFileClick(file: FileItem) {
        if (_uiState.value.isSelectionMode) {
            toggleSelection(file.id)
        } else {
            _uiState.value = _uiState.value.copy(selectedFileInfo = file)
        }
    }

    fun onFileLongPress(file: FileItem) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedFileIds = setOf(file.id)
        )
    }

    private fun toggleSelection(fileId: Long) {
        val current = _uiState.value.selectedFileIds
        val newSet = if (fileId in current) current - fileId else current + fileId
        _uiState.value = _uiState.value.copy(
            selectedFileIds = newSet,
            isSelectionMode = newSet.isNotEmpty()
        )
    }

    fun selectAll() {
        val ids = _uiState.value.filteredResults.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedFileIds = ids)
    }

    fun deselectAll() {
        _uiState.value = _uiState.value.copy(selectedFileIds = emptySet(), isSelectionMode = false)
    }

    // ═══════════════════════════════════════════
    // الحذف
    // ═══════════════════════════════════════════
    fun onDeleteClicked() {
        // تحقق من إذن الحذف أولاً
        if (_uiState.value.hasDeletePermission) {
            _uiState.value = _uiState.value.copy(showDeleteDialog = true)
        } else {
            // اطلب إذن الحذف
            _uiState.value = _uiState.value.copy(showDeletePermissionDialog = true)
        }
    }

    fun onDeleteConfirmDialogDismissed() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun onDeletePermissionDialogDismissed() {
        _uiState.value = _uiState.value.copy(showDeletePermissionDialog = false)
    }

    fun onDeleteConfirmed() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
        doDelete()
    }

    private fun doDelete() {
        val toDelete = _uiState.value.allResults.filter {
            it.id in _uiState.value.selectedFileIds
        }.distinctBy { it.path }

        if (toDelete.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteFiles(toDelete)
            withContext(Dispatchers.Main) {
                val message = if (result.first > 0) {
                    "Deleted ${result.first} file(s)"
                } else {
                    "Could not delete. Please grant storage permission in Settings."
                }
                _uiState.value = _uiState.value.copy(
                    deleteMessage = message,
                    selectedFileIds = emptySet(),
                    isSelectionMode = false
                )
                searchFiles()
            }
        }
    }

    fun onDeleteSingleFile(file: FileItem) {
        if (_uiState.value.hasDeletePermission) {
            viewModelScope.launch(Dispatchers.IO) {
                val deleted = repository.deleteSingleFile(file)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        selectedFileInfo = null,
                        deleteMessage = if (deleted) "Deleted" else "Could not delete"
                    )
                    searchFiles()
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                selectedFileInfo = null,
                showDeletePermissionDialog = true
            )
        }
    }

    fun onDeleteMessageShown() {
        _uiState.value = _uiState.value.copy(deleteMessage = null)
    }

    fun onFileInfoDismissed() {
        _uiState.value = _uiState.value.copy(selectedFileInfo = null)
    }

    fun onBackPressed() {
        val s = _uiState.value
        when {
            s.isSelectionMode -> {
                _uiState.value = s.copy(selectedFileIds = emptySet(), isSelectionMode = false)
            }
            s.selectedFileInfo != null -> {
                _uiState.value = s.copy(selectedFileInfo = null)
            }
            s.showResults -> {
                _uiState.value = s.copy(
                    showResults = false,
                    allResults = emptyList(),
                    filteredResults = emptyList(),
                    searchQuery = ""
                )
            }
        }
    }
}
