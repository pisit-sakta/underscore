/**
 * Underscore RP — Entry point
 *
 * "Your stories, scored."
 */
import express from "express";
import cookieParser from "cookie-parser";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { config, validateConfig } from "./core/config.js";
import { router } from "./api/routes.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

validateConfig();

const app = express();

app.use(express.json());
app.use(cookieParser());
app.use(express.static(path.join(__dirname, "..", "public")));
app.use(router);

app.listen(config.port, () => {
  const mode = config.demoMode
    ? "DEMO (no Spotify)"
    : "Spotify connected";
  const classifier = config.useMockClassifier
    ? "Mock classifier"
    : "Gemini Flash   ";

  console.log(`
  ╔═══════════════════════════════════════════╗
  ║           UNDERSCORE RP v0.1.0            ║
  ║         "Your stories, scored."           ║
  ╠═══════════════════════════════════════════╣
  ║  Server:     http://localhost:${String(config.port).padEnd(13)}║
  ║  Playback:   ${mode.padEnd(28)}║
  ║  Classifier: ${classifier.padEnd(28)}║
  ╚═══════════════════════════════════════════╝
  `);
});
