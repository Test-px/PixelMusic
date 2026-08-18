package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.unshoo.pixelmusic.R

@Composable
fun HomeShuffleFab(
    isShuffleEnabled: Boolean,
    isPlayerActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isGestureBarVisible = systemNavBarInset > 10.dp 
    
    // =========================================================================================
    // BUTTON HEIGHT CONFIGURATION
    // =========================================================================================
    
    // Increase or decrease these values to manage the height of the button when the Mini-Player is OPEN
    val activeHeight = if (isGestureBarVisible) {
        74.dp // <-- Player OPEN & Gesture Bar SHOWN
    } else {
        50.dp // <-- Player OPEN & Gesture Bar HIDDEN
    }

    // Increase or decrease these values to manage the height of the button when the Mini-Player is CLOSED
    val inactiveHeight = if (isGestureBarVisible) {
        24.dp // <-- Player CLOSED & Gesture Bar SHOWN
    } else {
        0.dp  // <-- Player CLOSED & Gesture Bar HIDDEN
    }
    
    // =========================================================================================

    var currentTargetOffset by remember(isGestureBarVisible) { 
        mutableStateOf(if (isPlayerActive) activeHeight else inactiveHeight) 
    }

    LaunchedEffect(isPlayerActive, isGestureBarVisible) {
        if (isPlayerActive) {
            currentTargetOffset = activeHeight
        } else {
            delay(4000) 
            currentTargetOffset = inactiveHeight
        }
    }

    val animatedBottomOffset by animateDpAsState(
        targetValue = currentTargetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabBottomOffset"
    )
    
    val dynamicHorizontalPadding = if (systemNavBarInset > 30.dp) 14.dp else systemNavBarInset
    val dynamicEndPadding = 16.dp + dynamicHorizontalPadding

    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (isShuffleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = CircleShape,
        modifier = modifier
            .padding(bottom = animatedBottomOffset.coerceAtLeast(0.dp), end = dynamicEndPadding)
            .size(64.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_shuffle_24),
            contentDescription = stringResource(R.string.cd_shuffle_play),
            modifier = Modifier.size(32.dp)
        )
    }
}
