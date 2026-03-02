#!/usr/bin/env bash
# ─────────────────────────────────────────────
#   Underscore Life — Start
#
#   Double-click this file (or run: bash start.sh)
#   It handles everything automatically.
# ─────────────────────────────────────────────

set -e

cd "$(dirname "$0")"

echo ""
echo "  Starting Underscore Life..."
echo ""

# ── Check for Node.js ──
if ! command -v node &> /dev/null; then
  echo "  Node.js is not installed."
  echo ""
  echo "  To install it:"
  echo "    Mac:     Visit https://nodejs.org and download the installer"
  echo "    Linux:   sudo apt install nodejs npm"
  echo ""
  echo "  After installing, double-click this file again."
  echo ""
  read -rp "  Press Enter to close..."
  exit 1
fi

NODE_VERSION=$(node -v | cut -d'.' -f1 | tr -d 'v')
if [ "$NODE_VERSION" -lt 18 ]; then
  echo "  Node.js is too old (need v18+, you have $(node -v))."
  echo "  Download the latest from https://nodejs.org"
  echo ""
  read -rp "  Press Enter to close..."
  exit 1
fi

# ── Install dependencies if needed ──
if [ ! -d "node_modules" ]; then
  echo "  First time setup — installing dependencies..."
  echo "  (This takes about 30 seconds)"
  echo ""
  npm install --no-audit --no-fund 2>&1 | tail -1
  echo ""
fi

# ── Start with tunnel so it works on any phone ──
echo "  Launching... (keep this window open)"
echo ""

TUNNEL=true npx tsx src/index.ts
