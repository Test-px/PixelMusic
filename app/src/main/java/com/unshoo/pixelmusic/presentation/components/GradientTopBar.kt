package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
import com.unshoo.pixelmusic.ui.theme.PixelMusicStatusBarStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import com.unshoo.pixelmusic.presentation.components.MusicRecognitionDialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.PlayerViewModel
import unshoo.ianshulyadav.pixelmusic.innertube.models.WatchEndpoint
import androidx.compose.material.icons.rounded.Search
import com.unshoo.pixelmusic.data.remote.youtube.toNativeSong
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.GraphicEq




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreGradientTopBar(
    title: String,
    startColor: Color,
    endColor: Color,
    contentColor: Color,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigationIconClick: () -> Unit,
) {
    val gradientBrush = remember(startColor, endColor) {
        Brush.verticalGradient(colors = listOf(startColor, endColor))
    }

    PixelMusicStatusBarStyle(color = startColor)

    LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = title,
                color = contentColor,
                fontFamily = GoogleSansRounded
            )
        },
        expandedHeight = 160.dp,
        modifier = Modifier.background(brush = gradientBrush),
        navigationIcon = {
            IconButton(
                modifier = Modifier.padding(start = 10.dp),
                onClick = onNavigationIconClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = contentColor
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.auth_cd_back),
                    tint = startColor
                )
            }
        },
        colors = topAppBarColors(
            containerColor = Color.Transparent, // Background is handled by the gradient brush
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, // Or a color that contrasts well with your typical gradient
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Same as title
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeGradientTopBar(
    onNavigationIconClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onBetaClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    isScrolled: Boolean = false,
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    var showRecognitionDialog by remember { mutableStateOf(false) }

    if (showRecognitionDialog) {
        MusicRecognitionDialog(
            onDismiss = { showRecognitionDialog = false },
            onPlayMusic = { recognizedSong ->
                showRecognitionDialog = false // Hide the dialog instantly
                
                coroutineScope.launch {
                    // Search YouTube Music and grab the first song result
                    val songToPlay = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val query = "${recognizedSong.title} ${recognizedSong.artist}"
                        val searchResult = unshoo.ianshulyadav.pixelmusic.innertube.YouTube.search(
                            query, 
                            unshoo.ianshulyadav.pixelmusic.innertube.YouTube.SearchFilter.FILTER_SONG
                        ).getOrNull()
                        
                        val topResult = searchResult?.items?.firstOrNull { it is unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem } as? unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem
                        
                        // Convert it to a PixelMusic Song object, and keep Shazam's gorgeous high-res cover art!
                        val nativeSong = topResult?.toNativeSong()
                        nativeSong?.copy(
                            albumArtUriString = recognizedSong.coverArtHqUrl ?: recognizedSong.coverArtUrl ?: nativeSong.albumArtUriString
                        )
                    }

                    if (songToPlay != null) {
                        // Boom! Play it using your standard player and build an endless radio queue
                        playerViewModel.playWithArchiveTuneQueueBuilder(
                            song = songToPlay,
                            queueName = "Recognized Music"
                        )
                    } else {
                        playerViewModel.sendToast("Could not find this track on YouTube Music.")
                    }
                }
            }
        )
    }
    
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHighest

    PixelMusicStatusBarStyle(color = surfaceContainerHigh)

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "topbar_alpha_transition"
    )

    TopAppBar(
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(surfaceContainerHigh.copy(alpha = animatedAlpha)),
        title = { /* nada, usamos solo acciones */ },
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "PixelMusic",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
     actions = {
            val infiniteTransition = rememberInfiniteTransition(label = "recognitionPulse")

            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "buttonScale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )
            val iconRotation by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconRotation"
            )

            val gradientBrush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = scale * 1.6f
                            scaleY = scale * 1.6f
                            alpha = glowAlpha
                        }
                        .background(gradientBrush, shape = CircleShape)
                        .blur(radius = 14.dp)
                )

                IconButton(
                    onClick = { showRecognitionDialog = true },
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(gradientBrush)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.GraphicEq,
                        contentDescription = "Recognize Music",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = iconRotation
                        }
                    )
                }
            }
        },
        colors = topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}
