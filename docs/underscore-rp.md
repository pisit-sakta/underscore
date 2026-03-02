# Underscore RP — Text Roleplay Soundtrack Layer

An integration layer that reads LLM text conversations in real time and plays contextually appropriate music based on narrative content.

---

## Distribution Channels

| Surface | Technology | Priority |
|---------|-----------|----------|
| SillyTavern extension | JavaScript (browser extension) | Primary target — open source RP community |
| Browser extension | Chrome/Firefox extension | For web-based LLM platforms: Claude, ChatGPT, Grok |
| Native API integration | Platform partnerships | For AI platform partnerships |
| Desktop companion | Electron/native app | Reading any text window |

---

## Why Every AI Company Would Want This

| Company | Why They Care |
|---------|--------------|
| **Anthropic** | Differentiates Claude as a creative EXPERIENCE, not just a text tool |
| **OpenAI** | Fits their multimodal roadmap; adds audio layer to ChatGPT |
| **xAI/Grok** | Dominates the less-restricted RP market with premium experience |
| **Character.AI** | Their entire product is character RP; soundtrack makes every chat immersive |
| **SillyTavern** | Most requested missing feature: dynamic context-aware music |

All AI platforms benefit from: 2x engagement time, near-zero churn for Underscore users, premium conversion driver, experiential differentiation in a market converging on model quality.

---

## Architecture

Same narrative engine. Different input parser.

```
Text Parser → Scene Classifier → Franchise Detector →
  Narrative Engine (Gemini 3 Flash) → Song Selection → Playback
```

Text parsing is EASIER than sensor fusion because text is explicit. The conversation LITERALLY SAYS "you walk through the gates of Liyue Harbor." No inference needed. Just reading comprehension.

---

## Scene Classification from Text

The Gemini call receives the latest message(s) and returns:

```json
{
  "franchise_detected": "Genshin Impact",
  "current_location": "Liyue Harbor",
  "time_of_day": "evening",
  "scene_type": "exploration_peaceful",
  "characters_present": ["Zhongli", "Traveler"],
  "emotional_register": "contemplative_wonder",
  "combat_status": "none",
  "narrative_beat": "first_arrival_new_region",
  "recommended_track": "Liyue theme - evening variant",
  "transition": "slow_crossfade_from_overworld"
}
```

---

## Franchise Detection Sources

1. Character card metadata (SillyTavern character cards contain franchise info)
2. Explicit text mentions ("Liyue," "Vision," "elemental burst")
3. Character names (Zhongli → Genshin Impact)
4. World-building elements (elemental reactions, Archons, etc.)
5. User's selected character profile
6. Conversation opening / scenario description

For original fiction with no franchise: fall back to genre-appropriate music from user's general library based on detected tone, setting, and atmosphere.

---

## SillyTavern Extension Specifics

- Reads character card on conversation load → sets franchise context
- Monitors each new message for scene changes
- Controls audio through browser audio API or Spotify
- Adds minimal UI: now-playing indicator showing current track + detected scene
- Respects SillyTavern's existing extension architecture
- Published to SillyTavern extension marketplace

### Character Card Integration

SillyTavern cards already contain name, description, personality, scenario, first message. Underscore reads this metadata to establish soundtrack context BEFORE the first message. Open a chat with Zhongli → Liyue theme plays immediately.

---

## Visual Novel / Dating Sim Scoring

Dating sims and visual novels rely on music more heavily than almost any other genre. Text RP recreations lose 90% of their emotional impact without the soundtrack. Underscore restores it.

### Franchise-Specific Tonal Identities

| Franchise | Sonic Identity |
|-----------|---------------|
| **Persona series** | Jazz-influenced, urban, stylish, Social Link rank-up moments |
| **Fire Emblem** | Orchestral, medieval, support-rank-driven emotional beats |
| **Clannad / Key VNs** | Piano-heavy, devastatingly emotional, will make users cry |
| **Danganronpa** | Electronic, tense, trial-phase vs daily-life phase switching |
| **Steins;Gate** | Atmospheric, sci-fi, tension-building, time-loop anxiety |
| **Ace Attorney** | Jazz, investigation vs courtroom phase switching |
| **Stardew Valley** | The coziest possible experience, seasonal ambient, anti-anxiety |
| **Doki Doki Literature Club** | See Horror VN Scoring section below |

---

## Horror VN Scoring — The DDLC Case

**CRITICAL:** The system MUST understand narrative phase transitions in horror VNs.

### DDLC-Style Horror Scoring

- Music degrades GRADUALLY when narrative shifts from cute to unsettling
- Same melodies, slightly corrupted — off-key notes, tempo drift, subliminal undertones
- LLM detects meta-horror cues (fourth wall breaks, self-reference, glitch descriptions)
- Adjusts soundtrack from "bubbly dating sim" to "something is fundamentally wrong" over the course of the conversation

### Optional Advanced Feature (Configurable, Off by Default)

Underscore can "participate" in horror by intentionally glitching its own playback — skipped bars, wrong-speed playback, corrupted track names in the UI — to blur the line between the story's fictional glitches and the app's real behavior. The user cannot tell if the app is broken or PERFORMING. This is the most Dan Salvato thing an app could do.

---

## The Ace Attorney Exception Rule

If the text contains ANYTHING resembling a dramatic revelation, contradiction, or "gotcha" moment — the Pursuit theme plays IMMEDIATELY. No crossfade. No buildup. **HARD CUT to Pursuit ~ Cornered.**

This applies EVEN outside Ace Attorney mode. If a user in ANY roleplay delivers a devastating logical argument, Underscore should have an option to fire the Pursuit theme.

**This is non-negotiable. This is the law.**
