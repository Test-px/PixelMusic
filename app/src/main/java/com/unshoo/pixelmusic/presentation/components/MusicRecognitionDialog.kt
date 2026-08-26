package com.unshoo.pixelmusic.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.unshoo.pixelmusic.data.shazam.MusicRecognizer
import com.unshoo.pixelmusic.data.shazam.RecognitionResult
import com.unshoo.pixelmusic.data.shazam.RecognitionStatus
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicRecognitionDialog(
    onDismiss: () -> Unit,
    isTransparentOverlay: Boolean = false,
    onPlayMusic: (RecognitionResult) -> Unit
) {
    var status by remember { mutableStateOf<RecognitionStatus>(RecognitionStatus.Ready) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            status = RecognitionStatus.Listening
            coroutineScope.launch {
                val result = MusicRecognizer.recognizeCurrentAudio()
                result.onSuccess {
                    status = RecognitionStatus.Success(it)
                }.onFailure {
                    status = RecognitionStatus.Error(it.message ?: "Could not recognize song")
                }
            }
        } else {
            status = RecognitionStatus.Error("Microphone permission is required to listen to music.")
        }
    }

    LaunchedEffect(isTransparentOverlay) {
        if (isTransparentOverlay && status is RecognitionStatus.Ready) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                status = RecognitionStatus.Listening
                coroutineScope.launch {
                    val result = MusicRecognizer.recognizeCurrentAudio()
                    result.onSuccess {
                        status = RecognitionStatus.Success(it)
                    }.onFailure {
                        status = RecognitionStatus.Error(it.message ?: "Could not recognize song")
                    }
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val onScannerClick: () -> Unit = {
        if (status is RecognitionStatus.Ready || status is RecognitionStatus.Error) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                status = RecognitionStatus.Listening
                coroutineScope.launch {
                    val result = MusicRecognizer.recognizeCurrentAudio()
                    result.onSuccess {
                        status = RecognitionStatus.Success(it)
                    }.onFailure {
                        status = RecognitionStatus.Error(it.message ?: "Could not recognize song")
                    }
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    if (isTransparentOverlay) {
        // =========================================================================================
        // ── OVERLAY MODE: Pure Google style floating design (No Solid Card) ──
        // =========================================================================================
        
        // Uses the dynamic theme background (Dark or Light) with 85% opacity for a beautiful glass effect
        val overlayBackground = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
        val textColor = MaterialTheme.colorScheme.onSurface
        val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBackground)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }

                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        fadeIn(tween(400)) + scaleIn(initialScale = 0.85f) togetherWith fadeOut(tween(300))
                    },
                    modifier = Modifier.align(Alignment.Center),
                    label = "overlay_content"
                ) { currentStatus ->
                    when (currentStatus) {
                        is RecognitionStatus.Success -> {
                            val song = currentStatus.result
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = AbsoluteSmoothCornerShape(28.dp, 60),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 8.dp
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    AsyncImage(
                                        model = song.coverArtHqUrl ?: song.coverArtUrl,
                                        contentDescription = "Album Art",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(AbsoluteSmoothCornerShape(20.dp, 60))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = subTextColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { onPlayMusic(song) },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Play on PixelMusic",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        is RecognitionStatus.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Error",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = currentStatus.message,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onScannerClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                ScannerButton(
                                    isListening = currentStatus is RecognitionStatus.Listening,
                                    onClick = onScannerClick
                                )
                                Spacer(modifier = Modifier.height(36.dp))
                                Text(
                                    text = if (currentStatus is RecognitionStatus.Listening) "Listening for music…" else "Tap to recognize",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // =========================================================================================
        // ── IN-APP MODE: Standard full dialog card floating over the UI ──
        // =========================================================================================
        val textColor = MaterialTheme.colorScheme.onSurface
        val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.85f)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = AbsoluteSmoothCornerShape(32.dp, 80),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (status) {
                                is RecognitionStatus.Listening -> "Listening..."
                                is RecognitionStatus.Success -> "Match found!"
                                is RecognitionStatus.Error -> "No match"
                                else -> "Tap to recognize 𝄞"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        ScannerButton(
                            isListening = status is RecognitionStatus.Listening,
                            onClick = onScannerClick
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                    ) {
                        AnimatedContent(
                            targetState = status,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(600, delayMillis = 100)) + slideInVertically(
                                    animationSpec = tween(600, delayMillis = 100),
                                    initialOffsetY = { fullHeight -> fullHeight / 2 }
                                ) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "result_animation",
                            contentAlignment = Alignment.Center
                        ) { currentStatus ->
                            when (currentStatus) {
                                is RecognitionStatus.Success -> {
                                    val song = currentStatus.result
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = song.coverArtHqUrl ?: song.coverArtUrl,
                                            contentDescription = "Album Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(200.dp)
                                                .clip(AbsoluteSmoothCornerShape(24.dp, 60))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = subTextColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(32.dp))

                                        Button(
                                            onClick = { onPlayMusic(song) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "Play on PixelMusic",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                is RecognitionStatus.Error -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = currentStatus.message,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = subTextColor,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                else -> {
                                    Box(modifier = Modifier.height(200.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "powerSurge")

    // ── We now animate the Progress (0f to 1f) instead of Scale ──
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isListening) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Progress"
    )

    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isListening) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Progress"
    )

    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(96.dp)
            .drawBehind { // ── Pure GPU Canvas Drawing (No Boxes!) ──
                if (!isListening) return@drawBehind

                // Base radius of the button
                val startRadius = size.minDimension / 2f
                // 16x multiplier easily covers edge-to-edge of any modern screen
                val maxRadius = startRadius * 16f 

                // Draw Outer Staggered Wave
                if (wave2Progress > 0f) {
                    val currentRadius = startRadius + (maxRadius - startRadius) * wave2Progress
                    val currentAlpha = 0.85f * (1f - wave2Progress)
                    drawCircle(
                        color = primaryColor,
                        radius = currentRadius,
                        alpha = currentAlpha
                    )
                }

                // Draw Leading Edge Wave
                if (wave1Progress > 0f) {
                    val currentRadius = startRadius + (maxRadius - startRadius) * wave1Progress
                    val currentAlpha = 0.85f * (1f - wave1Progress)
                    drawCircle(
                        color = primaryColor,
                        radius = currentRadius,
                        alpha = currentAlpha
                    )
                }
            }
    ) {
        // Center Action Button
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = buttonPulse
                    scaleY = buttonPulse
                },
            shape = AbsoluteSmoothCornerShape(32.dp, 60),
            color = if (isListening) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = if (isListening) 12.dp else 4.dp,
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "Microphone",
                    modifier = Modifier.size(42.dp),
                    tint = if (isListening) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
