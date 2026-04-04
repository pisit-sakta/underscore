package com.underscore.app.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class FranchiseProfile(
    val name: String = "",
    val mood: String = "",
    val color1: String = "",
    val color2: String = "",
    @SerializedName("color_reference") val colorReference: String = "",
    val aesthetic: String = "",
    @SerializedName("primary_genres") val primaryGenres: List<String> = emptyList()
) {
    companion object {
        @Transient private val gson = Gson()

        fun fromJson(json: String): FranchiseProfile? = try {
            if (json.isBlank()) null
            else gson.fromJson(json, FranchiseProfile::class.java)
        } catch (e: Exception) { null }

        fun toJson(profile: FranchiseProfile): String = gson.toJson(profile)
    }

    fun toJson(): String = Companion.toJson(this)
}
