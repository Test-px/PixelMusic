package com.saurav.pixelmusic.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.core.app.NotificationCompat
import com.saurav.pixelmusic.MainActivity
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.glancewidget.PlayerActions
import java.util.Arrays

object LiveNotificationHelper {
    private const val LIVE_CHANNEL_ID = "pixelmusic_live_progress_v12"
    private const val LIVE_NOTIFICATION_ID = 1002
    private const val DEFAULT_ACCENT_COLOR = 0xFFE91E63.toInt()
    private const val DEFAULT_WAVEFORM_ACCENT = 0xFFFFFFFF.toInt()

    private var lastArtworkBytes: ByteArray? = null
    private var cachedArtworkBitmap: Bitmap? = null
    private var cachedBadgedArtworkBitmap: Bitmap? = null
    private var cachedAccentColor: Int = DEFAULT_WAVEFORM_ACCENT
    @Volatile
    private var isChannelInitialized = false

    // Animation state for the dynamic island
    private val animatedNotes = arrayOf("♫", "♩", "♪", "𝅗𝅥", "𝅘𝅥𝅯", "𝅘𝅥𝅰", "𝅘𝅥𝅱", "𝅘𝅥𝅲", "𝄞")
    private val waveformFrames = arrayOf(
        "▂▅▇▃▅",
        "▄▇▃▆▂",
        "▆▂▅▇▃",
        "▃▆▂▅▇",
        "▅▃▇▂▆",
        "▇▅▂▆▃"
    )
    private var noteIndex = 0
    private var waveformIndex = 0

    fun createNotificationChannel(context: Context) {
        if (isChannelInitialized) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (i in 1..10) {
                notificationManager.deleteNotificationChannel("pixelmusic_live_progress_v$i")
            }
            notificationManager.deleteNotificationChannel("pixelmusic_live_progress")

            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Dynamic Island Tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the OriginOS Dynamic Island active"
                setShowBadge(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(channel)
        }
        isChannelInitialized = true
    }

    private fun formatTimeMs(ms: Long, zeroPadMinutes: Boolean = false): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val pattern = if (zeroPadMinutes) "%02d:%02d" else "%d:%02d"
        return String.format(java.util.Locale.US, pattern, minutes, seconds)
    }

    fun updateLiveNotification(
        context: Context,
        title: String,
        artist: String,
        positionMs: Long,
        durationMs: Long,
        artworkData: ByteArray?,
        style: String
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
        val isAlbumWaveform = style == "ALBUM_WAVEFORM"

        val criticalText = when (style) {
            "ANIMATED_NOTES" -> {
                val currentNote = animatedNotes[noteIndex]
                noteIndex = (noteIndex + 1) % animatedNotes.size
                currentNote
            }
            "PROGRESS_TIME" -> "${formatTimeMs(positionMs)}/${formatTimeMs(safeDuration)}"
            "ALBUM_WAVEFORM" -> {
                val frame = waveformFrames[waveformIndex]
                waveformIndex = (waveformIndex + 1) % waveformFrames.size
                frame
            }
            "STATIC_ICON" -> "🎧"
            else -> "🎧"
        }

        val bitmap = getOrDecodeArtwork(context, artworkData)
        val segmentColor = if (isAlbumWaveform) cachedAccentColor else DEFAULT_ACCENT_COLOR
        val subText = if (isAlbumWaveform) {
            "${formatTimeMs(positionMs, zeroPadMinutes = true)} / ${formatTimeMs(safeDuration, zeroPadMinutes = true)}"
        } else {
            "OriginOS Dynamic Island Tracker"
        }

        // We MUST feed the builder the full UI so the Island can mirror it
        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(subText)
            .setContentIntent(pendingAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(criticalText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSortKey("zzzzz_ghost") // Forces the card to the bottom of the shade

        if (isAlbumWaveform) {
            builder.setColor(segmentColor)
        }

        val progressPercent = if (safeDuration > 0L) {
            ((positionMs.toFloat() / safeDuration) * 100).toInt().coerceIn(0, 100)
        } else 0

        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val segment = NotificationCompat.ProgressStyle.Segment(100)
                segment.setColor(segmentColor)

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

        // RESTORED: Give the Island its art and buttons back
        val iconBitmap = if (isAlbumWaveform) cachedBadgedArtworkBitmap ?: bitmap else bitmap
        if (iconBitmap != null) {
            builder.setLargeIcon(iconBitmap)
        }

        builder.addAction(android.R.drawable.ic_media_previous, "⏮", prevIntent)
            .addAction(android.R.drawable.ic_media_pause, "❚❚", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "⏭", nextIntent)

        notificationManager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    private fun getOrDecodeArtwork(context: Context, artworkData: ByteArray?): Bitmap? {
        if (artworkData == null || artworkData.isEmpty()) {
            lastArtworkBytes = null
            cachedArtworkBitmap = null
            cachedBadgedArtworkBitmap = null
            cachedAccentColor = DEFAULT_WAVEFORM_ACCENT
            return null
        }

        if (lastArtworkBytes != null && Arrays.equals(lastArtworkBytes, artworkData)) {
            return cachedArtworkBitmap
        }

        return try {
            val decoded = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            lastArtworkBytes = artworkData
            cachedArtworkBitmap = decoded
            cachedAccentColor = if (decoded != null) extractAccentColor(decoded) else DEFAULT_WAVEFORM_ACCENT
            cachedBadgedArtworkBitmap = if (decoded != null) createBadgedArtwork(context, decoded) else null
            decoded
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders a rounded-square album art bitmap with the PixelMusic app icon badged in the
     * bottom-right corner, cached per track for the Album Art & Waveform island style.
     */
    private fun createBadgedArtwork(context: Context, source: Bitmap): Bitmap {
        val size = 256
        val cornerRadius = 44f
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Center-crop source to square
        val srcSize = minOf(source.width, source.height)
        val srcLeft = (source.width - srcSize) / 2
        val srcTop = (source.height - srcSize) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + srcSize, srcTop + srcSize)
        val dstRect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        val clipPath = Path().apply {
            addRoundRect(dstRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        canvas.restore()

        // Draw subtle PixelMusic app badge at bottom-right corner
        runCatching {
            val badgeDrawable = context.applicationInfo.loadIcon(context.packageManager)
            val badgeSize = 62
            val margin = 12
            val left = size - badgeSize - margin
            val top = size - badgeSize - margin
            val badgeCenterX = left + badgeSize / 2f
            val badgeCenterY = top + badgeSize / 2f
            val badgeRadius = badgeSize / 2f

            // Subtle dark ring behind badge for contrast on bright covers
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xCC000000.toInt()
            }
            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius + 4f, ringPaint)

            val badgeClip = Path().apply {
                addCircle(badgeCenterX, badgeCenterY, badgeRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(badgeClip)
            badgeDrawable.setBounds(left, top, left + badgeSize, top + badgeSize)
            badgeDrawable.draw(canvas)
            canvas.restore()
        }

        return output
    }

    /**
     * Fast HSV accent extraction from a 16x16 downsample so the island progress bar matches
     * the album artwork's vibrant highlight color on dark surfaces.
     */
    private fun extractAccentColor(source: Bitmap): Int {
        val sample = Bitmap.createScaledBitmap(source, 16, 16, true)
        val hsv = FloatArray(3)
        var bestScore = 0f
        var bestColor = DEFAULT_WAVEFORM_ACCENT

        for (y in 0 until sample.height) {
            for (x in 0 until sample.width) {
                val pixel = sample.getPixel(x, y)
                if (Color.alpha(pixel) < 200) continue
                Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                // Prefer vivid, mid-to-bright colors that read cleanly on pure black
                if (sat >= 0.30f && value >= 0.30f) {
                    val score = sat * 0.7f + value * 0.3f
                    if (score > bestScore) {
                        bestScore = score
                        hsv[1] = sat.coerceIn(0.55f, 0.90f)
                        hsv[2] = value.coerceIn(0.75f, 1.0f)
                        bestColor = Color.HSVToColor(hsv)
                    }
                }
            }
        }
        if (sample !== source) sample.recycle()
        return bestColor
    }

    fun dismissLiveNotification(context: Context) {
        lastArtworkBytes = null
        cachedArtworkBitmap = null
        cachedBadgedArtworkBitmap = null
        cachedAccentColor = DEFAULT_WAVEFORM_ACCENT
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LIVE_NOTIFICATION_ID)
    }
}
