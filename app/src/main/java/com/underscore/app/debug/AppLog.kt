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

    private const val MAX_LINES = 1000
    private const val MAX_ERRORS = 200
    private val buffer = ArrayDeque<String>(MAX_LINES)
    /** Separate buffer for warnings and errors so they don't get rotated out by debug noise */
    private val errorBuffer = ArrayDeque<String>(MAX_ERRORS)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    @Synchronized
    fun d(tag: String, msg: String) {
        write("D", tag, msg)
        Log.d(tag, msg)
    }

    @Synchronized
    fun w(tag: String, msg: String) {
        val line = write("W", tag, msg)
        if (errorBuffer.size >= MAX_ERRORS) errorBuffer.removeFirst()
        errorBuffer.addLast(line)
        Log.w(tag, msg)
    }

    @Synchronized
    fun e(tag: String, msg: String, t: Throwable? = null) {
        val line = write("E", tag, msg)
        if (errorBuffer.size >= MAX_ERRORS) errorBuffer.removeFirst()
        errorBuffer.addLast(line)
        if (t != null) {
            val stackLine = write("E", tag, t.stackTraceToString().take(500))
            if (errorBuffer.size >= MAX_ERRORS) errorBuffer.removeFirst()
            errorBuffer.addLast(stackLine)
            Log.e(tag, msg, t)
        } else {
            Log.e(tag, msg)
        }
    }

    @Synchronized
    fun getLines(): List<String> = buffer.toList()

    @Synchronized
    fun getErrors(): List<String> = errorBuffer.toList()

    @Synchronized
    fun dump(): String = buffer.joinToString("\n")

    private fun write(level: String, tag: String, msg: String): String {
        val time = timeFormat.format(Instant.now())
        val line = "$time $level/$tag: $msg"
        if (buffer.size >= MAX_LINES) buffer.removeFirst()
        buffer.addLast(line)
        return line
    }
}
