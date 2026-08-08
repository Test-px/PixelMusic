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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.R

@Composable
fun HomeShuffleFab(
    isShuffleEnabled: Boolean,
    isPlayerActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth spring animation for the vertical movement!
    // It will bounce smoothly between 96.dp and 76.dp when the player opens/closes.
    val animatedBottomOffset by animateDpAsState(
        targetValue = if (isPlayerActive) 60.dp else 40.dp,
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
        // Using the animated offset here instead of the static one
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
