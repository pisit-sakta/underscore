/**
 * Underscore — Entry point
 *
 * "Your stories, scored. Your day, scored."
 */
import express from "express";
import cookieParser from "cookie-parser";
import crypto from "node:crypto";
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

// ── Tunnel PIN protection ──
// When running in tunnel mode, generate a 4-digit PIN that the user
// must enter once on their phone. This prevents random strangers from
// using the public tunnel URL. Localhost access bypasses the PIN.
const tunnelPin = process.env.TUNNEL === "true"
  ? (process.env.TUNNEL_PIN || String(crypto.randomInt(1000, 9999)))
  : null;

if (tunnelPin) {
  // Single middleware handles both PIN verification and gating.
  // This avoids Express 5 route/middleware ordering issues.
  app.use((req, res, next) => {
    // Direct localhost connections (no proxy) bypass PIN.
    // Localtunnel always sets x-forwarded-for, so its presence
    // means the request came through the tunnel (public internet).
    if (!req.headers["x-forwarded-for"]) {
      return next();
    }

    // Handle PIN submission
    if (req.method === "POST" && req.path === "/api/pin") {
      const pin = String(req.body?.pin ?? "");
      if (pin === tunnelPin) {
        res.cookie("underscore_pin", tunnelPin, {
          httpOnly: true,
          maxAge: 7 * 24 * 60 * 60 * 1000, // 7 days
          sameSite: "lax",
        });
        return res.json({ ok: true });
      }
      return res.status(403).json({ ok: false, error: "Wrong code" });
    }

    // Already verified via cookie
    if (req.cookies?.underscore_pin === tunnelPin) {
      return next();
    }

    // Serve the PIN entry page for any non-API request
    if (!req.path.startsWith("/api/")) {
      return res.send(pinPageHtml());
    }

    // Block API calls without PIN
    res.status(403).json({ error: "PIN required" });
  });
}

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
  if (tunnelPin) {
  console.log(`  │  4. Enter the code: ${tunnelPin}                  │`);
  console.log(`  │  5. Tap any activity — music plays!         │`);
  } else {
  console.log(`  │  4. Tap any activity — music plays!         │`);
  }
  console.log(`  │                                             │`);
  console.log(`  │  Keep this window open while using the app. │`);
  console.log(`  │  Close it when you're done.                 │`);
  console.log(`  └─────────────────────────────────────────────┘`);
  if (tunnelPin) {
    console.log(`\n  CODE: ${tunnelPin}  (enter this on your phone)`);
  }
  if (lanUrl) {
    console.log(`\n  Same WiFi? You can also open: ${lanUrl}/life.html`);
  }
  console.log(``);
}

/** Inline HTML page for PIN entry — no external dependencies needed */
function pinPageHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover, user-scalable=no">
  <title>Underscore Life</title>
  <meta name="theme-color" content="#07070c">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      background: linear-gradient(145deg, #07070c 0%, #0d0d18 50%, #0a0a14 100%);
      color: #e4e4ee;
      min-height: 100vh;
      min-height: 100dvh;
      display: flex;
      align-items: center;
      justify-content: center;
      -webkit-font-smoothing: antialiased;
    }
    .pin-box {
      text-align: center;
      padding: 32px;
      max-width: 320px;
      width: 100%;
    }
    .logo {
      font-size: 32px;
      font-weight: 800;
      letter-spacing: 4px;
      margin-bottom: 8px;
      user-select: none;
    }
    .logo span { color: #7c5cff; font-size: 38px; }
    .subtitle {
      font-size: 14px;
      color: #666680;
      margin-bottom: 32px;
    }
    .label {
      font-size: 13px;
      color: #a0a0b8;
      margin-bottom: 12px;
    }
    .pin-input {
      width: 100%;
      text-align: center;
      font-size: 32px;
      font-weight: 700;
      letter-spacing: 12px;
      padding: 16px;
      background: #111119;
      border: 2px solid #252540;
      border-radius: 14px;
      color: #e4e4ee;
      outline: none;
      font-family: 'SF Mono', 'JetBrains Mono', monospace;
      transition: border-color 150ms ease;
      -webkit-appearance: none;
    }
    .pin-input:focus { border-color: #7c5cff; }
    .pin-input.error {
      border-color: #f87171;
      animation: shake 0.4s ease;
    }
    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      25% { transform: translateX(-8px); }
      75% { transform: translateX(8px); }
    }
    .submit {
      width: 100%;
      margin-top: 16px;
      padding: 16px;
      background: #7c5cff;
      color: white;
      border: none;
      border-radius: 14px;
      font-size: 16px;
      font-weight: 700;
      font-family: inherit;
      cursor: pointer;
      -webkit-tap-highlight-color: transparent;
    }
    .submit:active { transform: scale(0.97); }
    .submit:disabled { opacity: 0.5; }
    .hint {
      font-size: 12px;
      color: #666680;
      margin-top: 20px;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <div class="pin-box">
    <div class="logo"><span>_</span>LIFE</div>
    <div class="subtitle">Your day, scored.</div>
    <div class="label">Enter the 4-digit code shown on the computer</div>
    <input class="pin-input" id="pin" type="tel" maxlength="4"
           inputmode="numeric" pattern="[0-9]*" autocomplete="off" autofocus>
    <button class="submit" id="go">Continue</button>
    <div class="hint">The code is displayed in the terminal window<br>where Underscore is running.</div>
  </div>
  <script>
    const input = document.getElementById("pin");
    const btn = document.getElementById("go");

    async function submit() {
      const pin = input.value.trim();
      if (pin.length !== 4) return;
      btn.disabled = true;
      try {
        const res = await fetch("/api/pin", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ pin }),
        });
        if (res.ok) {
          window.location.reload();
        } else {
          input.classList.add("error");
          input.value = "";
          input.focus();
          setTimeout(() => input.classList.remove("error"), 500);
        }
      } catch {
        alert("Could not connect. Is the app still running?");
      } finally {
        btn.disabled = false;
      }
    }

    btn.addEventListener("click", submit);
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter") submit();
    });
    // Auto-submit when 4 digits entered
    input.addEventListener("input", () => {
      if (input.value.length === 4) submit();
    });
  </script>
</body>
</html>`;
}
