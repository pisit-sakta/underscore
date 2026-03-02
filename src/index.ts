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
    const lifeUrl = `${url}/life.html`;

    console.log(`\n  TUNNEL ACTIVE — scan this QR code with your phone:\n`);

    // Print QR code pointing straight to Life mode
    try {
      const qrMod = await import("qrcode-terminal");
      const qr = qrMod.default ?? qrMod;
      qr.generate(lifeUrl, { small: true }, (code: string) => {
        // Indent each line for alignment
        for (const line of code.split("\n")) {
          console.log(`    ${line}`);
        }
        printTunnelInfo(url, lifeUrl);
      });
    } catch {
      // Fallback if qrcode-terminal isn't available
      printTunnelInfo(url, lifeUrl);
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

function printTunnelInfo(url: string, lifeUrl: string): void {
  console.log(``);
  console.log(`  ${lifeUrl}`);
  console.log(``);
  console.log(`  1. Scan the QR code (or open the URL)`);
  console.log(`  2. Tap "Click to Continue" on the first visit`);
  console.log(`  3. Tap a preset — that's it!`);
  console.log(``);
  console.log(`  Tip: Add to Home Screen for a native app feel.`);
  if (!config.demoMode) {
    console.log(``);
    console.log(`  Spotify: add ${url}/callback`);
    console.log(`  to your app's Redirect URIs in the Spotify Dashboard.`);
  }
  console.log(``);
}
