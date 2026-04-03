# Underscore

**Your life, scored.**

Underscore detects real-world context from your phone's sensors and automatically plays the right song at the right moment via Spotify. Like having a film composer scoring your life in real time.

## How It Works

```
Phone Sensors (GPS, accelerometer, clock)
    -> Context Engine (classifies your current "scene")
        -> Song Selector (picks the right track)
            -> Spotify (plays it with cinematic transitions)
```

## Current Status: Sprint 0

Hardcoded context engine with rule-based scene classification and a curated song library. No LLM yet.

**What works:**
- Spotify OAuth authentication (PKCE)
- GPS + accelerometer sensor fusion
- Scene classification: TRANSIT, WALKING, ACTIVE, MORNING/DAYTIME/EVENING/NIGHT
- Hardcoded song mapping per scene (Metal Gear, DMC, Ghibli, Nick Cave)
- Foreground service for background scoring
- Automatic context-shift detection and track transitions

## Setup

1. Clone the repo
2. Open in Android Studio
3. Create a Spotify Developer app at https://developer.spotify.com/dashboard
   - Add redirect URI: `underscore://spotify-auth-callback`
   - Enable Web API + Android SDK
4. Paste your Client ID into `SpotifyAuth.kt` where marked
5. Download the [Spotify App Remote SDK AAR](https://github.com/spotify/android-sdk/releases) and place it in `app/libs/`
6. Build and run on a physical Android device with Spotify installed

## Requirements

- Android 8.0+ (API 26)
- Spotify Premium
- Spotify app installed on device

## Architecture

- **Kotlin + Jetpack Compose** — native Android
- **Spotify App Remote SDK** — playback control
- **Spotify Auth Library** — OAuth PKCE
- **Fused Location Provider** — battery-efficient GPS
- **Android SensorManager** — accelerometer

## Roadmap

- **Sprint 1:** Gemini 3 Flash integration for intelligent song selection
- **Sprint 2:** Heart rate, weather, protagonist profiles
- **Sprint 3:** Demo content and public beta
- **Sprint 4+:** Gaming companion, roleplay scoring, character profiles

---

*Internal codename: Deep Battle. Rolling in the deep battle, always.*
