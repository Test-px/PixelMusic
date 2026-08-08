package com.unshoo.pixelmusic.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    // The component manages its own height internally now!
    // 135.dp clears the mini player, 90.dp clears just the nav pill.
    val bottomOffset = if (isPlayerActive) 135.dp else 90.dp

    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (isShuffleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = CircleShape,
        modifier = modifier.padding(bottom = bottomOffset, end = 24.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_shuffle_24),
            contentDescription = stringResource(R.string.cd_shuffle_play),
            modifier = Modifier.size(28.dp)
        )
    }
}
