/**
 * Spotify Web API playback control.
 *
 * Underscore NEVER touches audio files. We are a remote control.
 * A very smart remote control.
 */
import type { TransitionStyle, TrackMatch } from "./types.js";

const SPOTIFY_API = "https://api.spotify.com/v1";

interface SpotifyDevice {
  id: string;
  name: string;
  type: string;
  is_active: boolean;
}

async function spotifyFetch(
  path: string,
  token: string,
  options: RequestInit = {}
): Promise<Response> {
  const res = await fetch(`${SPOTIFY_API}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...options.headers,
    },
  });
  return res;
}

/** Get available Spotify devices */
export async function getDevices(token: string): Promise<SpotifyDevice[]> {
  const res = await spotifyFetch("/me/player/devices", token);
  if (!res.ok) return [];
  const data = await res.json();
  return data.devices ?? [];
}

/** Get the currently playing track */
export async function getCurrentlyPlaying(
  token: string
): Promise<{ trackUri: string; trackName: string; artistName: string; isPlaying: boolean } | null> {
  const res = await spotifyFetch("/me/player/currently-playing", token);
  if (res.status === 204 || !res.ok) return null;
  const data = await res.json();
  if (!data.item) return null;
  return {
    trackUri: data.item.uri,
    trackName: data.item.name,
    artistName: data.item.artists?.map((a: any) => a.name).join(", ") ?? "",
    isPlaying: data.is_playing,
  };
}

/** Play a specific track */
export async function playTrack(
  token: string,
  trackUri: string,
  deviceId?: string
): Promise<boolean> {
  const params = deviceId ? `?device_id=${deviceId}` : "";
  const res = await spotifyFetch(`/me/player/play${params}`, token, {
    method: "PUT",
    body: JSON.stringify({ uris: [trackUri] }),
  });
  if (res.status === 403) {
    console.warn("[playback] Spotify Premium required for playback control");
    return false;
  }
  return res.ok || res.status === 204;
}

/** Set playback volume (0-100) */
export async function setVolume(
  token: string,
  volumePercent: number
): Promise<void> {
  await spotifyFetch(
    `/me/player/volume?volume_percent=${Math.round(volumePercent)}`,
    token,
    { method: "PUT" }
  );
}

/** Search Spotify for a track by query */
export async function searchTrack(
  token: string,
  query: string
): Promise<TrackMatch | null> {
  const params = new URLSearchParams({ q: query, type: "track", limit: "1" });
  const res = await spotifyFetch(`/search?${params}`, token);
  if (!res.ok) return null;
  const data = await res.json();
  const track = data.tracks?.items?.[0];
  if (!track) return null;
  return {
    spotifyUri: track.uri,
    trackName: track.name,
    artistName: track.artists?.map((a: any) => a.name).join(", ") ?? "",
    matchReason: `Search result for: ${query}`,
    energy: 0.5,
    valence: 0.5,
    tempo: 120,
  };
}

/**
 * Execute a playback command with the specified transition.
 *
 * MVP transitions:
 * - hard_cut: immediately play the new track
 * - crossfade: fade volume down, switch track, fade back up
 * - fade_out_fade_in: fade out, brief silence, fade in
 */
export async function executePlayback(
  token: string,
  track: TrackMatch,
  transition: TransitionStyle,
  deviceId?: string
): Promise<boolean> {
  switch (transition.type) {
    case "hard_cut":
      return playTrack(token, track.spotifyUri, deviceId);

    case "crossfade": {
      // Poor man's crossfade via volume ramping
      const steps = 5;
      const stepTime = transition.durationMs / steps / 2;

      // Fade out
      for (let i = steps; i >= 0; i--) {
        await setVolume(token, (i / steps) * 100);
        if (i > 0) await sleep(stepTime);
      }

      // Switch track
      await playTrack(token, track.spotifyUri, deviceId);

      // Fade in
      for (let i = 0; i <= steps; i++) {
        await setVolume(token, (i / steps) * 100);
        if (i < steps) await sleep(stepTime);
      }
      return true;
    }

    case "fade_out_fade_in": {
      // Fade out
      for (let i = 5; i >= 0; i--) {
        await setVolume(token, (i / 5) * 100);
        await sleep(100);
      }

      // Gap
      await sleep(transition.gapMs);

      // Play new track
      await playTrack(token, track.spotifyUri, deviceId);

      // Fade in
      for (let i = 0; i <= 5; i++) {
        await setVolume(token, (i / 5) * 100);
        await sleep(100);
      }
      return true;
    }
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
