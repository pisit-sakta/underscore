package com.underscore.app.narrative

import com.underscore.app.context.SceneClassification

data class TrackInfo(
    val uri: String,
    val title: String,
    val artist: String
)

class SongSelector {

    // Hardcoded song mappings for Sprint 0.
    // These are real Spotify track URIs from the founder's aesthetic profile.
    // Replace or extend with your own library.
    private val songMap: Map<SceneClassification, List<TrackInfo>> = mapOf(

        SceneClassification.MORNING_STATIONARY to listOf(
            TrackInfo("spotify:track:2igMBOgVCLHMFXl2MSt5Al", "Red Right Hand", "Nick Cave & The Bad Seeds"),
            TrackInfo("spotify:track:70wCIJlNJSFLiZEPWP30De", "Atmosphere", "Joy Division"),
        ),

        SceneClassification.TRANSIT to listOf(
            // MGSV / Action-cinematic riding music
            TrackInfo("spotify:track:1JOmcgnMITMTRaBgqoG9Hv", "Nuclear", "Mike Oldfield"),
            TrackInfo("spotify:track:4OlBnKvcZIBOKRmGmfJBjk", "Sins of the Father", "Donna Burke"),
            TrackInfo("spotify:track:0l3lGiCmoqcBdwKq9a1Y9S", "The Man Who Sold the World", "Midge Ure"),
        ),

        SceneClassification.DAYTIME_STATIONARY to listOf(
            // MGSV ambient — existential but purposeful
            TrackInfo("spotify:track:6c7jODMNpgVjjY7NWDVIMI", "Quiet's Theme", "Ludvig Forssell"),
            TrackInfo("spotify:track:6habFhsAe1ByKShYTGpiHx", "Behind the Mirror", "Ludvig Forssell"),
        ),

        SceneClassification.EVENING_STATIONARY to listOf(
            // Wind-down — atmospheric, contemplative
            TrackInfo("spotify:track:1WPHOM2VB9RFdm0BjVn3cP", "Merry-Go-Round of Life", "Joe Hisaishi"),
            TrackInfo("spotify:track:0ArMjW3gEzFz20cA8JXXGB", "One Summer's Day", "Joe Hisaishi"),
        ),

        SceneClassification.NIGHT_STATIONARY to listOf(
            TrackInfo("spotify:track:3p4TxOueZX7VWXBF8SNgnE", "Dearly Beloved", "Yoko Shimomura"),
            TrackInfo("spotify:track:0ArMjW3gEzFz20cA8JXXGB", "One Summer's Day", "Joe Hisaishi"),
        ),

        SceneClassification.WALKING to listOf(
            TrackInfo("spotify:track:6c7jODMNpgVjjY7NWDVIMI", "Quiet's Theme", "Ludvig Forssell"),
            TrackInfo("spotify:track:7A9rdAYaPjOi2RiIqMp0lb", "A Phantom Pain", "Ludvig Forssell"),
        ),

        SceneClassification.ACTIVE to listOf(
            // High intensity — Metal Gear Rising / DMC5
            TrackInfo("spotify:track:6CdJcEqmWCkXPJP2jGdTYX", "Bury the Light", "Casey Edwards"),
            TrackInfo("spotify:track:5sd7GI5Y0Fj8t9xLP4rN3Q", "Rules of Nature", "Jamie Christopherson"),
            TrackInfo("spotify:track:3ntydxRplLOXjpQgUoZdxB", "The Only Thing I Know for Real", "Jamie Christopherson"),
        ),

        SceneClassification.UNKNOWN to listOf(
            TrackInfo("spotify:track:6c7jODMNpgVjjY7NWDVIMI", "Quiet's Theme", "Ludvig Forssell"),
        ),
    )

    fun selectTrack(classification: SceneClassification): TrackInfo {
        val tracks = songMap[classification] ?: songMap[SceneClassification.UNKNOWN]!!
        return tracks.random()
    }

    fun getTracksForScene(classification: SceneClassification): List<TrackInfo> {
        return songMap[classification] ?: songMap[SceneClassification.UNKNOWN]!!
    }
}
