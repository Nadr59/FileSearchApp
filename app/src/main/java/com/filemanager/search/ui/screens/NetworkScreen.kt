package com.filemanager.search.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filemanager.search.data.monitor.AppFilter
import com.filemanager.search.data.monitor.AppNetworkUsage
import com.filemanager.search.data.monitor.StatsPeriod
import com.filemanager.search.ui.components.AppIconImage
import com.filemanager.search.ui.components.AppTypeBadge
import com.filemanager.search.ui.components.NetworkStatusCard
import com.filemanager.search.utils.formatFileSize
import com.filemanager.search.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = viewModel(),
    onNavigateToUsageAccess: () -> Unit = {},
    onAppClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استهلاك الإنترنت", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
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
                // ═══ بطاقة الشبكة ═══
                item {
                    NetworkStatusCard(
                        totalRx = uiState.systemStats.totalRxBytes,
                        totalTx = uiState.systemStats.totalTxBytes,
                        mobileTotal = uiState.systemStats.mobileTotalBytes,
                        wifiTotal = uiState.systemStats.wifiTotalBytes,
                        topAppName = uiState.topApps.firstOrNull()?.appName,
                        topAppTotal = uiState.topApps.firstOrNull()?.totalBytes ?: 0L
                    )
                }

                // ═══ اختيار الفترة الزمنية ═══
                item {
                    Text(
                        "الفترة الزمنية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = uiState.selectedPeriod == period,
                                onClick = { viewModel.onPeriodSelected(period) },
                                label = { Text(period.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // ═══ ملاحظة مصدر البيانات ═══
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (uiState.isNetworkStatsData) {
                                Text(
                                    "بيانات ${uiState.selectedPeriod.label} — مصدقة من NetworkStatsManager",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "البيانات منذ آخر تشغيل للجهاز (TrafficStats). " +
                                    "للحصول على إحصائيات ${uiState.selectedPeriod.label} " +
                                    "فعّل Usage Access.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ═══ تنبيه عدم الاتصال ═══
                if (!uiState.isNetworkAvailable) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "لا يوجد اتصال بالإنترنت",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // ═══ زر Usage Access ═══
                if (!uiState.hasUsageAccess) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "للحصول على بيانات تفصيلية لكل تطبيق:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "فعّل صلاحية Usage Access لعرض إحصائيات حسب الفترة المحددة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                        alpha = 0.7f
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = onNavigateToUsageAccess) {
                                    Text("فتح إعدادات Usage Access")
                                }
                            }
                        }
                    }
                }

                // ═══ فلاتر نوع التطبيق ═══
                item {
                    Text(
                        "فلترة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            AppFilter.ALL,
                            AppFilter.USER,
                            AppFilter.SYSTEM,
                            AppFilter.TOP_MEMORY
                        ).forEach { filter ->
                            FilterChip(
                                selected = uiState.filter == filter,
                                onClick = { viewModel.onFilterSelected(filter) },
                                label = { Text(filter.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // ═══ البحث ═══
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
                    key = { "net_${it.uid}_${it.packageName}" }
                ) { app ->
                    AppNetworkCard(
                        app = app,
                        totalSystemBytes = uiState.systemStats.totalBytes,
                        onClick = { onAppClick(app.packageName) }
                    )
                }

                // ═══ حالة فارغة ═══
                if (uiState.filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\uD83C\uDF10", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                if (!uiState.hasUsageAccess) {
                                    Text(
                                        "فعّل Usage Access لعرض بيانات التطبيقات",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        "لا توجد بيانات استخدام لهذه الفترة",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AppNetworkCard(
    app: AppNetworkUsage,
    totalSystemBytes: Long,
    onClick: () -> Unit
) {
    val percent = if (totalSystemBytes > 0L) {
        (app.totalBytes.toFloat() / totalSystemBytes * 100f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatFileSize(app.totalBytes),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (percent > 0f) {
                        Text(
                            "${String.format("%.1f", percent)}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTypeBadge(isSystemApp = app.isSystemApp)
                Text(
                    "↓ ${formatFileSize(app.rxBytes)}  ↑ ${formatFileSize(app.txBytes)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
