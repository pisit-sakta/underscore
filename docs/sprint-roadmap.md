# Sprint Roadmap

---

## Phase 1: Underscore Life MVP (Sprints 0-3)

### Sprint 0: Foundation (Week 1)

- [ ] GitHub repo initialized
- [ ] Spotify Developer account + API keys
- [ ] Basic React Native / Flutter project scaffold
- [ ] Spotify authentication flow (OAuth)
- [ ] Basic sensor reading: accelerometer, GPS speed, time of day

### Sprint 1: The Core Loop (Weeks 2-3)

- [ ] Gemini 3 Flash API integration
- [ ] First scene classification call: send sensor data → receive scene type
- [ ] First song match: scene type + user's top 50 Spotify songs → track recommendation
- [ ] Wire Spotify playback to LLM output
- [ ] **MILESTONE:** Press button → phone reads sensors → LLM picks song → Spotify plays it
- [ ] It will be ugly. It will sometimes be wrong. But it WORKS.

### Sprint 2: Automation (Weeks 4-5)

- [ ] Remove manual trigger — continuous sensor monitoring
- [ ] Automatic song switching when context changes
- [ ] Basic crossfade transitions (Spotify SDK)
- [ ] Weather API integration
- [ ] Heart rate integration (if smartwatch available)
- [ ] **MILESTONE:** Walk around Bangkok for an hour. Music changes automatically. At least 3 out of 10 transitions feel RIGHT.

### Sprint 3: Polish & Personal Tuning (Weeks 6-8)

- [ ] Tune LLM prompts until founder's commute feels cinematic
- [ ] Add place-linked learning (same song at same location = memorize)
- [ ] Add skip/volume-up feedback loop
- [ ] Bluetooth device detection for relationship awareness
- [ ] **MILESTONE:** The app plays something unexpected and PERFECT. The founder gets chills. The prototype is real.

---

## Phase 2: Content & Launch (Sprints 4-5)

### Sprint 4: Character Profiles v1 (Weeks 9-10)

- [ ] Implement roleplay mode UI (character select screen)
- [ ] Build 3 pre-built profiles: Bond, Shelby, Big Boss
- [ ] Character profile constrains song selection to franchise playlists
- [ ] Test each profile for a full day

### Sprint 5: Public Beta (Weeks 11-14)

- [ ] Clean UI, onboarding flow
- [ ] "Connect Spotify → Tell us your vibe → Live your movie"
- [ ] Remaining launch roster character profiles
- [ ] Play Store beta listing
- [ ] **MILESTONE:** Film the TikTok content slate (see [Go-To-Market](./go-to-market.md))

---

## Phase 3: Expansion (Sprints 6-10)

### Sprint 6: Community & Custom Characters

- [ ] Custom character generation via Gemini 3 Flash
- [ ] Profile sharing / marketplace
- [ ] Community rating system

### Sprint 7: Underscore RP

- [ ] SillyTavern extension (text parser + scene classifier + playback)
- [ ] Character card metadata reading
- [ ] Franchise detection from text
- [ ] Careless Whisper Protocol implementation
- [ ] Browser extension for Claude / ChatGPT / Grok

### Sprint 8: Underscore Gaming v1

- [ ] Desktop companion app
- [ ] Valorant integration (Riot API)
- [ ] CS2 integration (Game State Integration)
- [ ] Basic game state → scene classification → music

### Sprint 9: Platform Partnerships

- [ ] Prepare partnership materials (metrics, demo, integration docs)
- [ ] Spotify partnership outreach (after organic traction achieved)
- [ ] AI platform partnership outreach (Anthropic, OpenAI, etc.)

### Sprint 10: Advanced Features

- [ ] Moment Replay diary (Premium)
- [ ] Blend Mode (mix character profiles across time blocks)
- [ ] Recurring motif detection and establishment
- [ ] Advanced transition engine
- [ ] Multi-platform streaming support (Apple Music, YouTube Music)
