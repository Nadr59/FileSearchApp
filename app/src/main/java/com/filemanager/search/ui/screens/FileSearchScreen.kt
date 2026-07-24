package com.filemanager.search.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.filemanager.search.data.FileType
import com.filemanager.search.ui.components.DeleteConfirmDialog
import com.filemanager.search.ui.components.FileInfoBottomSheet
import com.filemanager.search.ui.components.FileItemCard
import com.filemanager.search.viewmodel.FileSearchUiState
import com.filemanager.search.viewmodel.FileSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSearchScreen(viewModel: FileSearchViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ═══════════════════════════════════════════
    // الصلاحيات
    // ═══════════════════════════════════════════
    val requiredPermissions = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionResult(allGranted)
        if (allGranted) {
            // فتح البحث مباشرة بعد منح الصلاحية
            viewModel.searchFiles()
        }
    }

    fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionResult(checkPermissions())
    }

    // ═══════════════════════════════════════════
    // حذف الملفات (Android 11+)
    // ═══════════════════════════════════════════
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteRequestCompleted(result.resultCode == Activity.RESULT_OK)
    }

    val pendingUris by viewModel.pendingDeleteUris.collectAsState()
    LaunchedEffect(pendingUris) {
        if (pendingUris.isNotEmpty()) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver, pendingUris
                )
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (_: Exception) {
                viewModel.onDeleteRequestCompleted(false)
            }
        }
    }

    BackHandler(
        enabled = uiState.showResults || uiState.isSelectionMode || uiState.selectedFileInfo != null
    ) {
        viewModel.onBackPressed()
    }

    // ═══════════════════════════════════════════
    // إعادة فحص الصلاحيات عند العودة للتطبيق
    // ═══════════════════════════════════════════
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            val granted = checkPermissions()
            if (granted != uiState.hasPermission) {
                viewModel.onPermissionResult(granted)
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.showResults) {
                TopAppBar(
                    title = {
                        Text(
                            if (uiState.isSelectionMode)
                                "${uiState.selectedFileIds.size} selected"
                            else
                                "Results (${uiState.filteredResults.size})"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onBackPressed() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (uiState.isSelectionMode) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.DoneAll, "Select All")
                            }
                            IconButton(onClick = { viewModel.onDeleteClicked() }) {
                                Icon(
                                    Icons.Default.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
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
                PermissionScreen(
                    onRequest = { permissionLauncher.launch(requiredPermissions) }
                )
            } else if (!uiState.showResults) {
                SearchSetupScreen(
                    selectedType = uiState.selectedFileType,
                    isSearching = uiState.isSearching,
                    onTypeSelected = { viewModel.onFileTypeSelected(it) },
                    onSearch = { viewModel.searchFiles() }
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

    if (uiState.selectedFileInfo != null) {
        FileInfoBottomSheet(
            file = uiState.selectedFileInfo!!,
            onDismiss = { viewModel.onFileInfoDismissed() },
            onDelete = { viewModel.onDeleteSingleFile(uiState.selectedFileInfo!!) }
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteConfirmDialog(
            count = uiState.selectedFileIds.size,
            onConfirm = { viewModel.onDeleteConfirmed() },
            onDismiss = { viewModel.onDeleteCancelled() }
        )
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning, null,
                Modifier.size(64.dp),
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
                "This app needs access to your files to search and display them.",
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
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    // فتح إعدادات التطبيق مباشرة
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open App Settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSetupScreen(
    selectedType: FileType,
    isSearching: Boolean,
    onTypeSelected: (FileType) -> Unit,
    onSearch: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 48.sp)
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
                onExpandedChange = { if (!isSearching) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "${selectedType.emoji}  ${selectedType.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("File Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
                            text = { Text("${type.emoji}  ${type.displayName}") },
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
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Searching...")
                } else {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search Files", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ResultsContent(
    uiState: FileSearchUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFileClick: (com.filemanager.search.data.FileItem) -> Unit,
    onFileLongPress: (com.filemanager.search.data.FileItem) -> Unit,
    onDeselectAll: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
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
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (uiState.isSelectionMode) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDeselectAll) { Text("Cancel") }
            }
        }

        if (uiState.isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredResults.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (uiState.searchQuery.isNotEmpty())
                            "No files match '${uiState.searchQuery}'"
                        else
                            "No files found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = uiState.filteredResults,
                    key = { it.id }
                ) { file ->
                    FileItemCard(
                        file = file,
                        isSelected = file.id in uiState.selectedFileIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongPress(file) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
