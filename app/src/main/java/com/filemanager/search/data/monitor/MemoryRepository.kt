package com.filemanager.search.data.monitor

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import java.util.SortedMap
import java.util.TreeMap

class MemoryRepository(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val packageManager = context.packageManager

    // ═══════════════════════════════════════════
    // فحص الصلاحيات
    // ═══════════════════════════════════════════

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun canGetProcessInfo(): Boolean {
        return try {
            val processes = activityManager.runningAppProcesses
            !processes.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════
    // معلومات النظام
    // ═══════════════════════════════════════════

    fun getSystemMemory(): SystemMemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem
        val available = memInfo.availMem
        val used = total - available
        val percent = if (total > 0) (used.toFloat() / total * 100f) else 0f

        return SystemMemoryInfo(
            totalBytes = total,
            availableBytes = available,
            usedBytes = used,
            usagePercent = percent,
            isLowMemory = memInfo.lowMemory,
            lowMemoryThreshold = memInfo.threshold
        )
    }

    // ═══════════════════════════════════════════
    // بيانات المراقبة الكاملة
    // ═══════════════════════════════════════════

    fun getMonitorData(): MonitorData {
        val systemMemory = getSystemMemory()
        val hasUsageAccess = hasUsageStatsPermission()
        val hasProcessAccess = canGetProcessInfo()

        // جمع التطبيقات المثبتة
        val installedApps = getInstalledApps()

        // محاولة الحصول على معلومات العمليات
        val processMap = if (hasProcessAccess) getProcessInfo() else emptyMap()

        // الحصول على إحصائيات الاستخدام
        val usageMap = if (hasUsageAccess) getUsageStats() else emptyMap()

        // دمج البيانات
        val appList = installedApps.map { app ->
            val process = processMap[app.packageName]
            val usage = usageMap[app.packageName]

            app.copy(
                memoryKb = process?.memoryKb ?: -1L,
                processState = determineState(app, process, usage),
                lastUsedTime = usage?.lastUsed ?: 0L,
                isRunning = process != null,
                pid = process?.pid ?: -1,
                hasBackgroundActivity = process != null && !app.isSystemApp
            )
        }.sortedByDescending { app ->
            when {
                app.isRunning && app.memoryKb > 0 -> 3L + app.memoryKb
                app.lastUsedTime > 0 -> 2L
                app.isRunning -> 1L
                else -> 0L
            }
        }

        val note = buildNote(hasProcessAccess, hasUsageAccess)

        return MonitorData(
            systemMemory = systemMemory,
            appList = appList,
            hasProcessAccess = hasProcessAccess,
            hasUsageStatsAccess = hasUsageAccess,
            dataNote = note
        )
    }

    // ═══════════════════════════════════════════
    // التطبيقات المثبتة
    // ═══════════════════════════════════════════

    private fun getInstalledApps(): List<AppProcessInfo> {
        return try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

            packages.map { appInfo ->
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                AppProcessInfo(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    isSystemApp = isSystem,
                    memoryKb = -1L,
                    processState = if (isSystem) AppProcessState.SYSTEM
                                   else AppProcessState.UNKNOWN,
                    lastUsedTime = 0L,
                    uid = appInfo.uid,
                    isRunning = false,
                    pid = -1,
                    hasBackgroundActivity = false
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ═══════════════════════════════════════════
    // معلومات العمليات
    // ═══════════════════════════════════════════

    private data class ProcessData(
        val memoryKb: Long,
        val pid: Int,
        val importance: Int
    )

    @Suppress("DEPRECATION")
    private fun getProcessInfo(): Map<String, ProcessData> {
        val result = mutableMapOf<String, ProcessData>()

        try {
            val processes = activityManager.runningAppProcesses ?: return result
            val pids = processes.map { it.pid }.toIntArray()

            if (pids.isEmpty()) return result

            val memoryInfos = try {
                activityManager.getProcessMemoryInfo(pids)
            } catch (_: Exception) {
                null
            }

            processes.forEachIndexed { index, process ->
                val memKb = memoryInfos?.getOrNull(index)?.let { info ->
                    val totalPss = info.totalPss
                    if (totalPss > 0) totalPss.toLong() else -1L
                } ?: -1L

                // لكل حزمة في العملية
                process.pkgList?.forEach { pkg ->
                    val existing = result[pkg]
                    if (existing == null || memKb > existing.memoryKb) {
                        result[pkg] = ProcessData(
                            memoryKb = memKb,
                            pid = process.pid,
                            importance = process.importance
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return result
    }

    // ═══════════════════════════════════════════
    // إحصائيات الاستخدام
    // ═══════════════════════════════════════════

    private data class UsageData(val lastUsed: Long, val totalTime: Long)

    private fun getUsageStats(): Map<String, UsageData> {
        val result = mutableMapOf<String, UsageData>()

        try {
            val usageStatsManager = context.getSystemService(
                Context.USAGE_STATS_SERVICE
            ) as? UsageStatsManager ?: return result

            val now = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 24 * 60 * 60 * 1000,
                now
            )

            stats?.forEach { stat ->
                val existing = result[stat.packageName]
                if (existing == null || stat.lastTimeUsed > existing.lastUsed) {
                    result[stat.packageName] = UsageData(
                        lastUsed = stat.lastTimeUsed,
                        totalTime = stat.totalTimeInForeground
                    )
                }
            }
        } catch (_: Exception) {}

        return result
    }

    // ═══════════════════════════════════════════
    // تحديد حالة التطبيق
    // ═══════════════════════════════════════════

    private fun determineState(
        app: AppProcessInfo,
        process: ProcessData?,
        usage: UsageData?
    ): AppProcessState {
        // تطبيق نظام
        if (app.isSystemApp) return AppProcessState.SYSTEM

        // يعمل حالياً
        if (process != null) {
            return when {
                process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE ->
                    AppProcessState.ACTIVE
                process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE ->
                    AppProcessState.SERVICE
                else -> AppProcessState.BACKGROUND
            }
        }

        // استُخدم مؤخراً
        if (usage != null && usage.lastUsed > 0) {
            val hoursSinceUse = (System.currentTimeMillis() - usage.lastUsed) / (1000 * 60 * 60)
            if (hoursSinceUse < 24) return AppProcessState.RECENTLY_USED
        }

        return AppProcessState.UNKNOWN
    }

    // ═══════════════════════════════════════════
    // ملاحظات حول القيود
    // ═══════════════════════════════════════════

    private fun buildNote(hasProcess: Boolean, hasUsage: Boolean): String? {
        val notes = mutableListOf<String>()

        if (!hasProcess) {
            notes.add("معلومات العمليات محدودة على إصدار Android هذا")
        }
        if (!hasUsage) {
            notes.add("بيانات الاستخدام تحتاج صلاحية Usage Access")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notes.add("Android ${Build.VERSION.RELEASE} ي限制 وصول التطبيقات لبيانات العمليات الأخرى")
        }

        return if (notes.isEmpty()) null else notes.joinToString(" · ")
    }
}
