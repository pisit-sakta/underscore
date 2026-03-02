/**
 * API routes for Underscore RP.
 */
import { Router, type Request, type Response } from "express";
import crypto from "node:crypto";
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
} from "../auth/session.js";
import { fetchUserLibrary } from "../core/library-cache.js";
import { processText } from "../core/narrative-engine.js";
import {
  getCurrentlyPlaying,
  getDevices,
} from "../core/playback-controller.js";

export const router = Router();

// ============================================
// Auth routes
// ============================================

/** Start Spotify OAuth flow */
router.get("/auth/login", (_req: Request, res: Response) => {
  const state = crypto.randomUUID();
  const url = getAuthUrl(state);
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
    const tokens = await exchangeCode(code);
    const sessionId = createSession(tokens);

    // Set session cookie
    res.cookie("underscore_session", sessionId, {
      httpOnly: true,
      maxAge: 24 * 60 * 60 * 1000, // 24 hours
    });

    // Fetch library in background
    fetchUserLibrary(tokens.accessToken).then((library) => {
      setLibrary(sessionId, library);
    });

    res.redirect("/?connected=true");
  } catch (err) {
    console.error("[auth] Token exchange failed:", err);
    res.redirect("/?error=token_exchange_failed");
  }
});

/** Check auth status */
router.get("/auth/status", async (req: Request, res: Response) => {
  const sessionId = getSessionId(req);
  if (!sessionId) {
    res.json({ connected: false });
    return;
  }

  const session = getSession(sessionId);
  if (!session) {
    res.json({ connected: false });
    return;
  }

  const libraryCount = getLibrary(sessionId).length;
  res.json({
    connected: true,
    libraryLoaded: libraryCount > 0,
    trackCount: libraryCount,
  });
});

// ============================================
// Core API routes
// ============================================

/** Submit text for scene classification and playback */
router.post("/api/scene", async (req: Request, res: Response) => {
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

  const { text } = req.body;
  if (!text || typeof text !== "string") {
    res.status(400).json({ error: "Missing 'text' field in request body." });
    return;
  }

  try {
    // Ensure valid token
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

    res.json(result);
  } catch (err) {
    console.error("[api] Scene processing failed:", err);
    res.status(500).json({ error: "Scene processing failed" });
  }
});

/** Get current playback state */
router.get("/api/now-playing", async (req: Request, res: Response) => {
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
  } catch {
    res.json({ playing: false });
  }
});

/** Get available Spotify devices */
router.get("/api/devices", async (req: Request, res: Response) => {
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
  } catch {
    res.json({ devices: [] });
  }
});

// ============================================
// Helpers
// ============================================

function getSessionId(req: Request): string | null {
  // Try cookie first, then header
  const fromCookie = req.cookies?.underscore_session;
  if (fromCookie) return fromCookie;

  const fromHeader = req.headers["x-session-id"];
  if (typeof fromHeader === "string") return fromHeader;

  return null;
}
