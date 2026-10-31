@file:OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.saurav.pixelmusic.presentation.components

import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import kotlin.math.max
import kotlin.math.min
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun PlaylistCreationTypeDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onManualSelected: () -> Unit,
    onAiSelected: () -> Unit,
    isAiEnabled: Boolean = true,
    onSetupAiClick: (() -> Unit)? = null
) {
    if (!visible) return

    val dialogShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 24.dp,
        smoothnessAsPercentTL = 60,
        cornerRadiusTR = 24.dp,
        smoothnessAsPercentTR = 60,
        cornerRadiusBL = 40.dp,
        smoothnessAsPercentBL = 60,
        cornerRadiusBR = 40.dp,
        smoothnessAsPercentBR = 60
    )

    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.02f)
            ) else listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }
    val sheenBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.05f else 0.10f),
                Color.Transparent
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = dialogShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = if (isLiquidGlass) 0.dp else 6.dp,
            shadowElevation = if (isLiquidGlass) 0.dp else 6.dp,
            modifier = Modifier.then(
                if (isLiquidGlass) {
                    Modifier
                        .graphicsLayer { shadowElevation = 0f }
                        .border(BorderStroke(0.75.dp, glassBorderBrush), dialogShape)
                        .clip(dialogShape)
                } else Modifier
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.presentation_batch_e_create_playlist_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = stringResource(R.string.presentation_batch_e_create_playlist_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                CreationModeCard(
                    title = stringResource(R.string.presentation_batch_e_creation_mode_manual),
                    subtitle = stringResource(R.string.presentation_batch_e_creation_mode_manual_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    onClick = onManualSelected,
                    enabled = true,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                CreationModeCard(
                    title = stringResource(R.string.presentation_batch_e_creation_mode_ai),
                    subtitle = if (isAiEnabled) {
                        stringResource(R.string.presentation_batch_e_creation_mode_ai_subtitle_enabled)
                    } else {
                        stringResource(R.string.presentation_batch_e_creation_mode_ai_subtitle_disabled)
                    },
                    icon = {
                        Icon(
                            imageVector = if (isAiEnabled) Icons.Rounded.AutoAwesome else Icons.Rounded.Key,
                            contentDescription = null,
                            tint = if (isAiEnabled) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    onClick = {
                        if (isAiEnabled) {
                            onAiSelected()
                        } else {
                            onDismiss()
                            onSetupAiClick?.invoke()
                        }
                    },
                    enabled = true,
                    containerColor = if (isAiEnabled) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (isAiEnabled) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (!isAiEnabled) {
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onSetupAiClick?.invoke()
                        },
                        elevation = if (isLiquidGlass) ButtonDefaults.filledTonalButtonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp) else ButtonDefaults.filledTonalButtonElevation(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isLiquidGlass) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.55f else 0.70f) else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassInteractiveSurface(shape = CircleShape),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.presentation_batch_e_setup_api_key))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreationModeCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color
) {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.02f)
            ) else listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }
    val sheenBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.05f else 0.10f),
                Color.Transparent
            )
        )
    }
    val cardShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 22.dp,
        smoothnessAsPercentTL = 60,
        cornerRadiusTR = 22.dp,
        smoothnessAsPercentTR = 60,
        cornerRadiusBL = 22.dp,
        smoothnessAsPercentBL = 60,
        cornerRadiusBR = 22.dp,
        smoothnessAsPercentBR = 60
    )

    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = if (isLiquidGlass) containerColor.copy(alpha = if (isDark) 0.55f else 0.70f) else containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiquidGlass) 0.dp else 2.dp),
        shape = cardShape,
        modifier = Modifier.liquidGlassInteractiveSurface(shape = cardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.16f),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 12.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusTR = 18.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBL = 18.dp,
                    smoothnessAsPercentBL = 60,
                    cornerRadiusBR = 12.dp,
                    smoothnessAsPercentBR = 60
                )
            ) {
                Row(modifier = Modifier.padding(10.dp)) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAiPlaylistDialog(
    visible: Boolean,
    isGenerating: Boolean,
    isSuccess: Boolean = false,
    status: String? = null,
    error: String?,
    onDismiss: () -> Unit,
    onGenerate: (playlistName: String?, prompt: String, minLength: Int, maxLength: Int) -> Unit
) {
    if (visible) {
        if (isGenerating || isSuccess) {
            // Full Screen Edge-to-Edge Generating Page
            AiPlaylistGeneratingFullScreenPage(
                isSuccess = isSuccess,
                status = status,
                error = error,
                onDismiss = onDismiss
            )
        } else {
            // Idle Prompt & Vibe Selection Bottom Sheet
            val sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            )

            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                },
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 28.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusTR = 28.dp,
                    smoothnessAsPercentTR = 60
                )
            ) {
                CreateAiPlaylistSheetContent(
                    error = error,
                    onDismiss = onDismiss,
                    onGenerate = onGenerate
                )
            }
        }
    }
}

@Composable
private fun AiPlaylistGeneratingFullScreenPage(
    isSuccess: Boolean,
    status: String?,
    error: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Minimize/Close button at top right
                AnimatedVisibility(
                    visible = !isSuccess,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Centered content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AiLibraryIndicatorsAnimation(
                        isGenerating = true,
                        isSuccess = isSuccess
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    AnimatedVisibility(
                        visible = !isSuccess,
                        enter = fadeIn(),
                        exit = fadeOut(tween(250))
                    ) {
                        AiPlaylistCreationAnimationView(
                            status = status,
                            isSuccess = isSuccess,
                            error = error,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAiPlaylistSheetContent(
    error: String?,
    onDismiss: () -> Unit,
    onGenerate: (playlistName: String?, prompt: String, minLength: Int, maxLength: Int) -> Unit
) {
    var basePrompt by rememberSaveable { mutableStateOf("") }
    var playlistName by rememberSaveable { mutableStateOf("") }
    var selectedLength by rememberSaveable { mutableStateOf("Auto") }
    var selectedEnergy by rememberSaveable { mutableStateOf("Auto") }
    var isRefining by rememberSaveable { mutableStateOf(false) }

    val triggerGeneration: () -> Unit = {
        if (basePrompt.isNotBlank()) {
            val builtPrompt = buildString {
                append(basePrompt.trim())
                if (selectedEnergy != "Auto") {
                    append(". Energy: $selectedEnergy.")
                }
                if (selectedLength != "Auto") {
                    append(". Length: $selectedLength tracks.")
                }
            }
            val (minCount, maxCount) = when (selectedLength) {
                "Up to 15" -> 10 to 15
                "Up to 25" -> 15 to 25
                "Up to 50" -> 25 to 50
                else -> 12 to 30
            }
            onGenerate(playlistName.trim().ifBlank { null }, builtPrompt, minCount, maxCount)
        }
    }

    AnimatedContent(
        targetState = isRefining,
        transitionSpec = {
            (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(240, easing = FastOutSlowInEasing))) togetherWith
                    (fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(180, easing = FastOutSlowInEasing)))
        },
        label = "ai_playlist_sheet_refine_state"
    ) { showRefine ->
        if (showRefine) {
            AiPlaylistRefineView(
                prompt = basePrompt,
                playlistName = playlistName,
                onPlaylistNameChange = { playlistName = it },
                selectedLength = selectedLength,
                onLengthChange = { selectedLength = it },
                selectedEnergy = selectedEnergy,
                onEnergyChange = { selectedEnergy = it },
                onBack = { isRefining = false },
                onDismiss = onDismiss
            )
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = 32.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row: Clean close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Persistently mounted library indicators in idle mode
                AiLibraryIndicatorsAnimation(isGenerating = false, isSuccess = false)

                Spacer(modifier = Modifier.height(20.dp))

                AiPlaylistMainTuneView(
                    prompt = basePrompt,
                    onPromptChange = { basePrompt = it },
                    selectedLength = selectedLength,
                    selectedEnergy = selectedEnergy,
                    error = error,
                    onOpenRefine = { isRefining = true },
                    onGenerate = triggerGeneration
                )
            }
        }
    }
}

@Composable
private fun AiPlaylistMainTuneView(
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedLength: String,
    selectedEnergy: String,
    error: String?,
    onOpenRefine: () -> Unit,
    onGenerate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Headline
        Text(
            text = "What do you want to hear?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Describe a vibe, scenario, or mood",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Prompt input container with dynamic app palette & Material You arrow button
        Surface(
            shape = AbsoluteSmoothCornerShape(28.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (prompt.isEmpty()) {
                        Text(
                            text = "Upbeat morning…",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                    BasicTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = false,
                        maxLines = 3
                    )
                }

                // Material You Expressive Action Button
                val canSubmit = prompt.isNotBlank()
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "arrow_btn_scale"
                )
                val buttonCorner by animateDpAsState(
                    targetValue = if (isPressed) 28.dp else 20.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "arrow_btn_corner"
                )

                Surface(
                    onClick = {
                        if (canSubmit) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGenerate()
                        }
                    },
                    enabled = canSubmit,
                    interactionSource = interactionSource,
                    shape = AbsoluteSmoothCornerShape(buttonCorner, 60),
                    color = if (canSubmit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (canSubmit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier
                        .size(width = 64.dp, height = 54.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Generate",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Horizontal scrollable line of thick Material You expressive shortcut pills
        ExpressiveQuickSelectRow(
            selectedPrompt = prompt,
            onPillClick = { vibe ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPromptChange(vibe)
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Refine Section Card following app theme palette
        Surface(
            shape = AbsoluteSmoothCornerShape(22.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(AbsoluteSmoothCornerShape(22.dp, 60))
                .clickable(onClick = onOpenRefine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = AbsoluteSmoothCornerShape(12.dp, 60)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Refine your mix",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RefineBadge(text = "Length: $selectedLength")
                        RefineBadge(text = "Energy: $selectedEnergy")
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Refine options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiPlaylistRefineView(
    prompt: String,
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    selectedLength: String,
    onLengthChange: (String) -> Unit,
    selectedEnergy: String,
    onEnergyChange: (String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = "Refine your mix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilledTonalIconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Prompt title echo
        Text(
            text = prompt.ifBlank { "Your mix" },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playlist length card (Full-filled chips, no borders)
        Surface(
            shape = AbsoluteSmoothCornerShape(22.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Playlist length",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Up to 15", "Up to 25", "Up to 50").forEach { option ->
                        ExpressiveChoiceChip(
                            label = option,
                            selected = selectedLength == option,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLengthChange(option)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Energy card (Full-filled chips, no borders)
        Surface(
            shape = AbsoluteSmoothCornerShape(22.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Energy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Calm", "Steady", "Energetic").forEach { option ->
                        ExpressiveChoiceChip(
                            label = option,
                            selected = selectedEnergy == option,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEnergyChange(option)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Optional custom playlist name
        Surface(
            shape = AbsoluteSmoothCornerShape(22.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom playlist title (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = onPlaylistNameChange,
                    placeholder = { Text("Auto-generated by AI if left blank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AbsoluteSmoothCornerShape(14.dp, 60)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Auto follows your request. Selecting energy adds a requirement; fewer songs may match.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Done button following Material You app palette with shape morphing touch feedback
        val doneInteractionSource = remember { MutableInteractionSource() }
        val isDonePressed by doneInteractionSource.collectIsPressedAsState()
        val doneScale by animateFloatAsState(
            targetValue = if (isDonePressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "done_scale"
        )
        val doneCorner by animateDpAsState(
            targetValue = if (isDonePressed) 16.dp else 26.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "done_corner"
        )

        Surface(
            onClick = onBack,
            interactionSource = doneInteractionSource,
            shape = AbsoluteSmoothCornerShape(doneCorner, 60),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .graphicsLayer {
                    scaleX = doneScale
                    scaleY = doneScale
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

internal const val SHAPE_STEP_COUNT = 60
internal val COS_TABLE = FloatArray(SHAPE_STEP_COUNT) { i ->
    cos((2.0 * PI * i) / SHAPE_STEP_COUNT).toFloat()
}
internal val SIN_TABLE = FloatArray(SHAPE_STEP_COUNT) { i ->
    sin((2.0 * PI * i) / SHAPE_STEP_COUNT).toFloat()
}

/**
 * Material You Expressive polar shapes with precomputed lookup tables.
 * Used for both the 4 large floating hero shapes and the 1:1 icon containers inside the shortcut pills.
 */
internal enum class AiExpressiveShapeType {
    PILL {
        override fun radiusAt(th: Double): Float {
            val c = abs(cos(th)) / 0.65
            val s = abs(sin(th))
            return (0.95 / (c.pow(4.0) + s.pow(4.0)).pow(0.25)).toFloat()
        }
    },
    COOKIE {
        override fun radiusAt(th: Double): Float = (0.85 + 0.15 * cos(8.0 * th)).toFloat()
    },
    FOUR_HEAD_COOKIE {
        override fun radiusAt(th: Double): Float = (0.82 + 0.18 * cos(4.0 * th)).toFloat()
    },
    ROUNDED_TRIANGLE {
        override fun radiusAt(th: Double): Float = (0.82 + 0.18 * cos(3.0 * th + PI / 2.0)).toFloat()
    },
    ROUNDED_SQUARE {
        override fun radiusAt(th: Double): Float {
            val c = abs(cos(th))
            val s = abs(sin(th))
            return (0.88 / (c.pow(4.0) + s.pow(4.0)).pow(0.25)).toFloat()
        }
    },
    CIRCLE {
        override fun radiusAt(th: Double): Float = 0.90f
    },
    SIX_COOKIE {
        override fun radiusAt(th: Double): Float = (0.84 + 0.16 * cos(6.0 * th)).toFloat()
    },
    SUNNY_COOKIE {
        override fun radiusAt(th: Double): Float = (0.86 + 0.14 * cos(12.0 * th)).toFloat()
    },
    FIVE_COOKIE {
        override fun radiusAt(th: Double): Float = (0.84 + 0.16 * cos(5.0 * th)).toFloat()
    },
    OVAL {
        override fun radiusAt(th: Double): Float {
            val c = abs(cos(th))
            val s = abs(sin(th))
            return (0.92 / (c * c + 0.55 * s * s).pow(0.5)).toFloat()
        }
    };

    abstract fun radiusAt(th: Double): Float

    val radii: FloatArray by lazy {
        FloatArray(SHAPE_STEP_COUNT) { i ->
            radiusAt((2.0 * PI * i) / SHAPE_STEP_COUNT)
        }
    }
}

private class AiMorphingExpressiveShape(
    private val from: AiExpressiveShapeType,
    private val to: AiExpressiveShapeType,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val clampedT = progress.coerceIn(0f, 1f)
        val fromRadii = from.radii
        val toRadii = to.radii
        val baseRadius = min(size.width, size.height) * 0.485f
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f

        val path = Path().apply {
            for (i in 0 until SHAPE_STEP_COUNT) {
                val r = (1f - clampedT) * fromRadii[i] + clampedT * toRadii[i]
                val radiusPx = baseRadius * r
                val x = centerX + radiusPx * COS_TABLE[i]
                val y = centerY + radiusPx * SIN_TABLE[i]
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun AiLibraryIndicatorsAnimation(
    isGenerating: Boolean,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "lib_indicators")

    // Slow floating translation phase when idle (cycle 6500ms)
    val idleFloatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_float_phase"
    )

    // Smooth dynamic movement when generating (cycle 2200ms)
    val genFloatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_float_phase"
    )

    // Gentle swaying tilt when idle (cycle 5000ms) - rocks smoothly, no fast spinning
    val idleSwayTilt by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_sway_tilt"
    )

    // Measured, smooth spinning when generating (cycle 4200ms)
    val genSpinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_spin_rotation"
    )

    // Zooming in/out pulse phase when generating (cycle 1100ms)
    val genScalePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_scale_phase"
    )

    // Subtle gentle breathing pulse phase when idle (cycle 3600ms)
    val idleScalePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_scale_phase"
    )

    // Base indicator size: 108dp when idle -> 124dp when generating (matches user's comparison in 260243.jpg)
    val baseIndicatorSize by animateDpAsState(
        targetValue = if (isGenerating) 124.dp else 108.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "base_indicator_size"
    )

    // Animate container height: 230dp when idle -> 290dp when generating
    val containerHeight by animateDpAsState(
        targetValue = if (isGenerating) 290.dp else 230.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "container_height"
    )

    // Horizontal spacing between indicators relative to the center
    val spacingPx = with(density) {
        if (isGenerating) 96.dp.toPx() else 84.dp.toPx()
    }

    // Success animation driver: handles convergence, edge-to-edge supernova expansion, and dissolve reveal
    val successProgress = remember { Animatable(0f) }
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            successProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1900, easing = LinearEasing)
            )
        } else {
            successProgress.snapTo(0f)
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val indicatorColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    // For glows: in dark mode, vibrant bright tones; in light mode, luminous pastel container tones
    val glowColors = listOf(
        if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer,
        if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight),
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until 3) {
            val offset = i * (6.2831855f / 3f)
            val color = indicatorColors[i % indicatorColors.size]
            val glowColor = glowColors[i % glowColors.size]

            val p = successProgress.value
            val isSuccessActive = isSuccess && p > 0f

            // Base position relative to exact center of screen
            val baseX = when (i) {
                0 -> -spacingPx
                2 -> spacingPx
                else -> 0f
            }

            // Convergence, Edge-to-Edge Expansion, and Dissolve Reveal math
            // Phase 1 (0f..0.42f): 3 pieces orbit and merge directly to center
            val convergeT = (p / 0.42f).coerceIn(0f, 1f)
            val convergeEase = FastOutSlowInEasing.transform(convergeT)

            // Phase 2 (0.42f..0.72f): Merged center component expands dramatically (up to 45x) covering the entire screen edge to edge
            val expandT = ((p - 0.42f) / 0.30f).coerceIn(0f, 1f)
            val expandCurve = FastOutLinearInEasing.transform(expandT)

            // Phase 3 (0.72f..1.0f): Portal smoothly dissolves/fades away, revealing the newly created playlist page
            val revealT = ((p - 0.72f) / 0.28f).coerceIn(0f, 1f)
            val portalAlpha = (1f - FastOutSlowInEasing.transform(revealT)).coerceIn(0f, 1f)

            // Dynamic scale: side components shrink smoothly into center as they converge
            val scale = when {
                isSuccessActive -> {
                    if (i == 1) {
                        // Center component swells smoothly as it absorbs side pieces (1.0x -> 1.4x),
                        // and then expands dramatically into the full edge-to-edge screen portal (up to 45x)
                        val convergeSwell = 1.0f + (convergeEase * 0.40f)
                        val portalExpansion = expandCurve * expandCurve * 45.0f
                        convergeSwell + portalExpansion
                    } else {
                        // Side components contract and dissolve seamlessly into the center (no clipping)
                        (1f - convergeEase).coerceAtLeast(0f)
                    }
                }
                isGenerating -> 1.06f + 0.14f * kotlin.math.sin((genScalePhase + offset).toDouble()).toFloat()
                else -> 1.0f + 0.035f * kotlin.math.sin((idleScalePhase + offset).toDouble()).toFloat()
            }

            // Fade multiplier for converging side pieces (applied directly to colors, avoiding layer clipping)
            val convergeFade = if (isSuccessActive && i != 1) (1f - convergeEase).coerceIn(0f, 1f) else 1f

            // Floating translations
            val idleFloatY = with(density) { 14.dp.toPx() } * kotlin.math.sin((idleFloatPhase + offset).toDouble()).toFloat()
            val idleFloatX = with(density) { 10.dp.toPx() } * kotlin.math.cos((idleFloatPhase + offset).toDouble()).toFloat()

            val genFloatY = with(density) { 16.dp.toPx() } * kotlin.math.sin((genFloatPhase + offset).toDouble()).toFloat()
            val genFloatX = with(density) { 10.dp.toPx() } * kotlin.math.cos((genFloatPhase + offset).toDouble()).toFloat()

            // Swirl / orbit offset during convergence
            val swirlAngle = convergeEase * 3.14159f * 1.5f
            val swirlRadius = with(density) { 24.dp.toPx() } * (1f - convergeEase)
            val swirlOffsetY = when (i) {
                0 -> -kotlin.math.sin(swirlAngle) * swirlRadius
                2 -> kotlin.math.sin(swirlAngle) * swirlRadius
                else -> kotlin.math.cos(swirlAngle) * (swirlRadius * 0.4f)
            }

            // Total translation X & Y:
            // When success is active, baseX gracefully interpolates to 0 (converging to center)
            val currentBaseX = if (isSuccessActive) baseX * (1f - convergeEase) else baseX
            val floatX = if (isSuccessActive) genFloatX * (1f - convergeEase) else (if (isGenerating) genFloatX else idleFloatX)
            val floatY = if (isSuccessActive) swirlOffsetY + (genFloatY * (1f - convergeEase)) else (if (isGenerating) genFloatY else idleFloatY)

            val transX = currentBaseX + floatX
            val transY = floatY

            // Rotation
            val rotZ = when {
                isSuccessActive -> {
                    (genSpinRotation * (if (i % 2 == 0) 1f else -1f)) + (convergeEase * 180f)
                }
                isGenerating -> genSpinRotation * (if (i % 2 == 0) 1f else -1f)
                else -> idleSwayTilt * (if (i % 2 == 0) 1f else -0.8f)
            }

            // Glow expansion scale for the merged component
            val glowExpansion = if (isSuccessActive && i == 1) {
                1f + (convergeEase * 0.5f) + (expandCurve * 12f)
            } else {
                1f
            }

            if (scale > 0.001f) {
                Box(
                    modifier = Modifier
                        .size(baseIndicatorSize)
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.translationX = transX
                            this.translationY = transY
                            this.rotationZ = rotZ
                            // In Phase 3, smoothly dissolve the expanded portal so the playlist page underneath is revealed
                            this.alpha = if (isSuccessActive && i == 1) portalAlpha else 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Soft depth shadow on the surface
                    val shadowAlpha = (if (isDark) 0.35f else 0.08f) * convergeFade
                    Box(
                        modifier = Modifier
                            .size(baseIndicatorSize * 0.95f)
                            .offset(y = if (isGenerating) 12.dp else 8.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (isDark) Color.Black.copy(alpha = shadowAlpha) else MaterialTheme.colorScheme.onSurface.copy(alpha = shadowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Layer 1: Ambient wide glow aura
                    Box(
                        modifier = Modifier
                            .size(baseIndicatorSize * (if (isGenerating) 2.20f else 1.80f) * glowExpansion)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = (if (isDark) 0.60f else 0.45f) * convergeFade),
                                        glowColor.copy(alpha = (if (isDark) 0.25f else 0.18f) * convergeFade),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Layer 2: Radiant core glow
                    Box(
                        modifier = Modifier
                            .size(baseIndicatorSize * (if (isGenerating) 1.45f else 1.25f) * glowExpansion)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = (if (isDark) 0.85f else 0.65f) * convergeFade),
                                        glowColor.copy(alpha = (if (isDark) 0.35f else 0.25f) * convergeFade),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    LoadingIndicator(
                        color = color.copy(alpha = convergeFade),
                        modifier = Modifier
                            .size(baseIndicatorSize)
                            .testTag("loading_indicator")
                    )
                }
            }
        }
    }
}

/**
 * Horizontal scrollable line of thick, expressive Material You shortcut pills.
 * Each pill has a left-side 1:1 Material You expressive shape (Cookie, 4-Head Cookie,
 * Circle, Rounded Triangle, 1:1 Rounded Square, etc.) housing an icon, right-side text,
 * and rich Material You shape-morphing touch feedback.
 */
private data class VibeItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val baseShape: AiExpressiveShapeType,
    val morphedShape: AiExpressiveShapeType
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpressiveQuickSelectRow(
    selectedPrompt: String,
    onPillClick: (String) -> Unit
) {
    val vibes = remember {
        listOf(
            VibeItem(
                label = "Feel good",
                icon = Icons.Rounded.Whatshot,
                baseShape = AiExpressiveShapeType.COOKIE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            VibeItem(
                label = "Upbeat morning",
                icon = Icons.Rounded.WbSunny,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.SUNNY_COOKIE
            ),
            VibeItem(
                label = "Workout energy",
                icon = Icons.Rounded.FitnessCenter,
                baseShape = AiExpressiveShapeType.ROUNDED_TRIANGLE,
                morphedShape = AiExpressiveShapeType.SIX_COOKIE
            ),
            VibeItem(
                label = "Chill evening",
                icon = Icons.Rounded.NightsStay,
                baseShape = AiExpressiveShapeType.CIRCLE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            VibeItem(
                label = "Late night drive",
                icon = Icons.Rounded.DirectionsCar,
                baseShape = AiExpressiveShapeType.ROUNDED_SQUARE,
                morphedShape = AiExpressiveShapeType.COOKIE
            ),
            VibeItem(
                label = "Music to study",
                icon = Icons.Rounded.Headphones,
                baseShape = AiExpressiveShapeType.SIX_COOKIE,
                morphedShape = AiExpressiveShapeType.ROUNDED_TRIANGLE
            ),
            VibeItem(
                label = "Party hype",
                icon = Icons.Rounded.Celebration,
                baseShape = AiExpressiveShapeType.SUNNY_COOKIE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            VibeItem(
                label = "Acoustic coffee",
                icon = Icons.Rounded.MusicNote,
                baseShape = AiExpressiveShapeType.COOKIE,
                morphedShape = AiExpressiveShapeType.ROUNDED_SQUARE
            ),
            VibeItem(
                label = "Nostalgic 2000s",
                icon = Icons.Rounded.History,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.CIRCLE
            ),
            VibeItem(
                label = "Piano peaceful",
                icon = Icons.Rounded.Piano,
                baseShape = AiExpressiveShapeType.ROUNDED_SQUARE,
                morphedShape = AiExpressiveShapeType.SIX_COOKIE
            ),
            VibeItem(
                label = "Deep focus",
                icon = Icons.Rounded.Star,
                baseShape = AiExpressiveShapeType.ROUNDED_TRIANGLE,
                morphedShape = AiExpressiveShapeType.COOKIE
            ),
            VibeItem(
                label = "Radio hits",
                icon = Icons.Rounded.GraphicEq,
                baseShape = AiExpressiveShapeType.CIRCLE,
                morphedShape = AiExpressiveShapeType.SUNNY_COOKIE
            )
        )
    }

    LazyHorizontalGrid(
        rows = GridCells.Fixed(3), 
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp), // Safely fits 3 rows of 48dp pills + spacing
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(vibes) { index, vibe ->
            val isSelected = selectedPrompt.trim().equals(vibe.label, ignoreCase = true)
            ExpressiveQuickSelectPill(
                vibe = vibe,
                index = index,
                isSelected = isSelected,
                onClick = { onPillClick(vibe.label) },
                modifier = Modifier.width(185.dp) // Locks pill width so they stay uniform while sliding horizontally
            )
        }
    }
}

@Composable
private fun ExpressiveQuickSelectPill(
    vibe: VibeItem,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_scale"
    )

    // Stadium pill idle (28.dp radius for 58.dp height), morphing to an expressive asymmetrical shape on press/select
    val cornerTL by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 14.dp else 28.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_ctl"
    )
    val cornerBR by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 14.dp else 28.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_cbr"
    )
    val cornerTR by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 28.dp else 14.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_ctr"
    )
    val cornerBL by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 28.dp else 14.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_cbl"
    )

    val pillShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = cornerTL,
        smoothnessAsPercentTL = 60,
        cornerRadiusTR = cornerTR,
        smoothnessAsPercentTR = 60,
        cornerRadiusBL = cornerBL,
        smoothnessAsPercentBL = 60,
        cornerRadiusBR = cornerBR,
        smoothnessAsPercentBR = 60
    )

    val morphFraction by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "icon_morph"
    )

    val iconExpressiveShape = remember(vibe.baseShape, vibe.morphedShape, morphFraction) {
        AiMorphingExpressiveShape(
            from = vibe.baseShape,
            to = vibe.morphedShape,
            progress = morphFraction
        )
    }

    val isHighlighted = isPressed || isSelected

    val containerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(150),
        label = "pill_color"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = tween(150),
        label = "badge_color"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = tween(150),
        label = "icon_tint"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = pillShape,
        color = containerColor,
        shadowElevation = if (isHighlighted) 3.dp else 1.dp,
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side 1:1 Material You expressive shape
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        rotationZ = morphFraction * (if (index % 2 == 0) 25f else -25f)
                    }
                    .clip(iconExpressiveShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vibe.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right side bold text label
            Text(
                text = vibe.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 3.dp)
            )
        }
    }
}

/**
 * Full-filled choice chip (no borders) with shape morphing & spring touch feedback.
 */
@Composable
private fun ExpressiveChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chip_scale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 22.dp else if (isPressed) 18.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "chip_corner"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "chip_color"
    )

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val chipShape = AbsoluteSmoothCornerShape(cornerRadius, 60)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = chipShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun RefineBadge(text: String) {
    Surface(
        shape = AbsoluteSmoothCornerShape(10.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AiPlaylistCreationAnimationView(
    status: String?,
    isSuccess: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    val phases = listOf(
        "Tuning to your taste & listening patterns…",
        "Scanning local & cloud tracks for matching vibes…",
        "Discovering fresh recommendations & flow…",
        "Harmonizing energy transitions & playlist order…",
        "Synthesizing smart titles & final touches…"
    )
    var phaseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isSuccess, error) {
        if (!isSuccess && error == null) {
            while (true) {
                kotlinx.coroutines.delay(2200)
                phaseIndex = (phaseIndex + 1) % phases.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Headline (the 4 floating Material You expressive shapes remain mounted right above this view)
        Text(
            text = if (isSuccess) "Mix synthesized!" else "Creating your AI Playlist…",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Animated stage subtitle
        AnimatedContent(
            targetState = if (isSuccess) {
                "Opening your new playlist now…"
            } else {
                status ?: phases[phaseIndex]
            },
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "ai_generation_phase_text"
        ) { currentText ->
            Text(
                text = currentText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Animated Equalizer Visualizer Bars
        if (!isSuccess && error == null) {
            AnimatedEqualizerBars()
        }

        // If error occurred
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        listOf(0.0f, 1.2f, 2.4f, 3.6f, 4.8f).forEach { offset ->
            val heightFraction = 0.3f + 0.7f * ((kotlin.math.sin(phase + offset) + 1f) / 2f)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((26 * heightFraction).dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(primary, tertiary)
                        )
                    )
            )
        }
    }
}
