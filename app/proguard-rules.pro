# Underscore ProGuard Rules

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.underscore.app.api.** { *; }
-keep class com.underscore.app.data.** { *; }
-keep class com.underscore.app.narrative.GeminiTagResult { *; }
-keep class com.underscore.app.narrative.GeminiSongSelection { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
