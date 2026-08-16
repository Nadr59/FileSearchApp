package com.filemanager.search.data.monitor

// ═══════════════════════════════════════════
// نماذج بيانات الذاكرة
// ═══════════════════════════════════════════

/**
 * معلومات الذاكرة الكلية للنظام
 */
data class SystemMemoryInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usagePercent: Float,
    val isLowMemory: Boolean,
    val lowMemoryThreshold: Long
) {
    companion object {
        val EMPTY = SystemMemoryInfo(0, 0, 0, 0f, false, 0)
    }
}

/**
 * حالة عملية التطبيق
 */
enum class AppProcessState(val label: String, val labelAr: String) {
    ACTIVE("Active", "نشط"),
    BACKGROUND("Background", "يعمل في الخلفية"),
    SERVICE("Service Running", "خدمة نشطة"),
    RECENTLY_USED("Recently Used", "استُخدم مؤخراً"),
    SYSTEM("System", "نظام"),
    UNKNOWN("Unknown", "غير معروف")
}

/**
 * معلومات تطبيق/عملية واحدة
 */
data class AppProcessInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val memoryKb: Long,
    val processState: AppProcessState,
    val lastUsedTime: Long,
    val uid: Int,
    val isRunning: Boolean,
    val pid: Int,
    val hasBackgroundActivity: Boolean
)

/**
 * بيانات المراقبة الكاملة
 */
data class MonitorData(
    val systemMemory: SystemMemoryInfo,
    val appList: List<AppProcessInfo>,
    val hasProcessAccess: Boolean,
    val hasUsageStatsAccess: Boolean,
    val dataNote: String?
) {
    companion object {
        val EMPTY = MonitorData(
            systemMemory = SystemMemoryInfo.EMPTY,
            appList = emptyList(),
            hasProcessAccess = false,
            hasUsageStatsAccess = false,
            dataNote = null
        )
    }
}

/**
 * فلاتر التطبيقات
 */
enum class AppFilter(val label: String) {
    ALL("الكل"),
    SYSTEM("النظام"),
    USER("المستخدم"),
    TOP_MEMORY("الأكثر استهلاكاً"),
    RUNNING("النشطة")
}
