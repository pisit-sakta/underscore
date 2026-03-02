/**
 * Underscore RP — UI Renderer
 *
 * All DOM manipulation lives here.
 * Exports pure functions: (element, data) => void
 */

// ── Helpers ──

function $(selector) {
  return document.querySelector(selector);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function formatLabel(str) {
  if (!str) return "\u2014";
  return str.replace(/_/g, " ");
}

// ── Status ──

export function renderStatus(data) {
  const dot = $(".js-status-dot");
  const text = $(".js-status-text");
  const btn = $(".js-connect-btn");
  const demoBadge = $(".js-badge-demo");
  const classifierBadge = $(".js-badge-classifier");

  // Reset
  dot.className = "status-dot";
  btn.classList.add("hidden");
  demoBadge.classList.add("hidden");
  classifierBadge.classList.add("hidden");

  if (data.demoMode) {
    dot.classList.add("status-dot--demo");
    demoBadge.classList.remove("hidden");
    text.textContent = "Demo mode";

    // Show classifier type
    classifierBadge.classList.remove("hidden");
    if (data.classifier === "gemini") {
      classifierBadge.textContent = "Gemini";
      classifierBadge.className = "badge badge--gemini js-badge-classifier";
    } else {
      classifierBadge.textContent = "Mock";
      classifierBadge.className = "badge badge--mock js-badge-classifier";
    }
  } else if (data.connected) {
    dot.classList.add("status-dot--connected");
    text.textContent = data.libraryLoaded
      ? `${data.trackCount} tracks loaded`
      : "Loading library\u2026";
  } else {
    text.textContent = "Not connected";
    btn.classList.remove("hidden");
  }
}

// ── Scene Card ──

export function renderScene(scene) {
  const card = $(".js-scene-card");
  card.classList.remove("card--hidden");
  card.classList.add("card--enter");

  // Remove animation class after it plays
  card.addEventListener("animationend", () => card.classList.remove("card--enter"), { once: true });

  // Type
  $(".js-scene-type").textContent = formatLabel(scene.sceneType).toUpperCase();

  // Franchise
  const franchise = $(".js-scene-franchise");
  if (scene.franchise) {
    franchise.textContent = scene.franchise;
    franchise.classList.remove("hidden");
  } else {
    franchise.classList.add("hidden");
  }

  // Energy bar
  renderEnergyBar(scene.energyLevel);

  // Detail grid
  $(".js-detail-emotion").textContent = formatLabel(scene.emotionalRegister);
  $(".js-detail-beat").textContent = formatLabel(scene.narrativeBeat);
  $(".js-detail-combat").textContent = formatLabel(scene.combatStatus);
  $(".js-detail-mood").textContent = formatLabel(scene.trackCriteria?.mood);
  $(".js-detail-archetype").textContent = formatLabel(scene.trackCriteria?.archetype);
  $(".js-detail-intimate").textContent = scene.intimateScene ? "Yes" : "No";
}

function renderEnergyBar(level) {
  const bar = $(".js-energy-bar");
  const value = $(".js-energy-value");
  bar.innerHTML = "";
  value.textContent = `${level}/10`;

  for (let i = 1; i <= 10; i++) {
    const pip = document.createElement("div");
    pip.className = "energy__pip";
    if (i <= level) {
      if (i >= 9) pip.classList.add("energy__pip--max");
      else if (i >= 7) pip.classList.add("energy__pip--high");
      else pip.classList.add("energy__pip--active");
    }
    bar.appendChild(pip);
  }
}

// ── Track Card ──

export function renderTrack(data) {
  const card = $(".js-track-card");

  if (!data.selectedTrack) {
    card.classList.add("card--hidden");
    return;
  }

  card.classList.remove("card--hidden");
  card.classList.add("card--enter");
  card.addEventListener("animationend", () => card.classList.remove("card--enter"), { once: true });

  $(".js-track-name").textContent = data.selectedTrack.trackName;
  $(".js-track-artist").textContent = data.selectedTrack.artistName;
  $(".js-track-reason").textContent = data.reasoning || "";

  // Action badge
  const badge = $(".js-action-badge");
  const action = data.playbackAction || "play";
  badge.textContent = formatLabel(action);
  badge.className = `action-badge action-badge--${action} js-action-badge`;

  // Art icon based on action
  const artIcon = $(".js-track-art-icon");
  if (action === "hard_cut") {
    artIcon.textContent = "\u26A1";
  } else if (action === "crossfade") {
    artIcon.textContent = "\uD83C\uDFB5";
  } else {
    artIcon.textContent = "\u25B6";
  }
}

// ── Empty State ──

export function hideEmptyState() {
  $(".js-empty-state").classList.add("hidden");
}

export function showEmptyState() {
  $(".js-empty-state").classList.remove("hidden");
  $(".js-scene-card").classList.add("card--hidden");
  $(".js-track-card").classList.add("card--hidden");
}

// ── History ──

export function addHistoryItem(data) {
  const section = $(".js-history");
  section.classList.remove("history--hidden");

  const list = $(".js-history-list");
  const item = document.createElement("div");
  item.className = "history__item";

  const sceneLabel = formatLabel(data.scene?.sceneType);
  const trackLabel = data.selectedTrack
    ? `${escapeHtml(data.selectedTrack.trackName)} \u2014 ${escapeHtml(data.selectedTrack.artistName)}`
    : "No track";

  const energy = data.scene?.energyLevel ?? 0;
  const energyBlocks = "\u2588".repeat(energy) + "\u2591".repeat(10 - energy);

  item.innerHTML = [
    `<span class="history__scene">${escapeHtml(sceneLabel)}</span>`,
    `<span class="history__track">${trackLabel}</span>`,
    `<span class="history__energy">${energyBlocks}</span>`,
  ].join("");

  list.insertBefore(item, list.firstChild);

  // Cap at 30 entries
  while (list.children.length > 30) {
    list.removeChild(list.lastChild);
  }
}

// ── Now Playing ──

export function renderNowPlaying(data) {
  const eq = $(".js-np-eq");
  const track = $(".js-np-track");
  const artist = $(".js-np-artist");
  const empty = $(".js-np-empty");

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

// ── Score Button ──

export function setScoreLoading(loading) {
  const btn = $(".js-btn-score");
  if (loading) {
    btn.disabled = true;
    btn.classList.add("btn-score--loading");
  } else {
    btn.disabled = false;
    btn.classList.remove("btn-score--loading");
  }
}
