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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ─── Tagged Songs (Sprint 1) ──────────────────────────

@Entity(tableName = "tagged_songs")
data class TaggedSong(
    @PrimaryKey val spotifyUri: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val sceneTypes: String = "[]",
    val energyCurve: String = "ambient",
    val emotionalRegister: String = "[]",
    val bestFor: String = "",
    val avoidFor: String = "",
    val culturalContext: String = "",
    val energy: Float = 0f,
    val valence: Float = 0f,
    val tempo: Float = 0f,
    val taggedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
    // Sprint 2: learning feedback
    @ColumnInfo(defaultValue = "0") val skipCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val boostCount: Int = 0
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

    @Query("UPDATE tagged_songs SET skipCount = skipCount + 1 WHERE spotifyUri = :uri")
    suspend fun recordSkip(uri: String)

    @Query("UPDATE tagged_songs SET boostCount = boostCount + 1 WHERE spotifyUri = :uri")
    suspend fun recordBoost(uri: String)

    @Query("SELECT * FROM tagged_songs WHERE lastPlayedAt < :cutoff OR lastPlayedAt = 0 ORDER BY RANDOM()")
    suspend fun getNotRecentlyPlayed(cutoff: Long): List<TaggedSong>

    @Query("SELECT spotifyUri FROM tagged_songs WHERE lastPlayedAt > :since ORDER BY lastPlayedAt DESC")
    suspend fun getRecentlyPlayedUris(since: Long): List<String>

    @Query("DELETE FROM tagged_songs")
    suspend fun deleteAll()
}

// ─── Scene History (Sprint 2) ──────────────────────────

@Entity(tableName = "scene_history")
data class SceneHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val classification: String,         // SceneClassification name
    val placeType: String? = null,
    val zoneCharacter: String? = null,
    val songUri: String,
    val songTitle: String,
    val songArtist: String,
    val matchReason: String,
    val transitionType: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val weather: String? = null,
    val minutesInScene: Int = 0,
    val feedback: String? = null        // "skip", "boost", or null
)

@Dao
interface SceneHistoryDao {
    @Insert
    suspend fun insert(entry: SceneHistoryEntry): Long

    @Query("SELECT * FROM scene_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<SceneHistoryEntry>

    @Query("SELECT * FROM scene_history WHERE placeType = :placeType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByPlaceType(placeType: String, limit: Int = 20): List<SceneHistoryEntry>

    @Query("UPDATE scene_history SET feedback = :feedback WHERE id = :id")
    suspend fun setFeedback(id: Long, feedback: String)

    @Query("SELECT songUri, COUNT(*) as cnt FROM scene_history WHERE placeType = :placeType AND feedback != 'skip' GROUP BY songUri ORDER BY cnt DESC LIMIT :limit")
    suspend fun getTopSongsForPlace(placeType: String, limit: Int = 10): List<SongFrequency>

    @Query("DELETE FROM scene_history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

data class SongFrequency(
    val songUri: String,
    val cnt: Int
)

// ─── Known Locations (Sprint 2) ────────────────────────

@Entity(tableName = "known_locations")
data class KnownLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,                  // "home", "office", "gym", user-editable
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val placeType: String? = null,
    val visitCount: Int = 1,
    val firstVisitedAt: Long = System.currentTimeMillis(),
    val lastVisitedAt: Long = System.currentTimeMillis(),
    val leitmotifUri: String? = null,   // Song that has become this place's theme
    val leitmotifTitle: String? = null
)

@Dao
interface KnownLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: KnownLocation): Long

    @Query("SELECT * FROM known_locations")
    suspend fun getAll(): List<KnownLocation>

    @Query("SELECT * FROM known_locations WHERE label = :label LIMIT 1")
    suspend fun getByLabel(label: String): KnownLocation?

    @Query("UPDATE known_locations SET visitCount = visitCount + 1, lastVisitedAt = :timestamp WHERE id = :id")
    suspend fun recordVisit(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE known_locations SET leitmotifUri = :uri, leitmotifTitle = :title WHERE id = :id")
    suspend fun setLeitmotif(id: Long, uri: String, title: String)

    @Query("DELETE FROM known_locations WHERE id = :id")
    suspend fun delete(id: Long)
}

// ─── Database ──────────────────────────────────────────

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add learning feedback columns to tagged_songs
        db.execSQL("ALTER TABLE tagged_songs ADD COLUMN skipCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tagged_songs ADD COLUMN boostCount INTEGER NOT NULL DEFAULT 0")

        // Create scene_history table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scene_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                classification TEXT NOT NULL,
                placeType TEXT,
                zoneCharacter TEXT,
                songUri TEXT NOT NULL,
                songTitle TEXT NOT NULL,
                songArtist TEXT NOT NULL,
                matchReason TEXT NOT NULL,
                transitionType TEXT NOT NULL,
                latitude REAL NOT NULL DEFAULT 0.0,
                longitude REAL NOT NULL DEFAULT 0.0,
                weather TEXT,
                minutesInScene INTEGER NOT NULL DEFAULT 0,
                feedback TEXT
            )
        """.trimIndent())

        // Create known_locations table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS known_locations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                radiusMeters REAL NOT NULL DEFAULT 100.0,
                placeType TEXT,
                visitCount INTEGER NOT NULL DEFAULT 1,
                firstVisitedAt INTEGER NOT NULL,
                lastVisitedAt INTEGER NOT NULL,
                leitmotifUri TEXT,
                leitmotifTitle TEXT
            )
        """.trimIndent())
    }
}

@Database(
    entities = [TaggedSong::class, SceneHistoryEntry::class, KnownLocation::class],
    version = 2,
    exportSchema = false
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun taggedSongDao(): TaggedSongDao
    abstract fun sceneHistoryDao(): SceneHistoryDao
    abstract fun knownLocationDao(): KnownLocationDao

    companion object {
        @Volatile
        private var INSTANCE: SongDatabase? = null

        fun getInstance(context: Context): SongDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SongDatabase::class.java,
                    "underscore_songs"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
