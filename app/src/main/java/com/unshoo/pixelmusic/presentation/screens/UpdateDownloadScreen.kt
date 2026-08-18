package com.unshoo.pixelmusic.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.unshoo.pixelmusic.utils.InAppUpdater
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDownloadScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val downloadState by InAppUpdater.downloadState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = downloadState,
                transitionSpec = {
                    fadeIn(tween(400)) + scaleIn(initialScale = 0.9f) togetherWith fadeOut(tween(400)) + scaleOut(targetScale = 0.9f)
                },
                label = "Download UI State"
            ) { state ->
                when (state) {
                    is InAppUpdater.GlobalDownloadState.Downloading -> {
                        DownloadingView(
                            progress = state.progress,
                            isPaused = state.isPaused,
                            versionName = state.versionName,
                            onPauseResume = {
                                if (state.isPaused) {
                                    // Fetch the URL from wherever you stored it, or pass it via navigation if preferred
                                    // InAppUpdater already knows the URL from the initial call!
                                    InAppUpdater.startOrResumeDownload(context, "", state.versionName) 
                                } else {
                                    InAppUpdater.pauseDownload()
                                }
                            },
                            onCancel = {
                                InAppUpdater.cancelDownload(context)
                                navController.popBackStack()
                            }
                        )
                    }
                    is InAppUpdater.GlobalDownloadState.Finished -> {
                        FinishedView(
                            versionName = state.versionName,
                            onInstall = { InAppUpdater.installApk(context, state.apkFile) },
                            onDelete = {
                                InAppUpdater.deleteApk(context)
                                navController.popBackStack()
                            }
                        )
                    }
                    is InAppUpdater.GlobalDownloadState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Text("Download Failed", style = MaterialTheme.typography.headlineMedium)
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        // Failsafe: if the user opens this screen manually without an active download
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No active download", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingView(
    progress: Float,
    isPaused: Boolean,
    versionName: String,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Massive expressive percentage
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 84.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isPaused) "Paused" else "Downloading $versionName...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Rich custom progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(AbsoluteSmoothCornerShape(12.dp, 60))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.coerceAtLeast(0.02f))
                    .clip(AbsoluteSmoothCornerShape(12.dp, 60))
                    .background(if (isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cancel")
            }

            Button(
                onClick = onPauseResume,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPaused) "Resume" else "Pause")
            }
        }
    }
}

@Composable
private fun FinishedView(
    versionName: String,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                Icons.Rounded.CheckCircle, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Download Complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Version $versionName is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onInstall,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = AbsoluteSmoothCornerShape(20.dp, 60)
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Install Update", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete APK file")
        }
    }
}

