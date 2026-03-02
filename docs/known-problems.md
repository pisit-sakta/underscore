# Known Hard Problems

---

## Battery Life

**Problem:** Constant sensor monitoring + LLM inference + music playback = phone dies fast.

**Mitigation:**
- Efficient on-device model for basic classification
- Gemini called only on CONTEXT CHANGES, not continuously
- Smart duty cycling
- Sensor polling frequency adjusts based on movement (stationary = slow poll, moving = fast poll)

---

## Latency

**Problem:** The soi dog doesn't wait for a cloud API call.

**Mitigation:**
- On-device model handles immediate responses
- Cloud model refines within 1-2 seconds
- The on-device model plays SOMETHING energetic immediately; the cloud model corrects to the PERFECT track shortly after

| Mode | Max Acceptable Latency | Strategy |
|------|----------------------|----------|
| Life | < 2 seconds | On-device immediate, cloud refines |
| Gaming | < 500ms | On-device immediate, cloud overrides if needed |
| RP | < 1 second | Parse while user reads response |

---

## False Positives

**Problem:** App thinks you're in a standoff but you're waiting for a crosswalk.

**Mitigation:**
- Confidence threshold for scene changes
- "Uncertain" classification defaults to maintaining current music
- Gradual transitions for ambiguous contexts (better to slowly build than to hard-cut to boss music at a traffic light)

---

## The Conversation Problem (Life Mode)

**Problem:** App can't hear what people say. Can't detect "I'm fine" in THAT tone.

**Mitigation:**
- Infer from secondary signals — heart rate changes, conversation stopping, stationary + emotional distance
- The 30-45 second delay before emotional music kicks in might actually be MORE cinematic (the dawning realization)
- The delay is a feature, not a bug

---

## Timing / Competition

**Problem:** This idea will occur to other people. Endel exists. Spotify is probably prototyping internally.

**Mitigation:**
- Move FAST. Ship ugly. Iterate in public.
- The advantage is SPECIFICITY OF VISION — nobody else thinks about this as "I want to feel like a Kojima protagonist"
- The wellness apps think "adaptive ambient for focus"
- We think "BURY THE LIGHT FOR A SCRAPED KNEE"
- That specificity is the moat
