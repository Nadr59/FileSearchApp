package com.filemanager.search.data

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

class FileRepository(private val context: Context) {

    fun searchFiles(fileType: FileType): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val seen = mutableSetOf<String>()

        // ═══════════════════════════════════════
        // الطريقة 1: MediaStore (سريع)
        // ═══════════════════════════════════════
        try {
            searchViaMediaStore(fileType, results, seen)
        } catch (_: Exception) {}

        // ═══════════════════════════════════════
        // الطريقة 2: مشي المجلدات (يجد كل شيء)
        // ═══════════════════════════════════════
        if (shouldUseFileSystem(fileType)) {
            try {
                searchViaFileSystem(fileType, results, seen)
            } catch (_: Exception) {}
        }

        return results.sortedByDescending { it.dateModified }
    }

    // ═══════════════════════════════════════
    // هل نحتاج مشي الملفات؟
    // ═══════════════════════════════════════
    private fun shouldUseFileSystem(fileType: FileType): Boolean {
        return when (fileType) {
            FileType.ALL,
            FileType.TEXT, FileType.OFFICE, FileType.PDF,
            FileType.EBOOKS, FileType.COMPRESSED, FileType.APK,
            FileType.CODE, FileType.GAMES, FileType.DATABASE,
            FileType.FONTS -> true
            FileType.IMAGES, FileType.VIDEOS, FileType.AUDIO -> false
        }
    }

    // ═══════════════════════════════════════
    // البحث عبر MediaStore (للصور والفيديو والصوت)
    // ═══════════════════════════════════════
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

                        if (seen.add("$name|$size")) {
                            results.add(
                                FileItem(id, name, size, dateModified, mimeType, path, ext, detectedType)
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════
    // البحث عبر مشي المجلدات (يجد PDF, Office, APK...)
    // ═══════════════════════════════════════
    private fun searchViaFileSystem(
        fileType: FileType,
        results: MutableList<FileItem>,
        seen: MutableSet<String>
    ) {
        val searchPaths = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS),
        ).filter { it != null && it.exists() }

        val extensions = if (fileType == FileType.ALL) {
            null // كل الامتدادات
        } else {
            fileType.extensions.map { it.lowercase() }.toSet()
        }

        for (dir in searchPaths) {
            try {
                walkDirectory(dir!!, extensions, results, seen, maxDepth = 8)
            } catch (_: Exception) {}
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
                        walkDirectory(file, extensions, results, seen, maxDepth - 1)
                    } else if (file.isFile) {
                        val name = file.name
                        val ext = name.substringAfterLast('.', "").lowercase()

                        if (extensions == null || ext in extensions) {
                            val size = file.length()
                            val dateModified = file.lastModified()

                            if (seen.add("$name|$size")) {
                                val mimeType = guessMimeType(name)
                                val detectedType = FileType.fromExtension(ext)
                                val id = getMediaStoreId(file.absolutePath) ?: file.hashCode().toLong()

                                results.add(
                                    FileItem(id, name, size, dateModified, mimeType, file.absolutePath, ext, detectedType)
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════
    // الحصول على MediaStore ID من المسار
    // ═══════════════════════════════════════
    private fun getMediaStoreId(path: String): Long? {
        return try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns._ID),
                "${MediaStore.Files.FileColumns.DATA} = ?",
                arrayOf(path),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else null
            }
        } catch (_: Exception) { null }
    }

    // ═══════════════════════════════════════
    // فتح الملف
    // ═══════════════════════════════════════
    fun getFileUri(file: FileItem): android.net.Uri? {
        // حاول أولاً من MediaStore
        try {
            val mediaUri = MediaStore.Files.getContentUri("external")
            val contentUri = ContentUris.withAppendedId(mediaUri, file.id)
            context.contentResolver.query(contentUri, arrayOf(MediaStore.Files.FileColumns._ID), null, null, null)?.use {
                if (it.moveToFirst()) return contentUri
            }
        } catch (_: Exception) {}

        // استخدم FileProvider
        return try {
            val javaFile = File(file.path)
            if (javaFile.exists()) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    javaFile
                )
            } else null
        } catch (_: Exception) { null }
    }

    fun deleteFiles(files: List<FileItem>): Boolean {
        var success = true
        for (file in files) {
            try {
                // حذف من MediaStore
                val uri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"), file.id
                )
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted == 0) {
                    // حذف مباشر من الملفات
                    val javaFile = File(file.path)
                    if (javaFile.exists() && javaFile.delete()) {
                        // تم الحذف
                    } else {
                        success = false
                    }
                }
            } catch (_: Exception) {
                success = false
            }
        }
        return success
    }

    fun getContentUri(fileId: Long) =
        ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), fileId)

    private fun getFilePathFromUri(uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.Files.FileColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
