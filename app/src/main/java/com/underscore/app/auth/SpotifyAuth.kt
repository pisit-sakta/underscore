package com.underscore.app.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import java.security.MessageDigest
import java.security.SecureRandom

class SpotifyAuth(private val context: Context) {

    companion object {
        // ⚠️ REPLACE THIS with your Spotify Developer App Client ID
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
        return token != null && System.currentTimeMillis() < expiry
    }

    fun getAccessToken(): String? {
        if (!isLoggedIn()) return null
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun startAuth(activity: Activity) {
        val request = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )
            .setScopes(SCOPES)
            .setShowDialog(false)
            .build()

        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
    }

    fun handleAuthResponse(resultCode: Int, data: Intent?): Boolean {
        val response = AuthorizationClient.getResponse(resultCode, data)
        return when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                saveToken(response.accessToken, response.expiresIn)
                Log.d(TAG, "Auth success, token expires in ${response.expiresIn}s")
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
    }
}
