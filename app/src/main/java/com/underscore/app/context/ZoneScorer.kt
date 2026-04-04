package com.underscore.app.context

import com.underscore.app.sensor.PlacesResult

data class ZoneScore(
    val placeType: String,            // "gym", "temple", "restaurant", etc.
    val zoneCharacter: String,        // "commercial", "nightlife", "cultural", etc.
    val tonalPalette: String,         // Musical/atmospheric descriptor for Gemini
    val narrativeFunction: String,    // What this place means in a protagonist's story
    val nearbyLandmarks: List<String>
)

class ZoneScorer {

    fun score(places: PlacesResult?, timeOfDay: TimeOfDay): ZoneScore {
        if (places == null) {
            return defaultZone(timeOfDay)
        }

        val tonalPalette = deriveTonalPalette(places.placeType, places.zoneCharacter, timeOfDay)
        val narrativeFunction = deriveNarrativeFunction(places.placeType, timeOfDay)

        return ZoneScore(
            placeType = places.placeType,
            zoneCharacter = places.zoneCharacter,
            tonalPalette = tonalPalette,
            narrativeFunction = narrativeFunction,
            nearbyLandmarks = places.nearbyLandmarks
        )
    }

    private fun deriveTonalPalette(
        placeType: String,
        zoneCharacter: String,
        timeOfDay: TimeOfDay
    ): String {
        // Place-specific palettes first
        val placePalette = when (placeType) {
            "gym" -> "combat_preparation, training_grounds, physical_intensity"
            "temple" -> "sacred_space, reverential, weighted_silence, spiritual"
            "park" -> "pastoral, open_air, nature, ghibli_adjacent"
            "restaurant" -> "social_warmth, culinary_atmosphere, grounded"
            "nightlife" -> "electronic, urban_pulse, neon, cyberpunk"
            "shopping" -> "commercial_energy, light_bouncy, browsing"
            "convenience_store" -> "rpg_shop_theme, mundane_made_epic, liminal"
            "bank" -> "institutional_gravity, formal, dramatic_ironic"
            "transit_hub" -> "transitional, liminal_space, between_worlds, departure"
            "hospital" -> "clinical_tension, sterile, emotional_weight"
            "school" -> "academic, structured, youthful_energy"
            "hotel" -> "temporary_refuge, waypoint, noir_undertones"
            "entertainment" -> "spectacle, excitement, shared_experience"
            "gas_station" -> "road_stop, liminal, americana_or_local_equivalent"
            "office" -> "daily_dungeon, professional, contained_energy"
            else -> zoneCharacterToPalette(zoneCharacter)
        }

        // Time-of-day modifier
        val timeModifier = when (timeOfDay) {
            TimeOfDay.MORNING -> "dawn_energy, fresh_start"
            TimeOfDay.AFTERNOON -> "midday_grind, sustained"
            TimeOfDay.EVENING -> "golden_hour, winding_arc"
            TimeOfDay.NIGHT -> "nocturnal, noir, intimate"
        }

        return "$placePalette, $timeModifier"
    }

    private fun zoneCharacterToPalette(zoneCharacter: String): String = when (zoneCharacter) {
        "commercial" -> "urban_commercial, bustling, street_level"
        "nightlife" -> "electronic, neon_soaked, after_dark"
        "dining" -> "social_warmth, ambient_conversation, grounded"
        "residential" -> "home_territory, familiar, save_point_adjacent"
        "cultural" -> "reverential, heritage, atmospheric_weight"
        "nature" -> "open_sky, pastoral, organic"
        "transit" -> "transitional, in_motion, liminal"
        "institutional" -> "structured, formal, institutional_gravity"
        else -> "urban_ambient, neutral"
    }

    private fun deriveNarrativeFunction(placeType: String, timeOfDay: TimeOfDay): String =
        when (placeType) {
            "gym" -> "training_grounds — protagonist preparing for battle"
            "temple" -> "sacred_ground — contemplation, spiritual weight, inner peace or inner storm"
            "park" -> "open_world_exploration — breathing room in the protagonist's arc"
            "restaurant" -> "social_encounter — breaking bread, alliance or intrigue"
            "nightlife" -> "underworld_district — night operations, social combat, letting loose"
            "shopping" -> "supply_run — equipping for the next arc"
            "convenience_store" -> "rpg_item_shop — mundane errand scored with ironic grandeur"
            "bank" -> "a_lannister_always_pays_his_debts — financial encounter as power play"
            "transit_hub" -> "departure_point — a journey begins or ends"
            "hospital" -> "medical_ward — vulnerability, stakes revealed"
            "office" -> "daily_dungeon — the grind, professional arena"
            "hotel" -> "waystation — temporary respite, noir atmosphere"
            "entertainment" -> "spectacle_arena — shared excitement, event energy"
            else -> when (timeOfDay) {
                TimeOfDay.MORNING -> "morning_patrol — surveying the territory"
                TimeOfDay.AFTERNOON -> "midday_operations — protagonist in motion"
                TimeOfDay.EVENING -> "evening_debrief — arc winding down"
                TimeOfDay.NIGHT -> "night_operations — protagonist after dark"
            }
        }

    private fun defaultZone(timeOfDay: TimeOfDay): ZoneScore = ZoneScore(
        placeType = "unknown",
        zoneCharacter = "urban",
        tonalPalette = "urban_ambient, neutral, ${when (timeOfDay) {
            TimeOfDay.MORNING -> "dawn_energy"
            TimeOfDay.AFTERNOON -> "midday_grind"
            TimeOfDay.EVENING -> "golden_hour"
            TimeOfDay.NIGHT -> "nocturnal"
        }}",
        narrativeFunction = when (timeOfDay) {
            TimeOfDay.MORNING -> "morning_patrol"
            TimeOfDay.AFTERNOON -> "midday_operations"
            TimeOfDay.EVENING -> "evening_debrief"
            TimeOfDay.NIGHT -> "night_operations"
        },
        nearbyLandmarks = emptyList()
    )
}
