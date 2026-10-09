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
        driveService.listFolders(parentId).files
    }

    suspend fun getAudioFiles(folderId: String? = null): List<Song> = withContext(Dispatchers.IO) {
        val files = driveService.listAudioFiles(folderId).files
        files.map { it.toSong() }
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val files = driveService.searchAudioFiles(query).files
        files.map { it.toSong() }
    }

    fun isConnected(): Boolean {
        return authManager.authState.value is GoogleDriveAuthState.SignedIn
    }
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
