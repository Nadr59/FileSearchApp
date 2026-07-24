package com.filemanager.search.data

data class FileItem(
    val id: Long,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val path: String,
    val extension: String,
    val fileType: FileType
)
