package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unshoo.pixelmusic.data.model.Lyrics
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ImmersiveSingleLineLyrics(
    lyrics: Lyrics?,
    playbackPositionFlow: StateFlow<Long>,
    syncOffsetMs: Int,
    modifier: Modifier = Modifier
) {
    val syncedLines = lyrics?.synced ?: emptyList()
    if (syncedLines.isEmpty()) {
        return
    }

    val currentPositionMs by playbackPositionFlow.collectAsStateWithLifecycle()
    val effectivePosition = currentPositionMs + syncOffsetMs

    val currentLine = remember(effectivePosition, syncedLines) {
        val index = syncedLines.indexOfLast { it.time <= effectivePosition }
        if (index >= 0) syncedLines[index].line else ""
    }

    // Only render the text if the line isn't blank (hides it during instrumental breaks)
    if (currentLine.isNotBlank()) {
        AnimatedContent(
            targetState = currentLine,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + slideInVertically { it / 2 }) togetherWith
                (fadeOut(animationSpec = tween(300)) + slideOutVertically { -it / 2 })
            },
            label = "lyrics_crossfade",
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        // Keeps the vertical space from collapsing during instrumental breaks
        Spacer(modifier = Modifier.height(30.dp))
    }
}

