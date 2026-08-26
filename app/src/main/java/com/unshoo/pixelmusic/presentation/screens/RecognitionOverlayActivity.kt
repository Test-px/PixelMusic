package com.unshoo.pixelmusic.presentation.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.presentation.components.MusicRecognitionDialog
import com.unshoo.pixelmusic.ui.theme.PixelMusicTheme

class RecognitionOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            finish()
            return
        }

        setContent {
            PixelMusicTheme {
                MusicRecognitionDialog(
                    onDismiss = { finish() },
                    isTransparentOverlay = true, // <-- Activates the Google-style transparent look!
                    onPlayMusic = { result ->
                        val videoId = result.youtubeVideoId
                        if (videoId != null) {
                            val playIntent = Intent(this@RecognitionOverlayActivity, MainActivity::class.java).apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://youtube.com/watch?v=$videoId")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(playIntent)
                        }
                        finish()
                    }
                )
            }
        }
    }
}
