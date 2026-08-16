package com.filemanager.search.data.monitor

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

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

        // ═══ 1. جمع كل التطبيقات المثبتة ═══
        val installedApps = getInstalledApps()

        // ═══ 2. محاولة الحصول على العمليات_RUNNING ═══
        val processMemoryMap = tryGetProcessMemory()

        // ═══ 3. إحصائيات الاستخدام (UsageStats) ═══
        val usageMap = if (hasUsageAccess) getUsageStats() else emptyMap()

        // ═══ 4. دمج البيانات ═══
        val appList = installedApps.map { app ->
            val memKb = processMemoryMap[app.packageName]
            val usage = usageMap[app.packageName]

            app.copy(
                memoryKb = memKb ?: -1L,
                processState = determineState(app, memKb != null, usage),
                lastUsedTime = usage?.lastUsed ?: 0L,
                isRunning = memKb != null,
                pid = -1,
                hasBackgroundActivity = memKb != null
            )
        }.sortedWith(
            compareByDescending<AppProcessInfo> { it.isRunning }
                .thenByDescending { it.lastUsedTime > 0L }
                .thenByDescending { it.memoryKb > 0L }
                .thenBy { it.appName.lowercase() }
        )

        val note = buildNote(hasUsageAccess, processMemoryMap.size)

        return MonitorData(
            systemMemory = systemMemory,
            appList = appList,
            hasProcessAccess = processMemoryMap.isNotEmpty(),
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
    // محاولة الحصول على ذاكرة العمليات
    // (مقيّد على Android 7+ — يعود بالتطبيق نفسه فقط أحياناً)
    // ═══════════════════════════════════════════
    private fun tryGetProcessMemory(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()

        try {
            val processes = activityManager.runningAppProcesses
            if (processes.isNullOrEmpty()) return result

            val pids = processes.map { it.pid }.toIntArray()
            if (pids.isEmpty()) return result

            val memoryInfos = try {
                activityManager.getProcessMemoryInfo(pids)
            } catch (_: Exception) {
                null
            }

            processes.forEachIndexed { index, process ->
                val totalPss = memoryInfos?.getOrNull(index)?.totalPss?.toLong() ?: 0L
                if (totalPss > 0L) {
                    process.pkgList?.forEach { pkg ->
                        val existing = result[pkg] ?: 0L
                        result[pkg] = maxOf(existing, totalPss)
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

            // آخر 7 أيام
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 7L * 24L * 60L * 60L * 1000L,
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
        isRunning: Boolean,
        usage: UsageData?
    ): AppProcessState {
        if (app.isSystemApp) return AppProcessState.SYSTEM

        if (isRunning) return AppProcessState.ACTIVE

        if (usage != null && usage.lastUsed > 0L) {
            val hoursSinceUse = (System.currentTimeMillis() - usage.lastUsed) / (1000L * 60L * 60L)
            return when {
                hoursSinceUse < 1 -> AppProcessState.RECENTLY_USED
                hoursSinceUse < 24 -> AppProcessState.RECENTLY_USED
                else -> AppProcessState.UNKNOWN
            }
        }

        return AppProcessState.UNKNOWN
    }

    // ═══════════════════════════════════════════
    // ملاحظات القيود
    // ═══════════════════════════════════════════
    private fun buildNote(hasUsage: Boolean, processCount: Int): String? {
        val notes = mutableListOf<String>()

        if (processCount <= 1) {
            notes.add("Android ي限制 الوصول لذاكرة التطبيقات الأخرى")
        }
        if (!hasUsage) {
            notes.add("بيانات الاستخدام تحتاج صلاحية Usage Access")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notes.add("ذاكرة التطبيقات الفردية غير متاحة على Android ${Build.VERSION.RELEASE}")
        }

        return if (notes.isEmpty()) null else notes.joinToString(" · ")
    }
}
