/**
 * Simple in-memory session store.
 * Maps session IDs to Spotify tokens and cached library data.
 */
import crypto from "node:crypto";
import type { SpotifyTokens } from "./spotify-auth.js";
import type { SessionState, TrackMatch } from "../core/types.js";

interface StoredSession {
  tokens: SpotifyTokens;
  state: SessionState;
  /** Cached library tracks with audio features */
  library: CachedTrack[];
}

export interface CachedTrack {
  id: string;
  uri: string;
  name: string;
  artist: string;
  albumArt: string | null;
  energy: number;
  valence: number;
  tempo: number;
  genres: string[];
}

const sessions = new Map<string, StoredSession>();

export function createSession(tokens: SpotifyTokens): string {
  const sessionId = crypto.randomUUID();
  sessions.set(sessionId, {
    tokens,
    state: {
      spotifyAccessToken: tokens.accessToken,
      activeCharacter: null,
      currentScene: null,
      currentTrack: null,
      messageHistory: [],
      maxHistorySize: 10,
    },
    library: [],
  });
  return sessionId;
}

export function getSession(sessionId: string): StoredSession | undefined {
  return sessions.get(sessionId);
}

export function updateTokens(
  sessionId: string,
  tokens: SpotifyTokens
): void {
  const session = sessions.get(sessionId);
  if (session) {
    session.tokens = tokens;
    session.state.spotifyAccessToken = tokens.accessToken;
  }
}

export function setLibrary(sessionId: string, library: CachedTrack[]): void {
  const session = sessions.get(sessionId);
  if (session) {
    session.library = library;
  }
}

export function getLibrary(sessionId: string): CachedTrack[] {
  return sessions.get(sessionId)?.library ?? [];
}

export function updateState(
  sessionId: string,
  update: Partial<SessionState>
): void {
  const session = sessions.get(sessionId);
  if (session) {
    Object.assign(session.state, update);
  }
}

export function getState(sessionId: string): SessionState | undefined {
  return sessions.get(sessionId)?.state;
}
