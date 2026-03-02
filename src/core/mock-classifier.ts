/**
 * Mock scene classifier — keyword-based fallback for offline dev / testing.
 *
 * No API calls. Works without a Gemini key.
 * The mock classifier means git clone + npm install + set Spotify keys = working demo.
 */
import type { SceneClassification, TextContext } from "./types.js";

interface ScenePattern {
  keywords: string[];
  sceneType: string;
  emotionalRegister: string;
  energyLevel: number;
  narrativeBeat: string;
  combatStatus: SceneClassification["combatStatus"];
  mood: string;
  archetype: string;
  intimateScene?: boolean;
}

const PATTERNS: ScenePattern[] = [
  // === Dramatic revelation (Ace Attorney rule — check FIRST) ===
  {
    keywords: ["objection", "contradiction", "reveal", "gotcha", "lied", "proof", "evidence"],
    sceneType: "dramatic_revelation",
    emotionalRegister: "triumphant_vindication",
    energyLevel: 9,
    narrativeBeat: "dramatic_revelation",
    combatStatus: "none",
    mood: "triumphant_aggressive",
    archetype: "pursuit_theme",
  },

  // === Intimate scene (Careless Whisper Protocol) ===
  {
    keywords: ["kiss", "intimate", "embrace", "passion", "caress", "undress", "bedroom"],
    sceneType: "intimate_scene",
    emotionalRegister: "romantic_intense",
    energyLevel: 6,
    narrativeBeat: "intimate_moment",
    combatStatus: "none",
    mood: "romantic",
    archetype: "romantic_ballad",
    intimateScene: true,
  },

  // === Combat (intense) ===
  {
    keywords: ["attack", "fight", "battle", "slash", "stab", "explosion", "combat", "duel"],
    sceneType: "combat_intense",
    emotionalRegister: "aggressive_determined",
    energyLevel: 8,
    narrativeBeat: "combat_escalation",
    combatStatus: "active",
    mood: "aggressive_defiant",
    archetype: "battle_theme",
  },

  // === Boss fight / high stakes ===
  {
    keywords: ["boss", "final", "ultimate", "overwhelming", "impossible", "last stand"],
    sceneType: "boss_fight",
    emotionalRegister: "desperate_defiant",
    energyLevel: 10,
    narrativeBeat: "protagonist_final_stand",
    combatStatus: "active",
    mood: "desperate_epic",
    archetype: "boss_fight",
  },

  // === Horror / unsettling ===
  {
    keywords: ["glitch", "wrong", "distorted", "corrupted", "error", "static", "creeping", "dread"],
    sceneType: "horror_unsettling",
    emotionalRegister: "dread_building",
    energyLevel: 6,
    narrativeBeat: "horror_escalation",
    combatStatus: "none",
    mood: "unsettling_ambient",
    archetype: "horror_ambient",
  },

  // === Victory ===
  {
    keywords: ["victory", "won", "defeated", "triumph", "celebrate", "succeeded"],
    sceneType: "victory",
    emotionalRegister: "triumphant",
    energyLevel: 8,
    narrativeBeat: "victory_celebration",
    combatStatus: "ending",
    mood: "triumphant_joyful",
    archetype: "victory_fanfare",
  },

  // === Defeat / loss ===
  {
    keywords: ["lost", "failed", "defeated", "fallen", "death", "died", "hopeless"],
    sceneType: "defeat",
    emotionalRegister: "melancholic_defiant",
    energyLevel: 4,
    narrativeBeat: "protagonist_lowest_point",
    combatStatus: "ending",
    mood: "somber_defiant",
    archetype: "defeat_theme",
  },

  // === Exploration / travel ===
  {
    keywords: ["walk", "explore", "village", "town", "city", "path", "road", "journey", "travel"],
    sceneType: "exploration_peaceful",
    emotionalRegister: "contemplative_wonder",
    energyLevel: 4,
    narrativeBeat: "exploration",
    combatStatus: "none",
    mood: "gentle_contemplative",
    archetype: "exploration_ambient",
  },

  // === Emotional / sad ===
  {
    keywords: ["cry", "tears", "grief", "sorrow", "goodbye", "farewell", "alone", "lonely"],
    sceneType: "emotional_scene",
    emotionalRegister: "melancholic",
    energyLevel: 3,
    narrativeBeat: "emotional_moment",
    combatStatus: "none",
    mood: "melancholic_tender",
    archetype: "emotional_piano",
  },

  // === Tense / suspense ===
  {
    keywords: ["tense", "suspicious", "danger", "watched", "stalked", "hidden", "trap"],
    sceneType: "suspense",
    emotionalRegister: "rising_tension",
    energyLevel: 6,
    narrativeBeat: "tension_building",
    combatStatus: "none",
    mood: "tense_atmospheric",
    archetype: "suspense_theme",
  },
];

/**
 * Classify a scene using keyword matching.
 */
export function classifyWithMock(context: TextContext): SceneClassification {
  const lower = context.rawText.toLowerCase();

  // Find best matching pattern
  let bestPattern: ScenePattern | null = null;
  let bestScore = 0;

  for (const pattern of PATTERNS) {
    const score = pattern.keywords.filter((kw) => lower.includes(kw)).length;
    if (score > bestScore) {
      bestScore = score;
      bestPattern = pattern;
    }
  }

  // Default to neutral dialogue if no keywords match
  if (!bestPattern) {
    return {
      franchise: context.detectedFranchise,
      sceneType: "dialogue_neutral",
      emotionalRegister: "neutral",
      energyLevel: 4,
      narrativeBeat: "conversation",
      charactersPresent: context.charactersMentioned,
      combatStatus: "none",
      trackCriteria: {
        mood: "calm_neutral",
        escalation: "stable",
        archetype: "ambient",
      },
      transition: { type: "crossfade", durationMs: 6000 },
      intimateScene: false,
    };
  }

  return {
    franchise: context.detectedFranchise,
    sceneType: bestPattern.sceneType,
    emotionalRegister: bestPattern.emotionalRegister,
    energyLevel: bestPattern.energyLevel,
    narrativeBeat: bestPattern.narrativeBeat,
    charactersPresent: context.charactersMentioned,
    combatStatus: bestPattern.combatStatus,
    trackCriteria: {
      mood: bestPattern.mood,
      escalation:
        bestPattern.energyLevel >= 7
          ? "building"
          : bestPattern.energyLevel <= 3
            ? "declining"
            : "stable",
      archetype: bestPattern.archetype,
    },
    transition:
      bestPattern.sceneType === "dramatic_revelation"
        ? { type: "hard_cut" }
        : { type: "crossfade", durationMs: bestPattern.energyLevel >= 7 ? 2000 : 6000 },
    intimateScene: bestPattern.intimateScene ?? false,
  };
}
