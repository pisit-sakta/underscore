/**
 * Life context parser — normalizes user input into a clean LifeInput.
 *
 * Auto-detects time of day from system clock when not provided.
 * Infers energy preference from activity when not explicit.
 */
import type { LifeInput, LifeActivity, TimeOfDay, EnergyPreference } from "./types.js";

/** Auto-detect time of day from the server clock */
export function detectTimeOfDay(): TimeOfDay {
  const hour = new Date().getHours();
  if (hour < 6)  return "late_night";
  if (hour < 9)  return "early_morning";
  if (hour < 12) return "morning";
  if (hour < 17) return "afternoon";
  if (hour < 21) return "evening";
  return "night";
}

/** Default energy level for each activity when user doesn't specify */
const ACTIVITY_ENERGY: Record<LifeActivity, EnergyPreference> = {
  // Focus — low to medium
  working:     "medium",
  studying:    "low",
  coding:      "medium",
  reading:     "low",
  // Movement — medium
  commuting:   "medium",
  walking:     "medium",
  driving:     "medium",
  // Exercise — high
  running:     "high",
  gym:         "high",
  yoga:        "low",
  // Social — medium to high
  hanging_out: "medium",
  party:       "high",
  date_night:  "medium",
  // Domestic — low to medium
  cooking:     "medium",
  cleaning:    "medium",
  eating:      "low",
  // Rest — low
  relaxing:    "low",
  meditating:  "low",
  waking_up:   "low",
  winding_down:"low",
  // Creative — medium
  creating:    "medium",
  gaming:      "medium",
};

/**
 * Normalize and fill in missing fields on a LifeInput.
 * Returns a fully-populated LifeInput ready for classification.
 */
export function parseLifeContext(raw: Partial<LifeInput>): LifeInput {
  const activity: LifeActivity = raw.activity ?? "relaxing";

  return {
    activity,
    mood:      raw.mood      ?? undefined,
    energy:    raw.energy    ?? ACTIVITY_ENERGY[activity],
    timeOfDay: raw.timeOfDay ?? detectTimeOfDay(),
    freeText:  raw.freeText?.trim() || undefined,
  };
}
