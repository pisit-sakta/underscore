package com.underscore.app.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    val size: Long
)

data class UpdateInfo(
    val buildNumber: Int,
    val releaseName: String,
    val downloadUrl: String,
    val fileSize: Long
)

class AppUpdater(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdater"
        private const val REPO = "pisit-sakta/underscore"
        private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"
        private const val PREFS_NAME = "underscore_updater"
        private const val KEY_DISMISSED_BUILD = "dismissed_build"
    }

    private val client = OkHttpClient()
    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API returned ${response.code}")
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

            // Find the APK asset
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext null

            UpdateInfo(
                buildNumber = remoteBuild,
                releaseName = release.name,
                downloadUrl = apk.downloadUrl,
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

    fun downloadAndInstall(update: UpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "underscore-${update.buildNumber}.apk"

        // Clean old downloads
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        dir?.listFiles()?.filter { it.name.startsWith("underscore-") && it.name.endsWith(".apk") }
            ?.forEach { it.delete() }

        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("Underscore Update")
            .setDescription("Build #${update.buildNumber}")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadId = downloadManager.enqueue(request)

        // Listen for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    ctx.unregisterReceiver(this)
                    installApk(fileName)
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
    }

    private fun installApk(fileName: String) {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        if (!file.exists()) {
            Log.e(TAG, "Downloaded APK not found: $fileName")
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
    }
}
