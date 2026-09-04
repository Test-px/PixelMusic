package com.unshoo.pixelmusic.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.ui.glancewidget.PlayerActions
import java.util.Arrays

object LiveNotificationHelper {
    private const val LIVE_CHANNEL_ID = "pixelmusic_live_progress_v6"
    private const val LIVE_NOTIFICATION_ID = 1002

    private var lastArtworkBytes: ByteArray? = null
    private var cachedArtworkBitmap: Bitmap? = null

    // Animation state for the dynamic island
    private val animatedNotes = arrayOf("♫", "♩", "♪", "𝅗𝅥", "𝅘𝅥𝅯", "𝅘𝅥𝅰", "𝅘𝅥𝅱", "𝅘𝅥𝅲", "𝄞")
    private var noteIndex = 0

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Live Progress Tracker",
                NotificationManager.IMPORTANCE_DEFAULT 
            ).apply {
                description = "Drives the dynamic island progress pill"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Helper to format milliseconds into m:ss format
    private fun formatTimeMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }

    fun updateLiveNotification(
        context: Context,
        title: String,
        artist: String,
        positionMs: Long,
        durationMs: Long,
        artworkData: ByteArray?,
        style: String // Controls what shows up in the right slot
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        val pendingAppIntent = PendingIntent.getActivity(
            context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            context, 1,
            Intent(context, MusicService::class.java).apply { action = PlayerActions.PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getService(
            context, 2,
            Intent(context, MusicService::class.java).apply { action = PlayerActions.PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getService(
            context, 3,
            Intent(context, MusicService::class.java).apply { action = PlayerActions.NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val safeDuration = durationMs.coerceAtLeast(0L)

        // Generate the text based on the user's preference
        val criticalText = when (style) {
            "ANIMATED_NOTES" -> {
                val currentNote = animatedNotes[noteIndex]
                noteIndex = (noteIndex + 1) % animatedNotes.size
                currentNote
            }
            "PROGRESS_TIME" -> "${formatTimeMs(positionMs)}/${formatTimeMs(safeDuration)}"
            "STATIC_ICON" -> "🎧"
            else -> "🎧" // Default fallback
        }

        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(pendingAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setRequestPromotedOngoing(true) 
            .setShortCriticalText(criticalText) // Applying the dynamic text
            .setSmallIcon(R.drawable.monochrome_player)

        val progressPercent = if (safeDuration > 0L) {
            ((positionMs.toFloat() / safeDuration) * 100).toInt().coerceIn(0, 100)
        } else 0

        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val segment = NotificationCompat.ProgressStyle.Segment(100)
                segment.setColor(0xFFE91E63.toInt())

                val progressStyle = NotificationCompat.ProgressStyle()
                    .setProgressSegments(arrayListOf(segment))
                    .setStyledByProgress(true)
                    .setProgress(progressPercent)

                builder.setStyle(progressStyle)
            } catch (_: Throwable) {
                builder.setProgress(100, progressPercent, safeDuration == 0L)
            }
        } else {
            builder.setProgress(100, progressPercent, safeDuration == 0L)
        }

        builder.addAction(android.R.drawable.ic_media_previous, "⏮", prevIntent)
            .addAction(android.R.drawable.ic_media_pause, "❚❚", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "⏭", nextIntent)

        val bitmap = getOrDecodeArtwork(artworkData)
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        notificationManager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    private fun getOrDecodeArtwork(artworkData: ByteArray?): Bitmap? {
        if (artworkData == null || artworkData.isEmpty()) {
            lastArtworkBytes = null
            cachedArtworkBitmap = null
            return null
        }

        if (lastArtworkBytes != null && Arrays.equals(lastArtworkBytes, artworkData)) {
            return cachedArtworkBitmap
        }

        return try {
            val decoded = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            lastArtworkBytes = artworkData
            cachedArtworkBitmap = decoded
            decoded
        } catch (_: Exception) {
            null
        }
    }

    fun dismissLiveNotification(context: Context) {
        lastArtworkBytes = null
        cachedArtworkBitmap = null
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LIVE_NOTIFICATION_ID)
    }
}
