# Architecture

## Product Suite Overview

### Underscore Life
Your daily existence, scored by AI using phone sensors.

- **Input:** GPS speed/direction, accelerometer, heart rate (smartwatch), time of day, weather API, calendar events, ambient sound classification, Bluetooth device proximity.
- **Output:** Contextually perfect music from user's streaming library, with seamless crossfade transitions, all day, automatically.

### Underscore Gaming
Your gaming sessions, scored by AI using game state data.

- **Input:** Game client APIs and state integration systems (Riot API, CS2 Game State Integration, Discord Game SDK, etc.)
- **Output:** Your personal music library scored to your gameplay moments — clutch situations, boss fights, victories, defeats — in real time.

### Underscore RP
Your text roleplay and LLM interactions, scored by AI using conversation text.

- **Input:** Text from LLM conversations (SillyTavern extension, browser extension for Claude/ChatGPT/Grok, native API integrations with AI platforms).
- **Output:** Franchise-appropriate or genre-appropriate music that scores the narrative in real time as you read/write.

---

## The Universal Narrative Scoring Engine

The same core engine powers all three products. Only the **INPUT LAYER** changes.

```
┌─────────────────────────────────────────────────────────────┐
│                    INPUT LAYER (varies by product)           │
│                                                             │
│  LIFE: Phone sensors ──┐                                    │
│  GAMING: Game state ───┼──▶ CONTEXT SIGNAL                  │
│  RP: Conversation text ┘                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              SCENE CLASSIFIER (Gemini 3 Flash)              │
│                                                             │
│  Takes context signal + user profile + active character     │
│  profile (if any) and returns:                              │
│                                                             │
│  {                                                          │
│    "scene_type": "commute_motorcycle",                      │
│    "emotional_register": "departure_with_purpose",          │
│    "energy_level": 7,                                       │
│    "narrative_beat": "protagonist_leaves_base",             │
│    "character_equivalent": "big_boss_deployment",           │
│    "recommended_track_criteria": {                          │
│      "mood": "propulsive_determined",                       │
│      "escalation": "building",                              │
│      "archetype": "departure_anthem"                        │
│    },                                                       │
│    "transition": "crossfade_from_ambient_8_seconds"         │
│  }                                                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    SONG MATCHER                              │
│                                                             │
│  Takes scene classification + user's library/playlist        │
│  (constrained by character profile if active)               │
│  Returns: specific track ID + playback instructions          │
│                                                             │
│  Uses Spotify audio features (tempo, energy, valence) +     │
│  Gemini's cultural knowledge of songs' NARRATIVE FUNCTION   │
│  (not just audio properties, but what the song MEANS)       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                 PLAYBACK CONTROLLER                          │
│                                                             │
│  Spotify SDK / Apple Music API / YouTube Music API          │
│  Handles: track queuing, crossfade transitions,             │
│  volume management, transition timing                        │
│                                                             │
│  NOTE: Underscore NEVER touches audio files.                │
│  We are a REMOTE CONTROL. A very smart remote control.      │
│  The streaming platform handles all licensing.               │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   LEARNING LAYER                             │
│                                                             │
│  Monitors user reactions:                                    │
│  - Skip = bad match, adjust                                 │
│  - Volume UP = perfect match, reinforce                     │
│  - Replay at location = place-linked theme, memorize        │
│  - Repeated context + song = recurring motif, establish     │
│                                                             │
│  Over time: builds complete emotional map of user's life    │
│  Places get themes. People get leitmotifs.                  │
│  The app becomes YOUR composer.                             │
└─────────────────────────────────────────────────────────────┘
```

---

## The LLM Layer — Gemini 3 Flash

### Why Gemini 3 Flash

- Trillion+ parameter knowledge base = deep cultural understanding of songs, characters, franchises, memes, narrative tropes
- Flash variant = fast inference, low cost, suitable for real-time scene classification
- Understands that "Bury the Light" isn't just "energetic rock" — it's specifically "rising from defeat, I AM THE STORM THAT IS APPROACHING" energy
- Can auto-generate complete character profiles for ANY fictional character on demand
- Cost-identical to Gemini 2 Flash but smarter

### What the LLM Does (and Doesn't Do)

**DOES:** Scene classification, song narrative analysis, character profile generation, cultural context mapping, conditional trigger evaluation

**DOES NOT:** Generate music, handle playback, store user data, run on-device (cloud API calls)

### Latency Requirements

| Mode | Max Latency | Rationale |
|------|------------|-----------|
| Life | < 2 seconds | Context changes aren't instant |
| Gaming | < 500ms | Clutch moments are time-critical |
| RP | < 1 second | Runs while user reads the response |

### Fallback Strategy

Lightweight on-device model (TFLite or similar) for basic scene classification when offline or when Gemini latency exceeds threshold. Maps simple sensor patterns to broad categories (moving fast = energetic, stationary + evening = chill). Gemini handles the NUANCED matching; on-device handles the basics.

---

## Streaming Platform Integration

### Primary Integration: Spotify

- **Spotify Web API** for library access, audio features, search
- **Spotify SDK** for playback control, queue management, crossfade
- User authenticates with their Spotify account; Underscore controls playback through the SDK
- All licensing handled by Spotify's existing agreements
- We are a playlist generator. There are thousands of these. Nobody sues playlist generators.

### Secondary Integrations (Post-MVP)

- Apple Music API (MusicKit)
- YouTube Music API
- Local library playback (for users without streaming subscriptions)

### Legal Position

Underscore does NOT play music. Underscore TELLS the streaming platform WHICH music to play. We never touch an audio file. We never store a song. We never stream a single byte of copyrighted content. We send a command: "play this track ID." The user's existing subscription handles the rest.

This is the same legal architecture as every third-party Spotify app, DJ app, and smart speaker integration in existence. The platforms BUILT their APIs for this purpose. They WANT us to do this.

---

## On-Device vs Cloud

**Cloud (Gemini 3 Flash):** All nuanced scene classification, character profile generation, cultural context matching, narrative arc awareness. This is where the intelligence lives.

**On-device (lightweight model):** Basic sensor-to-scene mapping for offline fallback and latency-critical moments. Simple rules: speed > X = moving, HR spike + stationary = tense, etc. NO cultural knowledge, NO character awareness. Just basics.

**The handoff rule:** On-device handles IMMEDIATE responses. Cloud handles NUANCED classification. If the on-device model says "user is moving fast, play energetic music" and then the cloud model returns "actually this is a motorcycle departure at sunset after a long day, play something with wistful purpose," the cloud response OVERRIDES the on-device response with a smooth transition.

---

## Technical Stack & Dependencies

### Core Stack

| Component | Technology | Notes |
|-----------|-----------|-------|
| Mobile app | React Native or Flutter | Cross-platform, sensor access, background service |
| LLM — narrative engine | Gemini 3 Flash API | Scene classification, song matching, character generation |
| Music playback | Spotify SDK (primary) | Queue control, crossfade, library access |
| Music metadata | Spotify Web API | Audio features (tempo, energy, valence), search, playlists |
| Weather | OpenWeatherMap API | Free tier sufficient for MVP |
| Backend | Firebase or Supabase | User profiles, character storage, community marketplace |
| SillyTavern extension | JavaScript (browser extension) | Reads conversation, controls audio |
| Gaming overlay | Desktop app (Electron or native) | Reads game state APIs, controls Spotify |

### API Dependencies

| API | Purpose | Rate Limits to Watch |
|-----|---------|---------------------|
| Gemini 3 Flash | Scene classification (~1 call per context change) | Monitor costs at scale |
| Spotify Web API | Library access, audio features, search | Standard rate limits |
| Spotify SDK | Playback control | Premium users only for full control |
| OpenWeatherMap | Weather context | 1 call per 10 minutes sufficient |
| Game APIs (various) | Game state reading | Varies by game |
