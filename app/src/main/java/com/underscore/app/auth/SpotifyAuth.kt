package com.underscore.app.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SpotifyAuth(private val context: Context) {

    companion object {
        const val CLIENT_ID = "656f4a5238d54b8795fdf171b4c04acf"
        const val REDIRECT_URI = "underscore://spotify-auth-callback"
        const val AUTH_REQUEST_CODE = 1337

        private const val PREFS_NAME = "underscore_spotify_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val TAG = "SpotifyAuth"

        private const val AUTH_URL = "https://accounts.spotify.com/authorize"

        private val SCOPES = listOf(
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
        Log.d(TAG, "isLoggedIn=$loggedIn (hasToken=${token != null}, expired=${System.currentTimeMillis() >= expiry})")
        return loggedIn
    }

    fun getAccessToken(): String? {
        if (!isLoggedIn()) return null
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Start Spotify auth via browser using implicit grant flow.
     * Builds the auth URL manually (Spotify SDK's openLoginInBrowser
     * was forcing response_type=code which requires a server).
     */
    fun startAuth(activity: Activity) {
        Log.d(TAG, "Starting Spotify auth (implicit grant, browser)...")

        val scopeString = SCOPES.joinToString(" ")
        val authUri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", scopeString)
            .appendQueryParameter("show_dialog", "true")
            .build()

        Log.d(TAG, "Auth URI: $authUri")

        try {
            // Try Chrome Custom Tab first (nicer UX, stays in-app)
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
     * Format: underscore://spotify-auth-callback#access_token=XXX&token_type=Bearer&expires_in=3600
     */
    fun handleRedirectUri(uri: Uri): Boolean {
        Log.d(TAG, "Handling redirect URI: $uri")

        val fragment = uri.fragment
        if (fragment == null) {
            Log.e(TAG, "Redirect URI has no fragment")
            val error = uri.getQueryParameter("error")
            if (error != null) Log.e(TAG, "Auth error: $error")
            return false
        }

        val params = fragment.split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }

        val accessToken = params["access_token"]
        val expiresIn = params["expires_in"]?.toIntOrNull()

        if (accessToken != null && expiresIn != null) {
            saveToken(accessToken, expiresIn)
            Log.d(TAG, "Auth success! Token expires in ${expiresIn}s")
            return true
        }

        val error = params["error"]
        Log.e(TAG, "Auth failed — token=${accessToken != null}, expiresIn=$expiresIn, error=$error")
        return false
    }

    fun logout() {
        Log.d(TAG, "Logging out")
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
