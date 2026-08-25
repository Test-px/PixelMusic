package com.unshoo.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    LaunchedEffect(Unit) {
        // 1. YouTube Music style: Quick fade in
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300)
            )
        }
        // 2. Bouncy scale "pop" effect
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f, 
                stiffness = Spring.StiffnessLow
            )
        )
        
        // 3. Hold the logo on screen for a moment
        delay(900L)
        
        // 4. Trigger transition to fade the main app in
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.pixelmusic_base_monochrome),
            contentDescription = "App Logo",
            tint = MaterialTheme.colorScheme.primary, // Adapts to their dynamic theme!
            modifier = Modifier
                .size(110.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}

