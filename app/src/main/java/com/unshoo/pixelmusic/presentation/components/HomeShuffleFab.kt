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
import com.unshoo.pixelmusic.R
import kotlinx.coroutines.delay

@Composable
fun HomeShuffleFab(
    isShuffleEnabled: Boolean,
    isPlayerActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // Determine Navigation Bar State based on height
    val navState = when {
        systemNavBarInset > 35.dp -> 2 // 3-Button Navigation (usually ~48dp)
        systemNavBarInset > 5.dp -> 1  // Gesture Navigation (usually ~16dp)
        else -> 0                      // Hidden / Fullscreen (0dp)
    }

    // =========================================================================================
    // BUTTON HEIGHT CONFIGURATION (3 Separate Positions)
    // =========================================================================================
    
    // Manage the height of the button when the Mini-Player is OPEN
    val activeHeight = when (navState) {
        2 -> 106.dp   // <-- Player OPEN & 3-Button Nav
        1 -> 74.dp    // <-- Player OPEN & Gesture Nav
        else -> 55.dp // <-- Player OPEN & Nav Hidden
    }

    // Manage the height of the button when the Mini-Player is CLOSED
    val inactiveHeight = when (navState) {
        2 -> 64.dp    // <-- Player CLOSED & 3-Button Nav
        1 -> 24.dp    // <-- Player CLOSED & Gesture Nav
        else -> 0.dp  // <-- Player CLOSED & Nav Hidden
    }
    
    // =========================================================================================

    // Only track the player's active state for the delay. 
    // Keying ONLY on `isPlayerActive` ensures the 4-second delay won't restart 
    // if the system navigation bar hides or shows dynamically.
    var isPlayerActiveDelayed by remember { mutableStateOf(isPlayerActive) }

    LaunchedEffect(isPlayerActive) {
        if (isPlayerActive) {
            isPlayerActiveDelayed = true
        } else {
            delay(4000) 
            isPlayerActiveDelayed = false
        }
    }

    // Apply the correct height based on the delayed player state
    val targetOffset = if (isPlayerActiveDelayed) activeHeight else inactiveHeight

    val animatedBottomOffset by animateDpAsState(
        targetValue = targetOffset,
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
