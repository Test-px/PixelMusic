package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.player.AodSourceArtStyle
import com.saurav.pixelmusic.presentation.components.player.ArtworkMorphTracker
import com.saurav.pixelmusic.presentation.components.player.LayoutCoordsHolder
import com.saurav.pixelmusic.presentation.components.player.MorphElementBounds
import com.saurav.pixelmusic.presentation.components.player.captureRootCoords
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 100% continuous organic S-curve wave bottom cutout that morphs seamlessly from the MiniPlayer
 * 44dp circle (at t = 0) into a full-bleed wavy artwork silhouette (at t = 1) with ZERO threshold
 * snap or sharp bottom corners during drag.
 */
private class MorphingOrganicWaveShape(
    private val morphProgressProvider: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = morphProgressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        // Continuous corner radii from full circle (min/2) at t=0 down to 0 at t=1
        val halfMin = minOf(w, h) * 0.5f
        val topCornerR = (halfMin * (1f - t).pow(1.15f)).coerceIn(0f, halfMin)
        val bottomCornerR = (halfMin * (1f - t).pow(1.35f)).coerceIn(0f, halfMin)

        // S-wave amplitude grows continuously from 0 at t=0 to full organic wave at t=1
        val waveT = t.pow(1.35f)
        val waveAmp = h * 0.16f * waveT

        // Endpoints & trough stay strictly inside [0, h]
        val leftY = h - waveAmp * 0.52f
        val rightY = h - waveAmp * 0.44f
        val troughY = h - 2f

        val clampedBottomR = bottomCornerR.coerceAtMost(minOf(w * 0.49f, leftY - topCornerR, rightY - topCornerR).coerceAtLeast(0f))

        val path = Path().apply {
            // Top-left corner
            if (topCornerR > 0.5f) {
                moveTo(0f, topCornerR)
                arcTo(Rect(0f, 0f, topCornerR * 2f, topCornerR * 2f), 180f, 90f, false)
            } else {
                moveTo(0f, 0f)
            }

            // Top-right corner
            if (topCornerR > 0.5f) {
                lineTo(w - topCornerR, 0f)
                arcTo(Rect(w - topCornerR * 2f, 0f, w, topCornerR * 2f), 270f, 90f, false)
            } else {
                lineTo(w, 0f)
            }

            // Right side down to bottom-right corner / wave start
            if (clampedBottomR > 0.5f) {
                lineTo(w, rightY - clampedBottomR)
                arcTo(
                    Rect(w - clampedBottomR * 2f, rightY - clampedBottomR * 2f, w, rightY),
                    0f,
                    90f,
                    false
                )
            } else {
                lineTo(w, rightY)
            }

            // Bottom edge: smoothly blends from flat/circular bottom (waveT=0) to lush S-wave (waveT=1)
            val rightSpanX = w - clampedBottomR
            val leftSpanX = clampedBottomR
            val spanW = (rightSpanX - leftSpanX).coerceAtLeast(1f)

            if (waveT > 0.005f) {
                cubicTo(
                    leftSpanX + spanW * 0.86f, lerp(rightY, troughY, waveT),
                    leftSpanX + spanW * 0.64f, lerp(rightY, troughY, waveT),
                    leftSpanX + spanW * 0.48f, lerp((rightY + leftY) * 0.5f, h - waveAmp * 0.52f, waveT)
                )
                cubicTo(
                    leftSpanX + spanW * 0.32f, lerp(leftY, h - waveAmp * 1.04f, waveT),
                    leftSpanX + spanW * 0.14f, lerp(leftY, h - waveAmp * 0.96f, waveT),
                    leftSpanX, leftY
                )
            } else {
                lineTo(leftSpanX, leftY)
            }

            // Bottom-left corner back up to left edge
            if (clampedBottomR > 0.5f) {
                arcTo(
                    Rect(0f, leftY - clampedBottomR * 2f, clampedBottomR * 2f, leftY),
                    90f,
                    90f,
                    false
                )
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Isolated Now Playing Style: Wave
 * Full-bleed top album artwork with a continuous morphing S-wave bottom divider,
 * 4 lifted layered translucent colored waves that spring up/down lazily while dragging,
 * neighbor-pushing expressive transport controls, and the exact BottomToggleRow pill geometry.
 */
@Composable
fun WavePlayerLayout(
    params: PlayerStyleParams,
    modifier: Modifier = Modifier
) {
    val baseTheme = LocalMaterialTheme.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

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
    val artHeightPx = with(density) { (screenHeightDp * 0.50f).toPx() }
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

    // 4 staggered slow-spring drag trackers so the 4 layered bottom waves float & spring lazily while dragging
    val currentExpansion = params.expansionFractionProvider().coerceIn(0f, 1f)
    val waveSpring1 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.46f, stiffness = 70f),
        label = "waveLayerSpring1"
    )
    val waveSpring2 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 98f),
        label = "waveLayerSpring2"
    )
    val waveSpring3 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 130f),
        label = "waveLayerSpring3"
    )
    val waveSpring4 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 165f),
        label = "waveLayerSpring4"
    )

    val waveOrder = remember {
        listOf(MiniTransportSlot.PREVIOUS, MiniTransportSlot.PLAY_PAUSE, MiniTransportSlot.NEXT)
    }
    val waveWeights = remember {
        mapOf(
            MiniTransportSlot.PREVIOUS to 0.92f,
            MiniTransportSlot.PLAY_PAUSE to 1.24f,
            MiniTransportSlot.NEXT to 0.92f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // 4 lifted layered translucent colored waves that spring up/down lazily while dragging
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(275.dp + navBarBottomPadding)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                }
        ) {
            val w = size.width
            val h = size.height

            fun drawLayeredWave(
                baseTopFraction: Float,
                springValue: Float,
                bouncePx: Float,
                leftFrac: Float,
                cp1X: Float,
                cp1Y: Float,
                midX: Float,
                midY: Float,
                cp2X: Float,
                cp2Y: Float,
                rightFrac: Float,
                color: Color
            ) {
                val dragOffsetY = (1f - springValue) * bouncePx
                val baseY = h * baseTopFraction + dragOffsetY
                val path = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, baseY + h * leftFrac)
                    cubicTo(
                        w * cp1X, baseY + h * cp1Y,
                        w * (midX - 0.14f), baseY + h * (midY + 0.08f),
                        w * midX, baseY + h * midY
                    )
                    cubicTo(
                        w * (midX + 0.16f), baseY + h * (midY - 0.10f),
                        w * cp2X, baseY + h * cp2Y,
                        w, baseY + h * rightFrac
                    )
                    lineTo(w, h)
                    close()
                }
                drawPath(path = path, color = color)
            }

            // Back-most lifted wave (highest & softest)
            drawLayeredWave(
                baseTopFraction = 0.16f,
                springValue = waveSpring1,
                bouncePx = 68.dp.toPx(),
                leftFrac = 0.08f,
                cp1X = 0.22f, cp1Y = -0.10f,
                midX = 0.52f, midY = 0.06f,
                cp2X = 0.82f, cp2Y = -0.08f,
                rightFrac = 0.04f,
                color = baseTheme.tertiary.copy(alpha = 0.07f)
            )
            // Second wave (counter-crest)
            drawLayeredWave(
                baseTopFraction = 0.30f,
                springValue = waveSpring2,
                bouncePx = 56.dp.toPx(),
                leftFrac = 0.14f,
                cp1X = 0.28f, cp1Y = 0.18f,
                midX = 0.56f, midY = -0.04f,
                cp2X = 0.84f, cp2Y = 0.10f,
                rightFrac = 0.02f,
                color = accentColor.copy(alpha = 0.09f)
            )
            // Third wave (mid rolling swell)
            drawLayeredWave(
                baseTopFraction = 0.45f,
                springValue = waveSpring3,
                bouncePx = 44.dp.toPx(),
                leftFrac = 0.06f,
                cp1X = 0.20f, cp1Y = -0.08f,
                midX = 0.48f, midY = 0.12f,
                cp2X = 0.78f, cp2Y = -0.06f,
                rightFrac = 0.06f,
                color = baseTheme.secondary.copy(alpha = 0.11f)
            )
            // Foreground lifted wave (richest accent)
            drawLayeredWave(
                baseTopFraction = 0.60f,
                springValue = waveSpring4,
                bouncePx = 32.dp.toPx(),
                leftFrac = 0.14f,
                cp1X = 0.26f, cp1Y = -0.02f,
                midX = 0.58f, midY = 0.14f,
                cp2X = 0.86f, cp2Y = -0.08f,
                rightFrac = -0.02f,
                color = accentColor.copy(alpha = 0.14f)
            )
        }

        // TOP LAYER: Continuous Morphing Organic Wave Artwork
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val sample = morphTracker.evaluate(params.expansionFractionProvider())
                    val fullW = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: screenWidthPx
                    val fullH = (constraints.maxHeight.toFloat() * 0.50f).takeIf { it > 0f } ?: artHeightPx
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

        // LOWER SECTION: Matches exact BottomToggleRow position & padding from existing styles
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottomPadding + 6.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Song Title + Artist + Favorite Heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StyleMorphingSongInfo(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    contentColor = contentColor,
                    artistAlpha = 0.78f,
                    titleFontSize = 27.sp,
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
                    containerColor = onAccentColor.copy(alpha = 0.80f),
                    activeContainerColor = accentColor,
                    iconTint = accentColor,
                    activeIconTint = onAccentColor,
                    size = 46.dp,
                    iconSize = 23.dp,
                    onClick = params.onFavoriteToggle,
                    modifier = Modifier.graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider + Timestamps + Optional Audio Meta
            StyleProgressSection(
                params = params,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                activeTrackColor = accentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.18f),
                thumbColor = accentColor,
                timeTextColor = contentColor.copy(alpha = 0.76f),
                metaPillBgColor = contentColor.copy(alpha = 0.10f),
                metaPillTextColor = contentColor.copy(alpha = 0.90f),
                waveAmplitudeWhenPlaying = 2.6.dp,
                trackHeight = 4.5.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Neighbor-pushing Expressive Transport Row (exact 80dp height & tight 6dp spacing)
            StyleMorphingTransportRow(
                order = waveOrder,
                baseWeights = waveWeights,
                screenWidthPx = screenWidthPx,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                expansionFractionProvider = params.expansionFractionProvider,
                aodSourceBounds = params.aodSourceBounds,
                aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                isPlaying = isPlaying,
                playContainerColor = accentColor,
                playContentColor = onAccentColor,
                skipContainerColor = onAccentColor,
                skipContentColor = accentColor,
                playCornerPlaying = 26.dp,
                playCornerPaused = 40.dp,
                skipCorner = 40.dp,
                onPrevious = params.onPrevious,
                onPlayPause = params.onPlayPause,
                onNext = params.onNext,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Exact BottomToggleRow geometry: 26dp horizontal padding, 66dp height, 6dp bottom padding
            StyleExpressiveBottomDock(
                isShuffleEnabled = isShuffle,
                repeatMode = repeatMode,
                onShuffleToggle = params.onShuffleToggle,
                onRepeatToggle = params.onRepeatToggle,
                onLyricsClick = params.onLyricsClick,
                onMoreOptionsClick = params.onMoreOptionsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .padding(bottom = 6.dp)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        alpha = a
                        translationY = (1f - a) * slideOffsetPx
                    }
            )
        }
    }
}
