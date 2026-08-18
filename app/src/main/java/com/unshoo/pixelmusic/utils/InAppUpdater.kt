package com.unshoo.pixelmusic.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("assets") val assets: List<GithubAsset>,
    @SerializedName("body") val body: String?
)

data class GithubAsset(
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("name") val name: String
)

sealed class UpdateState {
    object Checking : UpdateState()
    data class UpToDate(val changelog: String? = null) : UpdateState()
    data class Available(
        val versionName: String, 
        val downloadUrl: String,
        val changelog: String? = null
    ) : UpdateState()
    data class Downloading(
        val progress: Float,
        val changelog: String? = null
    ) : UpdateState()
}

object InAppUpdater {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val REPO_URL = "https://api.github.com/repos/atappu805/PixelMusic/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REPO_URL).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                val release = gson.fromJson(body, GithubRelease::class.java)
                
                // Compare tags (e.g., "v2.1" vs "2.0.0")
                val cleanLatest = release.tagName.replace(Regex("[^0-9.]"), "")
                val cleanCurrent = currentVersion.replace(Regex("[^0-9.]"), "")
                
                if (cleanLatest != cleanCurrent && release.assets.isNotEmpty()) {
                    val apkAssets = release.assets.filter { it.name.endsWith(".apk") }
                    val apkAsset = selectBestApkForDevice(apkAssets)
                    
                    if (apkAsset != null) {
                        return@withContext UpdateState.Available(
                            versionName = release.tagName, 
                            downloadUrl = apkAsset.downloadUrl,
                            changelog = release.body
                        )
                    }
                }
                return@withContext UpdateState.UpToDate(changelog = release.body)
            }
            return@withContext UpdateState.UpToDate(changelog = null)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UpdateState.UpToDate(changelog = null)
        }
    }
    
    // --- NEW LIFECYCLE-INDEPENDENT DOWNLOAD MANAGER ---

    sealed class GlobalDownloadState {
        object Idle : GlobalDownloadState()
        data class Downloading(val progress: Float, val isPaused: Boolean, val versionName: String) : GlobalDownloadState()
        data class Finished(val apkFile: File, val versionName: String) : GlobalDownloadState()
        data class Error(val message: String) : GlobalDownloadState()
    }

    private val updaterScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var downloadJob: kotlinx.coroutines.Job? = null
    
    val downloadState = kotlinx.coroutines.flow.MutableStateFlow<GlobalDownloadState>(GlobalDownloadState.Idle)
    
    private var currentDownloadUrl: String? = null
    private var currentFileName: String? = null
    private var currentVersionName: String? = null
    private var downloadedBytes = 0L
    private var totalBytes = 0L

    fun startOrResumeDownload(context: Context, url: String, versionName: String) {
        if (downloadJob?.isActive == true) return

        currentDownloadUrl = url
        currentVersionName = versionName
        currentFileName = "PixelMusic_$versionName.apk"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), currentFileName!!)

        downloadState.value = GlobalDownloadState.Downloading(
            progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f, 
            isPaused = false,
            versionName = versionName
        )

        downloadJob = updaterScope.launch {
            try {
                val requestBuilder = Request.Builder().url(url)
                
                // If we have partially downloaded data, use the Range header to resume!
                if (file.exists() && downloadedBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                } else {
                    file.delete()
                    downloadedBytes = 0L
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    downloadState.value = GlobalDownloadState.Error("Server rejected request")
                    return@launch
                }

                val body = response.body ?: return@launch
                if (totalBytes == 0L) {
                    // Only set total bytes on the first fresh download, otherwise it just returns the remaining size
                    totalBytes = body.contentLength() + downloadedBytes 
                }

                val inputStream = body.byteStream()
                // Open in append mode if we are resuming
                val outputStream = java.io.FileOutputStream(file, downloadedBytes > 0)
                val buffer = ByteArray(8 * 1024)
                var bytes = inputStream.read(buffer)
                var lastEmitTime = System.currentTimeMillis()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val notifId = 999

                while (bytes >= 0) {
                    // Check if the user hit pause or cancel
                    if (!isActive) break 

                    outputStream.write(buffer, 0, bytes)
                    downloadedBytes += bytes

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastEmitTime > 150 || downloadedBytes == totalBytes) {
                        val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                        downloadState.value = GlobalDownloadState.Downloading(progress, false, versionName)
                        
                        // Update Notification
                        val notif = androidx.core.app.NotificationCompat.Builder(context, "app_updates")
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                            .setContentTitle("Downloading Update $versionName")
                            .setProgress(100, (progress * 100).toInt(), false)
                            .setOngoing(true)
                            .build()
                        notificationManager.notify(notifId, notif)

                        lastEmitTime = currentTime
                    }
                    bytes = inputStream.read(buffer)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (downloadedBytes == totalBytes && totalBytes > 0) {
                    downloadState.value = GlobalDownloadState.Finished(file, versionName)
                    
                    // Finished Notification
                    val finishedNotif = androidx.core.app.NotificationCompat.Builder(context, "app_updates")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("Download Complete")
                        .setContentText("Ready to install PixelMusic $versionName")
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(notifId, finishedNotif)
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    downloadState.value = GlobalDownloadState.Error(e.message ?: "Download failed")
                }
            }
        }
    }

    fun pauseDownload() {
        downloadJob?.cancel()
        currentVersionName?.let {
            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
            downloadState.value = GlobalDownloadState.Downloading(progress, isPaused = true, versionName = it)
        }
    }

    fun cancelDownload(context: Context) {
        downloadJob?.cancel()
        currentFileName?.let {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), it)
            if (file.exists()) file.delete()
        }
        downloadedBytes = 0L
        totalBytes = 0L
        downloadState.value = GlobalDownloadState.Idle
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(999)
    }

    fun deleteApk(context: Context) {
        currentFileName?.let {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), it)
            if (file.exists()) file.delete()
        }
        downloadedBytes = 0L
        totalBytes = 0L
        downloadState.value = GlobalDownloadState.Idle
    }

    fun installApk(context: Context, file: File) {
        if (file.exists()) {
            val authority = "${context.packageName}.provider"
            val apkUri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }
    }
}
