package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
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
 * Isolated Now Playing Style: Expressive
 * Full-bleed gradient-faded top artwork, editorial title & uppercase artist with tonal heart,
 * wavy progress slider with centered audio metadata pill, asymmetric wide Play/Pause + twin
 * circular Previous/Next buttons, and a segmented pill bottom dock + circular '+' action button.
 */
@Composable
fun ExpressivePlayerLayout(
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
        bounds.washStart = 0.52f
        bounds.washMid = 0.80f
        bounds.washMidAlpha = 0.88f
        bounds.topShadowHeightDp = 120f
        bounds.topShadowStartAlpha = 0.42f
        bounds.topShadowMidAlpha = 0.16f
    }

    val isPlaying = params.isPlayingProvider()
    val isFavorite = params.isFavoriteProvider()
    val isShuffle = params.isShuffleEnabledProvider()
    val repeatMode = params.repeatModeProvider()

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "expressiveArtScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // TOP LAYER: Full-bleed Gradient-Faded Artwork morphing from MiniPlayer circle
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
                    val s = morphTracker.evaluate(params.expansionFractionProvider())
                    val cornerPx = lerp(miniArtSizePx * 0.5f, 0f, s.sizeT)
                    shape = RoundedCornerShape(cornerPx)
                    clip = true
                    alpha = if (params.aodTransitionProgressProvider() > 0.001f) 0f else 1f
                }
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

            // Top status bar scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Black.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Smooth bottom gradient fade into solidBackgroundColor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.46f)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to solidBackgroundColor.copy(alpha = 0.58f),
                                0.80f to solidBackgroundColor.copy(alpha = 0.92f),
                                1.0f to solidBackgroundColor
                            )
                        )
                    )
            )
        }

        // LOWER SECTION: Title/Artist + Tonal Heart, Wavy Slider + Meta Pill, Asymmetric Controls, Bottom Dock
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = (screenHeightDp * 0.56f).coerceAtLeast(310.dp),
                    start = 24.dp,
                    end = 24.dp,
                    bottom = navBarBottomPadding + 16.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Editorial Song Info + Circular Tonal Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StyleMorphingSongInfo(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    contentColor = contentColor,
                    artistAlpha = 0.72f,
                    titleFontSize = 28.sp,
                    artistFontSize = 14.sp,
                    uppercaseArtist = true,
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
                    containerColor = contentColor.copy(alpha = 0.14f),
                    activeContainerColor = accentColor.copy(alpha = 0.24f),
                    iconTint = contentColor.copy(alpha = 0.90f),
                    activeIconTint = accentColor,
                    size = 48.dp,
                    iconSize = 23.dp,
                    onClick = params.onFavoriteToggle,
                    modifier = Modifier.graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                )
            }

            // Wavy Progress Slider + Timestamps + Centered Audio Metadata Pill
            StyleProgressSection(
                params = params,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                activeTrackColor = contentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.22f),
                thumbColor = contentColor,
                timeTextColor = contentColor.copy(alpha = 0.78f),
                metaPillBgColor = contentColor.copy(alpha = 0.14f),
                metaPillTextColor = contentColor.copy(alpha = 0.90f),
                waveAmplitudeWhenPlaying = 3.5.dp,
                trackHeight = 5.dp
            )

            // Asymmetric Expressive Transport Row: Wide Rounded Play/Pause + Twin Circular Prev/Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    cornerRadius = if (isPlaying) 24.dp else 39.dp,
                    pressedCornerRadius = 18.dp,
                    iconSize = 34.dp,
                    isPlaying = isPlaying,
                    onClick = params.onPlayPause,
                    modifier = Modifier
                        .weight(1.42f)
                        .fillMaxHeight()
                )

                StyleMorphingTransportButton(
                    slot = MiniTransportSlot.PREVIOUS,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    containerColor = baseTheme.secondaryContainer,
                    contentColor = baseTheme.onSecondaryContainer,
                    cornerRadius = 39.dp,
                    pressedCornerRadius = 22.dp,
                    iconSize = 28.dp,
                    onClick = params.onPrevious,
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
                    containerColor = baseTheme.secondaryContainer,
                    contentColor = baseTheme.onSecondaryContainer,
                    cornerRadius = 39.dp,
                    pressedCornerRadius = 22.dp,
                    iconSize = 28.dp,
                    onClick = params.onNext,
                    modifier = Modifier.size(78.dp)
                )
            }

            // Bottom Segmented Pill Dock (Shuffle | Repeat | Lyrics) + Circular '+' More Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        alpha = a
                        translationY = (1f - a) * slideOffsetPx
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.24f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveDockSegment(
                            iconRes = R.drawable.rounded_shuffle_24,
                            contentDescription = "Shuffle",
                            isActive = isShuffle,
                            shape = RoundedCornerShape(
                                topStart = 24.dp,
                                bottomStart = 24.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            ),
                            activeBg = accentColor,
                            activeTint = onAccentColor,
                            inactiveBg = contentColor.copy(alpha = 0.12f),
                            inactiveTint = contentColor.copy(alpha = 0.88f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                params.onShuffleToggle()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        ExpressiveDockSegment(
                            iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
                                R.drawable.rounded_repeat_one_24
                            } else {
                                R.drawable.rounded_repeat_24
                            },
                            contentDescription = "Repeat",
                            isActive = repeatMode != Player.REPEAT_MODE_OFF,
                            shape = RoundedCornerShape(8.dp),
                            activeBg = accentColor,
                            activeTint = onAccentColor,
                            inactiveBg = contentColor.copy(alpha = 0.12f),
                            inactiveTint = contentColor.copy(alpha = 0.88f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                params.onRepeatToggle()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        ExpressiveDockSegment(
                            iconRes = R.drawable.rounded_lyrics_24,
                            contentDescription = "Lyrics",
                            isActive = false,
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp,
                                topEnd = 24.dp,
                                bottomEnd = 24.dp
                            ),
                            activeBg = accentColor,
                            activeTint = onAccentColor,
                            inactiveBg = contentColor.copy(alpha = 0.12f),
                            inactiveTint = contentColor.copy(alpha = 0.88f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                params.onLyricsClick()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // Standalone circular '+' button for More Options / Song Info
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                params.onMoreOptionsClick()
                            }
                        ),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.28f),
                    contentColor = contentColor.copy(alpha = 0.92f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "More options",
                            tint = contentColor.copy(alpha = 0.92f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveDockSegment(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    shape: RoundedCornerShape,
    activeBg: Color,
    activeTint: Color,
    inactiveBg: Color,
    inactiveTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) activeBg else inactiveBg,
        animationSpec = tween(200),
        label = "expSegBg"
    )
    val tint by animateColorAsState(
        targetValue = if (isActive) activeTint else inactiveTint,
        animationSpec = tween(200),
        label = "expSegTint"
    )
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = shape,
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
