/**
 * Underscore RP — App Controller
 *
 * Wires up events, manages state, coordinates API + UI.
 */

import { fetchStatus, scoreScene, fetchNowPlaying } from "./api.js";
import { DEMOS } from "./demos.js";
import {
  renderStatus,
  renderScene,
  renderTrack,
  hideEmptyState,
  addHistoryItem,
  renderNowPlaying,
  setScoreLoading,
} from "./ui.js";

// ── State ──

let isDemoMode = false;

// ── Init ──

document.addEventListener("DOMContentLoaded", () => {
  initStatus();
  initSnippets();
  initScoreButton();
  initKeyboardShortcut();
  initUrlParams();
  startNowPlayingPoll();
});

// ── Status check ──

async function initStatus() {
  try {
    const data = await fetchStatus();
    isDemoMode = data.demoMode;
    renderStatus(data);
  } catch {
    document.querySelector(".js-status-text").textContent = "Server unavailable";
  }
}

// ── Demo snippets ──

function initSnippets() {
  const container = document.querySelector(".js-snippets");
  const textarea = document.querySelector(".js-textarea");

  Object.entries(DEMOS).forEach(([key, demo]) => {
    const btn = document.createElement("button");
    btn.className = "snippet-btn";
    btn.textContent = demo.label;
    btn.dataset.key = key;
    btn.addEventListener("click", () => {
      textarea.value = demo.text;
      textarea.focus();
    });
    container.appendChild(btn);
  });
}

// ── Score button ──

function initScoreButton() {
  const btn = document.querySelector(".js-btn-score");
  btn.addEventListener("click", handleScore);
}

async function handleScore() {
  const textarea = document.querySelector(".js-textarea");
  const text = textarea.value.trim();
  if (!text) return;

  setScoreLoading(true);

  try {
    const data = await scoreScene(text);
    hideEmptyState();
    renderScene(data.scene);
    renderTrack(data);
    addHistoryItem(data);
  } catch (err) {
    alert(err.message || "Failed to score scene. Is the server running?");
  } finally {
    setScoreLoading(false);
  }
}

// ── Keyboard shortcut ──

function initKeyboardShortcut() {
  const textarea = document.querySelector(".js-textarea");
  textarea.addEventListener("keydown", (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      handleScore();
    }
  });
}

// ── URL params (post-OAuth redirect) ──

function initUrlParams() {
  const params = new URLSearchParams(window.location.search);

  if (params.get("connected") === "true") {
    initStatus(); // Re-check
    window.history.replaceState({}, "", "/");
  }

  const error = params.get("error");
  if (error && error !== "demo_mode") {
    alert("Spotify connection failed: " + error);
    window.history.replaceState({}, "", "/");
  }

  if (error === "demo_mode") {
    window.history.replaceState({}, "", "/");
  }
}

// ── Now-playing poll ──

function startNowPlayingPoll() {
  // Initial render
  renderNowPlaying({ playing: false, demoMode: isDemoMode });

  setInterval(async () => {
    if (isDemoMode) return;
    try {
      const data = await fetchNowPlaying();
      renderNowPlaying(data);
    } catch {
      // silent
    }
  }, 5000);
}
