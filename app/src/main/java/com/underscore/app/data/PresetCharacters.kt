package com.underscore.app.data

object PresetCharacters {

    val ALL: List<CharacterProfile> = listOf(
        CharacterProfile(
            name = "James Bond (Craig)",
            franchise = "Bond Films",
            tagline = "Shaken, not stirred.",
            color1 = "#1B3D2F",   // Dark forest green
            color2 = "#0A0A0A",   // Black
            colorReference = "Skyfall palette — Scottish Highlands green meets MI6 darkness",
            narrativeAesthetic = "Cold, calculated elegance. Every scene carries weight. " +
                "Smooth transitions, atmospheric tension. Even calm moments feel like the " +
                "quiet before a storm. Think Skyfall, Casino Royale — wounded sophistication.",
            primaryGenres = """["orchestral","cinematic","jazz","trip-hop"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Ominous readiness — cold open energy, the day is a mission",
                "commute": "Driving tension — Bond in the Aston Martin, purposeful movement",
                "work": "Espionage ambient — surveillance, calculation, hidden danger",
                "social": "Casino jazz — charm as weapon, reading the room",
                "confrontation": "Skyfall orchestral — controlled violence, precise and lethal",
                "evening": "Melancholic piano — the cost of the mission, Vesper's ghost",
                "setback": "Dark atmospheric — wounded but walking, MI6 won't break",
                "victory": "Triumphant brass — the Bond theme earned, not given"
            }""",
            humorPreference = "deadpan",
            isPreset = true
        ),
        CharacterProfile(
            name = "Twilight (Loid Forger)",
            franchise = "Spy x Family",
            tagline = "The mission is the family.",
            color1 = "#1A3A3A",   // Dark teal
            color2 = "#C4727F",   // Rose pink
            colorReference = "Espionage meets the Forger family warmth",
            narrativeAesthetic = "Dual identity scoring — sharp, efficient spy energy that " +
                "softens into family warmth. Comedy of maintaining cover. The tension between " +
                "duty and genuine affection. Elegant but human.",
            primaryGenres = """["jazz","orchestral","j-pop","anime OST"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Domestic warmth with an edge — preparing Anya's lunch while planning an op",
                "commute": "Spy thriller movement — efficient, purposeful, eyes scanning",
                "work": "Espionage precision — WISE agent in the field",
                "social": "Comedy of errors — maintaining the Forger cover, charming chaos",
                "confrontation": "Sharp, calculated action — Twilight unleashed, no hesitation",
                "evening": "Family warmth — the real feelings he won't admit to having",
                "setback": "Mission compromised tension — recalculating, adapting",
                "victory": "Family intact — the mission was always them"
            }""",
            humorPreference = "deadpan",
            isPreset = true
        ),
        CharacterProfile(
            name = "Saul Goodman",
            franchise = "Better Call Saul / Breaking Bad",
            tagline = "I know a guy.",
            color1 = "#C4962C",   // Muted gold
            color2 = "#1A2744",   // Deep navy
            colorReference = "The showman's flash meets legal darkness",
            narrativeAesthetic = "Hustler energy meets legal drama. Fast-talking confidence " +
                "masking genuine intelligence. The line between charm and desperation. " +
                "Albuquerque desert heat. Every conversation is a negotiation.",
            primaryGenres = """["blues","rock","jazz","southwestern"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Showtime readiness — putting on the suit, becoming Saul",
                "commute": "Desert drive rock — top down, scheming in motion",
                "work": "Courtroom jazz — performance, persuasion, the art of the deal",
                "social": "Schmooze blues — everyone's a potential client or mark",
                "confrontation": "Desperate brilliance — talking his way out, always",
                "evening": "Jimmy McGill quiet — the man behind the persona, alone",
                "setback": "Slipping Jimmy blues — the hustle that didn't land",
                "victory": "Showman's triumph — 'S'all good, man!'"
            }""",
            humorPreference = "chaotic",
            isPreset = true
        ),
        CharacterProfile(
            name = "Thomas Shelby",
            franchise = "Peaky Blinders",
            tagline = "By order of...",
            color1 = "#2A2A2A",   // Charcoal grey
            color2 = "#6B1A1A",   // Blood red
            colorReference = "Industrial Birmingham + violence",
            narrativeAesthetic = "Cold, industrial menace. Every moment carries the weight of " +
                "empire and violence. Nick Cave energy. Smoke and whiskey. The loneliness of " +
                "power. Post-war trauma masked by ruthless control.",
            primaryGenres = """["post-punk","industrial","dark folk","blues rock"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Ominous purpose — the factory opens, the empire demands",
                "commute": "Industrial movement — Birmingham streets, rain on cobblestones",
                "work": "Power ambient — running the Shelby empire, calculated decisions",
                "social": "Dangerous charm — every handshake is a power play",
                "confrontation": "Nick Cave violence — 'Red Right Hand' energy, lethal calm",
                "evening": "Whiskey solitude — the weight of the crown, PTSD whispers",
                "setback": "War flashbacks — the trenches never left, dark ambient",
                "victory": "Empire expanded — cold satisfaction, never celebration"
            }""",
            humorPreference = "deadpan",
            isPreset = true
        ),
        CharacterProfile(
            name = "Rick Sanchez",
            franchise = "Rick and Morty",
            tagline = "Nobody exists on purpose.",
            color1 = "#2D6B4A",   // Portal green
            color2 = "#1A3333",   // Dark teal
            colorReference = "Portal fluid + lab coat shadow",
            narrativeAesthetic = "Chaotic genius nihilism. Jarring tonal shifts from comedy to " +
                "existential devastation. Science fiction meets self-destruction. The smartest " +
                "person in any universe who can't fix himself.",
            primaryGenres = """["electronic","synthwave","lo-fi","experimental"]""",
            transitionStyle = "jarring",
            emotionalArchitecture = """{
                "morning": "Hungover genius — another dimension, another regret",
                "commute": "Portal hop energy — physics-defying movement, chaotic direction",
                "work": "Mad scientist ambient — invention, destruction, same thing",
                "social": "Nihilist at the party — too smart for the room, knows it, hates it",
                "confrontation": "Chaotic combat — overkill tech, improvised mayhem",
                "evening": "Existential lo-fi — alone with infinite knowledge and zero peace",
                "setback": "Self-destruction synthwave — the smartest failure in the multiverse",
                "victory": "Hollow triumph — won, but at what cost? (Don't answer that)"
            }""",
            humorPreference = "chaotic",
            isPreset = true
        ),
        CharacterProfile(
            name = "Subaru Natsuki",
            franchise = "Re:Zero",
            tagline = "I'll start from zero.",
            color1 = "#0A0A0A",   // Black
            color2 = "#B8C4D4",   // Starlight silver
            colorReference = "The darkness of Return by Death + the silver of hope",
            narrativeAesthetic = "Suffering as narrative engine. The weight of carrying memories " +
                "no one else has. Determination despite impossible odds. Moments of genuine " +
                "warmth earned through unimaginable pain. Never giving up.",
            primaryGenres = """["orchestral","j-rock","anime OST","piano"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Fragile hope — another loop, another chance to get it right",
                "commute": "Determined march — moving toward the people he needs to save",
                "work": "Desperate planning — running scenarios, counting deaths",
                "social": "Earned warmth — these connections cost everything to preserve",
                "confrontation": "Heroic desperation — not the strongest, but the most stubborn",
                "evening": "Quiet after the storm — alive, for now, holding what matters",
                "setback": "Return by Death — the weight of failure only he remembers",
                "victory": "Hard-won triumph — crying with relief, nothing came free"
            }""",
            humorPreference = "sincere",
            isPreset = true
        ),
        CharacterProfile(
            name = "Ainz Ooal Gown",
            franchise = "Overlord",
            tagline = "Sasuga.",
            color1 = "#2A1A3D",   // Deep purple
            color2 = "#C4A84D",   // Bone gold
            colorReference = "Nazarick's gothic throne room energy",
            narrativeAesthetic = "Overwhelming power disguised as strategy. Gothic magnificence. " +
                "An ordinary person wearing the skin of a supreme being, desperately trying " +
                "to look like they planned everything. Dark comedy meets genuine menace.",
            primaryGenres = """["orchestral","gothic","dark ambient","power metal"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Supreme Being awakens — Nazarick stirs, the throne room hums",
                "commute": "Imperial procession — the world parts for the Sorcerer King",
                "work": "4D chess (actually panicking) — maintaining the facade of omniscience",
                "social": "Terrifying charisma — everyone bows, he's sweating internally",
                "confrontation": "Overwhelming force — Grasp Heart, The Goal of All Life is Death",
                "evening": "Lonely at the top — misses guild members, guards can't see him sigh",
                "setback": "Emotion suppression activates — skeleton can't feel panic (convenient)",
                "victory": "Sasuga Ainz-sama — they think he planned it, he'll take it"
            }""",
            humorPreference = "deadpan",
            isPreset = true
        ),
        CharacterProfile(
            name = "Tony Stark",
            franchise = "Iron Man / MCU",
            tagline = "I am Iron Man.",
            color1 = "#8B1A1A",   // Hot rod red (muted)
            color2 = "#B8942D",   // Metallic gold
            colorReference = "Iron Man armor — obviously",
            narrativeAesthetic = "Genius billionaire energy. Rock and roll confidence masking " +
                "deep anxiety. Technology as armor — literal and emotional. The weight of " +
                "responsibility chosen, not inherited. AC/DC meets heroic sacrifice.",
            primaryGenres = """["rock","hard rock","electronic","orchestral"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Workshop dawn — already three coffees deep, building something",
                "commute": "Top down rock — Malibu highway, sunglasses, AC/DC",
                "work": "Engineering flow — JARVIS ambient, holographic interfaces",
                "social": "Charming deflection — humor as shield, nobody gets too close",
                "confrontation": "Suit up — progressive rock escalation, repulsors charging",
                "evening": "Anxious quiet — the suit can't fix everything, 3am in the workshop",
                "setback": "Cave flashback — the origin wound, building from scraps",
                "victory": "'I am Iron Man' — sacrifice and triumph, inseparable"
            }""",
            humorPreference = "ironic_dramatic",
            isPreset = true
        ),
        CharacterProfile(
            name = "Kiryu Kazuma",
            franchise = "Yakuza / Like a Dragon",
            tagline = "Dame da ne.",
            color1 = "#D4D4D4",   // White (Kiryu's suit)
            color2 = "#8A8A8A",   // Silver/grey
            colorReference = "Kiryu's iconic white suit + Kamurocho steel",
            narrativeAesthetic = "Honorable violence. A man who fights because someone has to. " +
                "Kamurocho neon nights. The most dangerous man in the room who just wants " +
                "to run an orphanage. Japanese crime drama meets genuine heart.",
            primaryGenres = """["j-rock","jazz","enka","game OST"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Quiet determination — another day protecting what matters",
                "commute": "Kamurocho streets — neon-lit walk, respectful nods from strangers",
                "work": "Honorable duty — real estate, orphanage, whatever life demands now",
                "social": "Karaoke king — 'Baka Mitai' energy, genuine warmth with chosen family",
                "confrontation": "'Pledge of Demon' — controlled fury, honor demands this fight",
                "evening": "Rooftop contemplation — looking over the city he can't leave behind",
                "setback": "Dragon's resolve — knocked down, getting back up, always",
                "victory": "Walk away — the dragon doesn't celebrate, he moves on"
            }""",
            humorPreference = "sincere",
            isPreset = true
        ),
        CharacterProfile(
            name = "Alastor",
            franchise = "Hazbin Hotel",
            tagline = "Never fully dressed without a smile.",
            color1 = "#7A1A1A",   // Crimson red
            color2 = "#0A0A0A",   // Black
            colorReference = "The Radio Demon's broadcast of blood",
            narrativeAesthetic = "Vintage menace. 1920s radio static over eldritch horror. " +
                "Charming showmanship hiding unfathomable power. Every conversation is a " +
                "performance, every smile has too many teeth. Vaudeville meets the abyss.",
            primaryGenres = """["jazz","swing","dark cabaret","electro-swing"]""",
            transitionStyle = "vinyl_crackle",
            emotionalArchitecture = """{
                "morning": "Radio broadcast begins — 'Good morning, sinners!' with static crackle",
                "commute": "Swing walk — jaunty menace, cane-twirling through the streets",
                "work": "Deal-making jazz — every favor has invisible strings attached",
                "social": "The performance — charming everyone while trusting no one",
                "confrontation": "Eldritch broadcast — the smile widens, reality bends, run",
                "evening": "Vintage quiet — radio static fading, alone with old memories",
                "setback": "Static intensifies — the mask slips, something ancient stirs",
                "victory": "Applause — the audience (willing or not) gives their standing ovation"
            }""",
            humorPreference = "chaotic",
            isPreset = true
        ),
        CharacterProfile(
            name = "Big Boss",
            franchise = "Metal Gear Solid V / MGS3",
            tagline = "Kept you waiting, huh?",
            color1 = "#2D3A24",   // Olive military green
            color2 = "#3D2E1E",   // Dark earth brown
            colorReference = "Jungle camo, Mother Base metal",
            narrativeAesthetic = "Kojima military philosophy. The soldier who became a legend " +
                "by questioning everything he fought for. Cold War paranoia meets personal " +
                "betrayal. Phantom pain. The boss who was never really the boss.",
            primaryGenres = """["ambient","military orchestral","80s synth","game OST"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Mother Base dawn — the phantom wakes, Diamond Dogs salute",
                "commute": "Helicopter ride — 80s cassette tapes, Afghanistan below",
                "work": "FOB management — building an army outside of nations",
                "social": "Comrade bond — the battlefield brotherhood, few words needed",
                "confrontation": "MGSV combat escalation — stealth breaks, action rises",
                "evening": "Phantom pain ambient — the limb that isn't there, the mentor betrayed",
                "setback": "Ground Zeroes devastation — everything lost in one night",
                "victory": "A Hideo Kojima game — the legend grows, the man diminishes"
            }""",
            humorPreference = "deadpan",
            isPreset = true
        ),
        CharacterProfile(
            name = "Gojo Satoru",
            franchise = "Jujutsu Kaisen",
            tagline = "Throughout heaven and earth, I alone am the honored one.",
            color1 = "#E8E8E8",   // White
            color2 = "#2B7CE8",   // Electric blue
            colorReference = "Infinity Void — blindfold blue + white hair aesthetic",
            narrativeAesthetic = "Overwhelming power worn casually. The strongest sorcerer " +
                "who treats everything like a game — until it isn't. Blindfold comes off " +
                "and the universe pays attention. JJK's mix of comedy and devastating stakes.",
            primaryGenres = """["j-pop","j-rock","hip-hop","anime OST"]""",
            transitionStyle = "smooth",
            emotionalArchitecture = """{
                "morning": "Lost in Paradise energy — casual, untouchable, vibing",
                "commute": "Blindfold walk — the world can't touch him, he knows it",
                "work": "Teaching mode — Gojo-sensei, annoying and brilliant",
                "social": "Playful arrogance — buying sweets, teasing students, being too much",
                "confrontation": "'Throughout heaven and earth' — Infinity activates, Hollow Purple",
                "evening": "The weight of being strongest — alone at the top, protecting everyone",
                "setback": "Blindfold off — this just got serious, no more games",
                "victory": "Casual flex — barely tried, already won, wants mochi now"
            }""",
            humorPreference = "ironic_dramatic",
            isPreset = true
        )
    )

    suspend fun installPresets(dao: CharacterProfileDao) {
        val existing = dao.presetCount()
        if (existing >= ALL.size) return
        dao.insertAll(ALL)
    }
}
