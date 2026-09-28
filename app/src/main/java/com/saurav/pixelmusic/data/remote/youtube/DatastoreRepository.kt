package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saurav.pixelmusic.data.model.youtube.Cookies
import com.saurav.pixelmusic.data.model.youtube.StoredYoutubeAccount
import com.saurav.pixelmusic.data.model.youtube.UmihiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.youtubeDataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.Datastore.NAME)

open class DatastoreRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    object PreferenceKeys {
        val SAVED_ACCOUNTS = stringPreferencesKey("saved_youtube_accounts")
        val ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_youtube_account_id")
        val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
        val COOKIES = stringPreferencesKey(Constants.Datastore.COOKIES_KEY)
        val DATA_SYNC_ID = stringPreferencesKey(Constants.Datastore.DATA_SYNC_ID)
        val UPDATE_CHANNEL = stringPreferencesKey(Constants.Datastore.UPDATE_CHANNEL_KEY)
        val SHOW_PODCAST_PLAYLIST = booleanPreferencesKey(Constants.Datastore.SHOW_PODCAST_PLAYLIST)
        val USE_SPECIAL_LANGUAGE = booleanPreferencesKey(Constants.Datastore.USE_SPECIAL_LANGUAGE)
        val USE_AUDIO_OFFLOAD = booleanPreferencesKey(Constants.Datastore.USE_AUDIO_OFFLOAD)
        val KEEP_SCREEN_ON = booleanPreferencesKey(Constants.Datastore.KEEP_SCREEN_ON)
        val USE_ANIMATED_LYRICS = booleanPreferencesKey(Constants.Datastore.USE_ANIMATED_LYRICS)
        val ANIMATED_LYRICS_BLUR_ENABLED = booleanPreferencesKey(Constants.Datastore.ANIMATED_LYRICS_BLUR_ENABLED)
        val USE_IMMERSIVE_LYRICS = booleanPreferencesKey("use_immersive_lyrics")
        val LYRICS_AUTOHIDE_DELAY = intPreferencesKey("lyrics_autohide_delay")
        val SHOW_PLAYER_FILE_INFO = booleanPreferencesKey("show_player_file_info")
        val PLAYER_THEME_PREFERENCE = stringPreferencesKey("player_theme_preference")
        val COLOR_PALETTE_PREFERENCE = stringPreferencesKey("color_palette_preference")
        val LYRICS_MINIPLAYER_POSITION = stringPreferencesKey("lyrics_miniplayer_position")
        val LYRICS_MINIPLAYER_ALIGNMENT = stringPreferencesKey("lyrics_miniplayer_alignment")
        val USE_IMMERSIVE_LYRICS_STATUS_BAR = booleanPreferencesKey("use_immersive_lyrics_status_bar")
        val AUTO_QUEUE_ENABLED = booleanPreferencesKey("auto_queue_enabled")
        val AVOID_REPETITIVE_SONGS = booleanPreferencesKey("avoid_repetitive_songs")
        val PRELOAD_QUEUE_ENABLED = booleanPreferencesKey("preload_queue_enabled")
        val PRELOAD_QUEUE_SIZE = intPreferencesKey("preload_queue_size")
        val PERSISTENT_QUEUE = stringPreferencesKey("persistent_queue")
        val YT_USERNAME = stringPreferencesKey("yt_username")
        val YT_HANDLE = stringPreferencesKey("yt_handle")
        val YT_AVATAR_URL = stringPreferencesKey("yt_avatar_url")
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.youtubeDataStore.edit {
            it[key] = value
        }
    }

    open val settings = context.youtubeDataStore.data.map {
        val updateChannel = it[PreferenceKeys.UPDATE_CHANNEL]?.let { value -> UmihiSettings.UpdateChannel.valueOf(value) }
            ?: UmihiSettings.UpdateChannel.Stable
        val showPodcastPlaylist = it[PreferenceKeys.SHOW_PODCAST_PLAYLIST] ?: true
        val useSpecialLanguage = it[PreferenceKeys.USE_SPECIAL_LANGUAGE] ?: false
        val useAudioOffload = it[PreferenceKeys.USE_AUDIO_OFFLOAD] ?: false
        val keepScreenOn = it[PreferenceKeys.KEEP_SCREEN_ON] ?: false
        val useAnimatedLyrics = it[PreferenceKeys.USE_ANIMATED_LYRICS] ?: true
        val animatedLyricsBlurEnabled = it[PreferenceKeys.ANIMATED_LYRICS_BLUR_ENABLED] ?: true
        val useImmersiveLyrics = it[PreferenceKeys.USE_IMMERSIVE_LYRICS] ?: true
        val lyricsAutoHideDelay = it[PreferenceKeys.LYRICS_AUTOHIDE_DELAY] ?: 4
        val showPlayerFileInfo = it[PreferenceKeys.SHOW_PLAYER_FILE_INFO] ?: false
        val playerThemePreference = it[PreferenceKeys.PLAYER_THEME_PREFERENCE] ?: "ALBUM_ART"
        val colorPalettePreference = it[PreferenceKeys.COLOR_PALETTE_PREFERENCE] ?: "SAGE"
        val lyricsMiniPlayerPosition = it[PreferenceKeys.LYRICS_MINIPLAYER_POSITION] ?: "TOP"
        val lyricsMiniPlayerAlignment = it[PreferenceKeys.LYRICS_MINIPLAYER_ALIGNMENT] ?: "LEFT"
        val useImmersiveLyricsStatusBar = it[PreferenceKeys.USE_IMMERSIVE_LYRICS_STATUS_BAR] ?: true
        val autoQueueEnabled = it[PreferenceKeys.AUTO_QUEUE_ENABLED] ?: true
        val avoidRepetitiveSongs = it[PreferenceKeys.AVOID_REPETITIVE_SONGS] ?: false
        val preloadQueueEnabled = it[PreferenceKeys.PRELOAD_QUEUE_ENABLED] ?: true
        val preloadQueueSize = it[PreferenceKeys.PRELOAD_QUEUE_SIZE] ?: 5
        val cookies = cookies.first()
        val dataSyncId = dataSyncId.first()

        UmihiSettings(
            updateChannel = updateChannel,
            showPodcastPlaylist = showPodcastPlaylist,
            cookies = cookies,
            dataSyncId = dataSyncId,
            useSpecialLanguage = useSpecialLanguage,
            useAudioOffload = useAudioOffload,
            keepScreenOn = keepScreenOn,
            useAnimatedLyrics = useAnimatedLyrics,
            animatedLyricsBlurEnabled = animatedLyricsBlurEnabled,
            useImmersiveLyrics = useImmersiveLyrics,
            lyricsAutoHideDelay = lyricsAutoHideDelay,
            showPlayerFileInfo = showPlayerFileInfo,
            playerThemePreference = playerThemePreference,
            colorPalettePreference = colorPalettePreference,
            lyricsMiniPlayerPosition = lyricsMiniPlayerPosition,
            lyricsMiniPlayerAlignment = lyricsMiniPlayerAlignment,
            useImmersiveLyricsStatusBar = useImmersiveLyricsStatusBar,
            autoQueueEnabled = autoQueueEnabled,
            avoidRepetitiveSongs = avoidRepetitiveSongs,
            preloadQueueEnabled = preloadQueueEnabled,
            preloadQueueSize = preloadQueueSize
        )
    }



    val savedAccounts: Flow<List<StoredYoutubeAccount>> = context.youtubeDataStore.data.map { prefs ->
        val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
        val accounts = if (jsonStr.isNotBlank()) {
            runCatching {
                json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val legacyCookie = prefs[PreferenceKeys.COOKIES].orEmpty()
        val legacyName = prefs[PreferenceKeys.YT_USERNAME].orEmpty()
        val legacyHandle = prefs[PreferenceKeys.YT_HANDLE].orEmpty()
        val legacyAvatar = prefs[PreferenceKeys.YT_AVATAR_URL].orEmpty()
        val legacySyncId = prefs[PreferenceKeys.DATA_SYNC_ID].orEmpty()
        val legacyIsPro = prefs[PreferenceKeys.IS_PRO_USER] ?: false

        if (accounts.isEmpty() && (legacyCookie.isNotBlank() || legacyName.isNotBlank())) {
            listOf(
                StoredYoutubeAccount(
                    id = legacySyncId.ifBlank { "primary_account" },
                    name = legacyName,
                    handle = legacyHandle,
                    avatarUrl = legacyAvatar,
                    cookie = legacyCookie,
                    dataSyncId = legacySyncId,
                    isPro = legacyIsPro
                )
            )
        } else {
            accounts.take(2)
        }
    }

    val activeAccountId: Flow<String> = context.youtubeDataStore.data.map { prefs ->
        val explicitActive = prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID].orEmpty()
        if (explicitActive.isNotBlank()) {
            explicitActive
        } else {
            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            if (jsonStr.isNotBlank()) {
                runCatching {
                    json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr)
                }.getOrNull()?.firstOrNull()?.id.orEmpty()
            } else {
                prefs[PreferenceKeys.DATA_SYNC_ID].orEmpty().ifBlank { "primary_account" }
            }
        }
    }

    val cookies = context.youtubeDataStore.data.map {
        Cookies(it[PreferenceKeys.COOKIES] ?: "")
    }

    val dataSyncId = context.youtubeDataStore.data.map {
        it[PreferenceKeys.DATA_SYNC_ID] ?: ""
    }

    val ytUsername = context.youtubeDataStore.data.map {
        it[PreferenceKeys.YT_USERNAME] ?: ""
    }

    val ytHandle = context.youtubeDataStore.data.map {
        it[PreferenceKeys.YT_HANDLE] ?: ""
    }

    val ytAvatarUrl = context.youtubeDataStore.data.map {
        it[PreferenceKeys.YT_AVATAR_URL] ?: ""
    }

    val ytIsProUser = context.youtubeDataStore.data.map {
        it[PreferenceKeys.IS_PRO_USER] ?: false
    }

    suspend fun addNewAccount(
        cookie: String,
        dataSyncId: String = "",
        name: String = "",
        handle: String = "",
        avatarUrl: String = "",
        makeActive: Boolean = true
    ): StoredYoutubeAccount {
        var resultAccount = StoredYoutubeAccount()
        context.youtubeDataStore.edit { prefs ->
            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            val currentList = if (jsonStr.isNotBlank()) {
                runCatching { json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                val legacyCookie = prefs[PreferenceKeys.COOKIES].orEmpty()
                val legacyName = prefs[PreferenceKeys.YT_USERNAME].orEmpty()
                if (legacyCookie.isNotBlank() || legacyName.isNotBlank()) {
                    listOf(
                        StoredYoutubeAccount(
                            id = prefs[PreferenceKeys.DATA_SYNC_ID].orEmpty().ifBlank { "primary_account" },
                            name = legacyName,
                            handle = prefs[PreferenceKeys.YT_HANDLE].orEmpty(),
                            avatarUrl = prefs[PreferenceKeys.YT_AVATAR_URL].orEmpty(),
                            cookie = legacyCookie,
                            dataSyncId = prefs[PreferenceKeys.DATA_SYNC_ID].orEmpty(),
                            isPro = prefs[PreferenceKeys.IS_PRO_USER] ?: false
                        )
                    )
                } else emptyList()
            }.toMutableList()

            val accountId = dataSyncId.ifBlank { java.util.UUID.randomUUID().toString() }
            val newAcc = StoredYoutubeAccount(
                id = accountId,
                name = name,
                handle = handle,
                avatarUrl = avatarUrl,
                cookie = cookie,
                dataSyncId = dataSyncId,
                isPro = false
            )
            resultAccount = newAcc

            val existingIndex = currentList.indexOfFirst {
                (it.id == accountId) || (handle.isNotBlank() && it.handle == handle)
            }
            if (existingIndex != -1) {
                currentList[existingIndex] = newAcc
            } else {
                if (currentList.size >= 2) {
                    val activeId = prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID].orEmpty()
                    val inactiveIndex = currentList.indexOfFirst { it.id != activeId }
                    if (inactiveIndex != -1) {
                        currentList[inactiveIndex] = newAcc
                    } else {
                        currentList[1] = newAcc
                    }
                } else {
                    currentList.add(newAcc)
                }
            }

            prefs[PreferenceKeys.SAVED_ACCOUNTS] = json.encodeToString(currentList.take(2))

            if (makeActive) {
                prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID] = newAcc.id
                prefs[PreferenceKeys.COOKIES] = newAcc.cookie
                prefs[PreferenceKeys.DATA_SYNC_ID] = newAcc.dataSyncId
                prefs[PreferenceKeys.YT_USERNAME] = newAcc.name
                prefs[PreferenceKeys.YT_HANDLE] = newAcc.handle
                prefs[PreferenceKeys.YT_AVATAR_URL] = newAcc.avatarUrl
                prefs[PreferenceKeys.IS_PRO_USER] = newAcc.isPro
            }
        }

        if (makeActive) {
            saurav.shru.pixelmusic.innertube.YouTube.cookie = cookie
            saurav.shru.pixelmusic.innertube.YouTube.dataSyncId = dataSyncId
        }

        return resultAccount
    }

    suspend fun switchAccount(accountId: String): StoredYoutubeAccount? {
        var switched: StoredYoutubeAccount? = null
        context.youtubeDataStore.edit { prefs ->
            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            val currentList = if (jsonStr.isNotBlank()) {
                runCatching { json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val target = currentList.find { it.id == accountId }
            if (target != null) {
                switched = target
                prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID] = target.id
                prefs[PreferenceKeys.COOKIES] = target.cookie
                prefs[PreferenceKeys.DATA_SYNC_ID] = target.dataSyncId
                prefs[PreferenceKeys.YT_USERNAME] = target.name
                prefs[PreferenceKeys.YT_HANDLE] = target.handle
                prefs[PreferenceKeys.YT_AVATAR_URL] = target.avatarUrl
                prefs[PreferenceKeys.IS_PRO_USER] = target.isPro
            }
        }

        switched?.let { acc ->
            saurav.shru.pixelmusic.innertube.YouTube.cookie = acc.cookie
            saurav.shru.pixelmusic.innertube.YouTube.dataSyncId = acc.dataSyncId
        }

        return switched
    }

    suspend fun removeAccount(accountId: String) {
        var newActive: StoredYoutubeAccount? = null
        context.youtubeDataStore.edit { prefs ->
            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            val currentList = if (jsonStr.isNotBlank()) {
                runCatching { json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }.toMutableList()

            currentList.removeAll { it.id == accountId }
            prefs[PreferenceKeys.SAVED_ACCOUNTS] = json.encodeToString(currentList)

            val currentActiveId = prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID].orEmpty()
            if (currentActiveId == accountId || currentActiveId.isEmpty()) {
                if (currentList.isNotEmpty()) {
                    val next = currentList.first()
                    newActive = next
                    prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID] = next.id
                    prefs[PreferenceKeys.COOKIES] = next.cookie
                    prefs[PreferenceKeys.DATA_SYNC_ID] = next.dataSyncId
                    prefs[PreferenceKeys.YT_USERNAME] = next.name
                    prefs[PreferenceKeys.YT_HANDLE] = next.handle
                    prefs[PreferenceKeys.YT_AVATAR_URL] = next.avatarUrl
                    prefs[PreferenceKeys.IS_PRO_USER] = next.isPro
                } else {
                    prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID] = ""
                    prefs[PreferenceKeys.COOKIES] = ""
                    prefs[PreferenceKeys.DATA_SYNC_ID] = ""
                    prefs[PreferenceKeys.YT_USERNAME] = ""
                    prefs[PreferenceKeys.YT_HANDLE] = ""
                    prefs[PreferenceKeys.YT_AVATAR_URL] = ""
                    prefs[PreferenceKeys.IS_PRO_USER] = false
                }
            }
        }

        if (newActive != null) {
            saurav.shru.pixelmusic.innertube.YouTube.cookie = newActive!!.cookie
            saurav.shru.pixelmusic.innertube.YouTube.dataSyncId = newActive!!.dataSyncId
        } else {
            saurav.shru.pixelmusic.innertube.YouTube.cookie = ""
            saurav.shru.pixelmusic.innertube.YouTube.dataSyncId = ""
        }
    }

    suspend fun saveYtProfile(name: String, handle: String, avatarUrl: String, isPro: Boolean = false) {
        context.youtubeDataStore.edit { prefs ->
            prefs[PreferenceKeys.YT_USERNAME] = name
            prefs[PreferenceKeys.YT_HANDLE] = handle
            prefs[PreferenceKeys.YT_AVATAR_URL] = avatarUrl
            prefs[PreferenceKeys.IS_PRO_USER] = isPro

            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            val currentList = if (jsonStr.isNotBlank()) {
                runCatching { json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }.toMutableList()

            var activeId = prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID].orEmpty()
            if (activeId.isEmpty() && currentList.isNotEmpty()) {
                activeId = currentList.first().id
                prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID] = activeId
            }

            val index = currentList.indexOfFirst { it.id == activeId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(
                    name = name,
                    handle = handle,
                    avatarUrl = avatarUrl,
                    isPro = isPro
                )
                prefs[PreferenceKeys.SAVED_ACCOUNTS] = json.encodeToString(currentList.take(2))
            } else if (name.isNotBlank() || handle.isNotBlank()) {
                val newAcc = StoredYoutubeAccount(
                    id = activeId.ifBlank { prefs[PreferenceKeys.DATA_SYNC_ID].orEmpty().ifBlank { java.util.UUID.randomUUID().toString() } },
                    name = name,
                    handle = handle,
                    avatarUrl = avatarUrl,
                    cookie = prefs[PreferenceKeys.COOKIES].orEmpty(),
                    dataSyncId = prefs[PreferenceKeys.DATA_SYNC_ID].orEmpty(),
                    isPro = isPro
                )
                if (currentList.size < 2) {
                    currentList.add(newAcc)
                } else {
                    currentList[0] = newAcc
                }
                prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID] = newAcc.id
                prefs[PreferenceKeys.SAVED_ACCOUNTS] = json.encodeToString(currentList.take(2))
            }
        }
    }

    suspend fun saveCookies(cookies: Cookies) {
        val raw = cookies.toRawCookie()
        context.youtubeDataStore.edit { prefs ->
            prefs[PreferenceKeys.COOKIES] = raw

            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            val currentList = if (jsonStr.isNotBlank()) {
                runCatching { json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }.toMutableList()

            val activeId = prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID].orEmpty()
            val index = currentList.indexOfFirst { it.id == activeId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(cookie = raw)
                prefs[PreferenceKeys.SAVED_ACCOUNTS] = json.encodeToString(currentList.take(2))
            }
        }
        saurav.shru.pixelmusic.innertube.YouTube.cookie = raw
    }

    suspend fun saveDataSyncId(newId: String) {
        context.youtubeDataStore.edit { prefs ->
            prefs[PreferenceKeys.DATA_SYNC_ID] = newId

            val jsonStr = prefs[PreferenceKeys.SAVED_ACCOUNTS].orEmpty()
            val currentList = if (jsonStr.isNotBlank()) {
                runCatching { json.decodeFromString<List<StoredYoutubeAccount>>(jsonStr) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }.toMutableList()

            val activeId = prefs[PreferenceKeys.ACTIVE_ACCOUNT_ID].orEmpty()
            val index = currentList.indexOfFirst { it.id == activeId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(dataSyncId = newId)
                prefs[PreferenceKeys.SAVED_ACCOUNTS] = json.encodeToString(currentList.take(2))
            }
        }
        saurav.shru.pixelmusic.innertube.YouTube.dataSyncId = newId
    }

    suspend fun getPersistentQueue(): String {
        return context.youtubeDataStore.data.first()[PreferenceKeys.PERSISTENT_QUEUE] ?: ""
    }

    suspend fun savePersistentQueue(json: String) {
        context.youtubeDataStore.edit {
            it[PreferenceKeys.PERSISTENT_QUEUE] = json
        }
    }
}
