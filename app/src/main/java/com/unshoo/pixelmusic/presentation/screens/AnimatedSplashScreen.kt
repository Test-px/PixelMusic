package com.unshoo.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val circleProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Logo Pop & Fade
        launch {
            alpha.animateTo(1f, tween(300))
        }
        launch {
            scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        }

        // 2. Logo Rotation Twist (0 -> 35 degrees -> 0)
        launch {
            delay(100) // Wait just a split second so it pops up before twisting
            rotation.animateTo(35f, tween(250, easing = FastOutSlowInEasing))
            rotation.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }

        // 3. Background Circles expanding and flying outward to the edges
        launch {
            circleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
        
        // Wait for all animations to settle
        delay(1200L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        
        // Top-Right Flying Circle
        Box(
            modifier = Modifier
                .offset(
                    x = (circleProgress.value * 250).dp,
                    y = (circleProgress.value * -350).dp
                )
                .size(300.dp)
                .scale(0.2f + (circleProgress.value * 1.5f)) // Grow from small to large
                .alpha(1f - (circleProgress.value * 0.4f)) // Fade out slightly as it moves
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        )

        // Bottom-Left Flying Circle
        Box(
            modifier = Modifier
                .offset(
                    x = (circleProgress.value * -250).dp,
                    y = (circleProgress.value * 350).dp
                )
                .size(300.dp)
                .scale(0.2f + (circleProgress.value * 1.5f)) // Grow from small to large
                .alpha(1f - (circleProgress.value * 0.4f)) // Fade out slightly as it moves
                .clip(CircleShape)
                // Use secondary container for a subtle gradient-like contrast
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) 
        )

        // Main App Logo
        Icon(
            painter = painterResource(id = R.drawable.pixelmusic_base_monochrome),
            contentDescription = "App Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(110.dp)
                .scale(scale.value)
                .rotate(rotation.value)
                .alpha(alpha.value)
        )
    }
}
