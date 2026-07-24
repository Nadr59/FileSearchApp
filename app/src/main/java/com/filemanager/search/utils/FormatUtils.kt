package com.filemanager.search.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatSizeDetailed(bytes: Long): String {
    return when {
        bytes < 0 -> "0 B"
        bytes < 1024 -> "$bytes Bytes"
        bytes < 1024 * 1024 -> "$bytes Bytes (${bytes / 1024} KB)"
        bytes < 1024L * 1024 * 1024 -> {
            val kb = bytes / 1024
            val mb = String.format("%.2f", bytes / (1024.0 * 1024.0))
            "$bytes Bytes ($kb KB / $mb MB)"
        }
        else -> {
            val mb = String.format("%.2f", bytes / (1024.0 * 1024.0))
            val gb = String.format("%.3f", bytes / (1024.0 * 1024.0 * 1024.0))
            "$bytes Bytes ($mb MB / $gb GB)"
        }
    }
}
