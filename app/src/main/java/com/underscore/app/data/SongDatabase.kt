package com.underscore.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "tagged_songs")
data class TaggedSong(
    @PrimaryKey val spotifyUri: String,
    val title: String,
    val artist: String,
    val album: String = "",
    // Narrative tags from Gemini
    val sceneTypes: String = "[]",       // JSON array
    val energyCurve: String = "ambient",
    val emotionalRegister: String = "[]", // JSON array
    val bestFor: String = "",
    val avoidFor: String = "",
    val culturalContext: String = "",
    // Spotify audio features
    val energy: Float = 0f,
    val valence: Float = 0f,
    val tempo: Float = 0f,
    // Metadata
    val taggedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val playCount: Int = 0,
    val lastPlayedAt: Long = 0
)

@Dao
interface TaggedSongDao {
    @Query("SELECT * FROM tagged_songs")
    suspend fun getAll(): List<TaggedSong>

    @Query("SELECT * FROM tagged_songs WHERE spotifyUri = :uri")
    suspend fun getByUri(uri: String): TaggedSong?

    @Query("SELECT COUNT(*) FROM tagged_songs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<TaggedSong>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: TaggedSong)

    @Query("UPDATE tagged_songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE spotifyUri = :uri")
    suspend fun recordPlay(uri: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tagged_songs WHERE lastPlayedAt < :cutoff OR lastPlayedAt = 0 ORDER BY RANDOM()")
    suspend fun getNotRecentlyPlayed(cutoff: Long): List<TaggedSong>

    @Query("SELECT spotifyUri FROM tagged_songs WHERE lastPlayedAt > :since ORDER BY lastPlayedAt DESC")
    suspend fun getRecentlyPlayedUris(since: Long): List<String>

    @Query("DELETE FROM tagged_songs")
    suspend fun deleteAll()
}

@Database(entities = [TaggedSong::class], version = 1, exportSchema = false)
abstract class SongDatabase : RoomDatabase() {
    abstract fun taggedSongDao(): TaggedSongDao

    companion object {
        @Volatile
        private var INSTANCE: SongDatabase? = null

        fun getInstance(context: Context): SongDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SongDatabase::class.java,
                    "underscore_songs"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
