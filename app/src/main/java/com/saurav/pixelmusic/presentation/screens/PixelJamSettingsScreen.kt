package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.session.PixelJamConfig
import com.saurav.pixelmusic.data.session.PixelJamManager
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PixelJamSettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val pixelJamManager: PixelJamManager
) : ViewModel() {

    object Keys {
        val KEEP_ALIVE = booleanPreferencesKey("pixeljam_keep_alive")
    }

    val serverUrl: StateFlow<String> = pixelJamManager.serverUrl

    val displayName: StateFlow<String> = dataStore.data
        .map { it[PixelJamManager.KEY_DISPLAY_NAME] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val keepAlive: StateFlow<Boolean> = dataStore.data
        .map { it[Keys.KEEP_ALIVE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setServerUrl(url: String) {
        viewModelScope.launch { pixelJamManager.setServerUrl(url) }
    }

    fun resetServerUrl() {
        viewModelScope.launch { pixelJamManager.setServerUrl(PixelJamConfig.DEFAULT_SERVER_URL) }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch {
            dataStore.edit { it[PixelJamManager.KEY_DISPLAY_NAME] = name.trim().take(32) }
        }
    }

    fun setKeepAlive(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[Keys.KEEP_ALIVE] = value } }
    }
}

@Composable
fun PixelJamSettingsScreen(
    navController: NavController,
    vm: PixelJamSettingsViewModel = hiltViewModel()
) {
    val serverUrl by vm.serverUrl.collectAsStateWithLifecycle()
    val displayName by vm.displayName.collectAsStateWithLifecycle()
    val keepAlive by vm.keepAlive.collectAsStateWithLifecycle()

    var showServerDialog by remember { mutableStateOf(false) }
    var serverDraft by remember(serverUrl) { mutableStateOf(serverUrl) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameDraft by remember(displayName) { mutableStateOf(displayName) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0
                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }
                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }
                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("PixelJam Server URL") },
            text = {
                OutlinedTextField(
                    value = serverDraft,
                    onValueChange = { serverDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Render / WebSocket Host") },
                    placeholder = { Text(PixelJamConfig.DEFAULT_SERVER_URL) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setServerUrl(serverDraft)
                    showServerDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.resetServerUrl()
                    showServerDialog = false
                }) { Text("Reset Default") }
            }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Default Display Name") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Your name in PixelJam rooms") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDisplayName(nameDraft)
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollMotionBlur(lazyListState),
            contentPadding = PaddingValues(top = currentTopBarHeightDp + 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(
                    title = "PixelJam Profile",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_group_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SettingsItem(
                            title = "Display name",
                            subtitle = displayName.ifBlank { "Not set — prompted when joining a room" },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_groups_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                nameDraft = displayName
                                showNameDialog = true
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = "Sync Server & Connection",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_cloud_sync_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SettingsItem(
                            title = "Sync server URL",
                            subtitle = serverUrl,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_cloud_sync_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                serverDraft = serverUrl
                                showServerDialog = true
                            }
                        )
                        SwitchSettingItem(
                            title = "Keep session alive in background",
                            subtitle = "Maintain WebSocket synchronization when PixelMusic is minimized",
                            checked = keepAlive,
                            onCheckedChange = { vm.setKeepAlive(it) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_music_note_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.pixeljam),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = { navController.popBackStack() }
        )
    }
}
