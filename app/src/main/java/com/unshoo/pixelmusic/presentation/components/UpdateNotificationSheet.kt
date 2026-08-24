package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.R
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
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f) // Dynamically takes up ~half the screen
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Text Content Area (Scrollable)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = AbsoluteSmoothCornerShape(12.dp, 60),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = changelog ?: "Bug fixes and performance improvements.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Animated Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "floating_image")
                val floatOffset by infiniteTransition.animateFloat(
                    initialValue = -8f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "float_offset"
                )

                // Swap these drawables with your exact welcome screen illustration names!
                val imageRes = if (isUpdateAvailable) {
                    R.drawable.ic_launcher_foreground // Replace with your "Before Update" visual
                } else {
                    R.drawable.ic_launcher_foreground // Replace with your "After Update" visual
                }

                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            translationY = floatOffset
                        }
                )
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
                    text = if (isUpdateAvailable) "Update Now" else "Awesome",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

