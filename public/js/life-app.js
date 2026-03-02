/**
 * Underscore Life — Frontend Controller
 *
 * "Your day, scored."
 */

import { fetchStatus } from "./api.js";

// ── State ──

let presets = [];
let selectedActivity = null;
let selectedMood = null;
let selectedEnergy = null;
let isDemoMode = false;
let hasAI = false;

// Auto-mode state
let autoModeActive = false;
let autoInterval = null;
let autoCountdown = null;
let autoSecondsLeft = 0;
const AUTO_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

// ── Init ──

document.addEventListener("DOMContentLoaded", () => {
  initWelcome();
  initStatus();
  loadPresets();
  initControls();
  initScoreButton();
  initCollapsible();
  initInstallBanner();
  initSettings();
  initAutoMode();
  startNowPlayingPoll();
});

// ── Status ──

async function initStatus() {
  try {
    const data = await fetchStatus();
    isDemoMode = data.demoMode;
    hasAI = data.classifier === "gemini" || data.hasGeminiKey;
    renderStatus(data);
  } catch {
    document.querySelector(".js-status-text").textContent = "Server unavailable";
  }
}

function renderStatus(data) {
  const dot = document.querySelector(".js-status-dot");
  const text = document.querySelector(".js-status-text");
  const btn = document.querySelector(".js-connect-btn");
  const demoBadge = document.querySelector(".js-badge-demo");
  const aiBadge = document.querySelector(".js-badge-ai");

  dot.className = "status-dot";
  btn.classList.add("hidden");
  demoBadge.classList.add("hidden");
  if (aiBadge) aiBadge.classList.add("hidden");

  if (data.demoMode) {
    dot.classList.add("status-dot--demo");
    demoBadge.classList.remove("hidden");
    text.textContent = "Demo mode";
  } else if (data.connected) {
    dot.classList.add("status-dot--connected");
    text.textContent = data.libraryLoaded
      ? `${data.trackCount} tracks loaded`
      : "Loading library\u2026";
  } else {
    text.textContent = "Not connected";
    btn.classList.remove("hidden");
  }

  // Show AI badge when Gemini is active
  if (aiBadge && (data.classifier === "gemini" || data.hasGeminiKey)) {
    aiBadge.classList.remove("hidden");
    hasAI = true;
  }
}

// ── Presets ──

async function loadPresets() {
  try {
    const res = await fetch("/api/life/presets");
    const data = await res.json();
    presets = data.presets;
    renderPresets();
  } catch {
    console.error("Failed to load presets");
  }
}

function renderPresets() {
  const grid = document.querySelector(".js-preset-grid");
  grid.innerHTML = "";

  for (const preset of presets) {
    const btn = document.createElement("button");
    btn.className = "preset-card";
    btn.dataset.id = preset.id;
    btn.innerHTML = [
      `<span class="preset-card__emoji">${preset.emoji}</span>`,
      `<span class="preset-card__label">${preset.label}</span>`,
    ].join("");

    btn.addEventListener("click", () => {
      applyPreset(preset);
    });

    grid.appendChild(btn);
  }
}

function applyPreset(preset) {
  // Set activity
  selectActivity(preset.input.activity);

  // Set mood
  if (preset.input.mood) {
    selectMood(preset.input.mood);
  }

  // Set energy
  if (preset.input.energy) {
    selectEnergy(preset.input.energy);
  }

  // Set free text
  const textarea = document.querySelector(".js-freetext");
  textarea.value = "";

  // Highlight the preset button
  document.querySelectorAll(".preset-card").forEach((el) => {
    el.classList.toggle("preset-card--active", el.dataset.id === preset.id);
  });

  // Auto-score
  handleScore();
}

// ── Activity / Mood / Energy controls ──

function initControls() {
  // Activity pills
  document.querySelectorAll(".js-activity-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      selectActivity(btn.dataset.value);
      clearPresetHighlight();
    });
  });

  // Mood pills
  document.querySelectorAll(".js-mood-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      selectMood(btn.dataset.value);
      clearPresetHighlight();
    });
  });

  // Energy pills
  document.querySelectorAll(".js-energy-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      selectEnergy(btn.dataset.value);
      clearPresetHighlight();
    });
  });
}

function selectActivity(value) {
  selectedActivity = value;
  document.querySelectorAll(".js-activity-btn").forEach((el) => {
    el.classList.toggle("pill--active", el.dataset.value === value);
  });
}

function selectMood(value) {
  selectedMood = value;
  document.querySelectorAll(".js-mood-btn").forEach((el) => {
    el.classList.toggle("pill--active", el.dataset.value === value);
  });
}

function selectEnergy(value) {
  selectedEnergy = value;
  document.querySelectorAll(".js-energy-btn").forEach((el) => {
    el.classList.toggle("pill--active", el.dataset.value === value);
  });
}

function clearPresetHighlight() {
  document.querySelectorAll(".preset-card").forEach((el) => {
    el.classList.remove("preset-card--active");
  });
}

// ── Score ──

function initScoreButton() {
  document.querySelector(".js-btn-score").addEventListener("click", handleScore);
}

/** Get the current time-of-day bucket */
function getTimeOfDay() {
  const h = new Date().getHours();
  if (h < 6)  return "late_night";
  if (h < 9)  return "early_morning";
  if (h < 12) return "morning";
  if (h < 17) return "afternoon";
  if (h < 21) return "evening";
  if (h < 24) return "night";
  return "night";
}

async function handleScore() {
  if (!selectedActivity) return;

  const btn = document.querySelector(".js-btn-score");
  btn.disabled = true;
  btn.classList.add("btn-score--loading");

  const body = {
    activity: selectedActivity,
    mood: selectedMood || undefined,
    energy: selectedEnergy || undefined,
    timeOfDay: getTimeOfDay(),
    freeText: document.querySelector(".js-freetext").value.trim() || undefined,
  };

  try {
    const res = await fetch("/api/life/score", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await res.json();
    if (data.error) throw new Error(data.error);

    renderResult(data);
    markFirstScore();
  } catch (err) {
    alert(err.message || "Scoring failed");
  } finally {
    btn.disabled = false;
    btn.classList.remove("btn-score--loading");
  }
}

// ── Render result ──

function renderResult(data) {
  const emptyState = document.querySelector(".js-empty-state");
  emptyState.classList.add("hidden");

  // Scene card
  const card = document.querySelector(".js-scene-card");
  card.classList.remove("card--hidden");
  card.classList.add("card--enter");
  card.addEventListener("animationend", () => card.classList.remove("card--enter"), { once: true });

  document.querySelector(".js-scene-activity").textContent =
    data.scene.activity.replace(/_/g, " ").toUpperCase();
  document.querySelector(".js-scene-style").textContent =
    data.scene.sessionStyle.replace(/_/g, " ");
  document.querySelector(".js-scene-context").textContent =
    data.scene.contextSummary;

  // Energy bar
  const bar = document.querySelector(".js-energy-bar");
  const val = document.querySelector(".js-energy-value");
  bar.innerHTML = "";
  val.textContent = `${data.scene.energyLevel}/10`;
  for (let i = 1; i <= 10; i++) {
    const pip = document.createElement("div");
    pip.className = "energy__pip";
    if (i <= data.scene.energyLevel) {
      if (i >= 9) pip.classList.add("energy__pip--max");
      else if (i >= 7) pip.classList.add("energy__pip--high");
      else pip.classList.add("energy__pip--active");
    }
    bar.appendChild(pip);
  }

  // Detail grid
  document.querySelector(".js-detail-mood").textContent =
    formatLabel(data.scene.mood);
  document.querySelector(".js-detail-archetype").textContent =
    formatLabel(data.scene.trackCriteria?.archetype);
  document.querySelector(".js-detail-genres").textContent =
    (data.scene.trackCriteria?.preferredGenres || []).join(", ") || "\u2014";

  // Track card
  const trackCard = document.querySelector(".js-track-card");
  if (data.selectedTrack) {
    trackCard.classList.remove("card--hidden");
    trackCard.classList.add("card--enter");
    trackCard.addEventListener("animationend", () => trackCard.classList.remove("card--enter"), { once: true });

    document.querySelector(".js-track-name").textContent = data.selectedTrack.trackName;
    document.querySelector(".js-track-artist").textContent = data.selectedTrack.artistName;
    document.querySelector(".js-track-reason").textContent = data.reasoning || "";
  } else {
    trackCard.classList.add("card--hidden");
  }

  // Add to history
  addHistoryItem(data);
}

function formatLabel(str) {
  if (!str) return "\u2014";
  return str.replace(/_/g, " ");
}

// ── History ──

function addHistoryItem(data) {
  const section = document.querySelector(".js-history");
  section.classList.remove("history--hidden");

  const list = document.querySelector(".js-history-list");
  const item = document.createElement("div");
  item.className = "history__item";

  const activity = data.scene.activity.replace(/_/g, " ");
  const track = data.selectedTrack
    ? `${data.selectedTrack.trackName} \u2014 ${data.selectedTrack.artistName}`
    : "No track";
  const energy = data.scene.energyLevel ?? 0;
  const blocks = "\u2588".repeat(energy) + "\u2591".repeat(10 - energy);

  item.innerHTML = [
    `<span class="history__scene">${escapeHtml(activity)}</span>`,
    `<span class="history__track">${escapeHtml(track)}</span>`,
    `<span class="history__energy">${blocks}</span>`,
  ].join("");

  list.insertBefore(item, list.firstChild);
  while (list.children.length > 30) list.removeChild(list.lastChild);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

// ── Now Playing ──

function startNowPlayingPoll() {
  renderNowPlaying({ playing: false, demoMode: isDemoMode });

  setInterval(async () => {
    if (isDemoMode) return;
    try {
      const res = await fetch("/api/now-playing");
      const data = await res.json();
      renderNowPlaying(data);
    } catch {
      // silent
    }
  }, 5000);
}

function renderNowPlaying(data) {
  const eq = document.querySelector(".js-np-eq");
  const track = document.querySelector(".js-np-track");
  const artist = document.querySelector(".js-np-artist");
  const empty = document.querySelector(".js-np-empty");

  if (data.playing && data.track) {
    eq.classList.remove("now-playing__eq--hidden");
    track.textContent = data.track.name;
    track.classList.remove("hidden");
    artist.textContent = `\u2014 ${data.track.artist}`;
    artist.classList.remove("hidden");
    empty.classList.add("hidden");
  } else {
    eq.classList.add("now-playing__eq--hidden");
    track.classList.add("hidden");
    artist.classList.add("hidden");
    empty.classList.remove("hidden");
    empty.textContent = data.demoMode
      ? "Demo mode \u2014 connect Spotify for real playback"
      : "Nothing playing";
  }
}

// ── Collapsible customize section ──

function initCollapsible() {
  const toggle = document.querySelector(".js-toggle-customize");
  const body = document.querySelector(".js-customize-body");
  const arrow = document.querySelector(".js-toggle-arrow");

  if (!toggle) return;

  toggle.addEventListener("click", () => {
    const isOpen = body.classList.toggle("section-body--open");
    arrow.classList.toggle("section-toggle__arrow--open", isOpen);
  });
}

// ── Settings Modal ──

function initSettings() {
  const btn = document.querySelector(".js-settings-btn");
  const modal = document.querySelector(".js-settings-modal");
  if (!btn || !modal) return;

  // Open
  btn.addEventListener("click", async () => {
    modal.classList.remove("hidden");
    // Load current settings
    try {
      const res = await fetch("/api/settings");
      const data = await res.json();
      const input = document.querySelector(".js-gemini-key");
      const status = document.querySelector(".js-key-status");
      if (data.hasGeminiKey) {
        input.placeholder = data.geminiKeyHint || "Key configured";
        status.textContent = "AI classifier active";
        status.className = "settings-field__status settings-field__status--ok";
      } else {
        input.placeholder = "AIza...";
        status.textContent = "No key set \u2014 using basic mode";
        status.className = "settings-field__status";
      }
    } catch {
      // ignore
    }
  });

  // Close (both backdrop and X button)
  modal.querySelectorAll(".js-settings-close").forEach((el) => {
    el.addEventListener("click", () => modal.classList.add("hidden"));
  });

  // Toggle key visibility
  const toggleBtn = document.querySelector(".js-toggle-key-vis");
  if (toggleBtn) {
    toggleBtn.addEventListener("click", () => {
      const input = document.querySelector(".js-gemini-key");
      input.type = input.type === "password" ? "text" : "password";
    });
  }

  // Save
  const saveBtn = document.querySelector(".js-save-settings");
  if (saveBtn) {
    saveBtn.addEventListener("click", saveSettings);
  }
}

async function saveSettings() {
  const input = document.querySelector(".js-gemini-key");
  const status = document.querySelector(".js-key-status");
  const key = input.value.trim();

  if (!key) {
    status.textContent = "Enter a key to enable AI";
    status.className = "settings-field__status settings-field__status--err";
    return;
  }

  status.textContent = "Saving...";
  status.className = "settings-field__status";

  try {
    const res = await fetch("/api/settings", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ geminiApiKey: key }),
    });
    const data = await res.json();

    if (data.ok) {
      status.textContent = "Saved! AI classifier is now active.";
      status.className = "settings-field__status settings-field__status--ok";
      input.value = "";
      input.placeholder = `${key.slice(0, 4)}...${key.slice(-4)}`;
      hasAI = true;

      // Update header badge
      const aiBadge = document.querySelector(".js-badge-ai");
      if (aiBadge) aiBadge.classList.remove("hidden");

      // Refresh status
      initStatus();
    } else {
      status.textContent = data.error || "Save failed";
      status.className = "settings-field__status settings-field__status--err";
    }
  } catch {
    status.textContent = "Network error";
    status.className = "settings-field__status settings-field__status--err";
  }
}

// ── Auto-mode ──

function initAutoMode() {
  const toggle = document.querySelector(".js-auto-toggle");
  if (!toggle) return;

  toggle.addEventListener("click", () => {
    if (autoModeActive) {
      stopAutoMode();
    } else {
      startAutoMode();
    }
  });
}

function startAutoMode() {
  if (!selectedActivity) {
    // Need an activity selected first
    const toggle = document.querySelector(".js-auto-toggle");
    toggle.style.animation = "shake 0.4s ease";
    toggle.addEventListener("animationend", () => { toggle.style.animation = ""; }, { once: true });
    return;
  }

  autoModeActive = true;
  const toggle = document.querySelector(".js-auto-toggle");
  const bar = document.querySelector(".js-auto-bar");

  toggle.classList.add("auto-toggle--active");
  bar.classList.remove("hidden");

  // Start countdown
  autoSecondsLeft = AUTO_INTERVAL_MS / 1000;
  updateAutoTimer();
  autoCountdown = setInterval(() => {
    autoSecondsLeft--;
    if (autoSecondsLeft <= 0) {
      autoSecondsLeft = AUTO_INTERVAL_MS / 1000;
    }
    updateAutoTimer();
  }, 1000);

  // Auto-score on interval
  autoInterval = setInterval(() => {
    handleScore();
  }, AUTO_INTERVAL_MS);
}

function stopAutoMode() {
  autoModeActive = false;
  const toggle = document.querySelector(".js-auto-toggle");
  const bar = document.querySelector(".js-auto-bar");

  toggle.classList.remove("auto-toggle--active");
  bar.classList.add("hidden");

  if (autoInterval) { clearInterval(autoInterval); autoInterval = null; }
  if (autoCountdown) { clearInterval(autoCountdown); autoCountdown = null; }
}

function updateAutoTimer() {
  const timer = document.querySelector(".js-auto-timer");
  if (!timer) return;
  const m = Math.floor(autoSecondsLeft / 60);
  const s = autoSecondsLeft % 60;
  timer.textContent = `${m}:${String(s).padStart(2, "0")}`;
}

// ── Welcome overlay (first visit) ──

function initWelcome() {
  const overlay = document.querySelector(".js-welcome");
  if (!overlay) return;

  // Show only on first visit
  if (localStorage.getItem("underscore_welcomed")) return;

  overlay.classList.remove("hidden");

  const startBtn = document.querySelector(".js-welcome-start");
  startBtn.addEventListener("click", () => {
    localStorage.setItem("underscore_welcomed", "1");
    overlay.classList.add("welcome--leaving");
    overlay.addEventListener("animationend", () => {
      overlay.classList.add("hidden");
    }, { once: true });
  });
}

// ── PWA Install Banner ──

let deferredInstallPrompt = null;

function initInstallBanner() {
  const banner = document.querySelector(".js-install-banner");
  if (!banner) return;

  // Don't show if already installed as PWA
  if (window.matchMedia("(display-mode: standalone)").matches) return;

  // Don't show if user dismissed it
  if (localStorage.getItem("underscore_install_dismissed")) return;

  // Android/Chrome: capture the beforeinstallprompt event
  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
    showInstallBanner();
  });

  // iOS: show manual instructions after first score
  if (isIOS() && !deferredInstallPrompt) {
    const hint = document.querySelector(".js-install-hint");
    if (hint) hint.textContent = "Tap Share → Add to Home Screen";
    // Show after a short delay so it doesn't overwhelm on first load
    setTimeout(() => {
      if (!localStorage.getItem("underscore_scored_once")) return;
      showInstallBanner();
    }, 2000);
  }

  // Install button
  document.querySelector(".js-install-btn").addEventListener("click", async () => {
    if (deferredInstallPrompt) {
      deferredInstallPrompt.prompt();
      const { outcome } = await deferredInstallPrompt.userChoice;
      if (outcome === "accepted") {
        banner.classList.add("hidden");
      }
      deferredInstallPrompt = null;
    } else if (isIOS()) {
      // Can't auto-install on iOS — just highlight the hint
      const hint = document.querySelector(".js-install-hint");
      if (hint) {
        hint.style.color = "var(--accent-bright)";
        hint.textContent = "Tap Share ↑ then \"Add to Home Screen\"";
      }
    }
  });

  // Dismiss
  document.querySelector(".js-install-close").addEventListener("click", () => {
    banner.classList.add("hidden");
    localStorage.setItem("underscore_install_dismissed", "1");
  });
}

function showInstallBanner() {
  const banner = document.querySelector(".js-install-banner");
  if (banner) banner.classList.remove("hidden");
}

function isIOS() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

/**
 * Mark that the user has scored at least once.
 * Used to decide when to show the install banner on iOS.
 */
function markFirstScore() {
  if (!localStorage.getItem("underscore_scored_once")) {
    localStorage.setItem("underscore_scored_once", "1");
    // Show install banner on iOS after first score
    if (isIOS() && !localStorage.getItem("underscore_install_dismissed")) {
      setTimeout(showInstallBanner, 1500);
    }
  }
}
