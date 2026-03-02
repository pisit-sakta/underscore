/**
 * AI-powered Life classifier — uses Gemini for fully dynamic scoring.
 *
 * Unlike the deterministic classifier that maps to fixed archetypes,
 * this lets the AI freely suggest specific tracks, artists, and vibes.
 * "Gym + energetic + 'feeling like a final boss'" → "Rules of Nature"
 * "Driving at night + melancholy" → "Nightcall by Kavinsky"
 *
 * Falls back to the deterministic classifier on failure.
 */
import { GoogleGenAI } from "@google/genai";
import { config } from "../core/config.js";
import { classifyLifeScene } from "./life-classifier.js";
import type { LifeInput, LifeScene, SessionStyle, LifeActivity } from "./types.js";
import type { TrackCriteria, TransitionStyle } from "../core/types.js";
import type { TasteProfile } from "../core/library-cache.js";

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

const SYSTEM_PROMPT = `You are Underscore — an AI that picks the PERFECT song for any moment in someone's life. You have encyclopedic knowledge of music across every genre, era, soundtrack, and vibe.

Your job: given what someone is doing right now, suggest the exact tracks that would make this moment feel like a movie scene.

Return a JSON object:
{
  "activity": string (closest match: working, studying, coding, reading, commuting, walking, driving, running, gym, yoga, hanging_out, party, date_night, cooking, cleaning, eating, relaxing, meditating, waking_up, winding_down, creating, gaming),
  "mood": string (freeform — describe the exact emotional texture, e.g. "protagonist_energy", "main_character_morning", "final_boss_aura", "night_drive_melancholy"),
  "energyLevel": number 1-10,
  "sessionStyle": string (one of: deep_focus, light_focus, energize, wind_down, social, soundtrack, ambient),
  "trackCriteria": {
    "mood": string (same as above),
    "escalation": "stable",
    "archetype": string (freeform vibe description, not a category),
    "suggestedTracks": string[] (3-5 SPECIFIC song names that would be PERFECT right now — real songs, from any genre/era/soundtrack),
    "preferredGenres": string[] (2-4 genres)
  },
  "contextSummary": string (short cinematic description of the vibe, like a movie scene direction)
}

CRITICAL RULES:
- suggestedTracks is the most important field. Be SPECIFIC and CREATIVE. Real song names by real artists.
- Don't default to generic "lo-fi beats" or "chill playlist" vibes. Think about what would make THIS specific moment cinematic.
- Use your deep knowledge of: game soundtracks (MGR, Persona, FF, Nier), film scores (Bond, Interstellar, Drive), anime OSTs, and every genre of music.
- "gym + feeling like a final boss" → "Rules of Nature" by Jamie Christopherson, "Bury the Light" by Casey Edwards
- "driving at night alone" → "Nightcall" by Kavinsky, "A Real Hero" by College & Electric Youth
- "cooking Sunday morning" → "Lovely Day" by Bill Withers, "September" by Earth Wind & Fire
- "coding late night in the zone" → "Crystalize" by Lindsey Stirling, "Derezzed" by Daft Punk
- Match the ENERGY and NARRATIVE of the moment, not just the activity category.
- Time of day dramatically affects the vibe. 2am coding is NOT the same as 2pm coding.
- The free text context is gold — use every word of it to nail the vibe.

Return ONLY the JSON object, no markdown.`;

/**
 * Classify a life context using Gemini AI — fully dynamic.
 * Falls back to deterministic classifier on any failure.
 */
export async function classifyLifeSceneAI(input: LifeInput, tasteProfile: TasteProfile | null): Promise<LifeScene> {
  if (config.useMockClassifier) {
    return classifyLifeScene(input);
  }

  try {
    const client = getClient();
    const userPrompt = buildPrompt(input, tasteProfile);

    const response = await client.models.generateContent({
      model: "gemini-2.0-flash",
      contents: userPrompt,
      config: {
        systemInstruction: SYSTEM_PROMPT,
        responseMimeType: "application/json",
        temperature: 0.7,
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

function buildPrompt(input: LifeInput, tasteProfile: TasteProfile | null): string {
  const parts: string[] = [];
  parts.push(`Activity: ${input.activity}`);
  if (input.mood) parts.push(`Mood: ${input.mood}`);
  if (input.energy) parts.push(`Energy preference: ${input.energy}`);
  if (input.timeOfDay) parts.push(`Time of day: ${input.timeOfDay.replace(/_/g, " ")}`);
  if (input.freeText) parts.push(`Context: "${input.freeText}"`);

  // Inject taste profile for personalized suggestions
  if (tasteProfile) {
    parts.push("");
    parts.push("=== USER'S MUSIC TASTE (from their Spotify) ===");
    if (tasteProfile.topArtists.length > 0) {
      parts.push(`Favorite artists: ${tasteProfile.topArtists.join(", ")}`);
    }
    if (tasteProfile.topTracks.length > 0) {
      parts.push(`Top tracks: ${tasteProfile.topTracks.join(", ")}`);
    }
    if (tasteProfile.topGenres.length > 0) {
      parts.push(`Genres they love: ${tasteProfile.topGenres.join(", ")}`);
    }
    parts.push("Use this taste profile to personalize your suggestions — lean toward their favorite artists and genres when appropriate, but still surprise them with perfect picks they might not expect.");
  }

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

  const suggestedTracks = Array.isArray(raw.trackCriteria?.suggestedTracks)
    ? raw.trackCriteria.suggestedTracks.filter((t: any) => typeof t === "string")
    : undefined;

  const trackCriteria: TrackCriteria = {
    mood: raw.trackCriteria?.mood ?? raw.mood ?? "neutral",
    escalation: "stable",
    archetype: raw.trackCriteria?.archetype ?? "ambient",
    suggestedTracks,
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
