/**
 * Core types for the Underscore narrative scoring engine.
 */

/** Scene classification returned by the LLM */
export interface SceneClassification {
  /** Detected franchise (e.g. "Genshin Impact", "Metal Gear Solid") or null for original fiction */
  franchise: string | null;
  /** Current scene type */
  sceneType: string;
  /** Emotional register of the scene */
  emotionalRegister: string;
  /** Energy level 1-10 */
  energyLevel: number;
  /** Narrative beat (e.g. "first_arrival_new_region", "combat_escalation") */
  narrativeBeat: string;
  /** Characters present in the scene */
  charactersPresent: string[];
  /** Combat status */
  combatStatus: "none" | "starting" | "active" | "ending";
  /** Recommended track criteria for song matching */
  trackCriteria: TrackCriteria;
  /** Transition style from current track */
  transition: TransitionStyle;
  /** Whether this triggers the Careless Whisper Protocol */
  intimateScene: boolean;
}

export interface TrackCriteria {
  mood: string;
  escalation: "building" | "sustaining" | "declining" | "stable";
  archetype: string;
  /** Specific track suggestions from the LLM's cultural knowledge */
  suggestedTracks?: string[];
  /** Genres to prefer */
  preferredGenres?: string[];
}

export type TransitionStyle =
  | { type: "hard_cut" }
  | { type: "crossfade"; durationMs: number }
  | { type: "fade_out_fade_in"; gapMs: number };

/** A matched track ready for playback */
export interface TrackMatch {
  spotifyUri: string;
  trackName: string;
  artistName: string;
  /** Why this track was selected */
  matchReason: string;
  /** Audio features from Spotify */
  energy: number;
  valence: number;
  tempo: number;
}

/** Playback command sent to the streaming platform */
export interface PlaybackCommand {
  track: TrackMatch;
  transition: TransitionStyle;
  /** Timestamp when this command was issued */
  timestamp: number;
}

/** Character profile for roleplay mode */
export interface CharacterProfile {
  characterId: string;
  displayName: string;
  tagline: string;
  franchise: string;
  /** Spotify playlist ID containing the character's soundtrack */
  soundtrackPlaylistId?: string;
  /** Scene-to-music mappings specific to this character */
  sceneMappings: Record<string, SceneMapping>;
  /** Transition style for this character */
  transitionStyle: string;
  /** Personality notes for the LLM */
  personalityNotes: string;
  /** Tracks to include/exclude */
  trackFilters?: {
    include: string[];
    exclude: string[];
  };
}

export interface SceneMapping {
  characterEquivalent: string;
  energy: string;
  preferredTracks: string[];
}

/** Parsed context from RP text */
export interface TextContext {
  /** The raw text that was parsed */
  rawText: string;
  /** Detected franchise from text cues */
  detectedFranchise: string | null;
  /** Characters mentioned or speaking */
  charactersMentioned: string[];
  /** Is this text describing action/combat? */
  hasCombat: boolean;
  /** Recent message history for context */
  recentMessages: string[];
}

/** User session state */
export interface SessionState {
  /** Spotify access token */
  spotifyAccessToken: string | null;
  /** Currently active character profile */
  activeCharacter: CharacterProfile | null;
  /** Current scene classification */
  currentScene: SceneClassification | null;
  /** Currently playing track */
  currentTrack: TrackMatch | null;
  /** Message history for context window */
  messageHistory: string[];
  /** Maximum messages to keep in context */
  maxHistorySize: number;
}
