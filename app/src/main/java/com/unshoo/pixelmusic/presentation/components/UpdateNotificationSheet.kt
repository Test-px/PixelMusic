package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.unshoo.pixelmusic.presentation.components.subcomps.SineWaveLine
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotificationSheet(
    isUpdateAvailable: Boolean,
    versionName: String,
    changelog: String?,
    onDismiss: () -> Unit,
    onConfirmClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f) // Takes up a generous half of the screen
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Text Content Area (Scrollable for large release notes)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUpdateAvailable) "Update Available! 🚀" else "You're up to date! 🎉",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = AbsoluteSmoothCornerShape(12.dp, 60),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = changelog ?: "Bug fixes and performance improvements.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Animated Visual Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "floating_image")
                val floatOffset by infiniteTransition.animateFloat(
                    initialValue = -12f,
                    targetValue = 12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "float_offset"
                )

                // The floating welcome art
                MaterialYouVectorDrawable(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp) // Leave room for the wave
                        .graphicsLayer { translationY = floatOffset },
                    drawableResId = R.drawable.welcome_art
                )

                // Only show the animated Sine Wave if the update is completed (celebration vibe!)
                if (!isUpdateAvailable) {
                    SineWaveLine(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(32.dp)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 4.dp),
                        animate = true,
                        color = MaterialTheme.colorScheme.primary,
                        alpha = 0.95f,
                        strokeWidth = 4.dp,
                        amplitude = 4.dp,
                        waves = 7.6f,
                        phase = 0f
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Action Button
            Button(
                onClick = {
                    onConfirmClick()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = AbsoluteSmoothCornerShape(20.dp, 60)
            ) {
                Text(
                    text = if (isUpdateAvailable) "Update Now" else "Awesome!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
