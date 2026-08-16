package com.filemanager.search.data.monitor

data class SystemNetworkStats(
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val mobileRxBytes: Long,
    val mobileTxBytes: Long,
    val wifiRxBytes: Long,
    val wifiTxBytes: Long
) {
    val totalBytes: Long get() = totalRxBytes + totalTxBytes
    val mobileTotalBytes: Long get() = mobileRxBytes + mobileTxBytes
    val wifiTotalBytes: Long get() = wifiRxBytes + wifiTxBytes

    companion object {
        val EMPTY = SystemNetworkStats(0, 0, 0, 0, 0, 0)
    }
}

data class AppNetworkUsage(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val rxBytes: Long,
    val txBytes: Long,
    val uid: Int
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

data class NetworkData(
    val systemStats: SystemNetworkStats,
    val appUsageList: List<AppNetworkUsage>,
    val hasUsageAccess: Boolean,
    val period: StatsPeriod = StatsPeriod.MONTH,
    val isNetworkStatsData: Boolean = false
) {
    companion object {
        val EMPTY = NetworkData(SystemNetworkStats.EMPTY, emptyList(), false)
    }
}

enum class StatsPeriod(val label: String, val days: Int) {
    TODAY("اليوم", 1),
    WEEK("آخر 7 أيام", 7),
    MONTH("آخر 30 يوماً", 30)
}
