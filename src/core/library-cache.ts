/**
 * Fetches and caches the user's Spotify library with audio features.
 * On auth, we grab their saved tracks so the song matcher has material to work with.
 */
import type { CachedTrack } from "../auth/session.js";

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
