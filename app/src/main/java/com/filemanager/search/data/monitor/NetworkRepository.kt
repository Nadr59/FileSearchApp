package com.filemanager.search.data.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.util.SparseArray
import androidx.core.util.forEach

class NetworkRepository(private val context: Context) {

    private val packageManager = context.packageManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

    fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        } catch (_: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════
    // إحصائيات النظام
    // ═══════════════════════════════════════════

    fun getSystemStats(): SystemNetworkStats {
        return try {
            val totalRx = android.net.TrafficStats.getTotalRxBytes()
            val totalTx = android.net.TrafficStats.getTotalTxBytes()
            val mobileRx = android.net.TrafficStats.getMobileRxBytes()
            val mobileTx = android.net.TrafficStats.getMobileTxBytes()

            if (totalRx == android.net.TrafficStats.UNSUPPORTED) {
                return SystemNetworkStats.EMPTY
            }

            SystemNetworkStats(
                totalRxBytes = totalRx.coerceAtLeast(0),
                totalTxBytes = totalTx.coerceAtLeast(0),
                mobileRxBytes = mobileRx.coerceAtLeast(0),
                mobileTxBytes = mobileTx.coerceAtLeast(0),
                wifiRxBytes = (totalRx - mobileRx).coerceAtLeast(0),
                wifiTxBytes = (totalTx - mobileTx).coerceAtLeast(0)
            )
        } catch (_: Exception) {
            SystemNetworkStats.EMPTY
        }
    }

    // ═══════════════════════════════════════════
    // إحصائيات كل تطبيق
    // ═══════════════════════════════════════════

    fun getPerAppUsage(): NetworkData {
        val systemStats = getSystemStats()
        val hasAccess = hasUsageStatsPermission()

        val appUsage = mutableListOf<AppNetworkUsage>()
        val uidMap = SparseArray<MutableList<String>>() // uid -> list of packages

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

            // تجميع الحزم حسب UID
            packages.forEach { appInfo ->
                val existing = uidMap[appInfo.uid]
                if (existing != null) {
                    existing.add(appInfo.packageName)
                } else {
                    uidMap.put(appInfo.uid, mutableListOf(appInfo.packageName))
                }
            }

            // جمع إحصائيات كل UID
            val seenUids = mutableSetOf<Int>()

            uidMap.forEach { uid, packageNames ->
                if (seenUids.contains(uid)) return@forEach
                seenUids.add(uid)

                val rx = android.net.TrafficStats.getUidRxBytes(uid)
                val tx = android.net.TrafficStats.getUidTxBytes(uid)

                if (rx == android.net.TrafficStats.UNSUPPORTED &&
                    tx == android.net.TrafficStats.UNSUPPORTED) return@forEach

                val rxBytes = rx.coerceAtLeast(0)
                val txBytes = tx.coerceAtLeast(0)
                val total = rxBytes + txBytes

                if (total <= 0) return@forEach

                // اختيار الحزمة الرئيسية (الأولى)
                val primaryPkg = packageNames.first()
                val appInfo = packages.find { it.packageName == primaryPkg }
                val isSystem = appInfo != null &&
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = try {
                    appInfo?.loadLabel(packageManager)?.toString() ?: primaryPkg
                } catch (_: Exception) {
                    primaryPkg
                }

                appUsage.add(
                    AppNetworkUsage(
                        packageName = primaryPkg,
                        appName = appName,
                        isSystemApp = isSystem,
                        rxBytes = rxBytes,
                        txBytes = txBytes,
                        uid = uid
                    )
                )
            }
        } catch (_: Exception) {}

        val sorted = appUsage.sortedByDescending { it.totalBytes }

        return NetworkData(
            systemStats = systemStats,
            appUsageList = sorted,
            hasUsageAccess = hasAccess
        )
    }
}
