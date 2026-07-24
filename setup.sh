 #!/bin/bash

# ═══════════════════════════════════════════
# إنشاء هيكل المشروع
# ═══════════════════════════════════════════

echo "📁 إنشاء المجلدات..."

mkdir -p app/src/main/java/com/filemanager/search/data
mkdir -p app/src/main/java/com/filemanager/search/viewmodel
mkdir -p app/src/main/java/com/filemanager/search/ui/screens
mkdir -p app/src/main/java/com/filemanager/search/ui/components
mkdir -p app/src/main/java/com/filemanager/search/utils
mkdir -p app/src/main/res/values
mkdir -p gradle/wrapper
mkdir -p .github/workflows

echo "✅ المجلدات جاهزة"

# ═══════════════════════════════════════════
# build.gradle.kts (Project)
# ═══════════════════════════════════════════

cat > build.gradle.kts << 'ENDOFFILE'
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
ENDOFFILE

echo "✅ build.gradle.kts (Project)"

# ═══════════════════════════════════════════
# app/build.gradle.kts
# ═══════════════════════════════════════════

cat > app/build.gradle.kts << 'ENDOFFILE'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.filemanager.search"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.filemanager.search"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
ENDOFFILE

echo "✅ app/build.gradle.kts"

# ═══════════════════════════════════════════
# settings.gradle.kts
# ═══════════════════════════════════════════

cat > settings.gradle.kts << 'ENDOFFILE'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FileSearchApp"
include(":app")
ENDOFFILE

echo "✅ settings.gradle.kts"

# ═══════════════════════════════════════════
# gradle.properties
# ═══════════════════════════════════════════

cat > gradle.properties << 'ENDOFFILE'
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
ENDOFFILE

echo "✅ gradle.properties"

# ═══════════════════════════════════════════
# gradle-wrapper.properties
# ═══════════════════════════════════════════

cat > gradle/wrapper/gradle-wrapper.properties << 'ENDOFFILE'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
ENDOFFILE

echo "✅ gradle-wrapper.properties"

# ═══════════════════════════════════════════
# .gitignore
# ═══════════════════════════════════════════

cat > .gitignore << 'ENDOFFILE'
.gradle/
build/
*.iml
local.properties
.DS_Store
ENDOFFILE

echo "✅ .gitignore"

# ═══════════════════════════════════════════
# .github/workflows/build.yml
# ═══════════════════════════════════════════

cat > .github/workflows/build.yml << 'ENDOFFILE'
name: Build APK

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Generate Wrapper
        run: gradle wrapper --gradle-version 8.5

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: file-search-debug
          path: app/build/outputs/apk/debug/*.apk
ENDOFFILE

echo "✅ build.yml"

# ═══════════════════════════════════════════
# AndroidManifest.xml
# ═══════════════════════════════════════════

cat > app/src/main/AndroidManifest.xml << 'ENDOFFILE'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <application
        android:name=".FileSearchApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
ENDOFFILE

echo "✅ AndroidManifest.xml"

# ═══════════════════════════════════════════
# strings.xml
# ═══════════════════════════════════════════

cat > app/src/main/res/values/strings.xml << 'ENDOFFILE'
<resources>
    <string name="app_name">File Search</string>
</resources>
ENDOFFILE

echo "✅ strings.xml"

# ═══════════════════════════════════════════
# FileSearchApp.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/FileSearchApp.kt << 'ENDOFFILE'
package com.filemanager.search

import android.app.Application

class FileSearchApp : Application()
ENDOFFILE

echo "✅ FileSearchApp.kt"

# ═══════════════════════════════════════════
# MainActivity.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/MainActivity.kt << 'ENDOFFILE'
package com.filemanager.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.filemanager.search.ui.FileSearchTheme
import com.filemanager.search.ui.screens.FileSearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileSearchTheme {
                FileSearchScreen()
            }
        }
    }
}
ENDOFFILE

echo "✅ MainActivity.kt"

# ═══════════════════════════════════════════
# data/FileType.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/data/FileType.kt << 'ENDOFFILE'
package com.filemanager.search.data

enum class FileType(
    val displayName: String,
    val emoji: String,
    val extensions: List<String>,
    val color: Long
) {
    ALL("All Files", "📁", emptyList(), 0xFF607D8B),
    IMAGES("Images", "🖼️", listOf("jpg","jpeg","png","gif","webp","bmp","svg"), 0xFF2196F3),
    VIDEOS("Videos", "🎬", listOf("mp4","mkv","avi","mov","flv","wmv","3gp"), 0xFFE91E63),
    AUDIO("Audio", "🎵", listOf("mp3","wav","m4a","ogg","flac","aac","wma"), 0xFFFF9800),
    TEXT("Text Files", "📄", listOf("txt","rtf","csv","log"), 0xFF9E9E9E),
    OFFICE("Office", "📑", listOf("doc","docx","xls","xlsx","ppt","pptx"), 0xFF1565C0),
    PDF("PDF", "📖", listOf("pdf"), 0xFFF44336),
    EBOOKS("E-Books", "📚", listOf("epub","mobi","azw","azw3"), 0xFF795548),
    COMPRESSED("Compressed", "📦", listOf("zip","rar","7z","tar","gz","bz2"), 0xFFFFC107),
    APK("APK Files", "⚙️", listOf("apk"), 0xFF4CAF50),
    CODE("Code", "💻", listOf("java","kt","py","js","ts","html","css","cpp","c","h","json"), 0xFF9C27B0),
    GAMES("Game Data", "🎮", listOf("iso","bin","cue","rom","nds","gba"), 0xFFE91E63),
    DATABASE("Database", "🗃️", listOf("db","sqlite","sqlite3"), 0xFF00BCD4),
    FONTS("Fonts", "🔤", listOf("ttf","otf","woff","woff2"), 0xFF3F51B5);

    companion object {
        fun fromExtension(ext: String): FileType {
            return entries.find { it != ALL && it.extensions.contains(ext.lowercase()) } ?: ALL
        }
    }
}
ENDOFFILE

echo "✅ FileType.kt"

# ═══════════════════════════════════════════
# data/FileItem.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/data/FileItem.kt << 'ENDOFFILE'
package com.filemanager.search.data

data class FileItem(
    val id: Long,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val path: String,
    val extension: String,
    val fileType: FileType
)
ENDOFFILE

echo "✅ FileItem.kt"

# ═══════════════════════════════════════════
# data/FileRepository.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/data/FileRepository.kt << 'ENDOFFILE'
package com.filemanager.search.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.webkit.MimeTypeMap

class FileRepository(private val context: Context) {

    fun searchFiles(fileType: FileType): List<FileItem> {
        val files = mutableListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        val selection: String?
        val selectionArgs: Array<String>?

        if (fileType == FileType.ALL) {
            selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} IS NOT NULL"
            selectionArgs = null
        } else {
            selection = fileType.extensions.joinToString(" OR ") {
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            }
            selectionArgs = fileType.extensions.map { "%.$it" }.toTypedArray()
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val dateModified = cursor.getLong(dateCol) * 1000L
                val mimeType = cursor.getString(mimeCol) ?: guessMimeType(name)
                val path = cursor.getString(pathCol) ?: ""

                val ext = name.substringAfterLast('.', "").lowercase()
                val detectedType = FileType.fromExtension(ext)

                if (fileType == FileType.ALL || detectedType == fileType) {
                    files.add(
                        FileItem(
                            id = id,
                            name = name,
                            size = size,
                            dateModified = dateModified,
                            mimeType = mimeType,
                            path = path,
                            extension = ext,
                            fileType = detectedType
                        )
                    )
                }
            }
        }

        return files
    }

    fun deleteFiles(files: List<FileItem>): Boolean {
        var success = true
        for (file in files) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"),
                file.id
            )
            try {
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted == 0) success = false
            } catch (e: Exception) {
                success = false
            }
        }
        return success
    }

    fun getContentUri(fileId: Long) =
        ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), fileId)

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExt(ext) ?: "application/octet-stream"
    }
}
ENDOFFILE

echo "✅ FileRepository.kt"

# ═══════════════════════════════════════════
# viewmodel/FileSearchViewModel.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/viewmodel/FileSearchViewModel.kt << 'ENDOFFILE'
package com.filemanager.search.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
        else all.filter { it.name.contains(query, ignoreCase = true) }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredResults = filtered
        )
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
        _uiState.value = _uiState.value.copy(
            selectedFileIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun onDeleteClicked() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun onDeleteConfirmed() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)

        val toDelete = _uiState.value.allResults.filter {
            it.id in _uiState.value.selectedFileIds
        }

        if (toDelete.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            _pendingDeleteUris.value = toDelete.map {
                repository.getContentUri(it.id)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteFiles(toDelete)
                withContext(Dispatchers.Main) {
                    searchFiles()
                }
            }
        }
    }

    fun onDeleteCancelled() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun onDeleteRequestCompleted(success: Boolean) {
        _pendingDeleteUris.value = emptyList()
        if (success) searchFiles()
    }

    fun onDeleteSingleFile(file: FileItem) {
        _uiState.value = _uiState.value.copy(
            selectedFileIds = setOf(file.id),
            selectedFileInfo = null,
            showDeleteDialog = true
        )
    }

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
ENDOFFILE

echo "✅ FileSearchViewModel.kt"

# ═══════════════════════════════════════════
# utils/FormatUtils.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/utils/FormatUtils.kt << 'ENDOFFILE'
package com.filemanager.search.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatSizeDetailed(bytes: Long): String {
    return when {
        bytes < 0 -> "0 B"
        bytes < 1024 -> "$bytes Bytes"
        bytes < 1024 * 1024 -> "$bytes Bytes (${bytes / 1024} KB)"
        bytes < 1024L * 1024 * 1024 -> {
            val kb = bytes / 1024
            val mb = String.format("%.2f", bytes / (1024.0 * 1024.0))
            "$bytes Bytes ($kb KB / $mb MB)"
        }
        else -> {
            val mb = String.format("%.2f", bytes / (1024.0 * 1024.0))
            val gb = String.format("%.3f", bytes / (1024.0 * 1024.0 * 1024.0))
            "$bytes Bytes ($mb MB / $gb GB)"
        }
    }
}
ENDOFFILE

echo "✅ FormatUtils.kt"

# ═══════════════════════════════════════════
# ui/Theme.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/ui/Theme.kt << 'ENDOFFILE'
package com.filemanager.search.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF42A5F5),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFE8E8E8),
    background = Color(0xFFF5F5F5),
    error = Color(0xFFD32F2F)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    secondary = Color(0xFF64B5F6),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    background = Color(0xFF121212),
    error = Color(0xFFEF5350)
)

@Composable
fun FileSearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
ENDOFFILE

echo "✅ Theme.kt"

# ═══════════════════════════════════════════
# ui/components/FileComponents.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/ui/components/FileComponents.kt << 'ENDOFFILE'
package com.filemanager.search.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemanager.search.data.FileItem
import com.filemanager.search.data.FileRepository
import com.filemanager.search.utils.formatDate
import com.filemanager.search.utils.formatFileSize
import com.filemanager.search.utils.formatSizeDetailed

@Composable
fun FileItemCard(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(file.fileType.color).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = file.fileType.emoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(file.dateModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoBottomSheet(
    file: FileItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(file.fileType.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(file.fileType.emoji, fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = file.fileType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(file.fileType.color)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            InfoRow("Name", file.name)
            InfoRow("Type", "${file.fileType.displayName} (.${file.extension})")
            InfoRow("MIME", file.mimeType.ifEmpty { "Unknown" })
            InfoRow("Size", formatSizeDetailed(file.size))
            InfoRow("Path", file.path.ifEmpty { "Unknown" })
            InfoRow("Modified", formatDate(file.dateModified))

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Default.FolderOpen,
                    label = "Open",
                    color = MaterialTheme.colorScheme.primary
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(
                            Uri.parse("file://${file.path.substringBeforeLast('/')}"),
                            "resource/folder"
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }

                ActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    try {
                        val uri = FileRepository(context).getContentUri(file.id)
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = file.mimeType.ifEmpty { "*/*" }
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
                    } catch (_: Exception) {}
                }

                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    color = MaterialTheme.colorScheme.error
                ) {
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(if (count == 1) "Delete file?" else "Delete $count files?", fontWeight = FontWeight.Bold) },
        text = { Text("This action cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
ENDOFFILE

echo "✅ FileComponents.kt"

# ═══════════════════════════════════════════
# ui/screens/FileSearchScreen.kt
# ═══════════════════════════════════════════

cat > app/src/main/java/com/filemanager/search/ui/screens/FileSearchScreen.kt << 'ENDOFFILE'
package com.filemanager.search.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
        viewModel.onPermissionResult(results.values.all { it })
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.onPermissionResult(allGranted)
    }

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
            } catch (e: Exception) {
                viewModel.onDeleteRequestCompleted(false)
            }
        }
    }

    BackHandler(
        enabled = uiState.showResults || uiState.isSelectionMode || uiState.selectedFileInfo != null
    ) {
        viewModel.onBackPressed()
    }

    Scaffold(
        topBar = {
            if (uiState.showResults) {
                TopAppBar(
                    title = {
                        Text(
                            if (uiState.isSelectionMode) "${uiState.selectedFileIds.size} selected"
                            else "Results (${uiState.filteredResults.size})"
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
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (!uiState.hasPermission) {
                PermissionScreen(onRequest = { permissionLauncher.launch(requiredPermissions) })
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Warning, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text("Storage Permission Required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("This app needs access to your files to search and display them.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text("Grant Permission") }
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
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("File Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Select a file type and tap Search", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!isSearching) expanded = !expanded }) {
                OutlinedTextField(
                    value = "${selectedType.emoji}  ${selectedType.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("File Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    FileType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text("${type.emoji}  ${type.displayName}") },
                            onClick = { onTypeSelected(type); expanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.filteredResults.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (uiState.searchQuery.isNotEmpty()) "No files match '${uiState.searchQuery}'" else "No files found",
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
                items(items = uiState.filteredResults, key = { it.id }) { file ->
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
ENDOFFILE

echo "✅ FileSearchScreen.kt"

# ═══════════════════════════════════════════
# التحقق النهائي
# ═══════════════════════════════════════════

echo ""
echo "═══════════════════════════════════════════"
echo "✅ تم إنشاء جميع الملفات!"
echo "═══════════════════════════════════════════"
echo ""
echo "📁 الملفات المُنشأة:"
find app/src -type f -name "*.kt" -o -name "*.xml" | sort
echo ""
echo "📋 الملفات الجذرية:"
ls -la build.gradle.kts settings.gradle.kts gradle.properties .gitignore 2>/dev/null 
