/**
 * Fetches and caches the user's Spotify library with audio features,
 * plus their taste profile (top artists + top tracks) for AI personalization.
 */
import type { CachedTrack } from "../auth/session.js";

/** Compact taste profile for passing to the AI */
export interface TasteProfile {
  topArtists: string[];
  topTracks: string[];
  topGenres: string[];
}

const SPOTIFY_API = "https://api.spotify.com/v1";

/** Fetch user's saved tracks (up to `limit` tracks) */
export async function fetchUserLibrary(
  token: string,
  limit = 200
): Promise<CachedTrack[]> {
  const tracks: CachedTrack[] = [];
  let offset = 0;
  const pageSize = 50;

  while (offset < limit) {
    const res = await fetch(
      `${SPOTIFY_API}/me/tracks?limit=${pageSize}&offset=${offset}`,
      { headers: { Authorization: `Bearer ${token}` } }
    );

    if (!res.ok) {
      console.warn(
        `[library] Failed to fetch saved tracks at offset ${offset}: ${res.status}`
      );
      break;
    }

    const data = await res.json();
    const items: any[] = data.items ?? [];
    if (items.length === 0) break;

    for (const item of items) {
      const t = item.track;
      if (!t) continue;
      tracks.push({
        id: t.id,
        uri: t.uri,
        name: t.name,
        artist: t.artists?.map((a: any) => a.name).join(", ") ?? "Unknown",
        albumArt: t.album?.images?.[0]?.url ?? null,
        energy: 0,
        valence: 0,
        tempo: 0,
        genres: [],
      });
    }

    if (!data.next) break;
    offset += pageSize;
  }

  // Fetch audio features in batches of 100
  await enrichWithAudioFeatures(token, tracks);

  console.log(`[library] Cached ${tracks.length} tracks for user`);
  return tracks;
}

/**
 * Fetch the user's top artists and tracks from Spotify.
 * This gives us their taste profile for AI-personalized suggestions.
 */
export async function fetchTasteProfile(token: string): Promise<TasteProfile> {
  const profile: TasteProfile = { topArtists: [], topTracks: [], topGenres: [] };

  try {
    // Top artists (medium term = last ~6 months)
    const artistRes = await fetch(
      `${SPOTIFY_API}/me/top/artists?limit=20&time_range=medium_term`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (artistRes.ok) {
      const data = await artistRes.json();
      for (const artist of data.items ?? []) {
        profile.topArtists.push(artist.name);
        for (const genre of artist.genres ?? []) {
          if (!profile.topGenres.includes(genre)) {
            profile.topGenres.push(genre);
          }
        }
      }
    }
  } catch {
    // non-critical
  }

  try {
    // Top tracks (medium term)
    const trackRes = await fetch(
      `${SPOTIFY_API}/me/top/tracks?limit=20&time_range=medium_term`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (trackRes.ok) {
      const data = await trackRes.json();
      for (const track of data.items ?? []) {
        const artists = track.artists?.map((a: any) => a.name).join(", ") ?? "";
        profile.topTracks.push(`${track.name} by ${artists}`);
      }
    }
  } catch {
    // non-critical
  }

  // Keep genres manageable
  profile.topGenres = profile.topGenres.slice(0, 15);

  console.log(
    `[library] Taste profile: ${profile.topArtists.length} artists, ` +
    `${profile.topTracks.length} tracks, ${profile.topGenres.length} genres`
  );

  return profile;
}

/** Enrich tracks with Spotify audio features (energy, valence, tempo) */
async function enrichWithAudioFeatures(
  token: string,
  tracks: CachedTrack[]
): Promise<void> {
  const batchSize = 100;

  for (let i = 0; i < tracks.length; i += batchSize) {
    const batch = tracks.slice(i, i + batchSize);
    const ids = batch.map((t) => t.id).join(",");

    const res = await fetch(`${SPOTIFY_API}/audio-features?ids=${ids}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) {
      // Audio features endpoint may be restricted for newer Spotify apps.
      // Fall back gracefully — the matcher will use defaults.
      console.warn(
        `[library] Audio features unavailable (${res.status}). ` +
          "Song matching will use heuristics instead."
      );
      return;
    }

    const data = await res.json();
    const features: any[] = data.audio_features ?? [];

    for (const feat of features) {
      if (!feat) continue;
      const track = tracks.find((t) => t.id === feat.id);
      if (track) {
        track.energy = feat.energy ?? 0;
        track.valence = feat.valence ?? 0;
        track.tempo = feat.tempo ?? 0;
      }
    }
  }
}
