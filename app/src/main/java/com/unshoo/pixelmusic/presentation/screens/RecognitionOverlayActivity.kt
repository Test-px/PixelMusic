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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
                            
                            // Advanced YouTube Resolution Pipeline
                            var videoId = result.youtubeVideoId
                            
                            if (videoId.isNullOrBlank()) {
                                videoId = withContext(Dispatchers.IO) {
                                    
                                    // Helper: Clean up titles to drastically improve YouTube Search SEO
                                    fun cleanText(text: String): String {
                                        return text.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "") // Removes (Official Video)
                                            .replace(Regex("(?i)(feat\\.|ft\\.).*"), "")     // Removes feat. Artist
                                            .trim()
                                    }

                                    val queriesToTry = mutableListOf("${cleanText(result.title)} ${cleanText(result.artist)}")

                                    // SUPERPOWER 1: MusicBrainz ISRC API Fallback (100% Free)
                                    // Finds the true regional or alternate title using the ISRC code
                                    if (!result.isrc.isNullOrBlank()) {
                                        runCatching {
                                            val url = URL("https://musicbrainz.org/ws/2/recording?query=isrc:${result.isrc}&fmt=json")
                                            val conn = url.openConnection() as HttpURLConnection
                                            conn.setRequestProperty("User-Agent", "PixelMusic/1.0 (Android)")
                                            conn.connectTimeout = 3000
                                            conn.readTimeout = 3000
                                            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                                            val json = JSONObject(jsonStr)
                                            
                                            val recordings = json.optJSONArray("recordings")
                                            if (recordings != null && recordings.length() > 0) {
                                                val first = recordings.getJSONObject(0)
                                                val mbTitle = first.optString("title")
                                                val mbArtist = first.optJSONArray("artist-credit")?.optJSONObject(0)?.optString("name") ?: ""
                                                
                                                val mbQuery = "${cleanText(mbTitle)} ${cleanText(mbArtist)}".trim()
                                                if (mbQuery.isNotBlank() && !queriesToTry.contains(mbQuery)) {
                                                    queriesToTry.add(mbQuery) // Add this as our highly accurate backup query!
                                                }
                                            }
                                        }
                                    }

                                    // SUPERPOWER 2: Two-Tier Search Execution
                                    var foundId: String? = null
                                    
                                    for (query in queriesToTry) {
                                        if (foundId != null) break

                                        // Tier 1: Strict ATV (Official Audio Tracks)
                                        val atvSearch = runCatching { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull() }.getOrNull()
                                        foundId = atvSearch?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.id

                                        // Tier 2: Unrestricted Video Search (Catches Folk, Live, Covers, & Lyrics Videos)
                                        if (foundId == null) {
                                            val videoSearch = runCatching { YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull() }.getOrNull()
                                            
                                            // We use a brilliant reflection trick here so we don't have to guess your VideoItem class import!
                                            foundId = videoSearch?.items?.firstOrNull()?.let { item ->
                                                runCatching { item.javaClass.getMethod("getId").invoke(item) as? String }.getOrNull()
                                            }
                                        }
                                    }
                                    foundId
                                }
                            }

                            if (!videoId.isNullOrBlank()) {
                                // Forces MainActivity to intercept the ID and start playing immediately
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
