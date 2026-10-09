package com.saurav.pixelmusic.data.gdrive

import com.saurav.pixelmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveRepository @Inject constructor(
    private val driveService: GoogleDriveService,
    private val authManager: GoogleDriveAuthManager
) {
    suspend fun getFolders(parentId: String? = null): List<GoogleDriveFile> = withContext(Dispatchers.IO) {
        val allFiles = driveService.listFolders(parentId).files
        allFiles.filter { it.isFolder }
    }

    suspend fun getAudioFiles(folderId: String? = null): List<Song> = withContext(Dispatchers.IO) {
        val files = driveService.listAudioFiles(folderId).files
        files.filter { it.isStrictAudio() }.map { it.toSong() }
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val files = driveService.searchAudioFiles(query).files
        files.filter { it.isStrictAudio() }.map { it.toSong() }
    }

    fun isConnected(): Boolean {
        return authManager.authState.value is GoogleDriveAuthState.SignedIn
    }
}

fun GoogleDriveFile.isStrictAudio(): Boolean {
    if (isFolder) return false

    val lowerName = name.lowercase().trim()
    val audioExtensions = listOf(
        ".mp3", ".flac", ".m4a", ".wav", ".wave", ".ogg", ".opus",
        ".aac", ".wma", ".alac", ".aif", ".aiff", ".m4p", ".mid", ".midi"
    )
    val hasAudioExt = audioExtensions.any { lowerName.endsWith(it) }

    // Known non-audio MIME prefixes / types
    val nonAudioMimes = listOf(
        "application/pdf",
        "application/zip",
        "application/x-zip",
        "application/x-zip-compressed",
        "application/vnd.android.package-archive",
        "application/vnd.google-apps",
        "application/json",
        "application/msword",
        "application/vnd.openxmlformats",
        "application/octet-stream",
        "image/",
        "video/",
        "text/"
    )

    // Known non-audio file extensions
    val nonAudioExtensions = listOf(
        ".pdf", ".zip", ".rar", ".7z", ".tar", ".gz", ".apk", ".xapk",
        ".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".bmp",
        ".mp4", ".mkv", ".mov", ".avi", ".webm", ".flv",
        ".txt", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".json", ".xml", ".html", ".css", ".js", ".bin", ".iso", ".exe"
    )

    // Reject non-audio extensions immediately
    if (nonAudioExtensions.any { lowerName.endsWith(it) }) return false

    // Reject non-audio MIME types
    if (nonAudioMimes.any { mimeType.startsWith(it, ignoreCase = true) }) {
        if (mimeType.equals("application/octet-stream", ignoreCase = true) && hasAudioExt) {
            return true
        }
        return false
    }

    val isAudioMime = mimeType.startsWith("audio/", ignoreCase = true)
    return isAudioMime || hasAudioExt
}

fun GoogleDriveFile.toSong(): Song {
    val cleanTitle = name.substringBeforeLast(".")
    return Song(
        id = "gdrive_$id",
        title = cleanTitle,
        artist = "Google Drive",
        artistId = 0L,
        album = "Google Drive",
        albumId = 0L,
        path = "gdrive://$id",
        contentUriString = "gdrive://$id",
        albumArtUriString = thumbnailLink,
        duration = 0L,
        mimeType = mimeType,
        bitrate = null,
        sampleRate = null,
        gdriveFileId = id
    )
}
