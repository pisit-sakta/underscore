import "dotenv/config";

export const config = {
  spotify: {
    clientId: process.env.SPOTIFY_CLIENT_ID ?? "",
    clientSecret: process.env.SPOTIFY_CLIENT_SECRET ?? "",
    redirectUri:
      process.env.SPOTIFY_REDIRECT_URI ?? "http://localhost:3000/callback",
  },
  gemini: {
    apiKey: process.env.GEMINI_API_KEY ?? "",
  },
  port: parseInt(process.env.PORT ?? "3000", 10),
  /** Use mock classifier when no Gemini key is set or forced via env */
  useMockClassifier:
    process.env.USE_MOCK_CLASSIFIER === "true" || !process.env.GEMINI_API_KEY,
  /** Demo mode: scene classification works, Spotify playback is simulated */
  demoMode: !process.env.SPOTIFY_CLIENT_ID,
};

export function validateConfig(): string[] {
  const warnings: string[] = [];

  if (config.demoMode) {
    console.warn(
      "[config] DEMO MODE — no Spotify credentials. Scene classification works, playback is simulated."
    );
    console.warn(
      "[config] Set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET for real playback."
    );
  }

  if (!config.gemini.apiKey) {
    console.warn(
      "[config] GEMINI_API_KEY not set — using mock classifier. Set it for real scene classification."
    );
  }

  return warnings;
}
