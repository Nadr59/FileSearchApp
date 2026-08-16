package com.filemanager.search.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filemanager.search.data.FileItem
import com.filemanager.search.data.FileType
import com.filemanager.search.ui.components.DeleteConfirmDialog
import com.filemanager.search.ui.components.FileInfoBottomSheet
import com.filemanager.search.ui.components.FileItemCard
import com.filemanager.search.viewmodel.FileSearchViewModel
import com.filemanager.search.viewmodel.SortField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSearchScreen(
    viewModel: FileSearchViewModel = viewModel(),
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalysis: (FileItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }

    // ═══════════════════════════════════════════
    // فحص الصلاحيات
    // ═══════════════════════════════════════════
    fun hasReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasWritePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    // ═══════════════════════════════════════════
    // مطلقات الصلاحيات
    // ═══════════════════════════════════════════
    val allPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionResult(hasReadPermission())
        viewModel.onDeletePermissionResult(hasWritePermission())
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onPermissionResult(hasReadPermission())
        viewModel.onDeletePermissionResult(hasWritePermission())
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionResult(hasReadPermission())
        viewModel.onDeletePermissionResult(hasWritePermission())
    }

    // ═══ معالجة النص المُشارك ═══
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            viewModel.onFileTypeSelected(FileType.ALL)
            viewModel.searchFiles(initialQuery = sharedText)
            onSharedTextConsumed()
        }
    }

    // ═══════════════════════════════════════════
    // طلب إذن القراءة
    // ═══════════════════════════════════════════
    fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                manageStorageLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else {
            allPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // ═══════════════════════════════════════════
    // طلب إذن الكتابة (للحذف)
    // ═══════════════════════════════════════════
    fun requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                manageStorageLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else {
            allPermissionsLauncher.launch(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    // ═══ رسالة الحذف ═══
    LaunchedEffect(uiState.deleteMessage) {
        uiState.deleteMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDeleteMessageShown()
        }
    }

    BackHandler(
        enabled = uiState.showResults ||
                uiState.isSelectionMode ||
                uiState.selectedFileInfo != null
    ) {
        viewModel.onBackPressed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.showResults) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.isSelectionMode) {
                                "${uiState.selectedFileIds.size} selected"
                            } else {
                                "Results (${uiState.filteredResults.size})"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onBackPressed() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.isSelectionMode) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Select All")
                            }
                            IconButton(onClick = { viewModel.onDeleteClicked() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortField.entries.forEach { field ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(field.displayName)
                                                    if (uiState.sortField == field) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = if (uiState.sortAscending) "↑" else "↓",
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.onSortFieldSelected(field)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!uiState.hasPermission) {
                PermissionScreen(onRequest = { requestReadPermission() })
            } else if (!uiState.showResults) {
                SearchSetupScreen(
                    selectedType = uiState.selectedFileType,
                    isSearching = uiState.isSearching,
                    onTypeSelected = { viewModel.onFileTypeSelected(it) },
                    onSearch = { viewModel.searchFiles() },
                    onSettings = onNavigateToSettings
                )
            } else {
                ResultsContent(
                    uiState = uiState,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onFileClick = { viewModel.onFileClick(it) },
                    onFileLongPress = { viewModel.onFileLongPress(it) },
                    onDeselectAll = { viewModel.deselectAll() }
                )
            }
        }
    }

    // ═══ معلومات الملف ═══
    if (uiState.selectedFileInfo != null) {
        FileInfoBottomSheet(
            file = uiState.selectedFileInfo!!,
            onDismiss = { viewModel.onFileInfoDismissed() },
            onDelete = { viewModel.onDeleteSingleFile(uiState.selectedFileInfo!!) },
            onAnalyze = {
                val file = uiState.selectedFileInfo!!
                viewModel.onFileInfoDismissed()
                onNavigateToAnalysis(file)
            }
        )
    }

    // ═══ تأكيد الحذف ═══
    if (uiState.showDeleteDialog) {
        DeleteConfirmDialog(
            count = uiState.selectedFileIds.size,
            onConfirm = { viewModel.onDeleteConfirmed() },
            onDismiss = { viewModel.onDeleteConfirmDialogDismissed() }
        )
    }

    // ═══ طلب إذن الحذف ═══
    if (uiState.showDeletePermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDeletePermissionDialogDismissed() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Permission Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        "Enable 'Allow management of all files' in the next screen."
                    } else {
                        "Grant write permission to storage."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeletePermissionDialogDismissed()
                    requestWritePermission()
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onDeletePermissionDialogDismissed()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// شاشة طلب الإذن
// ═══════════════════════════════════════════════════════════
@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Grant storage access to search and manage files.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// شاشة الإعداد الأولي للبحث
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSetupScreen(
    selectedType: FileType,
    isSearching: Boolean,
    onTypeSelected: (FileType) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Text("\uD83D\uDD0D", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "File Search",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select a file type and tap Search",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    if (!isSearching) expanded = !expanded
                }
            ) {
                OutlinedTextField(
                    value = "${selectedType.emoji}  ${selectedType.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("File Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FileType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text("${type.emoji}  ${type.displayName}")
                            },
                            onClick = {
                                onTypeSelected(type)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Searching...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search Files", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// محتوى النتائج
// ═══════════════════════════════════════════════════════════
@Composable
private fun ResultsContent(
    uiState: com.filemanager.search.viewmodel.FileSearchUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileLongPress: (FileItem) -> Unit,
    onDeselectAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ═══ شريط البحث ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search in results...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (uiState.isSelectionMode) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDeselectAll) {
                    Text("Cancel")
                }
            }
        }

        // ═══ مؤشر الترتيب ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${uiState.filteredResults.size} files",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "·",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Sorted by ${uiState.sortField.displayName} ${if (uiState.sortAscending) "↑" else "↓"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCC2", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) {
                            "No files match '${uiState.searchQuery}'"
                        } else {
                            "No files found"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = uiState.filteredResults,
                    key = { "${it.path}|${it.id}" }
                ) { file ->
                    FileItemCard(
                        file = file,
                        isSelected = file.id in uiState.selectedFileIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongPress(file) }
                    )
                }
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}
