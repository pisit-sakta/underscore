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

const errors = validateConfig();
if (errors.length > 0) {
  console.error("Configuration errors:");
  errors.forEach((e) => console.error(`  - ${e}`));
  console.error("\nCopy .env.example to .env and fill in your API keys.");
  process.exit(1);
}

const app = express();

app.use(express.json());
app.use(cookieParser());
app.use(express.static(path.join(__dirname, "..", "public")));
app.use(router);

app.listen(config.port, () => {
  console.log(`
  ╔═══════════════════════════════════════╗
  ║         UNDERSCORE RP v0.1.0          ║
  ║       "Your stories, scored."         ║
  ╠═══════════════════════════════════════╣
  ║  Server:  http://localhost:${config.port}        ║
  ║  Mode:    ${config.useMockClassifier ? "Mock classifier" : "Gemini 3 Flash "}       ║
  ╚═══════════════════════════════════════╝
  `);
});
