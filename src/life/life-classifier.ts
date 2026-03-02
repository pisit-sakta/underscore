/**
 * Life classifier — maps a LifeInput to a LifeScene.
 *
 * This is the "mock" keyword-based classifier for Life mode.
 * Deterministic: activity + mood + energy + time → scene.
 *
 * A future Gemini-powered classifier can use the freeText field
 * for richer, LLM-driven classification.
 */
import type {
  LifeInput,
  LifeScene,
  LifeActivity,
  MoodTag,
  SessionStyle,
  TimeOfDay,
} from "./types.js";
import type { TrackCriteria, TransitionStyle } from "../core/types.js";

// ── Activity → base scene mapping ──

interface ActivityProfile {
  sessionStyle: SessionStyle;
  energyLevel: number;
  mood: string;
  archetype: string;
  preferredGenres?: string[];
}

const ACTIVITY_PROFILES: Record<LifeActivity, ActivityProfile> = {
  // Focus
  working:      { sessionStyle: "light_focus", energyLevel: 4, mood: "focused_steady",      archetype: "lo_fi_beats",        preferredGenres: ["lo-fi", "electronic"] },
  studying:     { sessionStyle: "deep_focus",  energyLevel: 3, mood: "calm_concentrated",    archetype: "ambient_focus",      preferredGenres: ["ambient", "classical"] },
  coding:       { sessionStyle: "deep_focus",  energyLevel: 5, mood: "focused_flow",         archetype: "synthwave_focus",    preferredGenres: ["synthwave", "electronic"] },
  reading:      { sessionStyle: "ambient",     energyLevel: 2, mood: "calm_absorbed",        archetype: "ambient_minimal",    preferredGenres: ["ambient", "classical", "jazz"] },

  // Movement
  commuting:    { sessionStyle: "light_focus", energyLevel: 5, mood: "neutral_moving",       archetype: "indie_cruise",       preferredGenres: ["indie", "pop", "alt-rock"] },
  walking:      { sessionStyle: "light_focus", energyLevel: 4, mood: "contemplative_moving", archetype: "indie_walk",         preferredGenres: ["indie", "folk", "dream-pop"] },
  driving:      { sessionStyle: "energize",    energyLevel: 6, mood: "free_open",            archetype: "driving_anthems",    preferredGenres: ["rock", "pop", "indie"] },

  // Exercise
  running:      { sessionStyle: "energize",    energyLevel: 8, mood: "driven_pushing",       archetype: "high_bpm_workout",   preferredGenres: ["edm", "hip-hop", "rock"] },
  gym:          { sessionStyle: "energize",    energyLevel: 9, mood: "aggressive_pumped",    archetype: "gym_power",          preferredGenres: ["hip-hop", "metal", "edm"] },
  yoga:         { sessionStyle: "ambient",     energyLevel: 2, mood: "centered_breathing",   archetype: "ambient_zen",        preferredGenres: ["ambient", "new-age", "world"] },

  // Social
  hanging_out:  { sessionStyle: "social",      energyLevel: 5, mood: "upbeat_social",        archetype: "chill_vibes",        preferredGenres: ["pop", "r-and-b", "indie"] },
  party:        { sessionStyle: "social",      energyLevel: 8, mood: "euphoric_social",      archetype: "party_bangers",      preferredGenres: ["pop", "edm", "hip-hop"] },
  date_night:   { sessionStyle: "social",      energyLevel: 4, mood: "romantic_warm",        archetype: "romantic_evening",   preferredGenres: ["r-and-b", "jazz", "soul"] },

  // Domestic
  cooking:      { sessionStyle: "light_focus", energyLevel: 5, mood: "upbeat_domestic",      archetype: "kitchen_groove",     preferredGenres: ["soul", "funk", "pop"] },
  cleaning:     { sessionStyle: "energize",    energyLevel: 6, mood: "productive_upbeat",    archetype: "cleaning_power",     preferredGenres: ["pop", "disco", "funk"] },
  eating:       { sessionStyle: "ambient",     energyLevel: 3, mood: "relaxed_content",      archetype: "dinner_jazz",        preferredGenres: ["jazz", "bossa-nova", "soul"] },

  // Rest
  relaxing:     { sessionStyle: "ambient",     energyLevel: 3, mood: "peaceful_unwinding",   archetype: "chill_ambient",      preferredGenres: ["ambient", "lo-fi", "chill"] },
  meditating:   { sessionStyle: "ambient",     energyLevel: 1, mood: "still_present",        archetype: "meditation_drone",   preferredGenres: ["ambient", "drone", "new-age"] },
  waking_up:    { sessionStyle: "light_focus", energyLevel: 3, mood: "gentle_awakening",     archetype: "morning_ease",       preferredGenres: ["indie", "folk", "acoustic"] },
  winding_down: { sessionStyle: "wind_down",   energyLevel: 2, mood: "sleepy_settling",      archetype: "night_ambient",      preferredGenres: ["ambient", "lo-fi", "classical"] },

  // Creative
  creating:     { sessionStyle: "light_focus", energyLevel: 5, mood: "inspired_flowing",     archetype: "creative_flow",      preferredGenres: ["electronic", "indie", "art-pop"] },
  gaming:       { sessionStyle: "soundtrack",  energyLevel: 6, mood: "immersed_engaged",     archetype: "game_soundtrack",    preferredGenres: ["orchestral", "electronic", "rock"] },
};

// ── Mood modifiers ──

/** Mood adjustments applied on top of the activity base */
const MOOD_MODIFIERS: Record<MoodTag, { energyDelta: number; moodOverride?: string; styleOverride?: SessionStyle }> = {
  happy:      { energyDelta: +1, moodOverride: "happy_upbeat" },
  chill:      { energyDelta: -1, moodOverride: "chill_relaxed" },
  focused:    { energyDelta:  0, styleOverride: "deep_focus" },
  stressed:   { energyDelta: -1, moodOverride: "stressed_need_calm" },
  melancholy: { energyDelta: -2, moodOverride: "melancholy_reflective" },
  energetic:  { energyDelta: +2, moodOverride: "energetic_pumped" },
  anxious:    { energyDelta: -1, moodOverride: "anxious_need_grounding" },
  nostalgic:  { energyDelta:  0, moodOverride: "nostalgic_warm" },
  romantic:   { energyDelta:  0, moodOverride: "romantic_warm", styleOverride: "social" },
  angry:      { energyDelta: +2, moodOverride: "angry_intense" },
};

// ── Time-of-day modifiers ──

const TIME_ENERGY_DELTA: Record<TimeOfDay, number> = {
  early_morning: -1,
  morning:        0,
  afternoon:      0,
  evening:       -1,
  night:         -1,
  late_night:    -2,
};

// ── Classify ──

/**
 * Classify a life context into a LifeScene.
 * Deterministic: same input → same output.
 */
export function classifyLifeScene(input: LifeInput): LifeScene {
  const base = ACTIVITY_PROFILES[input.activity];

  // Start with base energy, then apply modifiers
  let energy = base.energyLevel;
  let mood = base.mood;
  let sessionStyle = base.sessionStyle;

  // Mood modifier
  if (input.mood) {
    const mod = MOOD_MODIFIERS[input.mood];
    energy += mod.energyDelta;
    if (mod.moodOverride) mood = mod.moodOverride;
    if (mod.styleOverride) sessionStyle = mod.styleOverride;
  }

  // Energy preference override
  if (input.energy === "low")  energy = Math.min(energy, 3);
  if (input.energy === "high") energy = Math.max(energy, 7);

  // Time-of-day nudge
  if (input.timeOfDay) {
    energy += TIME_ENERGY_DELTA[input.timeOfDay];
  }

  // Clamp
  energy = Math.max(1, Math.min(10, energy));

  // Build track criteria (reuses core TrackCriteria)
  const trackCriteria: TrackCriteria = {
    mood,
    escalation: "stable",   // Life mode is steady-state, not narrative
    archetype: base.archetype,
    preferredGenres: base.preferredGenres,
  };

  // Transition: Life mode uses gentle crossfades by default
  const transition: TransitionStyle = energy >= 7
    ? { type: "crossfade", durationMs: 2000 }
    : { type: "crossfade", durationMs: 6000 };

  // Context summary for reasoning output
  const parts = [input.activity.replace(/_/g, " ")];
  if (input.mood) parts.push(`feeling ${input.mood}`);
  if (input.timeOfDay) parts.push(input.timeOfDay.replace(/_/g, " "));
  if (input.freeText) parts.push(`"${input.freeText}"`);
  const contextSummary = parts.join(" — ");

  return {
    activity: input.activity,
    mood,
    energyLevel: energy,
    sessionStyle,
    trackCriteria,
    transition,
    contextSummary,
  };
}
