package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

@Composable
fun ExpressiveHeroCollageHeader(
    title: String,
    subtitle: String,
    songs: List<Song>,
    modifier: Modifier = Modifier,
    headerHeight: Dp = 330.dp,
    parallaxOffset: Float = 0f,
    collageAlpha: Float = 1f,
    showCollage: Boolean = true,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val albumArts = remember(songs) {
        songs.mapNotNull { it.albumArtUriString?.takeIf { uri -> uri.isNotBlank() } }
            .distinct()
            .take(3)
    }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryGlow = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        // Soft ambient glow + Expressive overlapping album-art collage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.22f),
                            surfaceColor.copy(alpha = 0.4f),
                            surfaceColor
                        )
                    )
                )
        )

        if (showCollage && albumArts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 18.dp, bottom = 58.dp)
                    .graphicsLayer {
                        translationY = parallaxOffset
                        alpha = collageAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-72).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    albumArts.forEachIndexed { index, artUrl ->
                        val size = when {
                            albumArts.size == 1 -> 196.dp
                            index == 1 -> 204.dp
                            else -> 164.dp
                        }
                        val rotation = when {
                            albumArts.size == 1 -> 0f
                            index == 0 -> -14f
                            index == 1 -> 0f
                            else -> 14f
                        }
                        val shape = threeShapeSwitch(index, thirdShapeCornerRadius = 28.dp)

                        if (index == 2) {
                            Box(
                                modifier = Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        Constraints.fixed(
                                            width = size.roundToPx(),
                                            height = size.roundToPx()
                                        )
                                    )
                                    layout(constraints.maxWidth, placeable.height) {
                                        val xOffset = (constraints.maxWidth - placeable.width) / 2
                                        placeable.placeRelative(xOffset, 0)
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { rotationZ = rotation }
                                        .clip(shape)
                                ) {
                                    SmartImage(
                                        model = artUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .graphicsLayer { rotationZ = rotation }
                                    .clip(shape)
                            ) {
                                SmartImage(
                                    model = artUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        } else if (showCollage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 24.dp, bottom = 64.dp)
                    .graphicsLayer {
                        translationY = parallaxOffset
                        alpha = collageAlpha * 0.45f
                    },
                contentAlignment = Alignment.Center
            ) {
                SmartImage(
                    model = R.drawable.rounded_album_24,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        // Smooth Apple Music-style fade into surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0.08f),
                            Color.Transparent,
                            surfaceColor.copy(alpha = 0.55f),
                            surfaceColor.copy(alpha = 0.92f),
                            surfaceColor
                        )
                    )
                )
        )

        // Centered Title + Subtitle anchored at the bottom of the hero
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (trailingAction != null) {
                    Spacer(modifier = Modifier.size(6.dp))
                    trailingAction()
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = GoogleSansRounded
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
