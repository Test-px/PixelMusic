package com.unshoo.pixelmusic.presentation.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.presentation.components.MusicRecognitionDialog
import com.unshoo.pixelmusic.ui.theme.PixelMusicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem

class RecognitionOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Extend into camera cutout to eliminate the top black bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 2. Full-screen immersive mode (hides status & nav bars for clean look)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 3. Overlay permission check
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
                    isTransparentOverlay = true,
                    onPlayMusic = { result ->
                        lifecycleScope.launch {
                            // Resolve YouTube ID from Shazam or fallback to quick search
                            var videoId = result.youtubeVideoId
                            if (videoId.isNullOrBlank()) {
                                val query = "${result.title} ${result.artist}"
                                videoId = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val search = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                        search?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.id
                                    }.getOrNull()
                                }
                            }

                            if (!videoId.isNullOrBlank()) {
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
                    }
                )
            }
        }
    }
}
