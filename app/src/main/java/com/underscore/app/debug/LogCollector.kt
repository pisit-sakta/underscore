package com.underscore.app.debug

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class LogCollector(private val context: Context) {

    companion object {
        private const val TAG = "LogCollector"
        private const val MAX_LINES = 500
        private const val REPO = "pisit-sakta/underscore"
    }

    /**
     * One-tap bug report: collects logs, opens a pre-filled GitHub issue in browser.
     * User just taps "Submit new issue" — that's it.
     */
    fun reportBug() {
        val report = collectReport()

        // GitHub new issue URL with pre-filled title and body
        // URL max length is ~8000 chars for most browsers, so truncate if needed
        val title = "Bug report (Build ${getAppBuildNumber()}, Android ${Build.VERSION.RELEASE})"
        val body = if (report.length > 6000) {
            report.take(5900) + "\n\n... (truncated, full report too long for URL)"
        } else {
            report
        }

        val url = "https://github.com/$REPO/issues/new" +
                "?title=${Uri.encode(title)}" +
                "&body=${Uri.encode(body)}" +
                "&labels=bug"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
            Log.d(TAG, "Opened GitHub issue (${report.length} chars)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser for bug report: ${e.message}", e)
            // Fallback to share sheet
            shareReport()
        }
    }

    fun collectReport(): String {
        val deviceInfo = buildDeviceInfo()
        val recentLogs = collectLogcat()

        return buildString {
            appendLine("## Underscore Debug Report")
            appendLine("Generated: `${java.time.Instant.now()}`")
            appendLine()
            appendLine("### Device Info")
            appendLine("```")
            append(deviceInfo)
            appendLine("```")
            appendLine()
            appendLine("### App Config")
            appendLine("```")
            append(collectAppConfig())
            appendLine("```")
            appendLine()
            appendLine("### Recent Logs")
            appendLine("<details><summary>Last $MAX_LINES lines (click to expand)</summary>")
            appendLine()
            appendLine("```")
            appendLine(recentLogs)
            appendLine("```")
            appendLine()
            appendLine("</details>")
        }
    }

    fun shareReport() {
        val report = collectReport()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Underscore Debug Report")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Share debug report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        Log.d(TAG, "Debug report shared (${report.length} chars)")
    }

    private fun buildDeviceInfo(): String = buildString {
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("App version: ${getAppVersion()}")
        appendLine("Build: ${getAppBuildNumber()}")
    }

    private fun collectAppConfig(): String {
        val prefs = context.getSharedPreferences("underscore_prefs", Context.MODE_PRIVATE)
        return buildString {
            appendLine("LLM Provider: ${prefs.getString("llm_provider", "GEMINI")}")
            appendLine("Custom API URL: ${if (prefs.getString("custom_api_url", "")?.isNotBlank() == true) "(set)" else "(empty)"}")
            appendLine("Custom Model: ${prefs.getString("custom_model", "(none)")}")
            appendLine("Gemini key: ${if (prefs.getString("gemini_api_key", "")?.isNotBlank() == true) "(set)" else "(empty)"}")
            appendLine("Claude key: ${if (prefs.getString("claude_api_key", "")?.isNotBlank() == true) "(set)" else "(empty)"}")
            appendLine("Weather key: ${if (prefs.getString("weather_api_key", "")?.isNotBlank() == true) "(set)" else "(empty)"}")
            appendLine("Places key: ${if (prefs.getString("places_api_key", "")?.isNotBlank() == true) "(set)" else "(empty)"}")
            appendLine("Battery saver: ${prefs.getBoolean("battery_saver", false)}")
        }
    }

    private fun collectLogcat(): String {
        return try {
            val tags = listOf(
                "UnderscoreService", "NarrativeEngine", "GeminiApi", "ClaudeApi",
                "OpenAiCompatibleApi", "SpotifyWebApi", "SpotifyAuth", "SensorAggregator",
                "LocationProvider", "MotionDetector", "HeartRateProvider", "PlacesProvider",
                "WeatherProvider", "ContextEngine", "PlaybackController", "TransitionManager",
                "LibraryAnalyzer", "KnownLocationManager", "ProtagonistProfile", "AppUpdater",
                "LogCollector", "ZoneScorer"
            )

            val filterArgs = tags.flatMap { listOf("$it:V") } + listOf("*:S")
            val cmd = listOf("logcat", "-d", "-t", MAX_LINES.toString()) + filterArgs

            val process = Runtime.getRuntime().exec(cmd.toTypedArray())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()
            process.waitFor()

            if (lines.isEmpty()) {
                val fallback = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "200"))
                val fallbackReader = BufferedReader(InputStreamReader(fallback.inputStream))
                val fallbackLines = fallbackReader.readLines()
                fallbackReader.close()
                fallback.waitFor()
                "(filtered logcat empty, showing last 200 lines unfiltered)\n" +
                    fallbackLines.joinToString("\n")
            } else {
                lines.joinToString("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect logcat: ${e.message}", e)
            "(logcat collection failed: ${e.message})"
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    private fun getAppBuildNumber(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
        } catch (e: Exception) { "unknown" }
    }
}
