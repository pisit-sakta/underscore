/**
 * Life mode types — Underscore Life
 *
 * "Your day, scored."
 *
 * Life mode scores your real-world context (activity, mood, time of day)
 * instead of roleplay text. Same pipeline downstream: classify → match → play.
 */
import type { TrackCriteria, TransitionStyle } from "../core/types.js";

// ── Activities ──

export type LifeActivity =
  // Focus
  | "working"
  | "studying"
  | "coding"
  | "reading"
  // Movement
  | "commuting"
  | "walking"
  | "driving"
  // Exercise
  | "running"
  | "gym"
  | "yoga"
  // Social
  | "hanging_out"
  | "party"
  | "date_night"
  // Domestic
  | "cooking"
  | "cleaning"
  | "eating"
  // Rest
  | "relaxing"
  | "meditating"
  | "waking_up"
  | "winding_down"
  // Creative
  | "creating"
  | "gaming";

export type MoodTag =
  | "happy"
  | "chill"
  | "focused"
  | "stressed"
  | "melancholy"
  | "energetic"
  | "anxious"
  | "nostalgic"
  | "romantic"
  | "angry";

export type TimeOfDay =
  | "early_morning"
  | "morning"
  | "afternoon"
  | "evening"
  | "night"
  | "late_night";

export type EnergyPreference = "low" | "medium" | "high";

// ── Input ──

/** What the user sends to /api/life/score */
export interface LifeInput {
  activity: LifeActivity;
  mood?: MoodTag;
  energy?: EnergyPreference;
  timeOfDay?: TimeOfDay;
  /** Optional free-text for nuance ("grinding leetcode at 2am", "road trip with friends") */
  freeText?: string;
}

// ── Classification ──

/** Session style — what kind of listening experience fits */
export type SessionStyle =
  | "deep_focus"     // No lyrics, low variation, steady BPM
  | "light_focus"    // Chill beats, lo-fi, soft vocals OK
  | "energize"       // High energy, upbeat, pump-up
  | "wind_down"      // Decreasing energy, ambient, soft
  | "social"         // Crowd-pleasers, upbeat, singalongs
  | "soundtrack"     // Cinematic, epic, immersive
  | "ambient";       // Background texture, minimal

/** The classified "scene" for Life mode — analogous to SceneClassification */
export interface LifeScene {
  activity: LifeActivity;
  mood: string;
  energyLevel: number;           // 1-10, same scale as RP mode
  sessionStyle: SessionStyle;
  trackCriteria: TrackCriteria;  // Reuse from core — same matching downstream
  transition: TransitionStyle;   // Reuse from core
  /** Context string for the LLM / reasoning output */
  contextSummary: string;
}

// ── Engine result ──

/** Returned from the Life engine — mirrors NarrativeResult shape */
export interface LifeResult {
  scene: LifeScene;
  selectedTrack: import("../core/types.js").TrackMatch | null;
  playbackAction: "play" | "crossfade" | "no_change";
  reasoning: string;
}

// ── Presets ──

/** A curated preset that fills LifeInput with one click */
export interface LifePreset {
  id: string;
  label: string;
  emoji: string;
  input: LifeInput;
}
