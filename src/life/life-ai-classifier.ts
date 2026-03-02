/**
 * AI-powered Life classifier — uses Gemini to interpret context.
 *
 * When the user provides free text or when richer classification is needed,
 * this classifier uses Gemini to understand nuance that the deterministic
 * classifier can't capture (e.g. "grinding leetcode at 2am while stressed").
 *
 * Falls back to the deterministic classifier on failure.
 */
import { GoogleGenAI } from "@google/genai";
import { config } from "../core/config.js";
import { classifyLifeScene } from "./life-classifier.js";
import type { LifeInput, LifeScene, SessionStyle, LifeActivity } from "./types.js";
import type { TrackCriteria, TransitionStyle } from "../core/types.js";

let genai: GoogleGenAI | null = null;

function getClient(): GoogleGenAI {
  if (!genai) {
    genai = new GoogleGenAI({ apiKey: config.gemini.apiKey });
  }
  return genai;
}

/** Reset the cached client (called when API key changes) */
export function resetLifeAiClient(): void {
  genai = null;
}

const SYSTEM_PROMPT = `You are the Underscore Life scoring engine. You classify real-life contexts into music scene parameters.

Given the user's current activity, mood, time of day, and optional free-text description, return a JSON object:

{
  "activity": string (one of: working, studying, coding, reading, commuting, walking, driving, running, gym, yoga, hanging_out, party, date_night, cooking, cleaning, eating, relaxing, meditating, waking_up, winding_down, creating, gaming),
  "mood": string (descriptive mood like "focused_flow", "melancholy_reflective", "energetic_pumped"),
  "energyLevel": number 1-10,
  "sessionStyle": string (one of: deep_focus, light_focus, energize, wind_down, social, soundtrack, ambient),
  "trackCriteria": {
    "mood": string (same as mood above),
    "escalation": "stable",
    "archetype": string (e.g. "lo_fi_beats", "gym_power", "indie_walk", "ambient_zen"),
    "preferredGenres": string[] (2-4 genres that fit this moment)
  },
  "contextSummary": string (brief human-readable summary of the vibe)
}

Key rules:
- Use the free text to understand NUANCE. "coding at 3am wired on coffee" is different from "casual afternoon coding".
- Time of day matters: late night should generally lower energy unless the context says otherwise.
- Mood text overrides preset mood tags when they conflict.
- Be creative with genre suggestions — match the vibe, not just the activity.
- energyLevel should reflect the REAL energy of the moment, considering all inputs.

Return ONLY the JSON object, no markdown formatting.`;

/**
 * Classify a life context using Gemini AI.
 * Falls back to deterministic classifier on any failure.
 */
export async function classifyLifeSceneAI(input: LifeInput): Promise<LifeScene> {
  if (config.useMockClassifier) {
    return classifyLifeScene(input);
  }

  try {
    const client = getClient();
    const userPrompt = buildPrompt(input);

    const response = await client.models.generateContent({
      model: "gemini-2.0-flash",
      contents: userPrompt,
      config: {
        systemInstruction: SYSTEM_PROMPT,
        responseMimeType: "application/json",
        temperature: 0.4,
      },
    });

    const text = response.text;
    if (!text) {
      return classifyLifeScene(input);
    }

    return normalizeAIScene(JSON.parse(text), input);
  } catch (err) {
    console.warn("[life-ai] Gemini call failed, falling back to deterministic:", err);
    return classifyLifeScene(input);
  }
}

function buildPrompt(input: LifeInput): string {
  const parts: string[] = [];
  parts.push(`Activity: ${input.activity}`);
  if (input.mood) parts.push(`Mood: ${input.mood}`);
  if (input.energy) parts.push(`Energy preference: ${input.energy}`);
  if (input.timeOfDay) parts.push(`Time of day: ${input.timeOfDay}`);
  if (input.freeText) parts.push(`Context: "${input.freeText}"`);
  return parts.join("\n");
}

const VALID_ACTIVITIES = new Set<string>([
  "working", "studying", "coding", "reading", "commuting", "walking", "driving",
  "running", "gym", "yoga", "hanging_out", "party", "date_night", "cooking",
  "cleaning", "eating", "relaxing", "meditating", "waking_up", "winding_down",
  "creating", "gaming",
]);

const VALID_STYLES = new Set<string>([
  "deep_focus", "light_focus", "energize", "wind_down", "social", "soundtrack", "ambient",
]);

function normalizeAIScene(raw: any, input: LifeInput): LifeScene {
  const activity = VALID_ACTIVITIES.has(raw.activity)
    ? (raw.activity as LifeActivity)
    : input.activity;

  const sessionStyle = VALID_STYLES.has(raw.sessionStyle)
    ? (raw.sessionStyle as SessionStyle)
    : "light_focus";

  const energyLevel = Math.max(1, Math.min(10, Number(raw.energyLevel) || 5));

  const trackCriteria: TrackCriteria = {
    mood: raw.trackCriteria?.mood ?? raw.mood ?? "neutral",
    escalation: "stable",
    archetype: raw.trackCriteria?.archetype ?? "ambient",
    preferredGenres: Array.isArray(raw.trackCriteria?.preferredGenres)
      ? raw.trackCriteria.preferredGenres
      : undefined,
  };

  const transition: TransitionStyle = energyLevel >= 7
    ? { type: "crossfade", durationMs: 2000 }
    : { type: "crossfade", durationMs: 6000 };

  return {
    activity,
    mood: raw.mood ?? "neutral",
    energyLevel,
    sessionStyle,
    trackCriteria,
    transition,
    contextSummary: raw.contextSummary ?? `${activity} — energy ${energyLevel}`,
  };
}
