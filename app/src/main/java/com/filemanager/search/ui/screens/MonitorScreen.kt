package com.filemanager.search.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filemanager.search.data.monitor.AppFilter
import com.filemanager.search.data.monitor.AppProcessInfo
import com.filemanager.search.ui.components.AppIconImage
import com.filemanager.search.ui.components.AppTypeBadge
import com.filemanager.search.ui.components.ProcessStateBadge
import com.filemanager.search.ui.components.RamStatusCard
import com.filemanager.search.utils.formatFileSize
import com.filemanager.search.viewmodel.MonitorViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel = viewModel(),
    onNavigateToUsageAccess: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showIntervalMenu by remember { mutableStateOf(false) }

    // استقرار dataNote لتجنب smart cast issues
    val dataNote = uiState.dataNote

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مراقبة الذاكرة", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        viewModel.onToggleMonitoring(!uiState.isMonitoring)
                    }) {
                        Icon(
                            imageVector = if (uiState.isMonitoring) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = "Toggle monitoring",
                            tint = if (uiState.isMonitoring) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Box {
                        TextButton(onClick = { showIntervalMenu = true }) {
                            Text(
                                "${uiState.refreshIntervalMs / 1000}s",
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(
                            expanded = showIntervalMenu,
                            onDismissRequest = { showIntervalMenu = false }
                        ) {
                            listOf(1000L, 2000L, 5000L, 10000L).forEach { ms ->
                                DropdownMenuItem(
                                    text = { Text("${ms / 1000} ثانية") },
                                    onClick = {
                                        viewModel.onRefreshIntervalChanged(ms)
                                        showIntervalMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.onRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══ بطاقة RAM ═══
                item {
                    RamStatusCard(
                        totalBytes = uiState.systemMemory.totalBytes,
                        usedBytes = uiState.systemMemory.usedBytes,
                        availableBytes = uiState.systemMemory.availableBytes,
                        usagePercent = uiState.systemMemory.usagePercent,
                        isLowMemory = uiState.systemMemory.isLowMemory
                    )
                }

                // ═══ ملاحظات البيانات ═══
                if (!dataNote.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = dataNote,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ═══ زر Usage Access ═══
                if (!uiState.hasUsageStatsAccess) {
                    item {
                        TextButton(
                            onClick = onNavigateToUsageAccess,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تفعيل Usage Access لمزيد من المعلومات")
                        }
                    }
                }

                // ═══ أعلى التطبيقات استهلاكاً ═══
                if (uiState.topMemoryApps.isNotEmpty()) {
                    item {
                        Text(
                            "أعلى التطبيقات استهلاكاً للذاكرة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(
                        items = uiState.topMemoryApps.take(5),
                        key = { "top_${it.packageName}" }
                    ) { app ->
                        TopMemoryItem(app)
                    }
                }

                // ═══ الفلاتر ═══
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = uiState.filter == filter,
                                onClick = { viewModel.onFilterSelected(filter) },
                                label = { Text(filter.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // ═══ شريط البحث ═══
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث عن تطبيق...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    viewModel.onSearchQueryChanged("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // ═══ عدد النتائج ═══
                item {
                    Text(
                        "${uiState.filteredApps.size} تطبيق",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ═══ قائمة التطبيقات ═══
                items(
                    items = uiState.filteredApps,
                    key = { it.packageName }
                ) { app ->
                    AppProcessCard(
                        app = app,
                        onOpenApp = { openApp(context, app.packageName) },
                        onAppInfo = { openAppInfo(context, app.packageName) }
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// عنصر أعلى تطبيقات استهلاكاً
// ═══════════════════════════════════════════════════════════

    // ═══ عدّل TopMemoryItem لعرض N/A عند عدم توفر الذاكرة ═══
@Composable
private fun TopMemoryItem(app: AppProcessInfo) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(packageName = app.packageName, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // ═══ عرض الذاكرة أو N/A ═══
            if (app.memoryKb > 0L) {
                Text(
                    formatFileSize(app.memoryKb * 1024),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    "N/A",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══ عدّل AppProcessCard لعرض N/A وحالة أفضل ═══
@Composable
private fun AppProcessCard(
    app: AppProcessInfo,
    onOpenApp: () -> Unit,
    onAppInfo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(packageName = app.packageName, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // ═══ الذاكرة: رقم أو N/A ═══
                if (app.memoryKb > 0L) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatFileSize(app.memoryKb * 1024),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "RAM",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "N/A",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "RAM محجوب",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTypeBadge(isSystemApp = app.isSystemApp)
                ProcessStateBadge(state = app.processState)
                if (app.lastUsedTime > 0L) {
                    Text(
                        "آخر استخدام: ${formatLastUsed(app.lastUsedTime)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenApp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "فتح",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onAppInfo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "معلومات",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}            
            

            

// ═══════════════════════════════════════════════════════════
// دوال مساعدة
// ═══════════════════════════════════════════════════════════
private fun formatLastUsed(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "الآن"
        diff < 3_600_000L -> "منذ ${diff / 60_000} دقيقة"
        diff < 86_400_000L -> "منذ ${diff / 3_600_000} ساعة"
        else -> "منذ ${diff / 86_400_000} يوم"
    }
}

private fun openApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (_: Exception) {}
}

private fun openAppInfo(context: Context, packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
