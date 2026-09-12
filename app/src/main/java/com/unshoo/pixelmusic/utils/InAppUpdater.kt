package com.unshoo.pixelmusic.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

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
}

object InAppUpdater {

    // ─── Notification constants ─────────────────────────────────────────────────────────
    // Channel for "update available" pings — created by UpdateWorker.kt
    private const val CHANNEL_UPDATE_PINGS = "pixelmusic_update_channel"

    // Channel for download progress — created lazily by us on first download
    private const val CHANNEL_DOWNLOADS = "pixelmusic_downloads"

    // Notification IDs (UpdateWorker uses 999 for update-available pings)
    private const val NOTIF_ID_DOWNLOAD = 1001

    // ─── Repo config ────────────────────────────────────────────────────────────────────
    // NOTE: This is patched to the test repo by Test.yaml for test builds.
    private const val REPO_URL = "https://api.github.com/repos/Saurav-02/PixelMusic/releases/latest"

    private val client = OkHttpClient()
    private val gson = Gson()

    // ─── Version comparison ─────────────────────────────────────────────────────────────

    /**
     * Compares two version strings semantically by extracting all digit groups.
     * Works for "4.5", "4.10", "4.5.2", "test-build-331", "v5.0.1", etc.
     *
     * Returns:
     *   > 0  →  [a] is newer than [b]
     *   < 0  →  [a] is older than [b]
     *     0  →  equal
     */
    private fun compareVersions(a: String, b: String): Int {
        val aNums = Regex("\\d+").findAll(a).map { it.value.toIntOrNull() ?: 0 }.toList()
        val bNums = Regex("\\d+").findAll(b).map { it.value.toIntOrNull() ?: 0 }.toList()
        val maxLen = maxOf(aNums.size, bNums.size)
        for (i in 0 until maxLen) {
            val cmp = aNums.getOrElse(i) { 0 }.compareTo(bNums.getOrElse(i) { 0 })
            if (cmp != 0) return cmp
        }
        return 0
    }

    // ─── Update check ───────────────────────────────────────────────────────────────────

    suspend fun checkForUpdate(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REPO_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateState.UpToDate(changelog = null)
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateState.UpToDate(changelog = null)
                val release = try {
                    gson.fromJson(body, GithubRelease::class.java)
                } catch (e: Exception) {
                    null
                } ?: return@withContext UpdateState.UpToDate(changelog = null)

                val isNewer = compareVersions(release.tagName, currentVersion) > 0
                if (isNewer && release.assets.isNotEmpty()) {
                    val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
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
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UpdateState.UpToDate(changelog = null)
        }
    }

    private fun selectBestApkForDevice(assets: List<GithubAsset>): GithubAsset? {
        if (assets.isEmpty()) return null
        if (assets.size == 1) return assets.first()

        val deviceAbis = android.os.Build.SUPPORTED_ABIS.map { it.lowercase() }

        for (abi in deviceAbis) {
            val abiMatch = when {
                abi.contains("arm64") -> assets.firstOrNull {
                    it.name.contains("arm64", ignoreCase = true) ||
                        it.name.contains("v8a", ignoreCase = true)
                }
                abi.contains("v7") -> assets.firstOrNull {
                    it.name.contains("armv7", ignoreCase = true) ||
                        it.name.contains("v7a", ignoreCase = true)
                }
                abi.contains("x86_64") -> assets.firstOrNull { it.name.contains("x86_64", ignoreCase = true) }
                abi.contains("x86") -> assets.firstOrNull { it.name.contains("x86", ignoreCase = true) }
                else -> null
            }
            if (abiMatch != null) return abiMatch
        }

        val universalMatch = assets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
        if (universalMatch != null) return universalMatch

        return assets.first()
    }

    // ─── Download state machine ─────────────────────────────────────────────────────────

    sealed class GlobalDownloadState {
        object Idle : GlobalDownloadState()
        data class Downloading(
            val progress: Float,
            val isPaused: Boolean,
            val versionName: String,
            val totalBytes: Long,
            val downloadedBytes: Long
        ) : GlobalDownloadState()
        data class Finished(val apkFile: File, val versionName: String) : GlobalDownloadState()
        data class Error(val message: String) : GlobalDownloadState()
    }

    private val updaterScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    val downloadState = MutableStateFlow<GlobalDownloadState>(GlobalDownloadState.Idle)

    private var currentDownloadUrl: String? = null
    private var currentFileName: String? = null
    private var currentVersionName: String? = null
    private var downloadedBytes = 0L
    private var totalBytes = 0L

    private var isReceiverRegistered = false
    private var appContext: Context? = null

    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "PIXELMUSIC_PAUSE" -> pauseDownload()
                "PIXELMUSIC_RESUME" -> resumeDownload(context)
                "PIXELMUSIC_CANCEL" -> cancelDownload(context)
            }
        }
    }

    private fun registerReceiverIfNeeded(context: Context) {
        if (!isReceiverRegistered) {
            val filter = android.content.IntentFilter().apply {
                addAction("PIXELMUSIC_PAUSE")
                addAction("PIXELMUSIC_RESUME")
                addAction("PIXELMUSIC_CANCEL")
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context.applicationContext,
                actionReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
    }

    // ─── Notification channel bootstrap ─────────────────────────────────────────────────

    private fun ensureDownloadChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_DOWNLOADS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DOWNLOADS,
                    "Update Downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows progress while downloading PixelMusic updates"
                    setShowBadge(false)
                }
            )
        }
    }

    // ─── Old APK cleanup ────────────────────────────────────────────────────────────────

    private fun cleanOldApks(context: Context, keepFileName: String) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        dir.listFiles()?.forEach { f ->
            if (f.isFile &&
                f.name.startsWith("PixelMusic_", ignoreCase = true) &&
                f.name.endsWith(".apk", ignoreCase = true) &&
                f.name != keepFileName
            ) {
                f.delete()
            }
        }
    }

    // ─── Download orchestration ─────────────────────────────────────────────────────────

    fun startOrResumeDownload(context: Context, url: String, versionName: String) {
        if (downloadJob?.isActive == true) return

        appContext = context.applicationContext
        registerReceiverIfNeeded(appContext!!)
        ensureDownloadChannel(appContext!!)

        currentDownloadUrl = url
        currentVersionName = versionName
        currentFileName = "PixelMusic_$versionName.apk"
        cleanOldApks(context, currentFileName!!)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), currentFileName!!)

        if (downloadState.value is GlobalDownloadState.Finished && file.exists()) {
            return
        }

        fun postFinishedNotification() {
            val authority = "${appContext!!.packageName}.provider"
            val apkUri = FileProvider.getUriForFile(appContext!!, authority, file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val pendingInstall = android.app.PendingIntent.getActivity(
                appContext, 0, installIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val finishedNotif = NotificationCompat.Builder(appContext!!, CHANNEL_DOWNLOADS)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText("Tap to install PixelMusic $versionName")
                .setContentIntent(pendingInstall)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            val nm = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID_DOWNLOAD, finishedNotif)
        }

        downloadState.value = GlobalDownloadState.Downloading(
            progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f,
            isPaused = false,
            versionName = versionName,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes
        )

        downloadJob = updaterScope.launch {
            try {
                val requestBuilder = Request.Builder().url(url)
                val hadPartial = file.exists() && downloadedBytes > 0

                if (hadPartial) {
                    if (totalBytes > 0 && downloadedBytes >= totalBytes) {
                        downloadState.value = GlobalDownloadState.Finished(file, versionName)
                        postFinishedNotification()
                        return@launch
                    }
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                } else {
                    file.delete()
                    downloadedBytes = 0L
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        downloadState.value = GlobalDownloadState.Error("Server rejected request. Try restarting the download.")
                        return@launch
                    }

                    // If we requested a partial (Range) but got a full 200 response,
                    // the server ignored our Range header. Start over to avoid appending
                    // a full file onto a partial one (which would corrupt the APK).
                    if (response.code == 200 && downloadedBytes > 0) {
                        file.delete()
                        downloadedBytes = 0L
                        totalBytes = 0L
                    }

                    val body = response.body
                    if (body == null) {
                        downloadState.value = GlobalDownloadState.Error("Empty response from server.")
                        return@launch
                    }

                    val contentLength = body.contentLength()
                    if (totalBytes <= 0L) {
                        totalBytes = if (contentLength > 0) contentLength + downloadedBytes else -1L
                    }

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(file, downloadedBytes > 0)

                    try {
                        val buffer = ByteArray(8 * 1024)
                        var bytes = inputStream.read(buffer)
                        var lastEmitTime = System.currentTimeMillis()

                        val nm = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        val pauseIntent = android.app.PendingIntent.getBroadcast(
                            appContext, 1,
                            Intent("PIXELMUSIC_PAUSE").setPackage(appContext!!.packageName),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        val cancelIntent = android.app.PendingIntent.getBroadcast(
                            appContext, 3,
                            Intent("PIXELMUSIC_CANCEL").setPackage(appContext!!.packageName),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        val openIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("pixelmusic://update_download")).apply {
                            setPackage(appContext!!.packageName)
                        }
                        val pendingOpenIntent = android.app.PendingIntent.getActivity(
                            appContext, 4, openIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        while (bytes >= 0) {
                            if (!isActive) break

                            outputStream.write(buffer, 0, bytes)
                            downloadedBytes += bytes

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastEmitTime > 150) {
                                val progress = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes.toFloat()
                                } else 0f

                                downloadState.value = GlobalDownloadState.Downloading(
                                    progress = progress,
                                    isPaused = false,
                                    versionName = versionName,
                                    totalBytes = totalBytes,
                                    downloadedBytes = downloadedBytes
                                )

                                val mbString = if (totalBytes > 0) {
                                    val totalMb = totalBytes / (1024f * 1024f)
                                    val downMb = downloadedBytes / (1024f * 1024f)
                                    String.format(Locale.US, "%.1f / %.1f MB", downMb, totalMb)
                                } else {
                                    val downMb = downloadedBytes / (1024f * 1024f)
                                    String.format(Locale.US, "%.1f MB downloaded", downMb)
                                }

                                val notif = NotificationCompat.Builder(appContext!!, CHANNEL_DOWNLOADS)
                                    .setSmallIcon(android.R.drawable.stat_sys_download)
                                    .setContentTitle("Downloading Update $versionName")
                                    .setContentText(mbString)
                                    .setProgress(100, (progress * 100).toInt(), false)
                                    .setContentIntent(pendingOpenIntent)
                                    .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
                                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                                    .setOngoing(true)
                                    .build()
                                nm.notify(NOTIF_ID_DOWNLOAD, notif)

                                lastEmitTime = currentTime
                            }

                            bytes = inputStream.read(buffer)
                        }

                        outputStream.flush()
                    } finally {
                        try { outputStream.close() } catch (_: Exception) {}
                        try { inputStream.close() } catch (_: Exception) {}
                    }
                }

                if (isActive) {
                    downloadState.value = GlobalDownloadState.Finished(file, versionName)
                    postFinishedNotification()
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    downloadState.value = GlobalDownloadState.Error(e.message ?: "Download failed")
                }
            }
        }
    }

    fun resumeDownload(context: Context) {
        if (currentDownloadUrl != null && currentVersionName != null) {
            startOrResumeDownload(context, currentDownloadUrl!!, currentVersionName!!)
        }
    }

    fun pauseDownload() {
        downloadJob?.cancel()
        currentVersionName?.let {
            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
            downloadState.value = GlobalDownloadState.Downloading(
                progress = progress,
                isPaused = true,
                versionName = it,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes
            )

            if (appContext != null) {
                val nm = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val resumeIntent = android.app.PendingIntent.getBroadcast(
                    appContext, 2,
                    Intent("PIXELMUSIC_RESUME").setPackage(appContext!!.packageName),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val cancelIntent = android.app.PendingIntent.getBroadcast(
                    appContext, 3,
                    Intent("PIXELMUSIC_CANCEL").setPackage(appContext!!.packageName),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val openIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("pixelmusic://update_download")).apply {
                    setPackage(appContext!!.packageName)
                }
                val pendingOpenIntent = android.app.PendingIntent.getActivity(
                    appContext, 4, openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val pausedNotif = NotificationCompat.Builder(appContext!!, CHANNEL_DOWNLOADS)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Update Paused ($it)")
                    .setProgress(100, (progress * 100).toInt(), false)
                    .setContentIntent(pendingOpenIntent)
                    .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                    .setOngoing(true)
                    .build()
                nm.notify(NOTIF_ID_DOWNLOAD, pausedNotif)
            }
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

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_DOWNLOAD)
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
