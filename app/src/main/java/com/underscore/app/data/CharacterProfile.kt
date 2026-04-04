package com.underscore.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "character_profiles")
data class CharacterProfile(
    @PrimaryKey val name: String,
    val franchise: String,
    val tagline: String,
    val color1: String,               // Hex color e.g. "#1B3D2F"
    val color2: String,               // Hex color e.g. "#0A0A0A"
    val colorReference: String,       // Why these colors
    val narrativeAesthetic: String,   // LLM instruction for scoring style
    val primaryGenres: String,        // JSON array of genre strings
    val transitionStyle: String,      // "smooth", "jarring", "vinyl_crackle", etc.
    val emotionalArchitecture: String, // JSON map: situation → music style
    val humorPreference: String,      // "ironic_dramatic", "sincere", "deadpan", "chaotic"
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CharacterProfileDao {
    @Query("SELECT * FROM character_profiles ORDER BY isPreset DESC, name ASC")
    suspend fun getAll(): List<CharacterProfile>

    @Query("SELECT * FROM character_profiles WHERE isPreset = 1 ORDER BY name ASC")
    suspend fun getPresets(): List<CharacterProfile>

    @Query("SELECT * FROM character_profiles WHERE isPreset = 0 ORDER BY createdAt DESC")
    suspend fun getCustom(): List<CharacterProfile>

    @Query("SELECT * FROM character_profiles WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CharacterProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CharacterProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<CharacterProfile>)

    @Query("DELETE FROM character_profiles WHERE name = :name AND isPreset = 0")
    suspend fun deleteCustom(name: String)

    @Query("SELECT COUNT(*) FROM character_profiles WHERE isPreset = 1")
    suspend fun presetCount(): Int
}
