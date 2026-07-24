package com.filemanager.search.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.webkit.MimeTypeMap

class FileRepository(private val context: Context) {

    fun searchFiles(fileType: FileType): List<FileItem> {
        val files = mutableListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        val selection: String?
        val selectionArgs: Array<String>?

        if (fileType == FileType.ALL) {
            selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} IS NOT NULL"
            selectionArgs = null
        } else {
            selection = fileType.extensions.joinToString(" OR ") {
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            }
            selectionArgs = fileType.extensions.map { "%.$it" }.toTypedArray()
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val dateModified = cursor.getLong(dateCol) * 1000L
                val mimeType = cursor.getString(mimeCol) ?: guessMimeType(name)
                val path = cursor.getString(pathCol) ?: ""

                val ext = name.substringAfterLast('.', "").lowercase()
                val detectedType = FileType.fromExtension(ext)

                if (fileType == FileType.ALL || detectedType == fileType) {
                    files.add(
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
        }

        return files
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
            } catch (e: Exception) {
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
