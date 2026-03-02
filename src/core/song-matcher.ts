/**
 * Song matcher — matches a SceneClassification to the best track
 * from the user's Spotify library.
 *
 * Uses Spotify audio features (energy, valence, tempo) combined with
 * the scene's emotional and narrative requirements.
 */
import type { SceneClassification, TrackMatch } from "./types.js";
import type { CachedTrack } from "../auth/session.js";
import { searchTrack } from "./playback-controller.js";

/** Recently played URIs to avoid repetition */
const recentlyPlayed: string[] = [];
const MAX_RECENT = 10;

/** Emotional register → target valence mapping */
const VALENCE_MAP: Record<string, [number, number]> = {
  triumphant: [0.6, 1.0],
  triumphant_vindication: [0.7, 1.0],
  triumphant_joyful: [0.7, 1.0],
  contemplative_wonder: [0.3, 0.6],
  aggressive_determined: [0.2, 0.5],
  desperate_defiant: [0.1, 0.4],
  melancholic: [0.0, 0.3],
  melancholic_defiant: [0.1, 0.4],
  melancholic_tender: [0.0, 0.3],
  romantic_intense: [0.4, 0.7],
  rising_tension: [0.2, 0.5],
  dread_building: [0.0, 0.3],
  neutral: [0.3, 0.7],
  somber_defiant: [0.1, 0.4],
};

/** Scene archetype → target tempo range (BPM) */
const TEMPO_MAP: Record<string, [number, number]> = {
  battle_theme: [120, 180],
  boss_fight: [130, 200],
  pursuit_theme: [140, 190],
  exploration_ambient: [60, 110],
  emotional_piano: [50, 100],
  romantic_ballad: [60, 110],
  ambient: [60, 100],
  victory_fanfare: [110, 160],
  defeat_theme: [60, 100],
  suspense_theme: [80, 120],
  horror_ambient: [60, 110],
};

/**
 * Match a scene to the best track from the user's library.
 *
 * Special cases:
 * - Careless Whisper Protocol: intimate scenes → search for Careless Whisper
 * - Ace Attorney Rule: dramatic revelations → search for Pursuit/Cornered theme
 */
export async function matchSong(
  scene: SceneClassification,
  library: CachedTrack[],
  token: string
): Promise<TrackMatch | null> {
  // === CARELESS WHISPER PROTOCOL ===
  if (scene.intimateScene) {
    const careless = await searchTrack(
      token,
      "Careless Whisper George Michael"
    );
    if (careless) {
      careless.matchReason = "Careless Whisper Protocol activated. The saxophone is non-negotiable.";
      return careless;
    }
  }

  // === ACE ATTORNEY EXCEPTION RULE ===
  if (scene.sceneType === "dramatic_revelation") {
    const pursuit = await searchTrack(
      token,
      "Pursuit Cornered Ace Attorney"
    );
    if (pursuit) {
      pursuit.matchReason = "Ace Attorney Exception Rule: HARD CUT to Pursuit ~ Cornered. This is the law.";
      return pursuit;
    }
  }

  // === STANDARD MATCHING ===
  if (library.length === 0) {
    // No library cached — try a Spotify search based on scene criteria
    return searchBySceneCriteria(scene, token);
  }

  const scored = library
    .filter((t) => !recentlyPlayed.includes(t.uri))
    .map((track) => ({
      track,
      score: scoreTrack(track, scene),
    }))
    .sort((a, b) => b.score - a.score);

  const best = scored[0];
  if (!best || best.score < 0.1) {
    // Library has no good match — fall back to search
    return searchBySceneCriteria(scene, token);
  }

  // Track as recently played
  recentlyPlayed.push(best.track.uri);
  if (recentlyPlayed.length > MAX_RECENT) recentlyPlayed.shift();

  return {
    spotifyUri: best.track.uri,
    trackName: best.track.name,
    artistName: best.track.artist,
    matchReason: `Matched: energy=${best.track.energy.toFixed(2)}, valence=${best.track.valence.toFixed(2)}, tempo=${best.track.tempo.toFixed(0)}bpm (score: ${best.score.toFixed(3)})`,
    energy: best.track.energy,
    valence: best.track.valence,
    tempo: best.track.tempo,
  };
}

/**
 * Score a track against a scene classification.
 * Higher = better match.
 */
function scoreTrack(track: CachedTrack, scene: SceneClassification): number {
  let score = 0;
  const targetEnergy = scene.energyLevel / 10;

  // Energy alignment (0-0.35)
  const energyDiff = Math.abs(track.energy - targetEnergy);
  score += (1 - energyDiff) * 0.35;

  // Valence alignment (0-0.25)
  const valenceRange = VALENCE_MAP[scene.emotionalRegister] ??
    VALENCE_MAP["neutral"]!;
  if (track.valence >= valenceRange[0] && track.valence <= valenceRange[1]) {
    score += 0.25;
  } else {
    const valenceDist = Math.min(
      Math.abs(track.valence - valenceRange[0]),
      Math.abs(track.valence - valenceRange[1])
    );
    score += Math.max(0, 0.25 - valenceDist * 0.5);
  }

  // Tempo alignment (0-0.2)
  const tempoRange = TEMPO_MAP[scene.trackCriteria.archetype] ??
    TEMPO_MAP["ambient"]!;
  if (track.tempo >= tempoRange[0] && track.tempo <= tempoRange[1]) {
    score += 0.2;
  } else {
    const tempoDist =
      Math.min(
        Math.abs(track.tempo - tempoRange[0]),
        Math.abs(track.tempo - tempoRange[1])
      ) / 100;
    score += Math.max(0, 0.2 - tempoDist * 0.3);
  }

  // Franchise keyword bonus (0-0.1)
  if (scene.franchise) {
    const franchiseLower = scene.franchise.toLowerCase();
    const trackLower = `${track.name} ${track.artist}`.toLowerCase();
    if (trackLower.includes(franchiseLower)) {
      score += 0.1;
    }
  }

  // Suggested track name match bonus (0-0.1)
  if (scene.trackCriteria.suggestedTracks) {
    const trackLower = track.name.toLowerCase();
    for (const suggested of scene.trackCriteria.suggestedTracks) {
      if (trackLower.includes(suggested.toLowerCase())) {
        score += 0.1;
        break;
      }
    }
  }

  return score;
}

/** Fall back to Spotify search when library matching fails */
async function searchBySceneCriteria(
  scene: SceneClassification,
  token: string
): Promise<TrackMatch | null> {
  // Build a search query from the scene
  const parts: string[] = [];

  if (scene.franchise) {
    parts.push(`${scene.franchise} soundtrack`);
  }

  if (scene.trackCriteria.suggestedTracks?.length) {
    // Try the first suggested track
    const result = await searchTrack(
      token,
      scene.trackCriteria.suggestedTracks[0]
    );
    if (result) {
      result.matchReason = `LLM suggested: "${scene.trackCriteria.suggestedTracks[0]}" for ${scene.sceneType}`;
      return result;
    }
  }

  // Generic search based on archetype
  const query = parts.length > 0
    ? parts.join(" ")
    : `${scene.trackCriteria.mood} ${scene.trackCriteria.archetype}`.replace(/_/g, " ");

  const result = await searchTrack(token, query);
  if (result) {
    result.matchReason = `Spotify search: "${query}" for scene type ${scene.sceneType}`;
  }
  return result;
}
