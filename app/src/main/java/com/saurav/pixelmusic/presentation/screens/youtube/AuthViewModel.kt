package com.saurav.pixelmusic.presentation.screens.youtube

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.remote.youtube.Constants
import com.saurav.pixelmusic.data.remote.youtube.DatastoreRepository
import com.saurav.pixelmusic.data.remote.youtube.UmihiHelper.printd
import com.saurav.pixelmusic.data.model.youtube.Cookies
import com.saurav.pixelmusic.data.worker.SyncManager
import saurav.shru.pixelmusic.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val datastoreRepository: DatastoreRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    //  val uiState = _uiState.asStateFlow()

    private val _eventsChannel = MutableSharedFlow<ScreenEvent.Out>()
    val eventFlow = _eventsChannel.asSharedFlow()

    private var currentDataSyncId: String = ""

    fun onPageFinished(url: String?) {
        viewModelScope.launch {
            if (url?.contains(Constants.Auth.END_URL) == true && !_uiState.value.isLoggedIn) {
                val cookies = CookieManager.getInstance().getCookie(url).orEmpty()
                if (cookies.isNotBlank()) {
                    _uiState.update { it.copy(isLoggedIn = true) }
                    val syncId = currentDataSyncId
                    YouTube.cookie = cookies
                    if (syncId.isNotBlank()) {
                        YouTube.dataSyncId = syncId
                    }

                    var name = ""
                    var handle = ""
                    var avatarUrl = ""
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            YouTube.accountInfo().getOrNull()
                        }.getOrNull()?.let { info ->
                            name = info.name
                            handle = info.channelHandle ?: ""
                            avatarUrl = info.thumbnailUrl ?: ""
                        }
                    }

                    datastoreRepository.addNewAccount(
                        cookie = cookies,
                        dataSyncId = syncId,
                        name = name,
                        handle = handle,
                        avatarUrl = avatarUrl,
                        makeActive = true
                    )

                    _eventsChannel.emit(ScreenEvent.Out.LoginCompleted)
                    // Trigger an immediate background synchronization of user playlists and library
                    syncManager.fullSync()
                }
            }
        }
    }

    fun onDataSyncIdFound(dataSyncId: String) {
        currentDataSyncId = dataSyncId
        viewModelScope.launch {
            datastoreRepository.saveDataSyncId(dataSyncId)
            YouTube.dataSyncId = dataSyncId
        }
    }

    fun saveManualCookie(tokenString: String) {
        viewModelScope.launch {
            var cookieStr = ""
            var dataSyncId = ""
            var accountName = ""
            var accountHandle = ""

            // 1. Check if it's the formatted ArchiveTune token string
            if (tokenString.contains("***INNERTUBE COOKIE***")) {
                // Helper to extract the value between the = and the next ***
                fun extractBlock(key: String): String {
                    val header = "***$key***"
                    val startIdx = tokenString.indexOf(header)
                    if (startIdx == -1) return ""
                    
                    val equalsIdx = tokenString.indexOf("=", startIdx + header.length)
                    if (equalsIdx == -1) return ""
                    
                    val nextHeaderIdx = tokenString.indexOf("***", equalsIdx)
                    return if (nextHeaderIdx != -1) {
                        tokenString.substring(equalsIdx + 1, nextHeaderIdx)
                    } else {
                        tokenString.substring(equalsIdx + 1)
                    }.trim()
                }

                // Extract all the pieces perfectly
                cookieStr = extractBlock("INNERTUBE COOKIE")
                dataSyncId = extractBlock("DATASYNC ID")
                accountName = extractBlock("ACCOUNT NAME")
                accountHandle = extractBlock("ACCOUNT CHANNEL HANDLE")
            } else {
                // Fallback: If they just pasted a raw raw cookie string without the extra metadata
                cookieStr = tokenString.trim()
            }

            var avatarUrl = ""
            if (cookieStr.isNotEmpty()) {
                YouTube.cookie = cookieStr
                if (dataSyncId.isNotEmpty()) {
                    YouTube.dataSyncId = dataSyncId
                }
                if (accountName.isEmpty() || accountHandle.isEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            YouTube.accountInfo().getOrNull()
                        }.getOrNull()?.let { info ->
                            if (accountName.isEmpty()) accountName = info.name
                            if (accountHandle.isEmpty()) accountHandle = info.channelHandle ?: ""
                            avatarUrl = info.thumbnailUrl ?: ""
                        }
                    }
                }
            }

            if (cookieStr.isNotEmpty()) {
                datastoreRepository.addNewAccount(
                    cookie = cookieStr,
                    dataSyncId = dataSyncId,
                    name = accountName.ifBlank { "YouTube User" },
                    handle = accountHandle,
                    avatarUrl = avatarUrl,
                    makeActive = true
                )
            }

            // 5. Complete the login and trigger the background sync
            _uiState.update { it.copy(isLoggedIn = true) }
            _eventsChannel.emit(ScreenEvent.Out.LoginCompleted)
            syncManager.fullSync()
        }
    }

    fun getFullTokenString(): Flow<String> = kotlinx.coroutines.flow.flow {
        val cookies = datastoreRepository.cookies.first().toRawCookie()
        val settings = datastoreRepository.settings.first()
        val name = datastoreRepository.ytUsername.first()
        val handle = datastoreRepository.ytHandle.first()

        val token = buildString {
            if (cookies.isNotEmpty()) {
                append("***INNERTUBE COOKIE*** =\n")
                append(cookies)
                append("\n")
            }
            if (settings.dataSyncId.isNotEmpty()) {
                append("***DATASYNC ID*** =\n")
                append(settings.dataSyncId)
                append("\n")
            }
            if (name.isNotEmpty()) {
                append("***ACCOUNT NAME*** =\n")
                append(name)
                append("\n")
            }
            if (handle.isNotEmpty()) {
                append("***ACCOUNT CHANNEL HANDLE*** =\n")
                append(handle)
                append("\n")
            }
        }.trim()
        
        emit(token)
    }

    private fun saveCookies(cookies: Cookies) {
        printd("Got cookies: $cookies")
        viewModelScope.launch {
            datastoreRepository.saveCookies(cookies)
            YouTube.cookie = cookies.toRawCookie()
        }
    }

    sealed interface ScreenEvent {
        sealed class Out {
            object LoginCompleted : Out()
        }
    }
}
