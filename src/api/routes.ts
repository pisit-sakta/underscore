/**
 * API routes for Underscore RP + Life.
 */
import { Router, type Request, type Response } from "express";
import crypto from "node:crypto";
import { config, setGeminiKey } from "../core/config.js";
import { resetGeminiClient } from "../core/scene-classifier.js";
import { resetLifeAiClient } from "../life/life-ai-classifier.js";
import {
  getAuthUrl,
  exchangeCode,
  getValidToken,
} from "../auth/spotify-auth.js";
import {
  createSession,
  getSession,
  updateTokens,
  setLibrary,
  getLibrary,
  setTasteProfile,
  getTasteProfile,
} from "../auth/session.js";
import { fetchUserLibrary, fetchTasteProfile } from "../core/library-cache.js";
import { processText } from "../core/narrative-engine.js";
import {
  getCurrentlyPlaying,
  getDevices,
} from "../core/playback-controller.js";
import { processLifeInput } from "../life/life-engine.js";
import { LIFE_PRESETS } from "../life/presets.js";

export const router = Router();

// ============================================
// Auth routes
// ============================================

/** Start Spotify OAuth flow */
router.get("/auth/login", (req: Request, res: Response) => {
  if (config.demoMode) {
    res.redirect("/?error=demo_mode");
    return;
  }
  const state = crypto.randomUUID();
  const origin = getOrigin(req);
  const url = getAuthUrl(state, origin);
  res.redirect(url);
});

/** Spotify OAuth callback */
router.get("/callback", async (req: Request, res: Response) => {
  const code = req.query.code as string | undefined;
  const error = req.query.error as string | undefined;

  if (error || !code) {
    res.redirect(`/?error=${encodeURIComponent(error ?? "no_code")}`);
    return;
  }

  try {
    const origin = getOrigin(req);
    const tokens = await exchangeCode(code, origin);
    const sessionId = createSession(tokens);

    res.cookie("underscore_session", sessionId, {
      httpOnly: true,
      maxAge: 24 * 60 * 60 * 1000,
    });

    // Fetch library + taste profile in background
    fetchUserLibrary(tokens.accessToken)
      .then((library) => setLibrary(sessionId, library))
      .catch((err) =>
        console.error("[auth] Background library fetch failed:", err)
      );
    fetchTasteProfile(tokens.accessToken)
      .then((profile) => setTasteProfile(sessionId, profile))
      .catch((err) =>
        console.error("[auth] Background taste profile fetch failed:", err)
      );

    res.redirect("/?connected=true");
  } catch (err) {
    console.error("[auth] Token exchange failed:", err);
    res.redirect("/?error=token_exchange_failed");
  }
});

/** Check auth status — also reports demo mode */
router.get("/auth/status", (req: Request, res: Response) => {
  if (config.demoMode) {
    res.json({
      connected: false,
      demoMode: true,
      classifier: config.useMockClassifier ? "mock" : "gemini",
      hasGeminiKey: !!config.gemini.apiKey,
    });
    return;
  }

  const sessionId = getSessionId(req);
  if (!sessionId) {
    res.json({ connected: false, demoMode: false });
    return;
  }

  const session = getSession(sessionId);
  if (!session) {
    res.json({ connected: false, demoMode: false });
    return;
  }

  const libraryCount = getLibrary(sessionId).length;
  res.json({
    connected: true,
    demoMode: false,
    libraryLoaded: libraryCount > 0,
    trackCount: libraryCount,
  });
});

// ============================================
// Core API routes
// ============================================

/**
 * Submit text for scene classification and playback.
 *
 * In demo mode: works without Spotify — returns classification + simulated track.
 * With Spotify: full pipeline including real playback.
 */
router.post("/api/scene", async (req: Request, res: Response) => {
  const { text } = req.body;
  if (!text || typeof text !== "string") {
    res.status(400).json({ error: "Missing 'text' field in request body." });
    return;
  }

  // === Demo mode: no Spotify needed ===
  if (config.demoMode) {
    try {
      const sessionId = "demo";
      const result = await processText(sessionId, text, "", []);
      res.json({ ...result, demoMode: true });
    } catch (err) {
      console.error("[api] Demo scene processing failed:", err);
      res.status(500).json({ error: "Scene processing failed" });
    }
    return;
  }

  // === Full mode: Spotify required ===
  const sessionId = getSessionId(req);
  if (!sessionId) {
    res
      .status(401)
      .json({ error: "Not authenticated. Connect Spotify first." });
    return;
  }

  const session = getSession(sessionId);
  if (!session) {
    res
      .status(401)
      .json({ error: "Session expired. Please reconnect Spotify." });
    return;
  }

  try {
    const validTokens = await getValidToken(session.tokens);
    if (validTokens !== session.tokens) {
      updateTokens(sessionId, validTokens);
    }

    const library = getLibrary(sessionId);
    const result = await processText(
      sessionId,
      text,
      validTokens.accessToken,
      library
    );

    res.json({ ...result, demoMode: false });
  } catch (err) {
    console.error("[api] Scene processing failed:", err);
    res.status(500).json({ error: "Scene processing failed" });
  }
});

/** Get current playback state */
router.get("/api/now-playing", async (req: Request, res: Response) => {
  if (config.demoMode) {
    res.json({ playing: false, demoMode: true });
    return;
  }

  const sessionId = getSessionId(req);
  if (!sessionId) {
    res.json({ playing: false });
    return;
  }

  const session = getSession(sessionId);
  if (!session) {
    res.json({ playing: false });
    return;
  }

  try {
    const validTokens = await getValidToken(session.tokens);
    if (validTokens !== session.tokens) {
      updateTokens(sessionId, validTokens);
    }

    const current = await getCurrentlyPlaying(validTokens.accessToken);
    res.json({
      playing: current?.isPlaying ?? false,
      track: current
        ? { name: current.trackName, artist: current.artistName }
        : null,
    });
  } catch (err) {
    console.error("[api] Now-playing check failed:", err);
    res.json({ playing: false });
  }
});

/** Get available Spotify devices */
router.get("/api/devices", async (req: Request, res: Response) => {
  if (config.demoMode) {
    res.json({ devices: [], demoMode: true });
    return;
  }

  const sessionId = getSessionId(req);
  if (!sessionId) {
    res.status(401).json({ error: "Not authenticated" });
    return;
  }

  const session = getSession(sessionId);
  if (!session) {
    res.status(401).json({ error: "Session expired" });
    return;
  }

  try {
    const validTokens = await getValidToken(session.tokens);
    const devices = await getDevices(validTokens.accessToken);
    res.json({ devices });
  } catch (err) {
    console.error("[api] Device list failed:", err);
    res.json({ devices: [] });
  }
});

// ============================================
// Life mode routes
// ============================================

/** Get available presets */
router.get("/api/life/presets", (_req: Request, res: Response) => {
  res.json({ presets: LIFE_PRESETS });
});

/**
 * Score a life context and pick a track.
 *
 * Body: { activity, mood?, energy?, timeOfDay?, freeText? }
 */
router.post("/api/life/score", async (req: Request, res: Response) => {
  const { activity } = req.body;
  if (!activity || typeof activity !== "string") {
    res.status(400).json({ error: "Missing 'activity' field in request body." });
    return;
  }

  // === Demo mode ===
  if (config.demoMode) {
    try {
      const result = await processLifeInput("demo-life", req.body, "", []);
      res.json({ ...result, demoMode: true });
    } catch (err) {
      console.error("[api] Life demo scoring failed:", err);
      res.status(500).json({ error: "Life scoring failed" });
    }
    return;
  }

  // === Full mode ===
  const sessionId = getSessionId(req);
  if (!sessionId) {
    res.status(401).json({ error: "Not authenticated. Connect Spotify first." });
    return;
  }

  const session = getSession(sessionId);
  if (!session) {
    res.status(401).json({ error: "Session expired. Please reconnect Spotify." });
    return;
  }

  try {
    const validTokens = await getValidToken(session.tokens);
    if (validTokens !== session.tokens) {
      updateTokens(sessionId, validTokens);
    }

    const library = getLibrary(sessionId);
    const tasteProfile = getTasteProfile(sessionId);
    const result = await processLifeInput(
      sessionId,
      req.body,
      validTokens.accessToken,
      library,
      tasteProfile,
    );

    res.json({ ...result, demoMode: false });
  } catch (err) {
    console.error("[api] Life scoring failed:", err);
    res.status(500).json({ error: "Life scoring failed" });
  }
});

// ============================================
// Settings
// ============================================

/** Get current settings (never exposes the full key) */
router.get("/api/settings", (_req: Request, res: Response) => {
  const key = config.gemini.apiKey;
  res.json({
    hasGeminiKey: !!key,
    geminiKeyHint: key ? `${key.slice(0, 4)}...${key.slice(-4)}` : null,
    classifier: config.useMockClassifier ? "mock" : "gemini",
    demoMode: config.demoMode,
  });
});

/** Save Gemini API key at runtime */
router.post("/api/settings", (req: Request, res: Response) => {
  const { geminiApiKey } = req.body;
  if (typeof geminiApiKey !== "string") {
    res.status(400).json({ error: "Missing geminiApiKey" });
    return;
  }

  const key = geminiApiKey.trim();
  setGeminiKey(key);
  resetGeminiClient();
  resetLifeAiClient();

  res.json({
    ok: true,
    hasGeminiKey: !!key,
    classifier: config.useMockClassifier ? "mock" : "gemini",
  });
});

// ============================================
// Helpers
// ============================================

function getSessionId(req: Request): string | null {
  const fromCookie = req.cookies?.underscore_session;
  if (fromCookie) return fromCookie;

  const fromHeader = req.headers["x-session-id"];
  if (typeof fromHeader === "string") return fromHeader;

  return null;
}

/**
 * Derive the origin (protocol + host) from the request.
 * Works through tunnels and reverse proxies via X-Forwarded headers.
 * Returns undefined when running on plain localhost (uses configured default).
 */
function getOrigin(req: Request): string | undefined {
  const host =
    (req.headers["x-forwarded-host"] as string) ?? req.headers.host;
  if (!host) return undefined;

  // If it's localhost, no need to override — the config default works
  if (host.startsWith("localhost") || host.startsWith("127.0.0.1")) {
    return undefined;
  }

  const proto =
    (req.headers["x-forwarded-proto"] as string) ?? "https";
  return `${proto}://${host}`;
}
