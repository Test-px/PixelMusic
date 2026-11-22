package com.saurav.pixelmusic.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.saurav.pixelmusic.MainActivity
import com.saurav.pixelmusic.data.model.PlayerInfo
import timber.log.Timber

/**
 * 1:1 iOS-inspired square widget with a full-bleed blurred album art background, smooth rounded
 * corners, compact header + focal track display, and Material You Expressive shaped controls.
 */
class BlurSquareWidget2x2 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            GlanceTheme {
                BlurSquareWidgetContent(playerInfo = playerInfo, context = context)
            }
        }
    }

    @Composable
    private fun BlurSquareWidgetContent(
        playerInfo: PlayerInfo,
        context: Context
    ) {
        val title = playerInfo.songTitle.ifEmpty { "PixelMusic" }
        val artist = playerInfo.artistName.ifEmpty { "Tap to open" }
        val isPlaying = playerInfo.isPlaying
        val colors = playerInfo.getWidgetColors()

        val widgetCornerRadius = 28.dp
        // Material You Expressive morphing shapes:
        // Play/Pause morphs between expressive squircle (playing) and full pill (paused).
        val playPauseCornerRadius = if (isPlaying) 14.dp else 22.dp
        val skipButtonCornerRadius = 16.dp

        val headerAccentColor: GlanceColorProvider = playerInfo.themeColors?.let { theme ->
            val lightAccent = lerp(Color(theme.lightPlayPauseBackground), Color.White, 0.45f)
            val darkAccent = lerp(Color(theme.darkPlayPauseBackground), Color.White, 0.25f)
            ColorProvider(day = lightAccent, night = darkAccent)
        } ?: ColorProvider(day = Color(0xFFE8C88B), night = Color(0xFFE8C88B))

        val focalTextColor = ColorProvider(day = Color(0xF2FFFFFF), night = Color(0xF2FFFFFF))
        val noteColor = ColorProvider(day = Color.White, night = Color.White)
        val headerText = if (playerInfo.songTitle.isNotBlank() && playerInfo.artistName.isNotBlank()) {
            "${playerInfo.songTitle.uppercase()} · ${playerInfo.artistName}"
        } else {
            title
        }

        val blurredBgProvider = resolveBlurredBackgroundProvider(
            context = context,
            bitmapData = playerInfo.albumArtBitmapData,
            albumArtUri = playerInfo.albumArtUri,
            cornerRadiusDp = widgetCornerRadius.value
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(widgetCornerRadius)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (blurredBgProvider != null) {
                Image(
                    provider = blurredBgProvider,
                    contentDescription = null,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(widgetCornerRadius),
                    contentScale = ContentScale.Crop
                )
            } else {
                WidgetRoundedBackgroundLayer(
                    color = colors.surface,
                    cornerRadius = widgetCornerRadius
                )
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // Top compact header (e.g. "MASAKALI · Mohit Chauhan")
                Text(
                    text = headerText,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = headerAccentColor
                    ),
                    maxLines = 1
                )

                // Middle focal section: musical note + track title
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(
                        text = "♪",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = noteColor
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = focalTextColor
                        ),
                        maxLines = 2
                    )
                    if (playerInfo.songTitle.isBlank()) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = artist,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(
                                    day = Color(0xB8FFFFFF),
                                    night = Color(0xB8FFFFFF)
                                )
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Bottom Material You Expressive shaped controls
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    PreviousButton(
                        modifier = GlanceModifier.size(width = 40.dp, height = 38.dp),
                        backgroundColor = colors.prevNextBackground,
                        iconColor = colors.prevNextIcon,
                        cornerRadius = skipButtonCornerRadius,
                        iconSize = 19.dp
                    )

                    Spacer(GlanceModifier.width(8.dp))

                    PlayPauseButton(
                        modifier = GlanceModifier.size(width = 50.dp, height = 40.dp),
                        isPlaying = isPlaying,
                        backgroundColor = colors.playPauseBackground,
                        iconColor = colors.playPauseIcon,
                        cornerRadius = playPauseCornerRadius,
                        iconSize = 22.dp
                    )

                    Spacer(GlanceModifier.width(8.dp))

                    NextButton(
                        modifier = GlanceModifier.size(width = 40.dp, height = 38.dp),
                        backgroundColor = colors.prevNextBackground,
                        iconColor = colors.prevNextIcon,
                        cornerRadius = skipButtonCornerRadius,
                        iconSize = 19.dp
                    )
                }
            }
        }
    }

    private fun resolveBlurredBackgroundProvider(
        context: Context,
        bitmapData: ByteArray?,
        albumArtUri: String?,
        cornerRadiusDp: Float
    ): ImageProvider? {
        val density = context.resources.displayMetrics.density
        val targetPx = (180f * density).toInt().coerceIn(220, 360)
        val cornerRadiusPx = cornerRadiusDp * density

        val bitmap: Bitmap? = bitmapData?.let { data ->
            val cacheKey = "${AlbumArtBitmapCache.getKey(data)}_iosblur_${targetPx}_r${cornerRadiusDp.toInt()}"
            AlbumArtBitmapCache.getBitmap(cacheKey) ?: runCatching {
                val decoded = BitmapFactory.decodeByteArray(data, 0, data.size)
                decoded?.let { src ->
                    createBlurredRoundedAlbumArtBitmap(
                        source = src,
                        targetWidthPx = targetPx,
                        targetHeightPx = targetPx,
                        cornerRadiusPx = cornerRadiusPx
                    ).also {
                        AlbumArtBitmapCache.putBitmap(cacheKey, it)
                        if (it !== src) src.recycle()
                    }
                }
            }.onFailure { e ->
                Timber.tag("BlurSquareWidget").w(e, "Failed to blur widget bitmap bytes")
            }.getOrNull()
        } ?: albumArtUri?.takeIf { it.isNotBlank() }?.let { rawUri ->
            val cacheKey = "uri:${rawUri}_iosblur_${targetPx}_r${cornerRadiusDp.toInt()}"
            AlbumArtBitmapCache.getBitmap(cacheKey) ?: runCatching {
                val decoded = decodeWidgetAlbumArtBitmap(
                    context = context,
                    rawUri = rawUri,
                    targetWidthPx = 160,
                    targetHeightPx = 160
                )
                decoded?.let { src ->
                    createBlurredRoundedAlbumArtBitmap(
                        source = src,
                        targetWidthPx = targetPx,
                        targetHeightPx = targetPx,
                        cornerRadiusPx = cornerRadiusPx
                    ).also {
                        AlbumArtBitmapCache.putBitmap(cacheKey, it)
                        if (it !== src) src.recycle()
                    }
                }
            }.onFailure { e ->
                Timber.tag("BlurSquareWidget").w(e, "Failed to blur widget art from URI")
            }.getOrNull()
        }

        return bitmap?.let { ImageProvider(it) }
    }
}
