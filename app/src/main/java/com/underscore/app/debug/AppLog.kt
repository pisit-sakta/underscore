package com.underscore.app.debug

import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * In-memory log buffer that captures logs even when logcat is inaccessible.
 * HONOR/Android 15 blocks logcat -d from reading the app's own logs,
 * so we need this to make bug reports actually useful.
 */
object AppLog {

    private const val MAX_LINES = 500
    private val buffer = ArrayDeque<String>(MAX_LINES)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    @Synchronized
    fun d(tag: String, msg: String) {
        write("D", tag, msg)
        Log.d(tag, msg)
    }

    @Synchronized
    fun w(tag: String, msg: String) {
        write("W", tag, msg)
        Log.w(tag, msg)
    }

    @Synchronized
    fun e(tag: String, msg: String, t: Throwable? = null) {
        write("E", tag, msg)
        if (t != null) {
            write("E", tag, t.stackTraceToString().take(500))
            Log.e(tag, msg, t)
        } else {
            Log.e(tag, msg)
        }
    }

    @Synchronized
    fun getLines(): List<String> = buffer.toList()

    @Synchronized
    fun dump(): String = buffer.joinToString("\n")

    private fun write(level: String, tag: String, msg: String) {
        val time = timeFormat.format(Instant.now())
        val line = "$time $level/$tag: $msg"
        if (buffer.size >= MAX_LINES) buffer.removeFirst()
        buffer.addLast(line)
    }
}
