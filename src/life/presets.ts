/**
 * Life mode presets — curated one-click scenarios.
 *
 * Each preset fills in a LifeInput so the user can just tap and go.
 */
import type { LifePreset } from "./types.js";

export const LIFE_PRESETS: LifePreset[] = [
  // ── Focus ──
  {
    id: "deep_work",
    label: "Deep Work",
    emoji: "\uD83C\uDFAF",
    input: { activity: "coding", mood: "focused", energy: "medium" },
  },
  {
    id: "study_session",
    label: "Study Session",
    emoji: "\uD83D\uDCDA",
    input: { activity: "studying", mood: "focused", energy: "low" },
  },

  // ── Movement ──
  {
    id: "morning_run",
    label: "Morning Run",
    emoji: "\uD83C\uDFC3",
    input: { activity: "running", mood: "energetic", energy: "high", timeOfDay: "morning" },
  },
  {
    id: "gym_grind",
    label: "Gym Grind",
    emoji: "\uD83D\uDCAA",
    input: { activity: "gym", mood: "energetic", energy: "high" },
  },
  {
    id: "road_trip",
    label: "Road Trip",
    emoji: "\uD83D\uDE97",
    input: { activity: "driving", mood: "happy", energy: "high" },
  },

  // ── Chill ──
  {
    id: "chill_evening",
    label: "Chill Evening",
    emoji: "\uD83C\uDF19",
    input: { activity: "relaxing", mood: "chill", energy: "low", timeOfDay: "evening" },
  },
  {
    id: "morning_coffee",
    label: "Morning Coffee",
    emoji: "\u2615",
    input: { activity: "waking_up", mood: "chill", energy: "low", timeOfDay: "morning" },
  },
  {
    id: "late_night_vibes",
    label: "Late Night",
    emoji: "\uD83C\uDF03",
    input: { activity: "relaxing", mood: "melancholy", energy: "low", timeOfDay: "late_night" },
  },

  // ── Social ──
  {
    id: "dinner_party",
    label: "Dinner Party",
    emoji: "\uD83C\uDF77",
    input: { activity: "hanging_out", mood: "happy", energy: "medium", timeOfDay: "evening" },
  },
  {
    id: "house_party",
    label: "House Party",
    emoji: "\uD83C\uDF89",
    input: { activity: "party", mood: "energetic", energy: "high", timeOfDay: "night" },
  },
  {
    id: "date_night",
    label: "Date Night",
    emoji: "\u2764\uFE0F",
    input: { activity: "date_night", mood: "romantic", energy: "medium", timeOfDay: "evening" },
  },

  // ── Domestic ──
  {
    id: "cooking_vibe",
    label: "Cooking",
    emoji: "\uD83C\uDF73",
    input: { activity: "cooking", mood: "happy", energy: "medium" },
  },
  {
    id: "cleaning_montage",
    label: "Cleaning Montage",
    emoji: "\uD83E\uDDF9",
    input: { activity: "cleaning", mood: "energetic", energy: "high" },
  },

  // ── Creative ──
  {
    id: "creative_flow",
    label: "Creative Flow",
    emoji: "\uD83C\uDFA8",
    input: { activity: "creating", mood: "focused", energy: "medium" },
  },
  {
    id: "gaming_session",
    label: "Gaming",
    emoji: "\uD83C\uDFAE",
    input: { activity: "gaming", mood: "focused", energy: "medium" },
  },
];
