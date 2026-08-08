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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.R

@Composable
fun HomeShuffleFab(
    isShuffleEnabled: Boolean,
    bottomOffset: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (isShuffleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = CircleShape,
        // The padding is passed from the outside, giving us exact control
        modifier = modifier.padding(bottom = bottomOffset, end = 24.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_shuffle_24),
            contentDescription = stringResource(R.string.cd_shuffle_play),
            modifier = Modifier.size(28.dp)
        )
    }
}

