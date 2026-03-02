/**
 * Underscore — Entry point
 *
 * "Your stories, scored. Your day, scored."
 */
import express from "express";
import cookieParser from "cookie-parser";
import path from "node:path";
import os from "node:os";
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

const host = process.env.HOST ?? "0.0.0.0";

app.listen(config.port, host, () => {
  const mode = config.demoMode
    ? "DEMO (no Spotify)"
    : "Spotify connected";
  const classifier = config.useMockClassifier
    ? "Mock classifier"
    : "Gemini Flash   ";
  const lanIp = getLanIp();
  const lanUrl = lanIp ? `http://${lanIp}:${config.port}` : "unavailable";

  console.log(`
  ╔═══════════════════════════════════════════╗
  ║         UNDERSCORE v0.2.0                 ║
  ║     "Your stories, scored."               ║
  ╠═══════════════════════════════════════════╣
  ║  Local:      http://localhost:${String(config.port).padEnd(13)}║
  ║  Network:    ${lanUrl.padEnd(28)}║
  ║  Life:       ${(lanUrl + "/life.html").padEnd(28)}║
  ║  Playback:   ${mode.padEnd(28)}║
  ║  Classifier: ${classifier.padEnd(28)}║
  ╚═══════════════════════════════════════════╝

  Open the Network URL on your phone to use Life mode.
  On iOS: Share → Add to Home Screen
  On Android: Menu → Add to Home Screen
  `);
});

/** Find the first non-internal IPv4 address */
function getLanIp(): string | null {
  const nets = os.networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name]!) {
      if (net.family === "IPv4" && !net.internal) {
        return net.address;
      }
    }
  }
  return null;
}
