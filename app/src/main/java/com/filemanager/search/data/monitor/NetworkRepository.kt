package com.filemanager.search.data.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import android.util.SparseArray
import androidx.core.util.forEach

class NetworkRepository(private val context: Context) {

    private val packageManager = context.packageManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

    fun getPerAppUsage(): NetworkData {
        val systemStats = getSystemStats()
        val appUsage = mutableListOf<AppNetworkUsage>()
        val uidMap = SparseArray<MutableList<String>>()

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

            packages.forEach { appInfo ->
                val existing = uidMap[appInfo.uid]
                if (existing != null) {
                    existing.add(appInfo.packageName)
                } else {
                    uidMap.put(appInfo.uid, mutableListOf(appInfo.packageName))
                }
            }

            val seenUids = mutableSetOf<Int>()

            uidMap.forEach { uid, packageNames ->
                if (uid in seenUids) return@forEach
                seenUids.add(uid)

                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)

                if (rx == TrafficStats.UNSUPPORTED.toLong() &&
                    tx == TrafficStats.UNSUPPORTED.toLong()
                ) return@forEach

                val rxBytes = rx.coerceAtLeast(0L)
                val txBytes = tx.coerceAtLeast(0L)
                val total = rxBytes + txBytes

                if (total <= 0L) return@forEach

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
            hasUsageAccess = hasUsageStatsPermission()
        )
    }
}
