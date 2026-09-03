package com.unshoo.pixelmusic.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.unshoo.pixelmusic.data.shazam.MusicRecognizer
import com.unshoo.pixelmusic.data.shazam.RecognitionResult
import com.unshoo.pixelmusic.data.shazam.RecognitionStatus
import com.unshoo.pixelmusic.ui.effects.recognitionRippleEffect
import com.unshoo.pixelmusic.ui.effects.successSweepEffect
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

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
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeight.toPx() }

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

    val isListening = status is RecognitionStatus.Listening
    val isSuccess = status is RecognitionStatus.Success

    // Fullscreen AGSL Shader Effect (Android 13+)
    val rippleModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier
            .recognitionRippleEffect(isTriggered = isListening)
            .successSweepEffect(isTriggered = isSuccess)
    } else {
        Modifier
    }

    if (isTransparentOverlay) {
        val overlayBackground = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
        val textColor = MaterialTheme.colorScheme.onSurface
        val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        val offscreenStartY = with(density) { -(screenHeight.toPx() + 400.dp.toPx()) }
        val cardDropOffsetY = remember { Animatable(offscreenStartY) }

        LaunchedEffect(status) {
            if (status is RecognitionStatus.Success) {
                cardDropOffsetY.snapTo(offscreenStartY)
                cardDropOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = 320f
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(rippleModifier)
                .background(overlayBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
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

            AnimatedVisibility(
                visible = status !is RecognitionStatus.Success && status !is RecognitionStatus.Error,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(180))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    ScannerButton(
                        isListening = status is RecognitionStatus.Listening,
                        onClick = onScannerClick
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Text(
                        text = if (status is RecognitionStatus.Listening) "Listening for music…" else "Tap to recognize",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(
                visible = status is RecognitionStatus.Error,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(180))
            ) {
                if (status is RecognitionStatus.Error) {
                    val currentError = status as RecognitionStatus.Error
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
                            text = currentError.message,
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
            }

            if (status is RecognitionStatus.Success) {
                val song = (status as RecognitionStatus.Success).result
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(0, cardDropOffsetY.value.roundToInt()) }
                        .width(320.dp)
                        .heightIn(min = 530.dp)
                        .padding(horizontal = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = AbsoluteSmoothCornerShape(32.dp, 60),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        AsyncImage(
                            model = song.coverArtHqUrl ?: song.coverArtUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 210.dp, height = 265.dp)
                                .clip(AbsoluteSmoothCornerShape(22.dp, 60))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = subTextColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        Button(
                            onClick = { onPlayMusic(song) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play on PixelMusic",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        val textColor = MaterialTheme.colorScheme.onSurface
        val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(rippleModifier)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = AbsoluteSmoothCornerShape(32.dp, 60),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = subTextColor
                            )
                        }

                        AnimatedContent(
                            targetState = status,
                            transitionSpec = {
                                (fadeIn(tween(350)) + scaleIn(initialScale = 0.92f))
                                    .togetherWith(fadeOut(tween(200)))
                            },
                            label = "in_app_dialog_content"
                        ) { currentStatus ->
                            when (currentStatus) {
                                is RecognitionStatus.Success -> {
                                    val song = currentStatus.result
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp)
                                    ) {
                                        Text(
                                            text = "Match found!",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        AsyncImage(
                                            model = song.coverArtHqUrl ?: song.coverArtUrl,
                                            contentDescription = "Album Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 210.dp, height = 265.dp)
                                                .clip(AbsoluteSmoothCornerShape(22.dp, 60))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )

                                        Spacer(modifier = Modifier.height(18.dp))

                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = subTextColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(26.dp))

                                        Button(
                                            onClick = { onPlayMusic(song) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Play on PixelMusic",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
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
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = currentStatus.message,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = subTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = onScannerClick,
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Try Again")
                                        }
                                    }
                                }

                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
                                    ) {
                                        Text(
                                            text = if (currentStatus is RecognitionStatus.Listening) "Listening..." else "Tap to recognize 𝄞",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )

                                        Spacer(modifier = Modifier.height(36.dp))

                                        ScannerButton(
                                            isListening = currentStatus is RecognitionStatus.Listening,
                                            onClick = onScannerClick
                                        )
                                    }
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
    
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 5.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )
    
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 1f else 0f,
        targetValue = 0f, 
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

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
        modifier = Modifier.size(250.dp)
    ) {
        if (isListening) {
            val surgeGradient = Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    Color.Transparent
                )
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp) 
                    .graphicsLayer {
                        scaleX = waveScale
                        scaleY = waveScale
                        alpha = waveAlpha
                    }
                    .clip(CircleShape)
                    .background(surgeGradient)
            )
        }

        Surface(
            modifier = Modifier
                .size(96.dp)
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
