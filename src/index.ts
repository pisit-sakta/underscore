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

// In tunnel mode, redirect root to Life page so non-technical users
// land on Life instead of the RP page.
if (process.env.TUNNEL === "true") {
  app.use((req, res, next) => {
    if (req.path === "/") return res.redirect("/life.html");
    next();
  });
}
app.use(express.static(path.join(__dirname, "..", "public")));
app.use(router);

const host = process.env.HOST ?? "0.0.0.0";

app.listen(config.port, host, async () => {
  const lanIp = getLanIp();
  const lanUrl = lanIp ? `http://${lanIp}:${config.port}` : null;

  // Start tunnel if requested — user-friendly output
  if (process.env.TUNNEL === "true") {
    console.log(`\n  Underscore Life is starting...\n`);
    await startTunnel(lanUrl);
  } else {
    // Developer-friendly output for npm run dev
    const mode = config.demoMode ? "demo" : "spotify";
    const ai = config.useMockClassifier ? "mock" : "gemini";
    console.log(`\n  _LIFE running  http://localhost:${config.port}  [${mode}] [${ai}]`);
    if (lanUrl) {
      console.log(`  Phone (WiFi):  ${lanUrl}/life.html`);
    }
    console.log(`  Phone (cell):  npm run tunnel\n`);
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
async function startTunnel(lanUrl: string | null): Promise<void> {
  try {
    const localtunnel = (await import("localtunnel")).default;

    const subdomain = process.env.TUNNEL_SUBDOMAIN ?? undefined;
    const tunnel = await localtunnel({
      port: config.port,
      subdomain,
    });

    const url = tunnel.url;
    const lifeUrl = `${url}/life.html`;

    console.log(`  Ready! Scan this QR code with your phone camera:\n`);

    // Print QR code pointing straight to Life mode
    try {
      const qrMod = await import("qrcode-terminal");
      const qr = qrMod.default ?? qrMod;
      qr.generate(lifeUrl, { small: true }, (code: string) => {
        for (const line of code.split("\n")) {
          console.log(`    ${line}`);
        }
        printTunnelInfo(lifeUrl, lanUrl);
      });
    } catch {
      printTunnelInfo(lifeUrl, lanUrl);
    }

    tunnel.on("close", () => {
      console.log("\n  Connection lost. Close this window and double-click Start again.");
    });

    tunnel.on("error", (err: Error) => {
      console.error("[tunnel] Error:", err.message);
    });
  } catch (err) {
    // Tunnel failed — fall back to LAN instructions
    console.error("  Could not create a public link.", err);
    if (lanUrl) {
      console.log(`\n  But it still works over WiFi!`);
      console.log(`  On your phone, open: ${lanUrl}/life.html\n`);
    }
  }
}

function printTunnelInfo(lifeUrl: string, lanUrl: string | null): void {
  console.log(``);
  console.log(`  Or type this URL on your phone:`);
  console.log(`  ${lifeUrl}`);
  console.log(``);
  console.log(`  ┌─────────────────────────────────────────────┐`);
  console.log(`  │  HOW TO USE:                                │`);
  console.log(`  │                                             │`);
  console.log(`  │  1. Point your phone camera at the QR code  │`);
  console.log(`  │  2. Tap the link that appears               │`);
  console.log(`  │  3. Tap "Click to Continue" (first time)    │`);
  console.log(`  │  4. Tap any activity — music plays!         │`);
  console.log(`  │                                             │`);
  console.log(`  │  Keep this window open while using the app. │`);
  console.log(`  │  Close it when you're done.                 │`);
  console.log(`  └─────────────────────────────────────────────┘`);
  if (lanUrl) {
    console.log(`\n  Same WiFi? You can also open: ${lanUrl}/life.html`);
  }
  console.log(``);
}
