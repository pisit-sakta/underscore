package com.underscore.app.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class SpotifyAuth(private val context: Context) {

    companion object {
        const val CLIENT_ID = "656f4a5238d54b8795fdf171b4c04acf"
        const val REDIRECT_URI = "underscore://spotify-auth-callback"
        const val AUTH_REQUEST_CODE = 1337

        private const val PREFS_NAME = "underscore_spotify_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val TAG = "SpotifyAuth"

        private const val AUTH_URL = "https://accounts.spotify.com/authorize"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"

        private val SCOPES = listOf(
            "user-library-read",
            "user-modify-playback-state",
            "user-read-playback-state",
            "playlist-read-private",
            "playlist-read-collaborative"
        )
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val client = OkHttpClient()

    fun isLoggedIn(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        val loggedIn = token != null && System.currentTimeMillis() < expiry
        Log.d(TAG, "isLoggedIn=$loggedIn (hasToken=${token != null}, expired=${System.currentTimeMillis() >= expiry})")
        return loggedIn
    }

    fun getAccessToken(): String? {
        if (!isLoggedIn()) return null
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Start Spotify auth via browser using Authorization Code + PKCE flow.
     * No client_secret needed — the code_verifier proves we're the same app.
     */
    fun startAuth(activity: Activity) {
        Log.d(TAG, "Starting Spotify auth (PKCE flow)...")

        // Generate PKCE code verifier + challenge
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // Save verifier for the token exchange step
        prefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply()

        val scopeString = SCOPES.joinToString(" ")
        val authUri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", scopeString)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("show_dialog", "true")
            .build()

        Log.d(TAG, "Auth URI: $authUri")

        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(activity, authUri)
            Log.d(TAG, "Opened auth in Chrome Custom Tab")
        } catch (e: Exception) {
            Log.w(TAG, "Chrome Custom Tab failed, falling back to browser: ${e.message}")
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, authUri)
                activity.startActivity(browserIntent)
                Log.d(TAG, "Opened auth in browser")
            } catch (e2: Exception) {
                Log.e(TAG, "All auth methods failed: ${e2.message}", e2)
            }
        }
    }

    /**
     * Handle the redirect URI coming back from browser auth.
     * PKCE flow: redirect has ?code=XXX (not fragment #access_token like implicit grant).
     * We need to exchange the code for tokens.
     */
    fun handleRedirectUri(uri: Uri): Boolean {
        Log.d(TAG, "Handling redirect URI: $uri")

        val error = uri.getQueryParameter("error")
        if (error != null) {
            Log.e(TAG, "Auth error: $error")
            return false
        }

        val code = uri.getQueryParameter("code")
        if (code == null) {
            Log.e(TAG, "No code in redirect URI")
            return false
        }

        Log.d(TAG, "Got auth code, will exchange for token")

        // Store code temporarily — the token exchange happens async
        prefs.edit().putString("pending_auth_code", code).apply()
        return true
    }

    /**
     * Exchange the auth code for access + refresh tokens.
     * Call this after handleRedirectUri returns true.
     */
    suspend fun exchangeCodeForToken(): Boolean = withContext(Dispatchers.IO) {
        val code = prefs.getString("pending_auth_code", null)
        val codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null)

        if (code == null || codeVerifier == null) {
            Log.e(TAG, "Missing code or verifier for token exchange")
            return@withContext false
        }

        try {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", codeVerifier)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Token exchange failed: ${response.code} — $responseBody")
                return@withContext false
            }

            val json = JSONObject(responseBody)
            val accessToken = json.getString("access_token")
            val expiresIn = json.getInt("expires_in")
            val refreshToken = json.optString("refresh_token", null)

            saveToken(accessToken, expiresIn, refreshToken)
            Log.d(TAG, "Token exchange success! Expires in ${expiresIn}s")

            // Mark scope version as current
            val userPrefs = com.underscore.app.data.UserPreferences(context)
            userPrefs.markScopeVersionCurrent()

            // Clean up
            prefs.edit()
                .remove("pending_auth_code")
                .remove(KEY_CODE_VERIFIER)
                .apply()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange exception: ${e.message}", e)
            false
        }
    }

    /**
     * Refresh the access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (refreshToken == null) {
            Log.e(TAG, "No refresh token stored")
            return@withContext false
        }

        try {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Token refresh failed: ${response.code} — $responseBody")
                return@withContext false
            }

            val json = JSONObject(responseBody)
            val accessToken = json.getString("access_token")
            val expiresIn = json.getInt("expires_in")
            val newRefreshToken = json.optString("refresh_token", refreshToken)

            saveToken(accessToken, expiresIn, newRefreshToken)
            Log.d(TAG, "Token refreshed! Expires in ${expiresIn}s")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh exception: ${e.message}", e)
            false
        }
    }

    fun hasPendingAuthCode(): Boolean {
        return prefs.getString("pending_auth_code", null) != null
    }

    fun logout() {
        Log.d(TAG, "Logging out")
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .remove(KEY_CODE_VERIFIER)
            .remove("pending_auth_code")
            .apply()
    }

    private fun saveToken(token: String, expiresIn: Int, refreshToken: String?) {
        val expiryTimestamp = System.currentTimeMillis() + (expiresIn * 1000L)
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            putLong(KEY_TOKEN_EXPIRY, expiryTimestamp)
            if (refreshToken != null) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            apply()
        }
        Log.d(TAG, "Token saved, expires at $expiryTimestamp")
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
