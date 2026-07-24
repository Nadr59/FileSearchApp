package com.filemanager.search.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap

class FileRepository(private val context: Context) {

    fun searchFiles(fileType: FileType): List<FileItem> {
        val results = mutableListOf<FileItem>()

        // ═══════════════════════════════════════
        // لكل نوع ملف: استخدم URI مناسب
        // ═══════════════════════════════════════
        val queries = buildQueries(fileType)

        for (query in queries) {
            try {
                queryFiles(query.first, query.second, results)
            } catch (_: Exception) {}
        }

        return results.sortedByDescending { it.dateModified }
    }

    private fun buildQueries(fileType: FileType): List<Pair<Uri, List<String>>> {
        val queries = mutableListOf<Pair<Uri, List<String>>>()

        when (fileType) {
            FileType.ALL -> {
                queries.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI to listOf("image/*"))
                queries.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI to listOf("video/*"))
                queries.add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to listOf("audio/*"))
                queries.add(MediaStore.Files.getContentUri("external") to listOf(
                    "application/pdf", "application/msword",
                    "application/vnd.openxmlformats-officedocument",
                    "application/zip", "application/octet-stream"
                ))
            }
            FileType.IMAGES -> {
                queries.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI to listOf("image/*"))
            }
            FileType.VIDEOS -> {
                queries.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI to listOf("video/*"))
            }
            FileType.AUDIO -> {
                queries.add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to listOf("audio/*"))
            }
            else -> {
                queries.add(MediaStore.Files.getContentUri("external") to fileType.extensions)
            }
        }

        return queries
    }

    private fun queryFiles(
        uri: Uri,
        extensions: List<String>,
        results: MutableList<FileItem>
    ) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection: String?
        val selectionArgs: Array<String>?

        if (extensions.size == 1 && extensions[0].contains("*")) {
            // نوع MIME عام (image/*, video/*, audio/*)
            selection = null
            selectionArgs = null
        } else if (extensions.isEmpty()) {
            selection = null
            selectionArgs = null
        } else {
            // بحث بالامتداد
            selection = extensions.joinToString(" OR ") {
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            }
            selectionArgs = extensions.map { "%.$it" }.toTypedArray()
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                uri, projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val mimeType = cursor.getString(mimeCol) ?: guessMimeType(name)

                    val ext = name.substringAfterLast('.', "").lowercase()
                    val detectedType = FileType.fromExtension(ext)

                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val path = getFilePath(contentUri) ?: ""

                    results.add(
                        FileItem(
                            id = id,
                            name = name,
                            size = size,
                            dateModified = dateModified,
                            mimeType = mimeType,
                            path = path,
                            extension = ext,
                            fileType = detectedType
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun getFilePath(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(MediaStore.Files.FileColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    return c.getString(idx)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun deleteFiles(files: List<FileItem>): Boolean {
        var success = true
        for (file in files) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"),
                file.id
            )
            try {
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted == 0) success = false
            } catch (_: Exception) {
                success = false
            }
        }
        return success
    }

    fun getContentUri(fileId: Long) =
        ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), fileId)

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
