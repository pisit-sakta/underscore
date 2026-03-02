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

app.listen(config.port, host, async () => {
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
  ║  Playback:   ${mode.padEnd(28)}║
  ║  Classifier: ${classifier.padEnd(28)}║
  ╚═══════════════════════════════════════════╝
  `);

  // Start tunnel if requested
  if (process.env.TUNNEL === "true") {
    await startTunnel();
  } else {
    console.log(`  WiFi:     Open the Network URL on your phone`);
    console.log(`  Cellular: TUNNEL=true npm run dev`);
    console.log(``);
  }
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

/**
 * Start a localtunnel to expose the server publicly.
 * Gives you an https URL that works on cellular.
 */
async function startTunnel(): Promise<void> {
  try {
    // Dynamic import — localtunnel is only needed when tunneling
    const localtunnel = (await import("localtunnel")).default;

    const subdomain = process.env.TUNNEL_SUBDOMAIN ?? undefined;
    const tunnel = await localtunnel({
      port: config.port,
      subdomain,
    });

    const url = tunnel.url;

    console.log(`  ┌─────────────────────────────────────────┐`);
    console.log(`  │  TUNNEL ACTIVE                          │`);
    console.log(`  │                                         │`);
    console.log(`  │  Public:  ${url.padEnd(29)}│`);
    console.log(`  │  Life:    ${(url + "/life.html").padEnd(29)}│`);
    console.log(`  │                                         │`);
    console.log(`  │  Open on your phone — works on cellular │`);
    console.log(`  │  Add to Home Screen for app experience  │`);
    console.log(`  └─────────────────────────────────────────┘`);

    if (!config.demoMode) {
      console.log(`
  IMPORTANT: Add this to your Spotify app's Redirect URIs:
    ${url}/callback
  (Spotify Dashboard → Your App → Settings → Redirect URIs)
      `);
    }

    tunnel.on("close", () => {
      console.log("[tunnel] Closed. Restart to reconnect.");
    });

    tunnel.on("error", (err: Error) => {
      console.error("[tunnel] Error:", err.message);
    });
  } catch (err) {
    console.error("[tunnel] Failed to start:", err);
    console.log("  Falling back to LAN-only mode.");
    console.log("  Install localtunnel: npm install localtunnel");
  }
}
