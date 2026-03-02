/**
 * Spotify OAuth 2.0 Authorization Code flow.
 *
 * We use raw fetch instead of the Spotify SDK for auth because
 * the SDK's auth helpers are designed for browser PKCE flow,
 * while our server needs the Authorization Code flow with a client secret.
 */
import { config } from "../core/config.js";

const SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize";
const SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";

/** Scopes needed for Underscore */
const SCOPES = [
  "user-read-private",
  "user-read-email",
  "user-library-read",
  "user-read-playback-state",
  "user-modify-playback-state",
  "user-read-currently-playing",
  "streaming",
].join(" ");

export interface SpotifyTokens {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

/** Generate the Spotify authorization URL */
export function getAuthUrl(state: string): string {
  const params = new URLSearchParams({
    response_type: "code",
    client_id: config.spotify.clientId,
    scope: SCOPES,
    redirect_uri: config.spotify.redirectUri,
    state,
  });
  return `${SPOTIFY_AUTH_URL}?${params}`;
}

/** Exchange an authorization code for tokens */
export async function exchangeCode(code: string): Promise<SpotifyTokens> {
  const res = await fetch(SPOTIFY_TOKEN_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: `Basic ${Buffer.from(
        `${config.spotify.clientId}:${config.spotify.clientSecret}`
      ).toString("base64")}`,
    },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri: config.spotify.redirectUri,
    }),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`Spotify token exchange failed: ${res.status} ${body}`);
  }

  const data = await res.json();
  return {
    accessToken: data.access_token,
    refreshToken: data.refresh_token,
    expiresAt: Date.now() + data.expires_in * 1000,
  };
}

/** Refresh an expired access token */
export async function refreshTokens(
  refreshToken: string
): Promise<SpotifyTokens> {
  const res = await fetch(SPOTIFY_TOKEN_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: `Basic ${Buffer.from(
        `${config.spotify.clientId}:${config.spotify.clientSecret}`
      ).toString("base64")}`,
    },
    body: new URLSearchParams({
      grant_type: "refresh_token",
      refresh_token: refreshToken,
    }),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`Spotify token refresh failed: ${res.status} ${body}`);
  }

  const data = await res.json();
  return {
    accessToken: data.access_token,
    refreshToken: data.refresh_token ?? refreshToken,
    expiresAt: Date.now() + data.expires_in * 1000,
  };
}

/** Get a valid access token, refreshing if needed */
export async function getValidToken(
  tokens: SpotifyTokens
): Promise<SpotifyTokens> {
  if (Date.now() < tokens.expiresAt - 60_000) {
    return tokens;
  }
  return refreshTokens(tokens.refreshToken);
}
