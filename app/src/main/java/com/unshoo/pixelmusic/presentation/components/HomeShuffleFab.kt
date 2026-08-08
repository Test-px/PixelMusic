package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
    // We use a state to hold our current desired height
    var currentTargetOffset by remember { mutableStateOf(if (isPlayerActive) 60.dp else 30.dp) }

    // When isPlayerActive changes, we trigger this effect.
    LaunchedEffect(isPlayerActive) {
        if (isPlayerActive) {
            // If the player opens, jump up immediately!
            currentTargetOffset = 64.dp
        } else {
            // If the player is swiped away, the Undo bar appears.
            // Wait 4 seconds for the Undo bar to disappear before dropping!
            delay(4000) 
            currentTargetOffset = 24.dp
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

    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (isShuffleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = CircleShape,
        modifier = modifier
            .padding(bottom = animatedBottomOffset, end = 16.dp)
            .size(64.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_shuffle_24),
            contentDescription = stringResource(R.string.cd_shuffle_play),
            modifier = Modifier.size(32.dp)
        )
    }
}
