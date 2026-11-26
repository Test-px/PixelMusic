package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Slices a single continuous album artwork through 3 staggered vertical rounded capsules,
 * morphing smoothly from the MiniPlayer 44dp circle into the 3-pillar composition.
 */
private class MorphingTriplePillarShape(
    private val morphProgressProvider: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = morphProgressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        if (t <= 0.14f) {
            val r = minOf(w, h) * 0.5f
            return Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(r, r)))
        }

        val splitT = ((t - 0.14f) / 0.86f).coerceIn(0f, 1f)
        val path = Path()

        // Interpolate each capsule from the full bounding circle toward its staggered pillar rect
        fun addInterpolatedPill(
            targetLeft: Float,
            targetTop: Float,
            targetRight: Float,
            targetBottom: Float
        ) {
            val l = lerp(0f, w * targetLeft, splitT)
            val top = lerp(0f, h * targetTop, splitT)
            val r = lerp(w, w * targetRight, splitT)
            val b = lerp(h, h * targetBottom, splitT)
            val radius = ((r - l) * 0.5f).coerceAtLeast(1f)
            path.addRoundRect(RoundRect(l, top, r, b, CornerRadius(radius, radius)))
        }

        // Left pillar (shorter, lower)
        addInterpolatedPill(0.11f, 0.42f, 0.36f, 0.94f)
        // Center pillar (tallest, spans most of height)
        addInterpolatedPill(0.39f, 0.16f, 0.64f, 0.99f)
        // Right pillar (tall, elevated)
        addInterpolatedPill(0.67f, 0.04f, 0.92f, 0.86f)

        return Outline.Generic(path)
    }
}

/**
 * Isolated Now Playing Style: Pillars
 * Displays continuous album artwork through three staggered vertical capsule windows
 * accented by Material You decorative circles and a subtle live audio waveform.
 */
@Composable
fun PillarsPlayerLayout(
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
    val tertiaryAccent = baseTheme.tertiary
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val artTopOffsetDp = statusBarTopPadding + 56.dp
    val artTopOffsetPx = with(density) { artTopOffsetDp.toPx() }
    val artHeightDp = (screenHeightDp * 0.44f).coerceIn(260.dp, 380.dp)
    val artHeightPx = with(density) { artHeightDp.toPx() }
    val miniArtSizePx = remember(density) { with(density) { 44.dp.toPx() } }
    val miniArtOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }
    val slideOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }

    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val artBounds = params.aodSourceBounds?.artBounds ?: remember { MorphElementBounds() }
    val morphTracker = remember { ArtworkMorphTracker() }

    params.aodSourceBounds?.let { bounds ->
        bounds.artStyle = AodSourceArtStyle.STANDARD_ROUNDED
        bounds.washColor = solidBackgroundColor
        bounds.topShadowHeightDp = 0f
        bounds.topShadowStartAlpha = 0f
        bounds.topShadowMidAlpha = 0f
    }

    val pillarShape = remember(morphTracker, params.expansionFractionProvider) {
        MorphingTriplePillarShape(
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
        label = "pillarsArtScale"
    )

    // Gentle animated waveform phase for the decorative upper-left equalizer bars
    val waveTransition = rememberInfiniteTransition(label = "pillarsWave")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pillarsWavePhase"
    )
    val liveWaveFactor by animateFloatAsState(
        targetValue = if (isPlaying && params.allowRealtimeUpdates) 1f else 0f,
        animationSpec = tween(320),
        label = "pillarsWaveLive"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // Decorative Material You organic backdrop circles + live waveform bars behind the pillars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = artTopOffsetDp)
                .height(artHeightDp)
                .graphicsLayer {
                    val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                }
        ) {
            val w = size.width
            val h = size.height

            // Soft large organic blob in upper-right behind right pillar
            drawCircle(
                color = accentColor.copy(alpha = 0.10f),
                radius = w * 0.44f,
                center = Offset(w * 0.84f, h * 0.24f)
            )
            // Coral/tertiary accent circle in upper-left
            drawCircle(
                color = tertiaryAccent.copy(alpha = 0.42f),
                radius = w * 0.11f,
                center = Offset(w * 0.23f, h * 0.11f)
            )
            // Peeking accent circle along mid-left edge behind left pillar
            drawCircle(
                color = tertiaryAccent.copy(alpha = 0.34f),
                radius = w * 0.19f,
                center = Offset(w * 0.04f, h * 0.68f)
            )

            // Decorative subtle audio waveform bars in upper-left space above the left pillar
            val baseHeights = floatArrayOf(0.24f, 0.48f, 0.34f, 0.82f, 0.96f, 0.56f, 0.30f, 0.44f, 0.28f)
            val barCount = baseHeights.size
            val regionStart = w * 0.06f
            val regionWidth = w * 0.29f
            val step = regionWidth / (barCount - 1).coerceAtLeast(1)
            val centerY = h * 0.30f
            val maxHalfH = h * 0.085f
            val strokeW = 3.2.dp.toPx()

            for (i in 0 until barCount) {
                val animMod = 0.65f + 0.35f * sin(wavePhase + i * 0.78f)
                val heightFraction = lerp(baseHeights[i] * 0.55f, baseHeights[i] * animMod, liveWaveFactor)
                val halfH = (maxHalfH * heightFraction).coerceAtLeast(strokeW)
                val x = regionStart + i * step
                drawLine(
                    color = contentColor.copy(alpha = 0.22f),
                    start = Offset(x, centerY - halfH),
                    end = Offset(x, centerY + halfH),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }

        // TOP LAYER: Morphing 3-Pillar Capsule Artwork
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val sample = morphTracker.evaluate(params.expansionFractionProvider())
                    val fullW = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: screenWidthPx
                    val fullH = artHeightPx
                    val baseW = lerp(miniArtSizePx, fullW, sample.sizeT)
                    val baseH = lerp(miniArtSizePx, fullH, sample.sizeT)
                    val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val baseX = lerp(miniArtOffsetPx, 0f, sample.posT)
                    val baseY = lerp(miniArtOffsetPx, artTopOffsetPx, sample.posT)
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
                .clip(pillarShape)
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
        }

        // LOWER SECTION: Song Info + Favorite, Progress, Expressive Controls, Utility Pill Row
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = artTopOffsetDp + artHeightDp + 16.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = navBarBottomPadding + 18.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StyleMorphingSongInfo(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    contentColor = contentColor,
                    artistAlpha = 0.76f,
                    titleFontSize = 26.sp,
                    artistFontSize = 15.sp,
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
                    containerColor = contentColor.copy(alpha = 0.10f),
                    activeContainerColor = tertiaryAccent.copy(alpha = 0.22f),
                    iconTint = contentColor.copy(alpha = 0.82f),
                    activeIconTint = tertiaryAccent,
                    size = 46.dp,
                    iconSize = 24.dp,
                    onClick = params.onFavoriteToggle,
                    modifier = Modifier.graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                )
            }

            StyleProgressSection(
                params = params,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                activeTrackColor = accentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.18f),
                thumbColor = accentColor,
                timeTextColor = contentColor.copy(alpha = 0.74f),
                metaPillBgColor = contentColor.copy(alpha = 0.10f),
                metaPillTextColor = contentColor.copy(alpha = 0.90f),
                waveAmplitudeWhenPlaying = 3.dp,
                trackHeight = 5.dp
            )

            // Capsule-themed Horizontal Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
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
                    cornerRadius = 32.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 28.dp,
                    onClick = params.onPrevious,
                    modifier = Modifier.size(width = 66.dp, height = 62.dp)
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
                    cornerRadius = if (isPlaying) 24.dp else 36.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 34.dp,
                    isPlaying = isPlaying,
                    onClick = params.onPlayPause,
                    modifier = Modifier.size(width = 104.dp, height = 72.dp)
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
                    cornerRadius = 32.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 28.dp,
                    onClick = params.onNext,
                    modifier = Modifier.size(width = 66.dp, height = 62.dp)
                )
            }

            // Bottom Pill Utility Dock
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        alpha = a
                        translationY = (1f - a) * slideOffsetPx
                    },
                shape = RoundedCornerShape(999.dp),
                color = contentColor.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillarsDockItem(
                        iconRes = R.drawable.rounded_shuffle_24,
                        contentDescription = "Shuffle",
                        isActive = isShuffle,
                        activeBg = accentColor,
                        activeTint = onAccentColor,
                        inactiveTint = contentColor.copy(alpha = 0.78f),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            params.onShuffleToggle()
                        }
                    )
                    PillarsDockItem(
                        iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
                            R.drawable.rounded_repeat_one_24
                        } else {
                            R.drawable.rounded_repeat_24
                        },
                        contentDescription = "Repeat",
                        isActive = repeatMode != Player.REPEAT_MODE_OFF,
                        activeBg = accentColor,
                        activeTint = onAccentColor,
                        inactiveTint = contentColor.copy(alpha = 0.78f),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            params.onRepeatToggle()
                        }
                    )
                    PillarsDockItem(
                        iconRes = R.drawable.rounded_lyrics_24,
                        contentDescription = "Lyrics",
                        isActive = false,
                        activeBg = accentColor,
                        activeTint = onAccentColor,
                        inactiveTint = contentColor.copy(alpha = 0.78f),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            params.onLyricsClick()
                        }
                    )
                    PillarsDockItem(
                        iconRes = R.drawable.rounded_more_horiz_24,
                        contentDescription = "More options",
                        isActive = false,
                        activeBg = accentColor,
                        activeTint = onAccentColor,
                        inactiveTint = contentColor.copy(alpha = 0.78f),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            params.onMoreOptionsClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PillarsDockItem(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    activeBg: Color,
    activeTint: Color,
    inactiveTint: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) activeBg else Color.Transparent,
        animationSpec = tween(200),
        label = "pillarsDockBg"
    )
    val tint by animateColorAsState(
        targetValue = if (isActive) activeTint else inactiveTint,
        animationSpec = tween(200),
        label = "pillarsDockTint"
    )
    Surface(
        modifier = Modifier
            .size(width = 58.dp, height = 38.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(999.dp),
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
