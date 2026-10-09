package com.saurav.pixelmusic.data.gdrive

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    private val _authState = MutableStateFlow<GoogleDriveAuthState>(GoogleDriveAuthState.SignedOut)
    val authState: StateFlow<GoogleDriveAuthState> = _authState.asStateFlow()

    companion object {
        private const val TAG = "GoogleDriveAuth"
        const val REDIRECT_URI = "com.saurav.pixelmusic://oauth2callback"
        const val OAUTH_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
    }

    init {
        scope.launch {
            val token = userPreferencesRepository.gdriveAccessTokenFlow.first()
            val email = userPreferencesRepository.gdriveAccountEmailFlow.first()
            val name = userPreferencesRepository.gdriveAccountNameFlow.first()
            if (!token.isNullOrBlank()) {
                _authState.value = GoogleDriveAuthState.SignedIn(email = email, name = name)
            }
        }
    }

    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val currentToken = userPreferencesRepository.gdriveAccessTokenFlow.first()
        val expiryMs = userPreferencesRepository.gdriveTokenExpiryFlow.first()
        val refreshToken = userPreferencesRepository.gdriveRefreshTokenFlow.first()

        if (currentToken.isNullOrBlank()) return@withContext null

        val isExpired = System.currentTimeMillis() >= (expiryMs - 60_000L)
        if (!isExpired) return@withContext currentToken

        if (!refreshToken.isNullOrBlank()) {
            val refreshed = refreshAccessToken(refreshToken)
            if (refreshed != null) return@withContext refreshed
        }

        currentToken
    }

    fun buildAuthorizationUrl(clientId: String): String {
        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
            .buildUpon()
            .appendQueryParameter("client_id", clientId.trim())
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", OAUTH_SCOPE)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()
            .toString()
    }

    fun launchAuthorizationFlow(clientId: String) {
        val url = buildAuthorizationUrl(clientId)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            _authState.value = GoogleDriveAuthState.Authenticating
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to launch browser for Google Drive OAuth")
            _authState.value = GoogleDriveAuthState.Error("Browser not found to complete Google Sign-In")
        }
    }

    suspend fun handleAuthRedirect(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")

        if (error != null) {
            _authState.value = GoogleDriveAuthState.Error("Authorization canceled or denied: $error")
            return@withContext false
        }

        if (code.isNullOrBlank()) {
            _authState.value = GoogleDriveAuthState.Error("No authorization code received")
            return@withContext false
        }

        val clientId = userPreferencesRepository.gdriveClientIdFlow.first() ?: ""
        val clientSecret = userPreferencesRepository.gdriveClientSecretFlow.first()

        exchangeCodeForTokens(code, clientId, clientSecret)
    }

    private suspend fun exchangeCodeForTokens(code: String, clientId: String, clientSecret: String?): Boolean {
        try {
            val bodyBuilder = FormBody.Builder()
                .add("code", code)
                .add("client_id", clientId)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")

            if (!clientSecret.isNullOrBlank()) {
                bodyBuilder.add("client_secret", clientSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(bodyBuilder.build())
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Timber.tag(TAG).e("Token exchange failed (${response.code}): $bodyString")
                _authState.value = GoogleDriveAuthState.Error("Failed to obtain tokens from Google (${response.code})")
                return false
            }

            val tokenResponse = json.decodeFromString<GoogleDriveTokenResponse>(bodyString)
            val accessToken = tokenResponse.access_token
            val refreshToken = tokenResponse.refresh_token
            val expiresIn = tokenResponse.expires_in ?: 3600L

            if (accessToken.isNullOrBlank()) {
                _authState.value = GoogleDriveAuthState.Error("Google returned empty access token")
                return false
            }

            val expiryMs = System.currentTimeMillis() + (expiresIn * 1000L)
            userPreferencesRepository.setGDriveAccessToken(accessToken)
            if (!refreshToken.isNullOrBlank()) {
                userPreferencesRepository.setGDriveRefreshToken(refreshToken)
            }
            userPreferencesRepository.setGDriveTokenExpiry(expiryMs)

            fetchAndSaveUserInfo(accessToken)
            return true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error exchanging code for tokens")
            _authState.value = GoogleDriveAuthState.Error("Network error during authorization: ${e.localizedMessage}")
            return false
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): String? {
        try {
            val clientId = userPreferencesRepository.gdriveClientIdFlow.first() ?: ""
            val clientSecret = userPreferencesRepository.gdriveClientSecretFlow.first()

            val bodyBuilder = FormBody.Builder()
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")

            if (!clientSecret.isNullOrBlank()) {
                bodyBuilder.add("client_secret", clientSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(bodyBuilder.build())
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Timber.tag(TAG).w("Refresh token failed (${response.code}): $bodyString")
                return null
            }

            val tokenResponse = json.decodeFromString<GoogleDriveTokenResponse>(bodyString)
            val newAccess = tokenResponse.access_token
            if (!newAccess.isNullOrBlank()) {
                val expiresIn = tokenResponse.expires_in ?: 3600L
                val expiryMs = System.currentTimeMillis() + (expiresIn * 1000L)
                userPreferencesRepository.setGDriveAccessToken(newAccess)
                userPreferencesRepository.setGDriveTokenExpiry(expiryMs)
                return newAccess
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error refreshing token")
        }
        return null
    }

    suspend fun fetchAndSaveUserInfo(accessToken: String) {
        try {
            val request = Request.Builder()
                .url(USERINFO_URL)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val info = json.decodeFromString<GoogleDriveUserInfo>(bodyString)
                userPreferencesRepository.setGDriveAccountInfo(info.email, info.name)
                _authState.value = GoogleDriveAuthState.SignedIn(email = info.email, name = info.name)
            } else {
                _authState.value = GoogleDriveAuthState.SignedIn(email = "Google Drive Connected", name = null)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching user info")
            _authState.value = GoogleDriveAuthState.SignedIn(email = "Google Drive Connected", name = null)
        }
    }

    suspend fun signOut() {
        userPreferencesRepository.clearGDriveAuth()
        _authState.value = GoogleDriveAuthState.SignedOut
    }
}
