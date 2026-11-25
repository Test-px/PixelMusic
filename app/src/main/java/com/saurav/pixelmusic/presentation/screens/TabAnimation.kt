package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

private val TabSmoothMorphEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun TabAnimation(
    modifier: Modifier = Modifier,
    index: Int,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    onSelectedColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.surface,
    onUnselectedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
    title: String,
    selectedIndex: Int,
    onClick: () -> Unit,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isSelected = index == selectedIndex
    val tabShape = CircleShape
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 140, easing = TabSmoothMorphEasing),
        label = "Tab Container Color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) onSelectedColor else onUnselectedColor,
        animationSpec = tween(durationMillis = 140, easing = TabSmoothMorphEasing),
        label = "Tab Content Color"
    )

    // Synchronized neighbor-push pulse across all tabs whenever selectedIndex changes:
    // the selected pill expands symmetrically from center while pushing both left and right neighbors,
    // then all pills smoothly fall back to their default shape and position.
    val pushPulse = remember { Animatable(0f) }
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        if (!hasInitialized) {
            hasInitialized = true
            return@LaunchedEffect
        }
        pushPulse.snapTo(0f)
        pushPulse.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 120, easing = TabSmoothMorphEasing)
        )
        pushPulse.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 260, easing = TabSmoothMorphEasing)
        )
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Tab Press Scale"
    )

    Tab(
        modifier = modifier
            .padding(all = 4.dp)
            .graphicsLayer {
                val pulse = pushPulse.value
                val sX = (if (isSelected) 1f + 0.14f * pulse else 1f) * pressScale
                val sY = (if (isSelected) 1f + 0.05f * pulse else 1f) * pressScale
                scaleX = sX
                scaleY = sY
                this.transformOrigin = transformOrigin
                translationX = when {
                    isSelected -> 0f
                    index >= 0 && index < selectedIndex -> {
                        val dpShift = if (index == selectedIndex - 1) -10.dp else -5.dp
                        dpShift.toPx() * pulse
                    }
                    else -> {
                        val dpShift = if (index == selectedIndex + 1) 10.dp else 5.dp
                        dpShift.toPx() * pulse
                    }
                }
            }
            .clip(tabShape)
            .background(containerColor, tabShape),
        selected = isSelected,
        interactionSource = interactionSource,
        text = content,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        selectedContentColor = contentColor,
        unselectedContentColor = contentColor
    )
}
