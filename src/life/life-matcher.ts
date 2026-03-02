/**
 * Life track matcher — wraps the core song-matcher with Life-specific
 * demo tracks and archetype mappings.
 *
 * In demo mode:  returns curated demo tracks per life archetype.
 * With Spotify:  delegates to the core matchSong() via a shim
 *                SceneClassification built from the LifeScene.
 */
import type { LifeScene } from "./types.js";
import type { SceneClassification, TrackMatch } from "../core/types.js";
import type { CachedTrack } from "../auth/session.js";
import { matchSong } from "../core/song-matcher.js";

/** Demo tracks for Life mode archetypes */
const LIFE_DEMO_TRACKS: Record<string, { name: string; artist: string }> = {
  // Focus
  lo_fi_beats:      { name: "Lofi Girl Mix",              artist: "Various Artists" },
  ambient_focus:    { name: "Gymnopédie No.1",            artist: "Erik Satie" },
  synthwave_focus:  { name: "Midnight City",              artist: "M83" },
  ambient_minimal:  { name: "Music for Airports",         artist: "Brian Eno" },

  // Movement
  indie_cruise:     { name: "Electric Feel",              artist: "MGMT" },
  indie_walk:       { name: "Dog Days Are Over",          artist: "Florence + The Machine" },
  driving_anthems:  { name: "Shut Up and Drive",          artist: "Rihanna" },

  // Exercise
  high_bpm_workout: { name: "Lose Yourself",              artist: "Eminem" },
  gym_power:        { name: "Stronger",                   artist: "Kanye West" },
  ambient_zen:      { name: "Weightless",                 artist: "Marconi Union" },

  // Social
  chill_vibes:      { name: "Blinding Lights",            artist: "The Weeknd" },
  party_bangers:    { name: "One More Time",              artist: "Daft Punk" },
  romantic_evening: { name: "At Last",                    artist: "Etta James" },

  // Domestic
  kitchen_groove:   { name: "Superstition",               artist: "Stevie Wonder" },
  cleaning_power:   { name: "Dancing Queen",              artist: "ABBA" },
  dinner_jazz:      { name: "Fly Me to the Moon",         artist: "Frank Sinatra" },

  // Rest
  chill_ambient:    { name: "Intro",                      artist: "The xx" },
  meditation_drone: { name: "Structures from Silence",    artist: "Steve Roach" },
  morning_ease:     { name: "Here Comes the Sun",         artist: "The Beatles" },
  night_ambient:    { name: "Clair de Lune",              artist: "Debussy" },

  // Creative
  creative_flow:    { name: "Baba O'Riley",               artist: "The Who" },
  game_soundtrack:  { name: "Main Theme - Skyrim",        artist: "Jeremy Soule" },
};

/**
 * Match a LifeScene to a track.
 *
 * In demo mode: returns a curated demo track.
 * With Spotify: converts to a SceneClassification shim and delegates
 *               to the core song-matcher.
 */
export async function matchLifeTrack(
  scene: LifeScene,
  library: CachedTrack[],
  token: string,
): Promise<TrackMatch | null> {
  const isDemo = !token;

  if (isDemo) {
    return demoMatchFromLifeScene(scene);
  }

  // Convert LifeScene → SceneClassification shim so we can reuse matchSong()
  const shim = lifeSceneToClassification(scene);
  return matchSong(shim, library, token);
}

/**
 * Build a SceneClassification from a LifeScene.
 * This lets us reuse the entire core scoring pipeline.
 */
function lifeSceneToClassification(scene: LifeScene): SceneClassification {
  return {
    franchise: null,
    sceneType: scene.activity,
    emotionalRegister: scene.mood,
    energyLevel: scene.energyLevel,
    narrativeBeat: scene.sessionStyle,
    charactersPresent: [],
    combatStatus: "none",
    trackCriteria: scene.trackCriteria,
    transition: scene.transition,
    intimateScene: false,
  };
}

/** Generate a demo match for Life mode */
function demoMatchFromLifeScene(scene: LifeScene): TrackMatch {
  // AI-suggested tracks take priority over archetype lookup
  if (scene.trackCriteria.suggestedTracks?.length) {
    const suggested = scene.trackCriteria.suggestedTracks[0];
    return {
      spotifyUri: `demo:life:${suggested.toLowerCase().replace(/\s+/g, "_")}`,
      trackName: suggested,
      artistName: "AI Pick",
      matchReason: `[DEMO] AI suggested: "${suggested}" — ${scene.contextSummary}`,
      energy: scene.energyLevel / 10,
      valence: 0.5,
      tempo: 120,
    };
  }

  const archetype = scene.trackCriteria.archetype;
  const suggestion = LIFE_DEMO_TRACKS[archetype]
    ?? LIFE_DEMO_TRACKS["chill_ambient"]!;

  return {
    spotifyUri: `demo:life:${suggestion.name.toLowerCase().replace(/\s+/g, "_")}`,
    trackName: suggestion.name,
    artistName: suggestion.artist,
    matchReason: `[DEMO] Life mode: ${archetype.replace(/_/g, " ")} → "${suggestion.name}" for ${scene.contextSummary}`,
    energy: scene.energyLevel / 10,
    valence: 0.5,
    tempo: 120,
  };
}
