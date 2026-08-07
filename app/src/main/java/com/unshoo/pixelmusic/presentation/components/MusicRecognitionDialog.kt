/*
 * PixelMusic - Music Recognition UI
 */

package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.unshoo.pixelmusic.data.shazam.MusicRecognizer
import com.unshoo.pixelmusic.data.shazam.RecognitionResult
import com.unshoo.pixelmusic.data.shazam.RecognitionStatus

@Composable
fun MusicRecognitionDialog(
    onDismiss: () -> Unit,
    onPlayMusic: (youtubeVideoId: String) -> Unit
) {
    var status by remember { mutableStateOf<RecognitionStatus>(RecognitionStatus.Ready) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Right Close Button
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (val currentStatus = status) {
                    is RecognitionStatus.Ready -> {
                        Text(
                            text = "Tap to recognize",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        BigListeningButton(isListening = false) {
                            status = RecognitionStatus.Listening
                            coroutineScope.launch {
                                val result = MusicRecognizer.recognizeCurrentAudio()
                                result.onSuccess {
                                    status = RecognitionStatus.Success(it)
                                }.onFailure {
                                    status = RecognitionStatus.Error(it.message ?: "Could not recognize song")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    is RecognitionStatus.Listening -> {
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        BigListeningButton(isListening = true) {}
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    is RecognitionStatus.Success -> {
                        val song = currentStatus.result
                        
                        AsyncImage(
                            model = song.coverArtHqUrl ?: song.coverArtUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (song.youtubeVideoId != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onPlayMusic(song.youtubeVideoId)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play on PixelMusic")
                            }
                        } else {
                            Text(
                                text = "YouTube ID not found for this track.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    is RecognitionStatus.Error -> {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Match Found",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentStatus.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        FilledTonalButton(
                            onClick = { status = RecognitionStatus.Ready },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Try Again")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun BigListeningButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Animate scale and alpha for the ripple effect when listening
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.5f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp)
    ) {
        // The pulsing ripple layer
        if (isListening) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = rippleScale
                        scaleY = rippleScale
                        alpha = rippleAlpha
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // The main static button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isListening) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .clickable(enabled = !isListening, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Microphone",
                modifier = Modifier.size(36.dp),
                tint = if (isListening) MaterialTheme.colorScheme.onPrimary 
                       else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

