package com.unshoo.pixelmusic.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.R

object LiveNotificationHelper {
    private const val LIVE_CHANNEL_ID = "pixelmusic_live_progress"
    // Distinct from Media3's NOTIFICATION_ID (101) so they don't overwrite each other
    private const val LIVE_NOTIFICATION_ID = 1002 

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Live Progress Tracker",
                NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't vibrate/buzz
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
        liveText: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.monochrome_player) // The icon shown in the capsule
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Hides it from the lock screen (Media3 handles that)
            .setRequestPromotedOngoing(true) // Android 16+ Magic Flag to trigger dynamic island
            .setShortCriticalText(liveText) // The dynamic timestamp!

        notificationManager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    fun dismissLiveNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LIVE_NOTIFICATION_ID)
    }
}
