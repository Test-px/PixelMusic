package com.unshoo.pixelmusic.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.ui.glancewidget.PlayerActions

object LiveNotificationHelper {
    private const val LIVE_CHANNEL_ID = "pixelmusic_live_progress"
    private const val LIVE_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Live Progress Tracker",
                NotificationManager.IMPORTANCE_DEFAULT // Raised so the expanded layout shows properly
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

        // 1. App Launch Intent
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        val pendingAppIntent = PendingIntent.getActivity(
            context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Disguised Media Control Intents (Routes directly to your existing widget actions!)
        val prevIntent = PendingIntent.getService(context, 1, Intent(context, MusicService::class.java).apply { action = PlayerActions.PREVIOUS }, PendingIntent.FLAG_IMMUTABLE)
        val playPauseIntent = PendingIntent.getService(context, 2, Intent(context, MusicService::class.java).apply { action = PlayerActions.PLAY_PAUSE }, PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = PendingIntent.getService(context, 3, Intent(context, MusicService::class.java).apply { action = PlayerActions.NEXT }, PendingIntent.FLAG_IMMUTABLE)

val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(pendingAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(durationMs.toInt(), positionMs.toInt(), false)
            .setRequestPromotedOngoing(true) // Call directly
            .setShortCriticalText(liveText)
            
            // Add Media Buttons
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)

        // 4. Attach Album Art so it shows up in the UI
        if (artworkData != null) {
            val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            builder.setLargeIcon(bitmap)
        }

        // 5. Android 16 / OEM Island Elevation Flag (via Reflection)
        try {
            val setRequestPromotedMethod = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.java)
            setRequestPromotedMethod.invoke(builder, true)

            // Feeds the dynamic text to the capsule
            val setShortTextMethod = builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
            setShortTextMethod.invoke(builder, liveText)
        } catch (e: Exception) {
            // Ignored on unsupported devices
        }

        notificationManager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    fun dismissLiveNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LIVE_NOTIFICATION_ID)
    }
}
