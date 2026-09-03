package com.unshoo.pixelmusic.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun MusicRecognitionOverlay(
    onDismiss: () -> Unit,
    onPlayMusic: (RecognitionResult) -> Unit
) {
    var status by remember { mutableStateOf<RecognitionStatus>(RecognitionStatus.Ready) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val screenCornerRadius = remember(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val insets = view.rootWindowInsets
            val radiusPx = insets?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
            if (radiusPx > 0) (radiusPx / view.resources.displayMetrics.density).dp else 36.dp
        } else {
            36.dp
        }
    }

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

    LaunchedEffect(Unit) {
        if (status is RecognitionStatus.Ready) {
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

    val rootShaderModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier
            .recognitionRippleEffect(isTriggered = isListening)
            .successSweepEffect(isTriggered = isSuccess)
    } else {
        Modifier
    }

    val overlayBackground = Color.Black.copy(alpha = 0.78f)
    val textColor = Color.White
    val subTextColor = Color.White.copy(alpha = 0.70f)

    val offscreenStartY = with(density) { -(screenHeight.toPx() + 450.dp.toPx()) }
    val cardDropOffsetY = remember { Animatable(offscreenStartY) }

    LaunchedEffect(status) {
        if (status is RecognitionStatus.Success) {
            cardDropOffsetY.snapTo(offscreenStartY)
            cardDropOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 340f
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(rootShaderModifier)
            .clip(RoundedCornerShape(screenCornerRadius))
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
                .padding(start = 16.dp, top = 44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = textColor
            )
        }

        when (val currentStatus = status) {
            is RecognitionStatus.Success -> {
                val song = currentStatus.result
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(0, cardDropOffsetY.value.roundToInt()) }
                        .width(330.dp)
                        .heightIn(min = 520.dp)
                        .padding(horizontal = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = AbsoluteSmoothCornerShape(32.dp, 60),
                    color = Color(0xFF1E1E1E),
                    tonalElevation = 8.dp,
                    shadowElevation = 18.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        AsyncImage(
                            model = song.coverArtHqUrl ?: song.coverArtUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 210.dp, height = 265.dp)
                                .clip(AbsoluteSmoothCornerShape(22.dp, 60))
                                .background(Color(0xFF2C2C2C))
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
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

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = { onPlayMusic(song) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Play on PixelMusic",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
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
