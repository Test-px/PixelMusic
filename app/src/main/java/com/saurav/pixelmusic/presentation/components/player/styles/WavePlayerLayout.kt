package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
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
 * Organic S-curve wave bottom cutout that morphs cleanly from the MiniPlayer 44dp circle
 * into a full-bleed wavy artwork silhouette.
 * Every control point stays strictly within [0, h] so the wave never clips flat at the bottom,
 * and top corners stay smoothly rounded during sheet drags (matching ComplexPlayerLayout).
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
        val waveAmp = h * 0.16f * waveT

        // Strictly inside [0, h]: left starts mid-wave, left-center crests up, right-center dips to h - 2f
        val leftY = h - waveAmp * 0.52f
        val rightY = h - waveAmp * 0.44f
        val troughY = h - 2f

        val maxTopR = minOf(w * 0.5f, leftY, rightY).coerceAtLeast(0f)
        val topCornerR = (maxTopR * (1f - t).pow(1.15f)).coerceIn(0f, maxTopR)

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
            // Right edge -> smooth deep trough (strictly <= h) -> center inflection
            cubicTo(
                w * 0.86f, troughY,
                w * 0.64f, troughY,
                w * 0.48f, h - waveAmp * 0.52f
            )
            // Center inflection -> smooth left crest -> left edge
            cubicTo(
                w * 0.32f, h - waveAmp * 1.04f,
                w * 0.14f, h - waveAmp * 0.96f,
                0f, leftY
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Isolated Now Playing Style: Wave
 * Full-bleed top album artwork with a clean, unclipped organic S-wave bottom divider,
 * bold editorial title/artist, balanced vertical rhythm, circular expressive transport controls,
 * and a full Material You Expressive segmented bottom dock.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // Subtle decorative bottom wave ribbon behind the dock
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp + navBarBottomPadding)
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
                lineTo(0f, h * 0.54f)
                cubicTo(w * 0.24f, h * 0.22f, w * 0.52f, h * 0.78f, w * 0.78f, h * 0.34f)
                cubicTo(w * 0.88f, h * 0.18f, w * 0.95f, h * 0.16f, w, h * 0.20f)
                lineTo(w, h)
                close()
            }
            drawPath(path = path, color = accentColor.copy(alpha = 0.12f))
        }

        // TOP LAYER: Morphing Organic Wave Artwork (50% screen height so lower controls breathe cleanly)
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

        // LOWER SECTION: Anchored cleanly from the bottom with harmonious spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = navBarBottomPadding + 18.dp
                ),
            verticalArrangement = Arrangement.Bottom
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
                    containerColor = onAccentColor.copy(alpha = 0.72f),
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

            Spacer(modifier = Modifier.height(16.dp))

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
                trackHeight = 4.5.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Centered Expressive Circular Transport Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
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
                    containerColor = onAccentColor,
                    contentColor = accentColor,
                    cornerRadius = 31.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 28.dp,
                    onClick = params.onPrevious,
                    modifier = Modifier.size(62.dp)
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
                    cornerRadius = if (isPlaying) 26.dp else 40.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 36.dp,
                    isPlaying = isPlaying,
                    onClick = params.onPlayPause,
                    modifier = Modifier.size(80.dp)
                )

                StyleMorphingTransportButton(
                    slot = MiniTransportSlot.NEXT,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    containerColor = onAccentColor,
                    contentColor = accentColor,
                    cornerRadius = 31.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 28.dp,
                    onClick = params.onNext,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Full Material You Expressive Segmented Bottom Dock
            StyleExpressiveBottomDock(
                isShuffleEnabled = isShuffle,
                repeatMode = repeatMode,
                onShuffleToggle = params.onShuffleToggle,
                onRepeatToggle = params.onRepeatToggle,
                onLyricsClick = params.onLyricsClick,
                onMoreOptionsClick = params.onMoreOptionsClick,
                modifier = Modifier
                    .fillMaxWidth()
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
