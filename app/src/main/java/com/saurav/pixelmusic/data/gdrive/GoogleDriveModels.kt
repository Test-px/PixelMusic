package com.saurav.pixelmusic.data.gdrive

import kotlinx.serialization.Serializable

@Serializable
data class GoogleDriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long = 0L,
    val modifiedTime: String? = null,
    val thumbnailLink: String? = null,
    val webContentLink: String? = null,
    val parents: List<String> = emptyList()
) {
    val isFolder: Boolean
        get() = mimeType == "application/vnd.google-apps.folder"

    val isAudio: Boolean
        get() = mimeType.startsWith("audio/") || 
                name.endsWith(".mp3", ignoreCase = true) ||
                name.endsWith(".flac", ignoreCase = true) ||
                name.endsWith(".m4a", ignoreCase = true) ||
                name.endsWith(".wav", ignoreCase = true) ||
                name.endsWith(".ogg", ignoreCase = true) ||
                name.endsWith(".aac", ignoreCase = true) ||
                name.endsWith(".opus", ignoreCase = true)
}

@Serializable
data class GoogleDriveFileListResponse(
    val nextPageToken: String? = null,
    val files: List<GoogleDriveFile> = emptyList()
)

@Serializable
data class GoogleDriveUserInfo(
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null
)

@Serializable
data class GoogleDriveTokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val token_type: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

sealed interface GoogleDriveAuthState {
    object SignedOut : GoogleDriveAuthState
    object Authenticating : GoogleDriveAuthState
    data class SignedIn(
        val email: String?,
        val name: String?
    ) : GoogleDriveAuthState
    data class Error(val message: String) : GoogleDriveAuthState
}
