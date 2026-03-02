/**
 * Life Engine — the orchestrator for Underscore Life.
 *
 * Mirrors the Narrative Engine but for real-life context:
 *   input → parse → classify → match → play → result
 *
 * Reuses the core song-matcher and playback-controller from RP mode.
 */
import { parseLifeContext } from "./context-parser.js";
import { classifyLifeScene } from "./life-classifier.js";
import { classifyLifeSceneAI } from "./life-ai-classifier.js";
import { matchLifeTrack } from "./life-matcher.js";
import { config } from "../core/config.js";
import { executePlayback } from "../core/playback-controller.js";
import type { LifeInput, LifeScene, LifeResult } from "./types.js";
import type { TrackMatch } from "../core/types.js";
import type { CachedTrack } from "../auth/session.js";

// ── Per-session state ──

interface LifeEngineState {
  previousScene: LifeScene | null;
  currentTrack: TrackMatch | null;
  lastProcessedAt: number;
}

const sessionStates = new Map<string, LifeEngineState>();

function getState(sessionId: string): LifeEngineState {
  let state = sessionStates.get(sessionId);
  if (!state) {
    state = {
      previousScene: null,
      currentTrack: null,
      lastProcessedAt: 0,
    };
    sessionStates.set(sessionId, state);
  }
  return state;
}

const DEBOUNCE_MS = 500;

/**
 * Process a life context through the full pipeline.
 *
 * input → parse → classify → match → play → result
 */
export async function processLifeInput(
  sessionId: string,
  rawInput: Partial<LifeInput>,
  token: string,
  library: CachedTrack[]
): Promise<LifeResult> {
  const state = getState(sessionId);

  // Debounce rapid submissions
  const now = Date.now();
  if (now - state.lastProcessedAt < DEBOUNCE_MS) {
    return {
      scene: state.previousScene ?? createDefaultLifeScene(),
      selectedTrack: state.currentTrack,
      playbackAction: "no_change",
      reasoning: "Debounced: too soon after last submission",
    };
  }
  state.lastProcessedAt = now;

  // 1. Parse — fill in missing fields
  const input = parseLifeContext(rawInput);

  // 2. Classify — use AI when Gemini is available, else deterministic
  const useAI = !config.useMockClassifier;
  const scene = useAI
    ? await classifyLifeSceneAI(input)
    : classifyLifeScene(input);

  // 3. Skip transition if scene is basically the same
  if (shouldSkipTransition(state.previousScene, scene)) {
    state.previousScene = scene;
    return {
      scene,
      selectedTrack: state.currentTrack,
      playbackAction: "no_change",
      reasoning: `Same vibe (${scene.activity}, energy ${scene.energyLevel}), keeping current track`,
    };
  }

  // 4. Match — find a track from library (or demo)
  const track = await matchLifeTrack(scene, library, token);

  if (!track) {
    state.previousScene = scene;
    return {
      scene,
      selectedTrack: null,
      playbackAction: "no_change",
      reasoning: "No matching track found for this context",
    };
  }

  // 5. Play (skip in demo mode)
  let success = false;
  if (token) {
    success = await executePlayback(token, track, scene.transition);
  } else {
    success = true; // demo mode
  }

  // 6. Update state
  state.previousScene = scene;
  state.currentTrack = track;

  return {
    scene,
    selectedTrack: track,
    playbackAction: success ? "play" : "no_change",
    reasoning: track.matchReason,
  };
}

/**
 * Life mode is more stable than RP — same activity + similar energy = skip.
 */
function shouldSkipTransition(
  prev: LifeScene | null,
  next: LifeScene
): boolean {
  if (!prev) return false;

  // Same activity + same session style + similar energy = keep the music going
  if (
    prev.activity === next.activity &&
    prev.sessionStyle === next.sessionStyle &&
    Math.abs(prev.energyLevel - next.energyLevel) <= 2
  ) {
    return true;
  }

  return false;
}

function createDefaultLifeScene(): LifeScene {
  return {
    activity: "relaxing",
    mood: "peaceful_unwinding",
    energyLevel: 3,
    sessionStyle: "ambient",
    trackCriteria: {
      mood: "peaceful_unwinding",
      escalation: "stable",
      archetype: "chill_ambient",
    },
    transition: { type: "crossfade", durationMs: 6000 },
    contextSummary: "relaxing",
  };
}
