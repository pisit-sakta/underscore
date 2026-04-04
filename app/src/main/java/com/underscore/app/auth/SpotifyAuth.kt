package com.underscore.app.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyAuth(private val context: Context) {

    companion object {
        const val CLIENT_ID = "656f4a5238d54b8795fdf171b4c04acf"
        const val REDIRECT_URI = "underscore://spotify-auth-callback"
        const val AUTH_REQUEST_CODE = 1337

        private const val PREFS_NAME = "underscore_spotify_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val TAG = "SpotifyAuth"

        private val SCOPES = arrayOf(
            "app-remote-control",
            "user-library-read",
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

    fun isLoggedIn(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        val loggedIn = token != null && System.currentTimeMillis() < expiry
        Log.d(TAG, "isLoggedIn=$loggedIn (token=${token != null}, expiry=$expiry, now=${System.currentTimeMillis()})")
        return loggedIn
    }

    fun getAccessToken(): String? {
        if (!isLoggedIn()) return null
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Start Spotify auth using browser (Chrome Custom Tab).
     * Works whether or not the Spotify app is installed.
     * The redirect comes back via intent filter on MainActivity.
     */
    fun startAuth(activity: Activity) {
        Log.d(TAG, "Starting Spotify auth (browser flow)...")

        val request = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )
            .setScopes(SCOPES)
            .setShowDialog(false)
            .build()

        // Use browser-based auth — more reliable than openLoginActivity
        // which silently fails when Spotify app isn't installed
        val authUri = request.toUri()
        Log.d(TAG, "Auth URI: $authUri")

        try {
            // Try openLoginInBrowser first (Chrome Custom Tab)
            AuthorizationClient.openLoginInBrowser(activity, request)
            Log.d(TAG, "Opened auth in browser")
        } catch (e: Exception) {
            Log.e(TAG, "openLoginInBrowser failed, falling back to raw intent: ${e.message}", e)
            // Fallback: open raw URI in any browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, authUri)
                activity.startActivity(browserIntent)
                Log.d(TAG, "Opened auth via raw browser intent")
            } catch (e2: Exception) {
                Log.e(TAG, "All auth methods failed: ${e2.message}", e2)
            }
        }
    }

    /**
     * Handle the redirect URI coming back from browser auth.
     * Called from MainActivity.onNewIntent() when the browser redirects
     * to underscore://spotify-auth-callback#access_token=...
     */
    fun handleRedirectUri(uri: Uri): Boolean {
        Log.d(TAG, "Handling redirect URI: $uri")

        // The token is in the fragment (after #), not query params
        // Format: underscore://spotify-auth-callback#access_token=XXX&token_type=Bearer&expires_in=3600
        val fragment = uri.fragment
        if (fragment == null) {
            Log.e(TAG, "Redirect URI has no fragment — auth failed or was cancelled")
            // Check for error in query params
            val error = uri.getQueryParameter("error")
            if (error != null) {
                Log.e(TAG, "Auth error: $error")
            }
            return false
        }

        // Parse fragment as query params
        val params = fragment.split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }

        val accessToken = params["access_token"]
        val expiresIn = params["expires_in"]?.toIntOrNull()

        if (accessToken != null && expiresIn != null) {
            saveToken(accessToken, expiresIn)
            Log.d(TAG, "Auth success via redirect! Token expires in ${expiresIn}s")
            return true
        }

        val error = params["error"]
        Log.e(TAG, "Auth failed — token=$accessToken, expiresIn=$expiresIn, error=$error")
        return false
    }

    /**
     * Handle auth response from openLoginActivity (legacy, kept as fallback).
     */
    fun handleAuthResponse(resultCode: Int, data: Intent?): Boolean {
        val response = AuthorizationClient.getResponse(resultCode, data)
        return when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                saveToken(response.accessToken, response.expiresIn)
                Log.d(TAG, "Auth success via onActivityResult, token expires in ${response.expiresIn}s")
                true
            }
            AuthorizationResponse.Type.ERROR -> {
                Log.e(TAG, "Auth error: ${response.error}")
                false
            }
            else -> {
                Log.w(TAG, "Auth cancelled or unknown response: ${response.type}")
                false
            }
        }
    }

    fun logout() {
        Log.d(TAG, "Logging out — clearing tokens")
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }

    private fun saveToken(token: String, expiresIn: Int) {
        val expiryTimestamp = System.currentTimeMillis() + (expiresIn * 1000L)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, expiryTimestamp)
            .apply()
        Log.d(TAG, "Token saved, expires at $expiryTimestamp")
    }
}
