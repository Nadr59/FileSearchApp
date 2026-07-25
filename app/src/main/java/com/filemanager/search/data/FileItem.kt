package com.filemanager.search.data

// ═══ من أين جاء الملف؟ ═══
enum class FileSource {
    MEDIASTORE,  // من MediaStore (لديه content URI حقيقي)
    FILESYSTEM   // من مشي المجلدات (يحتاج FileProvider)
}

data class FileItem(
    val id: Long,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val path: String,
    val extension: String,
    val fileType: FileType,
    val source: FileSource = FileSource.MEDIASTORE
)
