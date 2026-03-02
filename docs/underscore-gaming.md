# Underscore Gaming — Game Mode Integration

Integrates with game clients to read game state data in real time. Scores gaming sessions with the user's personal music library instead of (or layered with) the game's built-in soundtrack.

---

## Game State Integration Targets

| Game/Platform | Integration Method | Data Available |
|--------------|-------------------|----------------|
| Valorant | Riot Games API | Match state, round, kills, deaths, last alive status, agent |
| CS2 | Game State Integration (GSI) | Round state, health, kills, bomb status, money |
| League of Legends | Live Client Data API | Champion, health, gold, kills, objectives, game time |
| Apex Legends | Club API + overlay | Squad count, ring status, placement |
| Any game | Discord Game SDK | Game detection, presence status |
| Any game | OBS overlay API | Stream state, scene detection |

---

## The Clutch Scenario (Core Design Reference)

This is the canonical example that defines how Underscore Gaming SHOULD feel:

**Valorant. 12-12. Overtime. 1v3. You're last alive.**

The app reads game state: competitive match, overtime, 1v3 disadvantage, user is last player standing.
Classification: **MAXIMUM STAKES CLUTCH**.

The LLM matches this to the user's library. User profile: Doom Eternal / MGR / DMC aesthetic.
Selection: "The Only Thing They Fear Is You."

- **Kill one** → music KICKS from the drop
- **Kill two** → another guitar layer activates, drums intensify
- **Last enemy** → music drops to bass pulse for one beat (the slow-motion lock-eyes moment) → user gets the kill → FULL ERUPTION. Choir. Guitars. Victory.

### The Fail Version (Equally Important)

Same setup, same music, but the user whiffs every shot and dies. The Doom soundtrack going MAXIMUM INTENSITY while the player sprays into a wall is **COMEDY GOLD**. Wins AND losses are better content with Underscore.

---

## Game-Specific Scene Classifications

| Game State | Scene Classification | Music Energy |
|-----------|---------------------|-------------|
| Last alive, outnumbered | `CLUTCH_SITUATION` | Maximum intensity, build-to-climax |
| Boss fight, low health | `SURVIVAL_CRITICAL` | Desperate intensity, heartbeat tempo |
| Won the round/match | `VICTORY` | Triumphant resolution, brass/choir |
| Died / lost round | `DEFEAT` | Brief somber → defiant recovery |
| Exploring, no threats | `EXPLORATION` | Ambient, atmospheric, journey-like |
| Loading/menu screen | `LIMINAL` | Gentle, anticipatory, pre-mission |
| 0-8 getting destroyed | `PROTAGONIST_LOWEST_POINT` | Defiant anthem (Ra Ra Rasputin energy) |

---

## Scene Classification Response Format

```json
{
  "game": "valorant",
  "match_state": "competitive_overtime",
  "round_state": "1v3_disadvantage",
  "player_status": "last_alive",
  "scene_type": "CLUTCH_SITUATION",
  "emotional_register": "maximum_stakes",
  "energy_level": 10,
  "narrative_beat": "protagonist_final_stand",
  "recommended_track_criteria": {
    "mood": "aggressive_defiant",
    "escalation": "building_to_climax",
    "archetype": "boss_fight_theme"
  },
  "transition": "hard_cut_immediate",
  "dynamic_layers": {
    "on_kill": "escalate_intensity",
    "on_death": "defeat_resolution",
    "on_round_win": "victory_eruption"
  }
}
```

---

## Dynamic Music Layering

Unlike Life mode (which plays complete tracks), Gaming mode supports **dynamic layering** where music intensity adapts to real-time game events:

1. **Base layer** — ambient/atmospheric track matching overall game state
2. **Intensity layer** — builds based on threat proximity, player health, stakes
3. **Event triggers** — specific sound cues on kills, deaths, objectives
4. **Resolution** — victory fanfare or defeat resolution

The LLM handles layer classification. The playback controller handles the actual audio mixing through the streaming SDK's queue and volume controls.

---

## Content Ecosystem

Every Underscore Gaming clip is unique because every user has a different library. Two streamers clutch the same Valorant round — one with Doom Eternal, one with anime openings. Both clips are shareable. Both are personal. Both credit Underscore.

**Streamers build their BRAND around their Underscore profiles.** The music becomes part of their content identity. Compilations, montages, fail compilations — all user-generated, all featuring Underscore, all free marketing.

### Content Types Generated

- Clutch montages with personal soundtracks
- Fail compilations (dramatic music + terrible gameplay = comedy)
- Before/after comparisons (game audio vs Underscore)
- "My Underscore scored my ranked game" reaction videos
- Streamer personality through music choice
