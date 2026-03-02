# UNDERSCORE

> "Your life, scored."

An AI-powered context-aware life soundtrack platform that automatically plays the right song at the right moment — across your daily life, your gaming sessions, and your text-based stories.

**Codename:** Deep Battle

---

## Three Products. One Engine. Infinite Soundtracks.

| Product | What It Does | Input |
|---------|-------------|-------|
| **Underscore Life** | Scores your daily existence | Phone sensors (GPS, accelerometer, heart rate, weather) |
| **Underscore Gaming** | Scores your gaming sessions | Game state data (health, kills, round status) |
| **Underscore RP** | Scores your text stories | LLM conversation text (SillyTavern, Claude, ChatGPT) |

All three share the same core narrative engine (Gemini 3 Flash), the same playback system (Spotify SDK), and the same character profile infrastructure.

---

## How It Works

1. **Input Layer** — Sensors, game state, or conversation text produce a context signal
2. **Scene Classifier** — Gemini 3 Flash classifies the moment (narrative beat, emotional register, energy level)
3. **Song Matcher** — Matches the scene to the perfect track from the user's library using audio features + cultural knowledge
4. **Playback Controller** — Spotify/Apple Music/YouTube Music plays the track with cinematic transitions
5. **Learning Layer** — Skips, volume changes, and location patterns refine the soundtrack over time

Underscore NEVER touches audio files. We are a remote control. A very smart remote control.

---

## Documentation

Start with the **[Technical Handoff](./docs/TECHNICAL_HANDOFF.md)** for the complete overview, or jump to a specific area:

### Core
- [Architecture](./docs/architecture.md) — Narrative engine, LLM layer, streaming integration, tech stack
- [Underscore Life](./docs/underscore-life.md) — Sensor suite, scene classification, learning system
- [Underscore Gaming](./docs/underscore-gaming.md) — Game state APIs, clutch scenarios, dynamic layering
- [Underscore RP](./docs/underscore-rp.md) — Text parsing, franchise detection, VN scoring

### Features
- [Character System](./docs/character-system.md) — Pre-built profiles, custom characters, marketplace
- [Careless Whisper Protocol](./docs/careless-whisper-protocol.md) — The saxophone

### Strategy
- [Sprint Roadmap](./docs/sprint-roadmap.md) — Phases 1-3, Sprints 0-10
- [Go-To-Market](./docs/go-to-market.md) — Content slate, platform strategy
- [Business Model](./docs/business-model.md) — Revenue streams, partnerships

### Reference
- [Brand Guidelines](./docs/brand-guidelines.md) — Voice, terminology, lore
- [Known Problems](./docs/known-problems.md) — Battery, latency, false positives

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Mobile app | React Native or Flutter |
| Narrative engine | Gemini 3 Flash API |
| Music playback | Spotify SDK (primary) |
| Music metadata | Spotify Web API |
| Weather | OpenWeatherMap API |
| Backend | Firebase or Supabase |
| RP extension | JavaScript (browser) |
| Gaming overlay | Electron or native desktop |

---

*"This app was conceived during a conversation about German tank doctrine, Adele, and a soi dog. Codename: Deep Battle. Rolling in the deep, always."*
