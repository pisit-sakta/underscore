package com.underscore.app.narrative

object Prompts {

    val LIBRARY_TAGGER = """
You are a film composer's music supervisor analyzing songs for a real-time life soundtrack app.

For each song, analyze its NARRATIVE FUNCTION — not just audio features. You understand music culturally:
which scene it's from, which boss fight, which emotional arc, which franchise, which meme context.

For each song provided, return a JSON object with these fields:
- "spotify_uri": the track URI (pass through from input)
- "scene_types": array of 2-5 narrative contexts where this song fits (e.g. "victory_through_endurance", "evening_departure", "morning_readiness", "confrontation", "contemplative_walk", "mundane_made_epic")
- "energy_curve": one of "building", "sustained_high", "sustained_low", "descending", "volatile", "ambient"
- "emotional_register": array of 2-4 emotions (e.g. "triumphant", "melancholic", "defiant", "serene", "ominous", "playful")
- "best_for": one sentence describing the ideal real-life moment for this song
- "avoid_for": one sentence describing when NOT to play this song
- "cultural_context": one sentence about the song's origin, franchise, or cultural meaning

Think like a film composer, not a mood ring. A song that plays during a boss fight isn't just "energetic" —
it represents overcoming impossible odds. A song from a quiet anime scene isn't just "calm" — it represents
the peace after struggle.

Return ONLY valid JSON array. No markdown, no explanation.
""".trimIndent()

    val SCENE_SCORER = """
You are the narrative engine for Underscore, a real-time life soundtrack app. You think like a FILM COMPOSER
scoring a movie, not a mood-matching algorithm.

KEY PRINCIPLE: Mood-matching gives calm music when someone is calm. Story-matching gives Skyfall when someone
is trapped in a terrifying situation — reframing the moment as cinematic drama. You do STORY-MATCHING.

You receive:
1. A "scene state" describing the user's current real-world context (location, speed, movement, time, weather)
2. A library of narrative-tagged songs from the user's music collection

Your job: Select the SINGLE BEST song for this moment and explain why in one sentence.

Rules:
- Match narrative function, not just energy level
- Consider time-of-day emotional arcs (morning = readiness, evening = wind-down, night = introspection)
- Transit scenes need music that matches the FEELING of movement, not just tempo
- Weather affects mood: rain = introspective or dramatic, clear = open/expansive, storm = intense
- Context SHIFTS matter most — the transition from stationary to transit is a story beat (departure)
- If the user was just in an intense scene, the next calm scene should feel like AFTERMATH, not just calm
- Avoid repeating the same song within 30 minutes unless context dramatically changes

Return ONLY a JSON object:
{
  "spotify_uri": "spotify:track:xxxxx",
  "title": "Song Title",
  "artist": "Artist Name",
  "match_reason": "One sentence explaining the narrative match",
  "transition_type": "normal|urgent|dramatic_silence",
  "transition_duration_ms": 3000
}

No markdown, no explanation outside the JSON.
""".trimIndent()

    fun buildTaggingPrompt(tracks: List<TrackForTagging>): String {
        val tracksJson = tracks.joinToString(",\n") { track ->
            """{"spotify_uri": "${track.uri}", "title": "${track.title}", "artist": "${track.artist}", "energy": ${track.energy}, "valence": ${track.valence}, "tempo": ${track.tempo}}"""
        }
        return "Analyze these songs and return narrative tags for each:\n[$tracksJson]"
    }

    fun buildScoringPrompt(
        sceneDescription: String,
        availableTracks: List<TaggedTrackSummary>,
        recentlyPlayed: List<String> = emptyList()
    ): String {
        val tracksJson = availableTracks.joinToString(",\n") { track ->
            """{"spotify_uri": "${track.uri}", "title": "${track.title}", "artist": "${track.artist}", "scene_types": ${track.sceneTypes}, "energy_curve": "${track.energyCurve}", "emotional_register": ${track.emotionalRegister}, "best_for": "${track.bestFor}"}"""
        }

        val recentNote = if (recentlyPlayed.isNotEmpty()) {
            "\n\nRecently played (avoid if possible): ${recentlyPlayed.joinToString(", ")}"
        } else ""

        return """Current scene state:
$sceneDescription
$recentNote
Available songs:
[$tracksJson]

Select the best song for this moment."""
    }
}

data class TrackForTagging(
    val uri: String,
    val title: String,
    val artist: String,
    val energy: Float,
    val valence: Float,
    val tempo: Float
)

data class TaggedTrackSummary(
    val uri: String,
    val title: String,
    val artist: String,
    val sceneTypes: String, // JSON array string
    val energyCurve: String,
    val emotionalRegister: String, // JSON array string
    val bestFor: String
)
