package com.unshoo.pixelmusic.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.ui.glancewidget.PlayerActions

object LiveNotificationHelper {
    // Bumped channel ID to ensure the system registers the new IMPORTANCE_DEFAULT setting
    private const val LIVE_CHANNEL_ID = "pixelmusic_live_progress_v3"
    private const val LIVE_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Live Progress Tracker",
                NotificationManager.IMPORTANCE_DEFAULT // Required for the expanded card to show properly
            ).apply {
                description = "Drives the dynamic island progress pill"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateLiveNotification(
        context: Context,
        title: String,
        artist: String,
        liveText: String,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        artworkData: ByteArray?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        val pendingAppIntent = PendingIntent.getActivity(
            context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(context, 1, Intent(context, MusicService::class.java).apply { action = PlayerActions.PREVIOUS }, PendingIntent.FLAG_IMMUTABLE)
        val playPauseIntent = PendingIntent.getService(context, 2, Intent(context, MusicService::class.java).apply { action = PlayerActions.PLAY_PAUSE }, PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = PendingIntent.getService(context, 3, Intent(context, MusicService::class.java).apply { action = PlayerActions.NEXT }, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(pendingAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setRequestPromotedOngoing(true) 
            .setShortCriticalText(liveText)

        // Enable the Android 16 interactive Progress Style expansion card
        if (Build.VERSION.SDK_INT >= 36) {
            val progressPercent = if (durationMs > 0) ((positionMs.toFloat() / durationMs) * 100).toInt().coerceIn(0, 100) else 0
            
            val segment = NotificationCompat.ProgressStyle.Segment(100)
            segment.setColor(0xFFE91E63.toInt()) // Red progress bar color (can be dynamically extracted later)

            val progressStyle = NotificationCompat.ProgressStyle()
                .setProgressSegments(arrayListOf(segment))
                .setStyledByProgress(true)
                .setProgress(progressPercent)
            
            builder.setStyle(progressStyle)
        } else {
            builder.setProgress(durationMs.toInt(), positionMs.toInt(), false)
        }

        builder.addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)

        // Inject the album art into the capsule pill
        if (artworkData != null) {
            val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            builder.setLargeIcon(bitmap)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
            } else {
                builder.setSmallIcon(R.drawable.monochrome_player)
            }
        } else {
            builder.setSmallIcon(R.drawable.monochrome_player)
        }

        notificationManager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    fun dismissLiveNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LIVE_NOTIFICATION_ID)
    }
}
