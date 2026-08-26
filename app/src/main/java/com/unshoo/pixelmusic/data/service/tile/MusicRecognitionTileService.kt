package com.unshoo.pixelmusic.data.service.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.unshoo.pixelmusic.presentation.screens.RecognitionOverlayActivity

class MusicRecognitionTileService : TileService() {
    override fun onClick() {
        super.onClick()
        
        val intent = Intent(this, RecognitionOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        
        // Collapse the notification shade and start the overlay!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

