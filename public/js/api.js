/**
 * Underscore RP — API Client
 *
 * Thin wrapper around fetch calls to the Express backend.
 */

export async function fetchStatus() {
  const res = await fetch("/auth/status");
  return res.json();
}

export async function scoreScene(text) {
  const res = await fetch("/api/scene", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text }),
  });

  if (res.status === 401) {
    throw new Error("Connect Spotify first.");
  }

  const data = await res.json();
  if (data.error) {
    throw new Error(data.error);
  }

  return data;
}

export async function fetchNowPlaying() {
  const res = await fetch("/api/now-playing");
  return res.json();
}

export async function fetchDevices() {
  const res = await fetch("/api/devices");
  return res.json();
}
