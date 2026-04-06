package com.underscore.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    val url: String,  // API URL — works for private repos (browser_download_url doesn't)
    val size: Long
)

data class UpdateInfo(
    val buildNumber: Int,
    val releaseName: String,
    val downloadUrl: String,
    val fileSize: Long
)

data class DownloadProgress(
    val state: DownloadState,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null
) {
    val percent: Int get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
}

enum class DownloadState {
    IDLE, DOWNLOADING, INSTALLING, FAILED
}

class AppUpdater(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdater"
        const val REPO = "pisit-sakta/underscore"
        private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"
        private const val PREFS_NAME = "underscore_updater"
        private const val KEY_DISMISSED_BUILD = "dismissed_build"
        // Token revoked by GitHub secret scanning — public repo doesn't need auth anyway
        const val GITHUB_TOKEN = ""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()
    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _downloadProgress = MutableStateFlow(DownloadProgress(DownloadState.IDLE))
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    fun getCurrentBuildNumber(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Public repo — no auth needed
            val requestBuilder = Request.Builder()
                .url(API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
            if (GITHUB_TOKEN.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "token $GITHUB_TOKEN")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "GitHub API returned ${response.code} — update check failed")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val release = gson.fromJson(body, GitHubRelease::class.java)

            // Parse build number from tag: "build-42" -> 42
            val remoteBuild = release.tagName
                .removePrefix("build-")
                .toIntOrNull() ?: return@withContext null

            val localBuild = getCurrentBuildNumber()
            Log.d(TAG, "Local build: $localBuild, Remote build: $remoteBuild")

            if (remoteBuild <= localBuild) {
                Log.d(TAG, "Already up to date")
                return@withContext null
            }

            // Check if user dismissed this version
            val dismissed = prefs.getInt(KEY_DISMISSED_BUILD, 0)
            if (remoteBuild == dismissed) {
                Log.d(TAG, "User dismissed build $remoteBuild")
                return@withContext null
            }

            // Find the APK asset — use browser download URL (works for public repos)
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext null

            UpdateInfo(
                buildNumber = remoteBuild,
                releaseName = release.name,
                downloadUrl = apk.browserDownloadUrl,  // Direct download — public repo
                fileSize = apk.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }

    fun dismissUpdate(buildNumber: Int) {
        prefs.edit().putInt(KEY_DISMISSED_BUILD, buildNumber).apply()
    }

    /**
     * Download APK and trigger install.
     * Uses browserDownloadUrl which is a direct download for public repos.
     */
    suspend fun downloadAndInstall(update: UpdateInfo) = withContext(Dispatchers.IO) {
        val fileName = "underscore-${update.buildNumber}.apk"

        try {
            _downloadProgress.value = DownloadProgress(DownloadState.DOWNLOADING, 0, update.fileSize)

            // Clean old downloads
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            dir?.listFiles()?.filter { it.name.startsWith("underscore-") && it.name.endsWith(".apk") }
                ?.forEach { it.delete() }

            val file = File(dir, fileName)

            // Direct download — browserDownloadUrl works for public repos without auth
            val request = Request.Builder()
                .url(update.downloadUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val error = "Download failed: HTTP ${response.code}"
                Log.e(TAG, error)
                _downloadProgress.value = DownloadProgress(DownloadState.FAILED, error = error)
                return@withContext
            }

            val body = response.body ?: run {
                _downloadProgress.value = DownloadProgress(DownloadState.FAILED, error = "Empty response")
                return@withContext
            }

            val totalBytes = body.contentLength().let { if (it > 0) it else update.fileSize }
            var bytesDownloaded = 0L

            file.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        _downloadProgress.value = DownloadProgress(
                            DownloadState.DOWNLOADING, bytesDownloaded, totalBytes
                        )
                    }
                }
            }

            Log.d(TAG, "Download complete: ${file.length()} bytes")
            _downloadProgress.value = DownloadProgress(DownloadState.INSTALLING, totalBytes, totalBytes)

            // Trigger install
            withContext(Dispatchers.Main) {
                installApk(fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            _downloadProgress.value = DownloadProgress(DownloadState.FAILED, error = e.message)
        }
    }

    private fun installApk(fileName: String) {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        if (!file.exists()) {
            Log.e(TAG, "Downloaded APK not found: $fileName")
            _downloadProgress.value = DownloadProgress(DownloadState.FAILED, error = "APK file not found")
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        _downloadProgress.value = DownloadProgress(DownloadState.IDLE)
    }
}
