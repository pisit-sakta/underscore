/**
 * Scene classifier using Google Gemini 3 Flash.
 *
 * Takes parsed RP text and returns a SceneClassification with franchise,
 * emotional register, energy level, narrative beat, and track criteria.
 */
import { GoogleGenAI } from "@google/genai";
import { config } from "./config.js";
import type { SceneClassification, TextContext } from "./types.js";
import { classifyWithMock } from "./mock-classifier.js";

let genai: GoogleGenAI | null = null;

function getClient(): GoogleGenAI {
  if (!genai) {
    genai = new GoogleGenAI({ apiKey: config.gemini.apiKey });
  }
  return genai;
}

const SYSTEM_PROMPT = `You are the Underscore narrative scoring engine. Your job is to classify scenes from text roleplay conversations to determine what music should play.

Analyze the provided text and return a JSON object with this exact structure:
{
  "franchise": string or null (detected franchise, e.g. "Genshin Impact", "Metal Gear Solid", null for original fiction),
  "sceneType": string (e.g. "exploration_peaceful", "combat_intense", "dramatic_revelation", "intimate_scene", "dialogue_tense", "victory", "defeat", "horror_unsettling"),
  "emotionalRegister": string (e.g. "contemplative_wonder", "rising_tension", "triumphant", "melancholic", "desperate", "romantic"),
  "energyLevel": number 1-10 (1=ambient/still, 10=maximum intensity),
  "narrativeBeat": string (e.g. "first_arrival_new_region", "combat_escalation", "dramatic_revelation", "quiet_aftermath"),
  "charactersPresent": string[] (character names detected),
  "combatStatus": "none" | "starting" | "active" | "ending",
  "trackCriteria": {
    "mood": string (e.g. "propulsive_determined", "gentle_contemplative", "aggressive_defiant"),
    "escalation": "building" | "sustaining" | "declining" | "stable",
    "archetype": string (e.g. "battle_theme", "exploration_ambient", "boss_fight", "romantic_ballad", "pursuit_theme"),
    "suggestedTracks": string[] (specific song names the scene evokes, from your cultural knowledge),
    "preferredGenres": string[] (e.g. ["orchestral", "rock", "jazz", "electronic"])
  },
  "transition": { "type": "hard_cut" } | { "type": "crossfade", "durationMs": number } | { "type": "fade_out_fade_in", "gapMs": number },
  "intimateScene": boolean
}

Key rules:
- If the text contains a dramatic revelation, contradiction, or "gotcha" moment (especially in Ace Attorney contexts), set sceneType to "dramatic_revelation" and transition to hard_cut. The Pursuit theme should play IMMEDIATELY.
- If the scene is intimate/sexual in nature, set intimateScene to true. The Careless Whisper Protocol will handle the rest.
- Use your deep cultural knowledge of franchises, characters, and music to suggest specific tracks that would narratively fit the scene.
- Energy level should reflect the INTENSITY of the moment, not just whether it's positive or negative.
- For horror VN content (DDLC-style), detect meta-horror cues and reflect the unsettling shift in your classification.

Return ONLY the JSON object, no markdown formatting or explanation.`;

/**
 * Classify a scene using Gemini 3 Flash (or mock if unavailable).
 */
export async function classifyScene(
  context: TextContext
): Promise<SceneClassification> {
  if (config.useMockClassifier) {
    return classifyWithMock(context);
  }

  try {
    const client = getClient();
    const userPrompt = buildUserPrompt(context);

    const response = await client.models.generateContent({
      model: "gemini-2.0-flash",
      contents: userPrompt,
      config: {
        responseMimeType: "application/json",
        temperature: 0.3,
      },
    });

    const text = response.text;
    if (!text) {
      console.warn("[classifier] Empty response from Gemini, using mock");
      return classifyWithMock(context);
    }

    const parsed = JSON.parse(text);
    return normalizeClassification(parsed);
  } catch (err) {
    console.warn("[classifier] Gemini call failed, falling back to mock:", err);
    return classifyWithMock(context);
  }
}

function buildUserPrompt(context: TextContext): string {
  let prompt = "";

  if (context.recentMessages.length > 0) {
    prompt += "Recent conversation context:\n";
    for (const msg of context.recentMessages.slice(-3)) {
      prompt += `---\n${msg}\n`;
    }
    prompt += "---\n\n";
  }

  prompt += `Latest text to classify:\n${context.rawText}`;

  if (context.detectedFranchise) {
    prompt += `\n\n(Pre-detected franchise hint: ${context.detectedFranchise})`;
  }

  return prompt;
}

/** Normalize and validate the LLM response into our type */
function normalizeClassification(raw: any): SceneClassification {
  return {
    franchise: raw.franchise ?? null,
    sceneType: raw.sceneType ?? "dialogue_neutral",
    emotionalRegister: raw.emotionalRegister ?? "neutral",
    energyLevel: Math.min(10, Math.max(1, Number(raw.energyLevel) || 5)),
    narrativeBeat: raw.narrativeBeat ?? "unknown",
    charactersPresent: Array.isArray(raw.charactersPresent)
      ? raw.charactersPresent
      : [],
    combatStatus: ["none", "starting", "active", "ending"].includes(
      raw.combatStatus
    )
      ? raw.combatStatus
      : "none",
    trackCriteria: {
      mood: raw.trackCriteria?.mood ?? "neutral",
      escalation: raw.trackCriteria?.escalation ?? "stable",
      archetype: raw.trackCriteria?.archetype ?? "ambient",
      suggestedTracks: raw.trackCriteria?.suggestedTracks ?? [],
      preferredGenres: raw.trackCriteria?.preferredGenres ?? [],
    },
    transition: normalizeTransition(raw.transition),
    intimateScene: raw.intimateScene === true,
  };
}

function normalizeTransition(raw: any): SceneClassification["transition"] {
  if (!raw || typeof raw !== "object") {
    return { type: "crossfade", durationMs: 4000 };
  }
  if (raw.type === "hard_cut") return { type: "hard_cut" };
  if (raw.type === "fade_out_fade_in") {
    return { type: "fade_out_fade_in", gapMs: Number(raw.gapMs) || 1000 };
  }
  return {
    type: "crossfade",
    durationMs: Number(raw.durationMs) || 4000,
  };
}
