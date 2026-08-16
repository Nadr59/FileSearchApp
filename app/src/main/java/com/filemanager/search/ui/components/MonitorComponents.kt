package com.filemanager.search.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemanager.search.data.monitor.AppProcessState
import com.filemanager.search.utils.formatFileSize

// ═══════════════════════════════════════════════════════════
// بطاقة حالة الذاكرة
// ═══════════════════════════════════════════════════════════
@Composable
fun RamStatusCard(
    totalBytes: Long,
    usedBytes: Long,
    availableBytes: Long,
    usagePercent: Float,
    isLowMemory: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "RAM Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (isLowMemory) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            "LOW",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // شريط التقدم
            LinearProgressIndicator(
                progress = { (usagePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = when {
                    usagePercent > 90 -> MaterialTheme.colorScheme.error
                    usagePercent > 75 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MemoryLabel("الكلية", formatFileSize(totalBytes))
                MemoryLabel("المستخدمة", formatFileSize(usedBytes))
                MemoryLabel("المتاحة", formatFileSize(availableBytes))
                MemoryLabel("النسبة", "${String.format("%.0f", usagePercent)}%")
            }
        }
    }
}

@Composable
private fun MemoryLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

// ═══════════════════════════════════════════════════════════
// بطاقة حالة الشبكة
// ═══════════════════════════════════════════════════════════
@Composable
fun NetworkStatusCard(
    totalRx: Long,
    totalTx: Long,
    mobileTotal: Long,
    wifiTotal: Long,
    topAppName: String?,
    topAppTotal: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DataUsage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Network Usage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NetworkStatItem(
                    icon = Icons.Default.NetworkCheck,
                    label = "Total",
                    value = formatFileSize(totalRx + totalTx),
                    color = MaterialTheme.colorScheme.secondary
                )
                NetworkStatItem(
                    icon = Icons.Default.NetworkCheck,
                    label = "Mobile",
                    value = formatFileSize(mobileTotal),
                    color = Color(0xFFFF9800)
                )
                NetworkStatItem(
                    icon = Icons.Default.NetworkCheck,
                    label = "Wi-Fi",
                    value = formatFileSize(wifiTotal),
                    color = Color(0xFF4CAF50)
                )
            }

            if (topAppName != null && topAppTotal > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "الأكثر استهلاكاً: $topAppName — ${formatFileSize(topAppTotal)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NetworkStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, fontSize = 11.sp, color = color)
    }
}

// ═══════════════════════════════════════════════════════════
// شارة حالة التطبيق
// ═══════════════════════════════════════════════════════════
@Composable
fun AppTypeBadge(isSystemApp: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSystemApp)
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = if (isSystemApp) "System" else "User App",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSystemApp)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.primary
        )
    }
}

// ═══════════════════════════════════════════════════════════
// شارة حالة العملية
// ═══════════════════════════════════════════════════════════
@Composable
fun ProcessStateBadge(state: AppProcessState) {
    val (color, text) = when (state) {
        AppProcessState.ACTIVE -> Color(0xFF4CAF50) to state.label
        AppProcessState.BACKGROUND -> Color(0xFFFF9800) to state.label
        AppProcessState.SERVICE -> Color(0xFF2196F3) to state.label
        AppProcessState.RECENTLY_USED -> Color(0xFF9C27B0) to state.label
        AppProcessState.SYSTEM -> Color(0xFF607D8B) to state.label
        AppProcessState.UNKNOWN -> Color(0xFF9E9E9E) to state.label
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

// ═══════════════════════════════════════════════════════════
// بطاقة ميزة في الواجهة الرئيسية
// ═══════════════════════════════════════════════════════════
@Composable
fun DashboardFeatureCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
