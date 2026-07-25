package com.filemanager.search.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

class FileRepository(private val context: Context) {

    fun searchFiles(fileType: FileType): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val seen = mutableSetOf<String>()

        try { searchViaMediaStore(fileType, results, seen) } catch (_: Exception) {}

        if (shouldUseFileSystem(fileType)) {
            try { searchViaFileSystem(fileType, results, seen) } catch (_: Exception) {}
        }

        return results.sortedByDescending { it.dateModified }
    }

    private fun shouldUseFileSystem(fileType: FileType): Boolean {
        return when (fileType) {
            FileType.ALL, FileType.TEXT, FileType.OFFICE, FileType.PDF,
            FileType.EBOOKS, FileType.COMPRESSED, FileType.APK,
            FileType.CODE, FileType.GAMES, FileType.DATABASE, FileType.FONTS -> true
            FileType.IMAGES, FileType.VIDEOS, FileType.AUDIO -> false
        }
    }

    private fun searchViaMediaStore(
        fileType: FileType,
        results: MutableList<FileItem>,
        seen: MutableSet<String>
    ) {
        val uris = when (fileType) {
            FileType.IMAGES -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            FileType.VIDEOS -> listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            FileType.AUDIO -> listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            FileType.ALL -> listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
            else -> return
        }

        for (uri in uris) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.MediaColumns.MIME_TYPE
                    ),
                    null, null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val size = cursor.getLong(sizeCol)
                        val dateModified = cursor.getLong(dateCol) * 1000L
                        val mimeType = cursor.getString(mimeCol) ?: guessMimeType(name)
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val detectedType = FileType.fromExtension(ext)

                        if (fileType != FileType.ALL && detectedType != fileType) continue

                        val contentUri = ContentUris.withAppendedId(uri, id)
                        val path = getFilePathFromUri(contentUri) ?: ""

                        if (path.isNotEmpty() && seen.add(path)) {
                            results.add(FileItem(id, name, size, dateModified, mimeType, path, ext, detectedType))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun searchViaFileSystem(
        fileType: FileType,
        results: MutableList<FileItem>,
        seen: MutableSet<String>
    ) {
        val searchPaths = listOfNotNull(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        ).filter { it.exists() }

        val extensions = if (fileType == FileType.ALL) null
        else fileType.extensions.map { it.lowercase() }.toSet()

        for (dir in searchPaths) {
            try { walkDirectory(dir, extensions, results, seen, 10) } catch (_: Exception) {}
        }
    }

    private fun walkDirectory(
        dir: File,
        extensions: Set<String>?,
        results: MutableList<FileItem>,
        seen: MutableSet<String>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0) return
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                try {
                    if (file.isHidden) continue
                    if (file.isDirectory) {
                        if (file.name == "Android" || file.name.startsWith(".")) continue
                        walkDirectory(file, extensions, results, seen, maxDepth - 1)
                    } else if (file.isFile) {
                        val name = file.name
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val path = file.absolutePath
                        if (extensions == null || ext in extensions) {
                            if (seen.add(path)) {
                                results.add(
                                    FileItem(
                                        id = path.hashCode().toLong(),
                                        name = name,
                                        size = file.length(),
                                        dateModified = file.lastModified(),
                                        mimeType = guessMimeType(name),
                                        path = path,
                                        extension = ext,
                                        fileType = FileType.fromExtension(ext)
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    // حذف — مباشر بالملف
    // ═══════════════════════════════════════════
    fun deleteSingleFile(file: FileItem): Boolean {
        return try {
            val f = File(file.path)
            if (f.exists()) {
                val deleted = f.delete()
                if (deleted) {
                    // أخبر النظام بتحديث
                    android.media.MediaScannerConnection.scanFile(
                        context, arrayOf(file.path), null, null
                    )
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFiles(files: List<FileItem>): Pair<Int, Int> {
        var success = 0
        var failed = 0
        for (file in files) {
            if (deleteSingleFile(file)) success++ else failed++
        }
        return Pair(success, failed)
    }

    // ═══════════════════════════════════════════
    // URI لفتح/مشاركة الملف
    // ═══════════════════════════════════════════
    fun getFileUri(file: FileItem): Uri? {
        // FileProvider
        return try {
            val f = File(file.path)
            if (f.exists()) {
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", f
                )
            } else null
        } catch (_: Exception) { null }
    }

    fun getContentUri(fileId: Long) =
        ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), fileId)

    private fun getFilePathFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri, arrayOf(MediaStore.Files.FileColumns.DATA), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) { null }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
