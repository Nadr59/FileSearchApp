package com.filemanager.search.data.monitor

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Process

class NetworkRepository(private val context: Context) {

    private val packageManager = context.packageManager

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
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network)
            caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        } catch (_: Exception) {
            false
        }
    }

    fun getSystemStats(): SystemNetworkStats {
        return try {
            val totalRx = TrafficStats.getTotalRxBytes()
            val totalTx = TrafficStats.getTotalTxBytes()
            val mobileRx = TrafficStats.getMobileRxBytes()
            val mobileTx = TrafficStats.getMobileTxBytes()

            if (totalRx == TrafficStats.UNSUPPORTED.toLong()) {
                return SystemNetworkStats.EMPTY
            }

            SystemNetworkStats(
                totalRxBytes = totalRx.coerceAtLeast(0L),
                totalTxBytes = totalTx.coerceAtLeast(0L),
                mobileRxBytes = mobileRx.coerceAtLeast(0L),
                mobileTxBytes = mobileTx.coerceAtLeast(0L),
                wifiRxBytes = (totalRx - mobileRx).coerceAtLeast(0L),
                wifiTxBytes = (totalTx - mobileTx).coerceAtLeast(0L)
            )
        } catch (_: Exception) {
            SystemNetworkStats.EMPTY
        }
    }

    fun getPerAppUsage(period: StatsPeriod): NetworkData {
        val systemStats = getSystemStats()

        if (hasUsageStatsPermission()) {
            try {
                val appUsage = queryViaNetworkStatsManager(period)
                if (appUsage.isNotEmpty()) {
                    return NetworkData(
                        systemStats = systemStats,
                        appUsageList = appUsage.sortedByDescending { it.totalBytes },
                        hasUsageAccess = true,
                        period = period,
                        isNetworkStatsData = true
                    )
                }
            } catch (_: Exception) {}
        }

        val appUsage = queryViaTrafficStats()

        return NetworkData(
            systemStats = systemStats,
            appUsageList = appUsage.sortedByDescending { it.totalBytes },
            hasUsageAccess = hasUsageStatsPermission(),
            period = period,
            isNetworkStatsData = false
        )
    }

    // ═══════════════════════════════════════════
    // الطريقة 1: NetworkStatsManager
    // ═══════════════════════════════════════════
    private fun queryViaNetworkStatsManager(
        period: StatsPeriod
    ): List<AppNetworkUsage> {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE)
            as? NetworkStatsManager
            ?: throw Exception("NetworkStatsManager not available")

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (period.days.toLong() * 24L * 60L * 60L * 1000L)

        val uidData = mutableMapOf<Int, Pair<Long, Long>>()

        // WiFi
        try {
            val wifiStats = nsm.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            while (wifiStats.hasNextBucket()) {
                val bucket = wifiStats.getNextBucket()
                val uid = bucket.uid
                if (uid <= 0) continue
                val existing = uidData[uid] ?: Pair(0L, 0L)
                uidData[uid] = Pair(
                    existing.first + bucket.getRxBytes(),
                    existing.second + bucket.getTxBytes()
                )
            }
            wifiStats.close()
        } catch (_: Exception) {}

        // Mobile
        try {
            val mobileStats = nsm.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            while (mobileStats.hasNextBucket()) {
                val bucket = mobileStats.getNextBucket()
                val uid = bucket.uid
                if (uid <= 0) continue
                val existing = uidData[uid] ?: Pair(0L, 0L)
                uidData[uid] = Pair(
                    existing.first + bucket.getRxBytes(),
                    existing.second + bucket.getTxBytes()
                )
            }
            mobileStats.close()
        } catch (_: Exception) {}

        return mapUidDataToApps(uidData)
    }

    // ═══════════════════════════════════════════
    // الطريقة 2: TrafficStats
    // ═══════════════════════════════════════════
    private fun queryViaTrafficStats(): List<AppNetworkUsage> {
        val uidData = mutableMapOf<Int, Pair<Long, Long>>()
        val seenUids = mutableSetOf<Int>()

        try {
            val packages = getInstalledPackages()

            packages.forEach { appInfo ->
                val uid = appInfo.uid
                if (uid in seenUids) return@forEach
                seenUids.add(uid)

                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)

                if (rx == TrafficStats.UNSUPPORTED.toLong() &&
                    tx == TrafficStats.UNSUPPORTED.toLong()
                ) return@forEach

                val rxBytes = rx.coerceAtLeast(0L)
                val txBytes = tx.coerceAtLeast(0L)

                if (rxBytes + txBytes > 0L) {
                    uidData[uid] = Pair(rxBytes, txBytes)
                }
            }
        } catch (_: Exception) {}

        return mapUidDataToApps(uidData)
    }

    // ═══════════════════════════════════════════
    // تحويل UID إلى تطبيقات
    // ═══════════════════════════════════════════
    private fun mapUidDataToApps(
        uidData: Map<Int, Pair<Long, Long>>
    ): List<AppNetworkUsage> {
        val result = mutableListOf<AppNetworkUsage>()

        for ((uid, data) in uidData) {
            val total = data.first + data.second
            if (total <= 0L) continue

            val packages = packageManager.getPackagesForUid(uid)
            if (packages.isNullOrEmpty()) continue

            val primaryPkg = packages.first()
            val appInfo = try {
                getApplicationInfo(primaryPkg)
            } catch (_: Exception) {
                null
            }

            val isSystem = appInfo != null &&
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appName = try {
                appInfo?.loadLabel(packageManager)?.toString() ?: primaryPkg
            } catch (_: Exception) {
                primaryPkg
            }

            result.add(
                AppNetworkUsage(
                    packageName = primaryPkg,
                    appName = appName,
                    isSystemApp = isSystem,
                    rxBytes = data.first,
                    txBytes = data.second,
                    uid = uid
                )
            )
        }

        return result
    }

    private fun getInstalledPackages(): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    }
}
