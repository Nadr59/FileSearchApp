package com.filemanager.search.data.remote

import com.filemanager.search.data.FileItem
import com.filemanager.search.utils.formatFileSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileContentReader {

    private val TEXT_EXTENSIONS = setOf(
        "txt", "rtf", "csv", "log", "md",
        "java", "kt", "py", "js", "ts", "html", "css",
        "cpp", "c", "h", "hpp", "cs", "go", "rs", "swift",
        "json", "xml", "yaml", "yml", "toml", "ini", "cfg", "conf",
        "sh", "bat", "ps1", "sql", "rb", "php",
        "gradle", "properties", "gitignore", "dockerfile"
    )

    fun canReadContent(file: FileItem): Boolean {
        return file.extension.lowercase() in TEXT_EXTENSIONS
    }

    fun readContent(file: FileItem, maxChars: Int = 4000): String? {
        if (!canReadContent(file)) return null

        return try {
            val f = File(file.path)
            if (!f.exists() || !f.canRead()) return null
            if (f.length() > 5 * 1024 * 1024) return null

            val content = f.readText(Charsets.UTF_8)
            if (content.isBlank()) return null

            if (content.length > maxChars) {
                content.take(maxChars) + "\n... (truncated)"
            } else {
                content
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getFileInfoSummary(file: FileItem): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(file.dateModified))
        return buildString {
            appendLine("Name: ${file.name}")
            appendLine("Type: ${file.fileType.displayName} (.${file.extension})")
            appendLine("MIME: ${file.mimeType.ifEmpty { "unknown" }}")
            appendLine("Size: ${formatFileSize(file.size)}")
            appendLine("Path: ${file.path}")
            appendLine("Modified: $dateStr")
        }
    }
}
