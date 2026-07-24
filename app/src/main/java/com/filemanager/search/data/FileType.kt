package com.filemanager.search.data

enum class FileType(
    val displayName: String,
    val emoji: String,
    val extensions: List<String>,
    val color: Long
) {
    ALL("All Files", "📁", emptyList(), 0xFF607D8B),
    IMAGES("Images", "🖼️", listOf("jpg","jpeg","png","gif","webp","bmp","svg"), 0xFF2196F3),
    VIDEOS("Videos", "🎬", listOf("mp4","mkv","avi","mov","flv","wmv","3gp"), 0xFFE91E63),
    AUDIO("Audio", "🎵", listOf("mp3","wav","m4a","ogg","flac","aac","wma"), 0xFFFF9800),
    TEXT("Text Files", "📄", listOf("txt","rtf","csv","log"), 0xFF9E9E9E),
    OFFICE("Office", "📑", listOf("doc","docx","xls","xlsx","ppt","pptx"), 0xFF1565C0),
    PDF("PDF", "📖", listOf("pdf"), 0xFFF44336),
    EBOOKS("E-Books", "📚", listOf("epub","mobi","azw","azw3"), 0xFF795548),
    COMPRESSED("Compressed", "📦", listOf("zip","rar","7z","tar","gz","bz2"), 0xFFFFC107),
    APK("APK Files", "⚙️", listOf("apk"), 0xFF4CAF50),
    CODE("Code", "💻", listOf("java","kt","py","js","ts","html","css","cpp","c","h","json"), 0xFF9C27B0),
    GAMES("Game Data", "🎮", listOf("iso","bin","cue","rom","nds","gba"), 0xFFE91E63),
    DATABASE("Database", "🗃️", listOf("db","sqlite","sqlite3"), 0xFF00BCD4),
    FONTS("Fonts", "🔤", listOf("ttf","otf","woff","woff2"), 0xFF3F51B5);

    companion object {
        fun fromExtension(ext: String): FileType {
            return entries.find { it != ALL && it.extensions.contains(ext.lowercase()) } ?: ALL
        }
    }
}
