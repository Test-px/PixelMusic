package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
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
 * Position-driven, draw-phase scroll transition for LazyColumn list items.
 * Reveals items visibly right above the bottom navigation bar / mini player
 * and softly scales & fades items as they pass the top edge, tracking scroll
 * smoothly in both directions without per-frame recomposition.
 */
fun Modifier.expressiveListItemScrollAnimation(
    @Suppress("UNUSED_PARAMETER") listState: LazyListState? = null
): Modifier = composed {
    val itemTopPx = remember { mutableFloatStateOf(0f) }
    val itemHeightPx = remember { mutableFloatStateOf(0f) }
    val viewportHeightPx = remember { mutableFloatStateOf(0f) }

    this
        .onGloballyPositioned { coordinates ->
            val parent = coordinates.parentLayoutCoordinates
            if (parent != null) {
                itemTopPx.floatValue = parent.localPositionOf(coordinates, Offset.Zero).y
                itemHeightPx.floatValue = coordinates.size.height.toFloat()
                viewportHeightPx.floatValue = parent.size.height.toFloat()
            }
        }
        .graphicsLayer {
            val vh = viewportHeightPx.floatValue
            val ih = itemHeightPx.floatValue
            if (vh <= 0f || ih <= 0f) return@graphicsLayer

            val topY = itemTopPx.floatValue
            val centerY = topY + ih * 0.5f

            // Start the bottom reveal right above the mini player + navigation bar chrome
            val bottomChromePx = 148.dp.toPx()
            val bottomZonePx = 120.dp.toPx()
            val bottomEdgeY = (vh - bottomChromePx).coerceAtLeast(ih)

            val rawBottom = ((bottomEdgeY - centerY) / bottomZonePx).coerceIn(0f, 1f)
            val bottomProgress = rawBottom * rawBottom * (3f - 2f * rawBottom)

            // Top exit/re-entry triggers cleanly as an item crosses the top viewport edge
            val rawTop = if (topY < 0f) {
                (1f + topY / ih.coerceAtLeast(1f)).coerceIn(0f, 1f)
            } else {
                1f
            }
            val topProgress = rawTop * rawTop * (3f - 2f * rawTop)

            if (bottomProgress < 1f) {
                val scale = 0.88f + 0.12f * bottomProgress
                alpha = 0.25f + 0.75f * bottomProgress
                scaleX = scale
                scaleY = scale
                translationY = (1f - bottomProgress) * 22.dp.toPx()
                transformOrigin = TransformOrigin(0.5f, 0.25f)
            } else if (topProgress < 1f) {
                val scale = 0.90f + 0.10f * topProgress
                alpha = 0.35f + 0.65f * topProgress
                scaleX = scale
                scaleY = scale
                translationY = -(1f - topProgress) * 14.dp.toPx()
                transformOrigin = TransformOrigin(0.5f, 0.75f)
            } else {
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                translationY = 0f
            }
        }
}
