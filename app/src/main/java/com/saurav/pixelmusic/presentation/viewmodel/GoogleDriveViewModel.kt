package com.saurav.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.gdrive.GoogleDriveAuthManager
import com.saurav.pixelmusic.data.gdrive.GoogleDriveFile
import com.saurav.pixelmusic.data.gdrive.GoogleDriveRepository
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BreadcrumbFolder(
    val id: String?,
    val name: String
)

@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    val authManager: GoogleDriveAuthManager,
    private val driveRepository: GoogleDriveRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val authState = authManager.authState

    val configuredClientId: StateFlow<String?> = userPreferencesRepository.gdriveClientIdFlow
        .map { it?.takeIf { s -> s.isNotBlank() } ?: GoogleDriveAuthManager.BUILT_IN_CLIENT_ID }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoogleDriveAuthManager.BUILT_IN_CLIENT_ID)

    val configuredClientSecret: StateFlow<String?> = userPreferencesRepository.gdriveClientSecretFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _folders = MutableStateFlow<List<GoogleDriveFile>>(emptyList())
    val folders: StateFlow<List<GoogleDriveFile>> = _folders.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<BreadcrumbFolder>>(listOf(BreadcrumbFolder(null, "My Drive")))
    val breadcrumbs: StateFlow<List<BreadcrumbFolder>> = _breadcrumbs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is com.saurav.pixelmusic.data.gdrive.GoogleDriveAuthState.SignedIn) {
                    loadCurrentFolder()
                }
            }
        }
    }

    fun updateCredentials(clientId: String?, clientSecret: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setGDriveClientId(clientId?.trim()?.takeIf { it.isNotBlank() })
            userPreferencesRepository.setGDriveClientSecret(clientSecret?.trim()?.takeIf { it.isNotBlank() })
        }
    }

    fun connectDrive(clientId: String? = null) {
        val idToUse = clientId?.takeIf { it.isNotBlank() } ?: configuredClientId.value
        authManager.launchAuthorizationFlow(idToUse)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _folders.value = emptyList()
            _songs.value = emptyList()
            _breadcrumbs.value = listOf(BreadcrumbFolder(null, "My Drive"))
        }
    }

    fun loadCurrentFolder() {
        val currentFolderId = _breadcrumbs.value.lastOrNull()?.id
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _folders.value = driveRepository.getFolders(currentFolderId)
                _songs.value = driveRepository.getAudioFiles(currentFolderId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openFolder(folder: GoogleDriveFile) {
        _breadcrumbs.value = _breadcrumbs.value + BreadcrumbFolder(folder.id, folder.name)
        _searchQuery.value = ""
        loadCurrentFolder()
    }

    fun navigateToBreadcrumb(index: Int) {
        if (index >= 0 && index < _breadcrumbs.value.size) {
            _breadcrumbs.value = _breadcrumbs.value.take(index + 1)
            _searchQuery.value = ""
            loadCurrentFolder()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadCurrentFolder()
        } else {
            search(query)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _folders.value = emptyList()
                _songs.value = driveRepository.searchSongs(query)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
