package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unshoo.pixelmusic.data.model.Lyrics
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun KaraokeLyricsView(
    lyrics: Lyrics?,
    playbackPositionFlow: StateFlow<Long>,
    syncOffsetMs: Int,
    modifier: Modifier = Modifier
) {
    val syncedLines = lyrics?.synced ?: emptyList()
    if (syncedLines.isEmpty()) {
        Spacer(modifier = modifier)
        return
    }

    val currentPositionMs by playbackPositionFlow.collectAsStateWithLifecycle()
    val effectivePosition = currentPositionMs + syncOffsetMs

    val currentIndex = remember(effectivePosition, syncedLines) {
        val index = syncedLines.indexOfLast { it.time <= effectivePosition }
        if (index >= 0) index else 0
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentIndex) {
        // Keeps the active line slightly below the top edge
        val targetIndex = maxOf(0, currentIndex - 1)
        coroutineScope.launch {
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Creates the seamless fade-out effect at the top and bottom of the lyrics view
    val topBottomFade = Brush.verticalGradient(
        0f to Color.Transparent,
        0.15f to Color.Black,
        0.85f to Color.Black,
        1f to Color.Transparent
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(brush = topBottomFade, blendMode = BlendMode.DstIn)
            },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(syncedLines) { index, line ->
            val isActive = index == currentIndex
            val isPast = index < currentIndex

            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPast) 0.3f else 0.5f,
                animationSpec = tween(300),
                label = "alpha"
            )

            Text(
                text = line.line,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    lineHeight = 36.sp
                ),
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

