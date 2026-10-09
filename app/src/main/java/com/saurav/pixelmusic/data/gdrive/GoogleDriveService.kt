package com.saurav.pixelmusic.data.gdrive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    private val authManager: GoogleDriveAuthManager,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "GoogleDriveService"
        private const val BASE_URL = "https://www.googleapis.com/drive/v3"
        private const val FILE_FIELDS = "nextPageToken, files(id, name, mimeType, size, modifiedTime, thumbnailLink, webContentLink, parents)"
    }

    suspend fun listAudioFiles(folderId: String? = null, pageToken: String? = null): GoogleDriveFileListResponse = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken() ?: return@withContext GoogleDriveFileListResponse()

        val query = buildString {
            append("trashed = false and ")
            if (!folderId.isNullOrBlank()) {
                append("'$folderId' in parents and ")
            }
            append("(mimeType contains 'audio/' or name contains '.mp3' or name contains '.flac' or name contains '.m4a' or name contains '.wav' or name contains '.ogg')")
        }

        executeFilesQuery(token, query, pageToken)
    }

    suspend fun listFolders(parentId: String? = null, pageToken: String? = null): GoogleDriveFileListResponse = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken() ?: return@withContext GoogleDriveFileListResponse()

        val query = buildString {
            append("trashed = false and mimeType = 'application/vnd.google-apps.folder'")
            if (!parentId.isNullOrBlank()) {
                append(" and '$parentId' in parents")
            }
        }

        executeFilesQuery(token, query, pageToken)
    }

    suspend fun searchAudioFiles(searchQuery: String, pageToken: String? = null): GoogleDriveFileListResponse = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken() ?: return@withContext GoogleDriveFileListResponse()

        val safeQuery = searchQuery.replace("'", "\\'")
        val query = "trashed = false and name contains '$safeQuery' and (mimeType contains 'audio/' or name contains '.mp3' or name contains '.flac' or name contains '.m4a' or name contains '.wav')"

        executeFilesQuery(token, query, pageToken)
    }

    private fun executeFilesQuery(token: String, query: String, pageToken: String?): GoogleDriveFileListResponse {
        try {
            val urlBuilder = "$BASE_URL/files".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("q", query)
                ?.addQueryParameter("fields", FILE_FIELDS)
                ?.addQueryParameter("pageSize", "100")
                ?.addQueryParameter("orderBy", "name_natural")

            if (!pageToken.isNullOrBlank()) {
                urlBuilder?.addQueryParameter("pageToken", pageToken)
            }

            val url = urlBuilder?.build()?.toString() ?: return GoogleDriveFileListResponse()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag(TAG).e("Drive API query failed (${response.code})")
                return GoogleDriveFileListResponse()
            }

            val body = response.body?.string() ?: return GoogleDriveFileListResponse()
            return json.decodeFromString<GoogleDriveFileListResponse>(body)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error executing Google Drive query")
            return GoogleDriveFileListResponse()
        }
    }
}
