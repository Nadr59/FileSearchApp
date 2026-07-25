package com.filemanager.search.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemanager.search.data.FileItem
import com.filemanager.search.data.FileRepository
import com.filemanager.search.data.FileSource
import com.filemanager.search.data.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FileSearchUiState(
    val hasPermission: Boolean = false,
    val selectedFileType: FileType = FileType.ALL,
    val isSearching: Boolean = false,
    val showResults: Boolean = false,
    val allResults: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val filteredResults: List<FileItem> = emptyList(),
    val selectedFileIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val selectedFileInfo: FileItem? = null,
    val deleteResult: String? = null,
    val error: String? = null
)

class FileSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)

    private val _uiState = MutableStateFlow(FileSearchUiState())
    val uiState: StateFlow<FileSearchUiState> = _uiState.asStateFlow()

    private val _pendingDeleteUris = MutableStateFlow<List<Uri>>(emptyList())
    val pendingDeleteUris: StateFlow<List<Uri>> = _pendingDeleteUris.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    fun onFileTypeSelected(type: FileType) {
        _uiState.value = _uiState.value.copy(selectedFileType = type)
    }

    // ═══════════════════════════════════════════
    // البحث
    // ═══════════════════════════════════════════
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

    // ═══════════════════════════════════════════
    // بحث فوري في النتائج
    // ═══════════════════════════════════════════
    fun onSearchQueryChanged(query: String) {
        val all = _uiState.value.allResults
        val filtered = if (query.isBlank()) all
        else all.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.path.contains(query, ignoreCase = true)
        }
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredResults = filtered
        )
    }

    // ═══════════════════════════════════════════
    // التحديد
    // ═══════════════════════════════════════════
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
        _uiState.value = _uiState.value.copy(
            selectedFileIds = emptySet(),
            isSelectionMode = false
        )
    }

    // ═══════════════════════════════════════════
    // الحذف
    // ═══════════════════════════════════════════
    fun onDeleteClicked() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun onDeleteConfirmed() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)

        val toDelete = _uiState.value.allResults.filter {
            it.id in _uiState.value.selectedFileIds
        }

        if (toDelete.isEmpty()) return

        // ═══ تحديد: هل نحتاج طلب النظام؟ ═══
        val hasMediaStoreFiles = toDelete.any { it.source == FileSource.MEDIASTORE }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasMediaStoreFiles) {
            // ═══ Android 11+: استخدم createDeleteRequest للملفات من MediaStore ═══
            val uris = toDelete
                .filter { it.source == FileSource.MEDIASTORE }
                .map { repository.getContentUri(it.id) }

            if (uris.isNotEmpty()) {
                _pendingDeleteUris.value = uris
            }

            // احذف ملفات FileSystem مباشرة
            val fsFiles = toDelete.filter { it.source == FileSource.FILESYSTEM }
            if (fsFiles.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteFiles(fsFiles)
                    withContext(Dispatchers.Main) {
                        refreshResults()
                    }
                }
            }
        } else {
            // ═══ Android 10 وأقل: حذف مباشر ═══
            viewModelScope.launch(Dispatchers.IO) {
                val (success, _) = repository.deleteFiles(toDelete)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        deleteResult = if (success) "Deleted ${toDelete.size} file(s)" else "Some files could not be deleted"
                    )
                    refreshResults()
                }
            }
        }
    }

    fun onDeleteCancelled() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun onDeleteRequestCompleted(success: Boolean) {
        _pendingDeleteUris.value = emptyList()
        _uiState.value = _uiState.value.copy(
            deleteResult = if (success) "Files deleted" else "Delete cancelled"
        )
        if (success) refreshResults()
    }

    fun onDeleteSingleFile(file: FileItem) {
        _uiState.value = _uiState.value.copy(
            selectedFileIds = setOf(file.id),
            selectedFileInfo = null,
            showDeleteDialog = true
        )
    }

    fun onDeleteResultShown() {
        _uiState.value = _uiState.value.copy(deleteResult = null)
    }

    private fun refreshResults() {
        _uiState.value = _uiState.value.copy(
            selectedFileIds = emptySet(),
            isSelectionMode = false
        )
        searchFiles()
    }

    // ═══════════════════════════════════════════
    // معلومات الملف
    // ═══════════════════════════════════════════
    fun onFileInfoRequested(file: FileItem) {
        _uiState.value = _uiState.value.copy(selectedFileInfo = file)
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
