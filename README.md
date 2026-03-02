# UNDERSCORE

> "Your life, scored."

An AI-powered context-aware life soundtrack platform that automatically plays the right song at the right moment — across your daily life, your gaming sessions, and your text-based stories.

**Codename:** Deep Battle

---

## Quick Start (Underscore RP)

Works immediately with zero API keys — demo mode runs the full pipeline with simulated playback.

```bash
npm install
npm run dev
# Open http://localhost:3000
```

That's it. Paste roleplay text, click "Score This Scene", watch the AI classify your scene and select music.

### Optional: Real Playback

```bash
cp .env.example .env
```

| Variable | Required? | What It Does |
|----------|-----------|-------------|
| `SPOTIFY_CLIENT_ID` | For playback | Enables real Spotify playback (get from [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)) |
| `SPOTIFY_CLIENT_SECRET` | For playback | Spotify OAuth secret |
| `GEMINI_API_KEY` | For AI classification | Enables Gemini-powered scene classification (get from [Google AI Studio](https://aistudio.google.com/apikey)). Without it, the mock classifier uses keyword matching. |

### Modes

| Config | Classifier | Playback | Best For |
|--------|-----------|----------|----------|
| No `.env` | Mock (keywords) | Simulated | Quick demo, development |
| `GEMINI_API_KEY` only | Gemini Flash | Simulated | Testing AI classification |
| All keys set | Gemini Flash | Real Spotify | Full experience |

---

## How It Works

```
Text Input → Parse → Classify → Match → Play
```

1. **Text Parser** — Strips OOC markers, detects franchise keywords (Genshin, MGS, Ace Attorney...), extracts character names
2. **Scene Classifier** — Gemini Flash (or mock) returns scene type, emotional register, energy level, narrative beat
3. **Song Matcher** — Scores tracks from your Spotify library by energy/valence/tempo alignment, or suggests tracks in demo mode
4. **Playback Controller** — Plays on Spotify with crossfade/hard-cut transitions

### Special Protocols

- **Careless Whisper Protocol** — Intimate scenes trigger George Michael. The saxophone is non-negotiable.
- **Ace Attorney Exception Rule** — Dramatic revelations get an immediate hard cut to the Pursuit theme. This is the law.

---

## Project Structure

```
src/
  core/
    types.ts               Shared type definitions
    config.ts              Environment config
    scene-classifier.ts    Gemini scene classification
    mock-classifier.ts     Keyword-based offline fallback
    song-matcher.ts        Scene → track matching
    playback-controller.ts Spotify playback control
    library-cache.ts       User library + audio features
    narrative-engine.ts    Orchestrator (the full pipeline)
  rp/
    text-parser.ts         RP text parsing + franchise detection
  auth/
    spotify-auth.ts        Spotify OAuth 2.0
    session.ts             In-memory session store
  api/
    routes.ts              Express API routes
  index.ts                 Server entry point
public/
  index.html               Web UI
docs/                      Full product documentation
```

---

## Three Products. One Engine. Infinite Soundtracks.

| Product | What It Does | Input | Status |
|---------|-------------|-------|--------|
| **Underscore RP** | Scores your text stories | LLM conversation text | **MVP built** |
| **Underscore Life** | Scores your daily existence | Phone sensors | Planned |
| **Underscore Gaming** | Scores your gaming sessions | Game state data | Planned |

---

## Documentation

- [Technical Handoff](./docs/TECHNICAL_HANDOFF.md) — Complete product overview
- [Architecture](./docs/architecture.md) — Core engine, LLM layer, streaming integration
- [Underscore RP](./docs/underscore-rp.md) — Text parsing, franchise detection, VN scoring
- [Character System](./docs/character-system.md) — Pre-built profiles, custom characters
- [Careless Whisper Protocol](./docs/careless-whisper-protocol.md) — The saxophone
- [Sprint Roadmap](./docs/sprint-roadmap.md) — Development plan
- [Go-To-Market](./docs/go-to-market.md) — Content strategy
- [Business Model](./docs/business-model.md) — Revenue streams

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Server | Node.js + Express + TypeScript |
| Narrative engine | Gemini Flash API (`@google/genai`) |
| Music playback | Spotify Web API |
| Frontend | Vanilla HTML/CSS/JS |

---

*"This app was conceived during a conversation about German tank doctrine, Adele, and a soi dog. Codename: Deep Battle. Rolling in the deep, always."*
