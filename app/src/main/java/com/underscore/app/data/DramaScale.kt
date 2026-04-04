package com.underscore.app.data

data class DramaLevel(
    val level: Int,
    val name: String,
    val oneLiner: String,
    val foodName: String,
    val foodOneLiner: String,
    val llmInstruction: String
)

object DramaScale {

    val levels: List<DramaLevel> = listOf(
        DramaLevel(
            level = 1,
            name = "Wallpaper",
            oneLiner = "Background music for a background character.",
            foodName = "Vanilla Ice Cream",
            foodOneLiner = "Vanilla ice cream. On a Tuesday. Alone.",
            llmInstruction = "Minimal scoring. Pick ambient, unobtrusive background music. " +
                "Avoid anything dramatic. The user wants to barely notice the soundtrack."
        ),
        DramaLevel(
            level = 2,
            name = "Ghibli",
            oneLiner = "Life is a Ghibli film. Nothing goes wrong.",
            foodName = "White Rice",
            foodOneLiner = "White rice. Warm. Reliable. Zero surprises.",
            llmInstruction = "Gentle, pastoral scoring. Prefer warm, comforting tracks. " +
                "Everything feels safe and pleasant. No tension, no edge."
        ),
        DramaLevel(
            level = 3,
            name = "Sundance",
            oneLiner = "Indie film. Sundance. Polite applause.",
            foodName = "Buttered Toast",
            foodOneLiner = "Buttered toast. Nobody's ever been hurt by toast.",
            llmInstruction = "Tasteful, understated scoring. Indie film energy. " +
                "Songs should feel curated and thoughtful but never overwhelming."
        ),
        DramaLevel(
            level = 4,
            name = "Slice of Life",
            oneLiner = "Slice of life. Everything matters gently.",
            foodName = "Ketchup",
            foodOneLiner = "Ketchup. A HINT of something. Just a hint.",
            llmInstruction = "Warm narrative scoring. Moments have gentle weight. " +
                "Songs acknowledge what's happening but don't over-dramatize."
        ),
        DramaLevel(
            level = 5,
            name = "Your Movie",
            oneLiner = "Your life is a real movie.",
            foodName = "Salt & Pepper",
            foodOneLiner = "Salt & pepper. The universal baseline.",
            llmInstruction = "Balanced cinematic scoring. Match the emotional reality of " +
                "each moment. This is the default — a real film score for a real life."
        ),
        DramaLevel(
            level = 6,
            name = "HBO",
            oneLiner = "HBO called. They want your daily commute.",
            foodName = "Chili Flakes",
            foodOneLiner = "Chili flakes. Your sinuses notice.",
            llmInstruction = "Elevated dramatic scoring. Lean into tension and atmosphere. " +
                "Mundane moments get a hint of narrative weight. Transitions feel deliberate."
        ),
        DramaLevel(
            level = 7,
            name = "Skyfall",
            oneLiner = "Skyfall. Bond on the beach. Dormant, not retired.",
            foodName = "Whole Raw Chili",
            foodOneLiner = "Whole raw chili. You've committed.",
            llmInstruction = "High cinematic scoring. Every scene feels like it matters. " +
                "Favor bold, atmospheric tracks. Transitions should feel like scene cuts in a thriller."
        ),
        DramaLevel(
            level = 8,
            name = "Anime Protagonist",
            oneLiner = "You are an anime protagonist. Act accordingly.",
            foodName = "That Pepper",
            foodOneLiner = "That pepper someone dared you to eat. No turning back.",
            llmInstruction = "Intense narrative scoring. Lean hard into dramatic moments. " +
                "Even calm scenes carry an undercurrent of destiny. Allow epic tracks for everyday situations."
        ),
        DramaLevel(
            level = 9,
            name = "Medically Inadvisable",
            oneLiner = "Medically inadvisable narrative intensity.",
            foodName = "Ghost Pepper",
            foodOneLiner = "Ghost pepper. The pain is the point.",
            llmInstruction = "Maximum dramatic scoring. Every moment is a scene. Ironic-dramatic " +
                "is encouraged — epic orchestral for buying groceries. Tension everywhere. Go hard."
        ),
        DramaLevel(
            level = 10,
            name = "THE STORM",
            oneLiner = "YOU ARE THE STORM THAT IS APPROACHING.",
            foodName = "Carolina Reaper",
            foodOneLiner = "Carolina Reaper. God has left the kitchen.",
            llmInstruction = "ABSOLUTE MAXIMUM. Score everything like the climax of an epic. " +
                "Ironic-dramatic is the default mode. A taco stand gets orchestral strings. " +
                "A red light gets boss music. Nothing is mundane. EVERYTHING is cinema."
        )
    )

    fun getLevel(scale: Int): DramaLevel {
        return levels.getOrElse(scale.coerceIn(1, 10) - 1) { levels[4] }
    }

    fun getDisplayName(scale: Int, foodMode: Boolean): String {
        val level = getLevel(scale)
        return if (foodMode) level.foodName else level.name
    }

    fun getOneLiner(scale: Int, foodMode: Boolean): String {
        val level = getLevel(scale)
        return if (foodMode) level.foodOneLiner else level.oneLiner
    }
}
