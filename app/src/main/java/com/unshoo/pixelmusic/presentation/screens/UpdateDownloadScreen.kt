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
import androidx.compose.ui.text.style.TextAlign
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

    // MAGIC FIX: We map the state to a string so AnimatedContent ONLY fires
    // when the phase actually changes (Idle -> Downloading -> Finished), NOT on every 1% tick.
    val uiPhase by remember(downloadState) {
        derivedStateOf {
            when (downloadState) {
                is InAppUpdater.GlobalDownloadState.Idle -> "IDLE"
                is InAppUpdater.GlobalDownloadState.Downloading -> "DOWNLOADING"
                is InAppUpdater.GlobalDownloadState.Finished -> "FINISHED"
                is InAppUpdater.GlobalDownloadState.Error -> "ERROR"
            }
        }
    }

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
                targetState = uiPhase,
                transitionSpec = {
                    fadeIn(tween(400)) + scaleIn(initialScale = 0.9f) togetherWith fadeOut(tween(400)) + scaleOut(targetScale = 0.9f)
                },
                label = "Download UI State"
            ) { phase ->
                when (phase) {
                    "DOWNLOADING" -> {
                        val state = downloadState as? InAppUpdater.GlobalDownloadState.Downloading
                        DownloadingView(
                            progress = state?.progress ?: 0f,
                            isPaused = state?.isPaused ?: false,
                            versionName = state?.versionName ?: "",
                            onPauseResume = {
                                if (state?.isPaused == true) {
                                    InAppUpdater.resumeDownload(context) 
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
                    "FINISHED" -> {
                        val state = downloadState as? InAppUpdater.GlobalDownloadState.Finished
                        FinishedView(
                            versionName = state?.versionName ?: "",
                            onInstall = { InAppUpdater.installApk(context, state!!.apkFile) },
                            onDelete = {
                                InAppUpdater.deleteApk(context)
                                navController.popBackStack()
                            }
                        )
                    }
                    "ERROR" -> {
                        val state = downloadState as? InAppUpdater.GlobalDownloadState.Error
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Text("Download Failed", style = MaterialTheme.typography.headlineMedium)
                            Text(state?.message ?: "Unknown error", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                        }
                    }
                    else -> {
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
        
        // NEW WARNING TEXT
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = AbsoluteSmoothCornerShape(12.dp, 60)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Please do not close the app during the update.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

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
