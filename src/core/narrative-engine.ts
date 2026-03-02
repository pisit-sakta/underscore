/**
 * The Narrative Engine — the heart of Underscore.
 *
 * Orchestrates: text → parse → classify → match → play
 *
 * One function. The whole pipeline.
 */
import { parseText } from "../rp/text-parser.js";
import { classifyScene } from "./scene-classifier.js";
import { matchSong } from "./song-matcher.js";
import { executePlayback } from "./playback-controller.js";
import type { SceneClassification, TrackMatch } from "./types.js";
import type { CachedTrack } from "../auth/session.js";

export interface NarrativeResult {
  scene: SceneClassification;
  selectedTrack: TrackMatch | null;
  playbackAction: "play" | "crossfade" | "hard_cut" | "no_change";
  reasoning: string;
}

/** Per-session state to track previous scenes and avoid redundant switches */
interface EngineState {
  previousScene: SceneClassification | null;
  currentTrack: TrackMatch | null;
  messageHistory: string[];
  lastProcessedAt: number;
}

const sessionStates = new Map<string, EngineState>();

function getEngineState(sessionId: string): EngineState {
  let state = sessionStates.get(sessionId);
  if (!state) {
    state = {
      previousScene: null,
      currentTrack: null,
      messageHistory: [],
      lastProcessedAt: 0,
    };
    sessionStates.set(sessionId, state);
  }
  return state;
}

/**
 * Debounce: ignore submissions within this window.
 * Short in demo mode (rapid testing), longer in production (prevents API spam).
 */
const DEBOUNCE_MS = 500;

/**
 * Process text through the full narrative pipeline.
 *
 * text in → parse → classify → match → play → result out
 */
export async function processText(
  sessionId: string,
  rawText: string,
  token: string,
  library: CachedTrack[]
): Promise<NarrativeResult> {
  const state = getEngineState(sessionId);

  // Debounce rapid submissions
  const now = Date.now();
  if (now - state.lastProcessedAt < DEBOUNCE_MS) {
    return {
      scene: state.previousScene ?? createDefaultScene(),
      selectedTrack: state.currentTrack,
      playbackAction: "no_change",
      reasoning: "Debounced: too soon after last submission",
    };
  }
  state.lastProcessedAt = now;

  // 1. Parse
  const context = parseText(rawText, state.messageHistory);

  // Add to message history (keep last 10)
  state.messageHistory.push(rawText);
  if (state.messageHistory.length > 10) {
    state.messageHistory.shift();
  }

  // 2. Classify
  const scene = await classifyScene(context);

  // 3. Determine if we need to change tracks
  if (shouldSkipTransition(state.previousScene, scene)) {
    state.previousScene = scene;
    return {
      scene,
      selectedTrack: state.currentTrack,
      playbackAction: "no_change",
      reasoning: `Scene similar to previous (${scene.sceneType}), keeping current track`,
    };
  }

  // 4. Match
  const track = await matchSong(scene, library, token);

  if (!track) {
    state.previousScene = scene;
    return {
      scene,
      selectedTrack: null,
      playbackAction: "no_change",
      reasoning: "No matching track found for this scene",
    };
  }

  // 5. Play (skip in demo mode when no Spotify token is available)
  let success = false;
  if (token) {
    success = await executePlayback(token, track, scene.transition);
  } else {
    success = true; // demo mode — always "succeed"
  }

  // 6. Update state
  state.previousScene = scene;
  state.currentTrack = track;

  const action =
    scene.transition.type === "hard_cut"
      ? "hard_cut"
      : scene.transition.type === "crossfade"
        ? "crossfade"
        : "play";

  return {
    scene,
    selectedTrack: track,
    playbackAction: success ? action : "no_change",
    reasoning: track.matchReason,
  };
}

/**
 * Determine if the scene change is significant enough to warrant a track switch.
 * Avoids jarring switches on minor scene variations.
 */
function shouldSkipTransition(
  prev: SceneClassification | null,
  next: SceneClassification
): boolean {
  if (!prev) return false;

  // Always transition on these high-impact scene types
  if (
    next.sceneType === "dramatic_revelation" ||
    next.intimateScene ||
    next.sceneType === "boss_fight"
  ) {
    return false;
  }

  // Same scene type + similar energy = no change needed
  if (
    prev.sceneType === next.sceneType &&
    Math.abs(prev.energyLevel - next.energyLevel) <= 2
  ) {
    return true;
  }

  return false;
}

function createDefaultScene(): SceneClassification {
  return {
    franchise: null,
    sceneType: "dialogue_neutral",
    emotionalRegister: "neutral",
    energyLevel: 4,
    narrativeBeat: "conversation",
    charactersPresent: [],
    combatStatus: "none",
    trackCriteria: {
      mood: "calm_neutral",
      escalation: "stable",
      archetype: "ambient",
    },
    transition: { type: "crossfade", durationMs: 4000 },
    intimateScene: false,
  };
}
