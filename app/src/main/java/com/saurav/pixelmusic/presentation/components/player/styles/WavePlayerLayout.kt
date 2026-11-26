package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.media3.common.Player
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.player.AodSourceArtStyle
import com.saurav.pixelmusic.presentation.components.player.ArtworkMorphTracker
import com.saurav.pixelmusic.presentation.components.player.LayoutCoordsHolder
import com.saurav.pixelmusic.presentation.components.player.MorphElementBounds
import com.saurav.pixelmusic.presentation.components.player.captureRootCoords
import kotlin.math.roundToInt

/**
 * Organic S-curve wave bottom cutout that morphs cleanly from the MiniPlayer 44dp circle
 * into a full-bleed wavy artwork silhouette.
 */
private class MorphingOrganicWaveShape(
    private val morphProgressProvider: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = morphProgressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        if (t <= 0.08f) {
            val r = minOf(w, h) * 0.5f
            return Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(r, r)))
        }

        val waveT = ((t - 0.08f) / 0.92f).coerceIn(0f, 1f)
        val topCornerR = lerp(minOf(w, h) * 0.5f, 0f, waveT)
        val waveAmp = h * 0.145f * waveT

        // Smooth S-curve: left rises slightly above bottom, middle peaks gently, right dips down
        val leftY = h - waveAmp * 0.72f
        val rightY = h - waveAmp * 0.85f

        val path = Path().apply {
            if (topCornerR > 0.5f) {
                moveTo(0f, topCornerR)
                arcTo(Rect(0f, 0f, topCornerR * 2f, topCornerR * 2f), 180f, 90f, false)
                lineTo(w - topCornerR, 0f)
                arcTo(Rect(w - topCornerR * 2f, 0f, w, topCornerR * 2f), 270f, 90f, false)
            } else {
                moveTo(0f, 0f)
                lineTo(w, 0f)
            }
            lineTo(w, rightY)
            // Right dip to center peak to left edge
            cubicTo(
                w * 0.76f, h + waveAmp * 0.42f,
                w * 0.50f, h - waveAmp * 0.12f,
                w * 0.34f, h - waveAmp * 1.02f
            )
            cubicTo(
                w * 0.20f, h - waveAmp * 1.48f,
                w * 0.08f, h - waveAmp * 1.12f,
                0f, leftY
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Isolated Now Playing Style: Wave
 * Full-bleed top album artwork with an organic S-wave bottom divider, bold editorial title/artist,
 * circular expressive transport controls, and a soft decorative wave ribbon at the bottom edge.
 */
@Composable
fun WavePlayerLayout(
    params: PlayerStyleParams,
    modifier: Modifier = Modifier
) {
    val baseTheme = LocalMaterialTheme.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptics = LocalHapticFeedback.current

    val (optimizedArtUri, resolvedTitle, resolvedArtist) = rememberResolvedArtAndMeta(
        song = params.song,
        currentSongArtists = params.currentSongArtists,
        albumArtQuality = params.albumArtQuality
    )

    val solidBackgroundColor = baseTheme.primaryContainer
    val contentColor = baseTheme.onPrimaryContainer
    val accentColor = baseTheme.primary
    val onAccentColor = baseTheme.onPrimary
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val artHeightPx = with(density) { (screenHeightDp * 0.54f).toPx() }
    val miniArtSizePx = remember(density) { with(density) { 44.dp.toPx() } }
    val miniArtOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }
    val slideOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }

    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val artBounds = params.aodSourceBounds?.artBounds ?: remember { MorphElementBounds() }
    val morphTracker = remember { ArtworkMorphTracker() }

    params.aodSourceBounds?.let { bounds ->
        bounds.artStyle = AodSourceArtStyle.IMMERSIVE_RECT
        bounds.washColor = solidBackgroundColor
        bounds.topShadowHeightDp = 104f
        bounds.topShadowStartAlpha = 0.36f
        bounds.topShadowMidAlpha = 0.10f
    }

    val waveShape = remember(morphTracker, params.expansionFractionProvider) {
        MorphingOrganicWaveShape(
            morphProgressProvider = {
                morphTracker.evaluate(params.expansionFractionProvider()).sizeT
            }
        )
    }

    val isPlaying = params.isPlayingProvider()
    val isFavorite = params.isFavoriteProvider()
    val isShuffle = params.isShuffleEnabledProvider()
    val repeatMode = params.repeatModeProvider()

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "waveArtScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // Decorative subtle bottom wave ribbon
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp + navBarBottomPadding)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                }
        ) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.58f)
                cubicTo(w * 0.24f, h * 0.25f, w * 0.52f, h * 0.85f, w * 0.78f, h * 0.36f)
                cubicTo(w * 0.88f, h * 0.16f, w * 0.95f, h * 0.14f, w, h * 0.18f)
                lineTo(w, h)
                close()
            }
            drawPath(path = path, color = accentColor.copy(alpha = 0.14f))
        }

        // TOP LAYER: Morphing Organic Wave Artwork
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val sample = morphTracker.evaluate(params.expansionFractionProvider())
                    val fullW = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: screenWidthPx
                    val fullH = (constraints.maxHeight.toFloat() * 0.54f).takeIf { it > 0f } ?: artHeightPx
                    val baseW = lerp(miniArtSizePx, fullW, sample.sizeT)
                    val baseH = lerp(miniArtSizePx, fullH, sample.sizeT)
                    val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val baseX = lerp(miniArtOffsetPx, 0f, sample.posT)
                    val baseY = lerp(miniArtOffsetPx, 0f, sample.posT)
                    val placeX = (baseX - (targetW - baseW) * 0.5f).roundToInt()
                    val placeY = (baseY - (targetH - baseH) * 0.5f).roundToInt()

                    val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                    layout(targetW, targetH) {
                        artBounds.offset = Offset(placeX.toFloat(), placeY.toFloat())
                        artBounds.size = IntSize(targetW, targetH)
                        artBounds.hasBounds = true
                        placeable.place(placeX, placeY)
                    }
                }
                .graphicsLayer {
                    alpha = if (params.aodTransitionProgressProvider() > 0.001f) 0f else 1f
                }
                .clip(waveShape)
                .styleArtworkGestures(
                    songId = params.song.id,
                    aodScreenEnabled = params.aodScreenEnabled,
                    onEnterAodScreen = params.onEnterAodScreen,
                    onNext = params.onNext,
                    onPrevious = params.onPrevious
                )
        ) {
            SmartImage(
                model = optimizedArtUri,
                contentDescription = resolvedTitle,
                contentScale = ContentScale.Crop,
                targetSize = coil.size.Size.ORIGINAL,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
            )

            // Top scrim for TopAppBar legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.36f),
                                Color.Black.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // LOWER SECTION: Title/Artist + Favorite, Seekbar, Circular Transport, Secondary Row
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = (screenHeightDp * 0.54f).coerceAtLeast(280.dp),
                    start = 26.dp,
                    end = 26.dp,
                    bottom = navBarBottomPadding + 18.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Song Title + Artist + Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StyleMorphingSongInfo(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    contentColor = contentColor,
                    artistAlpha = 0.78f,
                    titleFontSize = 28.sp,
                    artistFontSize = 16.sp,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    onTitleClick = params.onTitleClick,
                    onArtistClick = params.onArtistClick,
                    modifier = Modifier.weight(1f)
                )

                StyleFavoriteButton(
                    isFavorite = isFavorite,
                    containerColor = Color.Transparent,
                    activeContainerColor = accentColor.copy(alpha = 0.14f),
                    iconTint = contentColor.copy(alpha = 0.72f),
                    activeIconTint = accentColor,
                    size = 44.dp,
                    iconSize = 26.dp,
                    onClick = params.onFavoriteToggle,
                    modifier = Modifier.graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                )
            }

            // Progress Slider + Timestamps + Optional Audio Meta
            StyleProgressSection(
                params = params,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                activeTrackColor = accentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.18f),
                thumbColor = accentColor,
                timeTextColor = contentColor.copy(alpha = 0.74f),
                metaPillBgColor = contentColor.copy(alpha = 0.10f),
                metaPillTextColor = contentColor.copy(alpha = 0.88f),
                waveAmplitudeWhenPlaying = 2.2.dp,
                trackHeight = 4.5.dp
            )

            // Centered Expressive Circular Transport Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyleMorphingTransportButton(
                    slot = MiniTransportSlot.PREVIOUS,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    containerColor = onAccentColor.copy(alpha = 0.92f),
                    contentColor = accentColor,
                    cornerRadius = 30.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 28.dp,
                    onClick = params.onPrevious,
                    modifier = Modifier.size(60.dp)
                )

                StyleMorphingTransportButton(
                    slot = MiniTransportSlot.PLAY_PAUSE,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    containerColor = accentColor,
                    contentColor = onAccentColor,
                    cornerRadius = if (isPlaying) 28.dp else 40.dp,
                    pressedCornerRadius = 20.dp,
                    iconSize = 36.dp,
                    isPlaying = isPlaying,
                    onClick = params.onPlayPause,
                    modifier = Modifier.size(78.dp)
                )

                StyleMorphingTransportButton(
                    slot = MiniTransportSlot.NEXT,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    containerColor = onAccentColor.copy(alpha = 0.92f),
                    contentColor = accentColor,
                    cornerRadius = 30.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 28.dp,
                    onClick = params.onNext,
                    modifier = Modifier.size(60.dp)
                )
            }

            // Secondary Clean Action Strip (Shuffle, Lyrics, Repeat, More)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        alpha = a
                        translationY = (1f - a) * slideOffsetPx
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaveUtilityButton(
                    iconRes = R.drawable.rounded_shuffle_24,
                    contentDescription = "Shuffle",
                    isActive = isShuffle,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        params.onShuffleToggle()
                    }
                )
                WaveUtilityButton(
                    iconRes = R.drawable.rounded_lyrics_24,
                    contentDescription = "Lyrics",
                    isActive = false,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        params.onLyricsClick()
                    }
                )
                WaveUtilityButton(
                    iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        R.drawable.rounded_repeat_one_24
                    } else {
                        R.drawable.rounded_repeat_24
                    },
                    contentDescription = "Repeat",
                    isActive = repeatMode != Player.REPEAT_MODE_OFF,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        params.onRepeatToggle()
                    }
                )
                WaveUtilityButton(
                    iconRes = R.drawable.rounded_more_horiz_24,
                    contentDescription = "More options",
                    isActive = false,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        params.onMoreOptionsClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun WaveUtilityButton(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(200),
        label = "waveUtilBg"
    )
    val tint by animateColorAsState(
        targetValue = if (isActive) accentColor else contentColor.copy(alpha = 0.72f),
        animationSpec = tween(200),
        label = "waveUtilTint"
    )
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
