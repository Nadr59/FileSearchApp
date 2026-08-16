package com.filemanager.search.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemanager.search.data.FileItem
import com.filemanager.search.data.remote.AiConfig
import com.filemanager.search.data.remote.AiService
import com.filemanager.search.data.remote.FileContentReader
import com.filemanager.search.utils.formatDate
import com.filemanager.search.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileAnalysisScreen(
    file: FileItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiService = remember { AiService() }

    var isLoading by remember { mutableStateOf(false) }
    var loadingStep by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadConfig(): AiConfig {
        val prefs = context.getSharedPreferences("filesearch_prefs", Context.MODE_PRIVATE)
        return AiConfig(
            provider = prefs.getString("ai_provider", "groq") ?: "groq",
            apiKey = prefs.getString("ai_key", "") ?: "",
            model = prefs.getString("ai_model", "") ?: "",
            baseUrl = prefs.getString("ai_base_url", "") ?: ""
        )
    }

    fun startAnalysis() {
        scope.launch {
            isLoading = true
            error = null
            result = null

            try {
                val config = withContext(Dispatchers.IO) { loadConfig() }

                if (config.apiKey.isBlank()) {
                    error = "Enter an AI API key in Settings first"
                    isLoading = false
                    return@launch
                }

                loadingStep = "Reading file info..."
                withContext(Dispatchers.IO) { kotlinx.coroutines.delay(300) }

                val fileInfo = FileContentReader.getFileInfoSummary(file)
                val fileContent = withContext(Dispatchers.IO) {
                    FileContentReader.readContent(file)
                }

                loadingStep = if (fileContent != null) {
                    "Analyzing file content with AI..."
                } else {
                    "Analyzing file with AI..."
                }
                withContext(Dispatchers.IO) { kotlinx.coroutines.delay(300) }
                loadingStep = "Analyzing... may take 30-60 seconds"

                val response = withContext(Dispatchers.IO) {
                    aiService.analyzeFile(fileInfo, fileContent, config)
                }

                result = response
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                error = when {
                    msg.contains("timeout", true) ->
                        "Timeout — server is slow. Try again or switch provider"
                    msg.contains("429") ->
                        "Rate limit exceeded — wait a moment then retry"
                    msg.contains("401") ->
                        "Invalid API key — check Settings"
                    msg.contains("403") ->
                        "API key rejected or expired"
                    msg.contains("connect", true) ->
                        "Connection failed — check internet"
                    else -> msg
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(file.path) { startAnalysis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("File Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            file.name,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = "File: ${file.name}\nType: ${file.fileType.displayName}\nSize: ${formatFileSize(file.size)}\n\n${result ?: ""}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }, enabled = result != null) {
                        Icon(Icons.Filled.Share, "Share")
                    }
                    IconButton(
                        onClick = { startAnalysis() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, "Retry")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> LoadingContent(loadingStep)
                error != null && result == null -> ErrorContent(
                    error = error!!,
                    onRetry = { startAnalysis() },
                    onSettings = { onBack() }
                )
                result != null -> AnalysisContent(
                    file = file,
                    analysis = result!!
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(step: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(20.dp))
        Text(step, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "May take 30-60 seconds...",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Analysis Failed", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Text(
                error,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSettings) { Text("Settings") }
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun AnalysisContent(
    file: FileItem,
    analysis: String
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ═══ بطاقة معلومات الملف ═══
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${file.fileType.emoji} ${file.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${file.fileType.displayName} · ${formatFileSize(file.size)} · ${formatDate(file.dateModified)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    if (file.path.isNotBlank()) {
                        Text(
                            file.path,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ═══ أزرار الإجراءات ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("analysis", analysis))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val text = buildString {
                            appendLine("File: ${file.name}")
                            appendLine("Type: ${file.fileType.displayName}")
                            appendLine("Size: ${formatFileSize(file.size)}")
                            appendLine("")
                            appendLine(analysis)
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ═══ نتيجة التحليل ═══
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "AI Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        analysis,
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}
