# UNDERSCORE — Complete Technical Handoff Document

**Codename:** Deep Battle
**Version:** 1.0 — The Sacred Handoff
**Date:** March 2026

> "This app was conceived during a conversation about German tank doctrine, Adele, and a soi dog. Codename: Deep Battle. Rolling in the deep, always."

---

## Executive Summary

### What is Underscore?

An AI-powered context-aware life soundtrack platform that automatically plays the right song at the right moment — across your daily life, your gaming sessions, and your text-based stories — using sensor data, game state, or conversation text as input, an LLM narrative intelligence layer (Gemini 3 Flash) for scene classification, and streaming platform APIs (Spotify, Apple Music, YouTube Music) for playback.

**The one-line pitch:**
> "What if your phone played the right song at the right time, automatically?"

**The founder's pitch:**
> "Your life deserves a soundtrack. We built the thing that gives you one. Now go fight a soi dog. The music's ready."

### Three Products. One Engine. Infinite Soundtracks.

| Product | Input Source | Target User |
|---------|------------|-------------|
| **Underscore Life** | Phone sensors (GPS, accelerometer, heart rate, weather) | Everyone with a phone and emotions |
| **Underscore Gaming** | Game state data (health, kills, round status) | Gamers, streamers, content creators |
| **Underscore RP** | Text conversation (parsed by LLM for scene context) | LLM roleplay users, visual novel fans, AI chat users |

All three share the same core narrative engine, the same playback system, and the same character profile infrastructure.

---

## Table of Contents

### Core Documentation

| Document | Description |
|----------|-------------|
| [Architecture](./architecture.md) | Core narrative engine, LLM layer, streaming integration, tech stack |
| [Underscore Life](./underscore-life.md) | Daily life soundtrack — sensors, scene classification, learning |
| [Underscore Gaming](./underscore-gaming.md) | Game mode integration — game state APIs, clutch scenarios |
| [Underscore RP](./underscore-rp.md) | Text roleplay soundtrack layer — SillyTavern, browser extensions |
| [Character System](./character-system.md) | Character profiles, custom characters, community marketplace |
| [Careless Whisper Protocol](./careless-whisper-protocol.md) | The saxophone. Non-negotiable. |

### Strategy & Planning

| Document | Description |
|----------|-------------|
| [Sprint Roadmap](./sprint-roadmap.md) | Phase-by-phase development plan (Sprints 0-10) |
| [Go-To-Market Strategy](./go-to-market.md) | Content slate, platform strategy, growth playbook |
| [Business Model](./business-model.md) | Revenue streams, pricing, platform partnerships |

### Reference

| Document | Description |
|----------|-------------|
| [Brand Guidelines](./brand-guidelines.md) | Voice, tone, terminology, easter eggs |
| [Known Hard Problems](./known-problems.md) | Battery, latency, false positives, timing |

---

## Product Vision & Thesis

### The Core Insight

Nobody listens to music for the music.

97% of listeners are casting themselves as the main character of the song. When someone puts on a battle theme at the gym, they're not appreciating polyrhythmic complexity — they're FIGHTING SOMEONE in their head. When someone plays a sad ballad after a breakup, they're not analyzing chord progressions — they're the STAR of a melancholy indie film.

Music is not entertainment. Music is an **IDENTITY ENGINE**. People use it to become someone MORE.

Every existing music product asks: "What song do you want to listen to?"

The actual question people are answering is: **"Who do I want to BE right now?"**

### What Underscore Does

Underscore automates the thing people already do — scoring their own lives with music — but with the intelligence to do it RIGHT. It plays the right song at the right moment, with seamless transitions, zero manual input, all day.

The app doesn't match songs to MOODS. It matches songs to IDENTITIES.

The question isn't "the user is exercising, what exercise music should play?" The question is "the user is exercising — WHO DO THEY WANT TO BE RIGHT NOW?"

### The OpenClaw Parallel

This product follows the OpenClaw playbook:

- Simple concept, obvious value ("AI that does things" → "Music that knows things")
- Vibe-codeable MVP — the prototype is three API calls in a trenchcoat
- The "holy shit" moment sells itself — every perfectly timed song is a viral moment
- Exponential improvement over time — the app learns YOUR patterns
- Succeed independently FIRST, then let the platforms come to you

### Why This Is Worth Billions

- **TAM:** Everyone who listens to music on their phone (1B+ streaming users)
- **Moat:** Personalization data. After 6 months, the app knows your emotional architecture better than you do. Switching means losing your identity.
- **Retention:** The app creates emotional dependency. Not on the music — on the FEELING of being the protagonist. People don't cancel subscriptions on feelings.
- **Viral loop:** Every perfectly scored moment is shareable content. The product IS the marketing.

---

## Quick Start for Developers

1. Read [Architecture](./architecture.md) for the core engine design
2. Pick your product area: [Life](./underscore-life.md) | [Gaming](./underscore-gaming.md) | [RP](./underscore-rp.md)
3. Review the [Character System](./character-system.md) for cross-product character profiles
4. Check the [Sprint Roadmap](./sprint-roadmap.md) for current priorities
5. Read [Known Problems](./known-problems.md) for technical constraints and mitigations

---

## First Commit Message

```
feat: initialize Underscore repository

Codename: Deep Battle
Origin: A conversation about German tank doctrine and Adele

Three products. One engine. Infinite soundtracks.
- Underscore Life: your daily existence, scored
- Underscore Gaming: your games, scored
- Underscore RP: your stories, scored

Plus: character profiles, custom characters,
community marketplace, and a saxophone.

The saxophone is non-negotiable.

"Your life, scored."
```
