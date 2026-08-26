/*
 * PixelMusic - Music Recognition UI
 */

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
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // REMOVED the Compose Dialog! The transparent Activity acts as the dialog now.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)) // Smoothly dims the apps behind it
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss // Dismiss the overlay if they tap the dim background
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // A massive container that spans most of the screen
                .height(screenHeight * 0.85f)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.systemBars) // Protects content from the transparent nav bars
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Prevents clicking INSIDE the card from dismissing the app
                ),
            shape = AbsoluteSmoothCornerShape(32.dp, 80),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ── Close Button ──
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

                // ── TOP AREA (The Trigger & The Scanner) ──
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    ScannerButton(
                        isListening = status is RecognitionStatus.Listening,
                        onClick = {
                            if (status is RecognitionStatus.Ready || status is RecognitionStatus.Error) {
                                val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
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
                    )
                }

                // ── BOTTOM AREA (The Results fade in here) ──
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            else -> {
                                // Idle state at the bottom is empty, waiting for a result
                                Box(modifier = Modifier.height(200.dp))
                            }
                        }
                    }
                }
            } // End Box
        } // End Surface
    } // End Box (Replaces Dialog)

@Composable
fun ScannerButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "powerSurge")
    
    // The "Power Surge" Wave
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 4f else 1f, // Expands massively outwards
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )
    
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.6f else 0f,
        targetValue = 0f, // Fades out completely as it expands
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    // A subtle inner pulse for the button itself
    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp) // Large area for the wave to travel
    ) {
        // The Surge Wave
        if (isListening) {
            val surgeGradient = Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                    Color.Transparent
                )
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp) // Base size of the wave origin
                    .graphicsLayer {
                        scaleX = waveScale
                        scaleY = waveScale
                        alpha = waveAlpha
                    }
                    .clip(CircleShape)
                    .background(surgeGradient)
            )
        }

        // The Trigger Button
        Surface(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = buttonPulse
                    scaleY = buttonPulse
                },
            shape = AbsoluteSmoothCornerShape(32.dp, 60), // Cool squircle trigger
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
