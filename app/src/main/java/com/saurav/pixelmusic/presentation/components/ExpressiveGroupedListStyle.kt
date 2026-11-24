package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Material You Expressive grouped list card styling matching the Now Playing
 * action menu: rounded outer corners (24.dp), softly rounded inner corners (4.dp),
 * and tight 2.dp separation between adjacent items in a group.
 */
val ExpressiveGroupedCardSpacing = 2.dp

val ExpressiveSingleCardShape = RoundedCornerShape(24.dp)
val ExpressiveTopCardShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 4.dp,
    bottomEnd = 4.dp
)
val ExpressiveMiddleCardShape = RoundedCornerShape(4.dp)
val ExpressiveBottomCardShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 24.dp,
    bottomEnd = 24.dp
)

fun expressiveGroupedCardShape(index: Int, totalCount: Int): Shape {
    if (totalCount <= 1) return ExpressiveSingleCardShape
    return when (index) {
        0 -> ExpressiveTopCardShape
        totalCount - 1 -> ExpressiveBottomCardShape
        else -> ExpressiveMiddleCardShape
    }
}

/**
 * Smooth, draw-phase directional scroll-in transition for LazyColumn list items.
 * Animates alpha, subtle scale, and a directional vertical glide inside graphicsLayer
 * so scrolling stays fluid at 60/120fps without per-frame recomposition.
 */
fun Modifier.expressiveListItemScrollAnimation(
    listState: LazyListState? = null
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val enterDirection = remember {
        if (listState != null && listState.lastScrolledBackward) -1f else 1f
    }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = 320f,
                visibilityThreshold = 0.002f
            )
        )
    }
    graphicsLayer {
        val p = progress.value
        alpha = 0.25f + 0.75f * p
        val scale = 0.96f + 0.04f * p
        scaleX = scale
        scaleY = scale
        translationY = (1f - p) * 18.dp.toPx() * enterDirection
        transformOrigin = TransformOrigin(0.5f, 0.5f)
    }
}
