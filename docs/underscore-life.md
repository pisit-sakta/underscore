# Underscore Life — Daily Life Soundtrack

Your daily existence, scored by AI using phone sensors.

---

## Sensor Suite

| Sensor | Source | What It Tells Us |
|--------|--------|-----------------|
| GPS speed + direction | Phone | Moving/stationary, vehicle vs walking, route deviation |
| Accelerometer | Phone | Walking, running, vehicle, falling, sudden impacts |
| Heart rate + HRV | Smartwatch | Emotional arousal, stress, excitement, calm |
| Time of day | Phone | Morning routine, commute, work hours, evening, night |
| Weather API (OpenWeatherMap) | API | Rain, sun, temperature — free atmosphere |
| Calendar | Phone (with permission) | Meetings, events, transitions between activities |
| Ambient sound | Phone mic (classification only, not recording) | Loud/quiet, voices/no voices, music/no music |
| Bluetooth proximity | Phone | Known devices nearby (friends, partner, car, headphones) |
| Fall detection | Smartwatch | Sudden impacts = injury/fall |

---

## Context to Scene Classification Examples

| Context Signal Combination | Scene Classification | Example Song Selection |
|---------------------------|---------------------|----------------------|
| Speed >30km/h + motorcycle accelerometer pattern + evening + route deviation from usual | Departure scene, protagonist on a mission | "The Other Shadow" (Cyn) |
| Sudden impact + elevated HR + stationary after movement | Injury/fall, warrior's wound | "Bury the Light" (DMC5) |
| Stationary + elevated HR + no movement + domestic location + second BT device nearby + reduced ambient conversation | Emotional tension, relationship distance | "Heavens Divide" (MGS: Peace Walker) |
| Stationary + confrontation posture + elevated HR + outdoor + evening | Standoff, holding ground | "The Only Thing I Know For Real" (MGR) |
| Arriving at work location + morning + routine pattern | Entering the dungeon, daily quest begins | Work-appropriate atmospheric transition |
| Leaving work + evening + consistent daily pattern | Departure, quest complete, heading home | User's established departure motif |
| Rain detected + walking + evening + solo | Contemplative walk, atmospheric moment | Ambient/atmospheric from user's library |

---

## Transition System

Songs don't CUT. They TRANSITION. The transition style depends on the narrative shift:

### Transition Types

| Shift Type | Style | Duration | Behavior |
|-----------|-------|----------|----------|
| **Gradual context shift** (walking → sitting) | Slow crossfade | 8-12 seconds | Instruments drop out naturally |
| **Moderate context shift** (working → commute) | Medium crossfade | 4-8 seconds | Energy builds through transition |
| **Sudden context shift** (calm → danger) | Fast crossfade | 1-2 seconds | Previous track drops immediately, new track enters at energy |
| **Emotional shift** (happy → tense) | Pre-transition | Variable | Music energy changes FIRST, then full track transition, creating a "something changed" feeling before the new song arrives |

---

## Learning Over Time

The app builds a complete emotional map:

### Week 1
Basic context matching. Energetic music when moving fast. Chill music in the evening. Rough but functional.

### Week 4
Learned daily patterns. Plays departure theme at 6:15pm as you pack up (2 minutes BEFORE you leave). Knows your commute route. Distinguishes weekday patterns from weekend.

### Week 12
Relationship awareness. Knows your partner's Bluetooth device. Adjusts emotional register when they're nearby. Your partner has a LEITMOTIF — a specific musical motif that plays when their device is detected. They have a theme song and they don't know it.

### Week 52
Complete emotional geography. Your office has a theme. Your favorite cafe has a theme. Your gym has a theme. Your parents' house has a theme. The app knows what song you need before you do. It knows that Mondays need Bury the Light energy and Fridays need Ghibli piano. It's become YOUR composer.

---

## The "Moment Replay" Feature (Premium)

The app logs: timestamp + location + song played + sensor data snapshot for every song transition. This creates an **AUTOBIOGRAPHY written in music**.

Users can go back and see:

> "On March 15th at 7:23pm, while riding your motorcycle through Chinatown in light rain at 35km/h with a heart rate of 88bpm, Underscore played 'The Other Shadow.' You turned the volume up."

That's a MEMORY. Encoded in data. Anchored in music. People would pay significant money for a complete musical diary of their life.

### Data Model

```json
{
  "timestamp": "2026-03-15T19:23:00+07:00",
  "location": { "lat": 13.7411, "lng": 100.5131, "name": "Chinatown" },
  "sensors": {
    "speed_kmh": 35,
    "heart_rate": 88,
    "weather": "light_rain",
    "vehicle": "motorcycle"
  },
  "track": {
    "id": "spotify:track:xxxxx",
    "name": "The Other Shadow",
    "artist": "Cyn"
  },
  "scene_classification": "departure_protagonist_on_mission",
  "user_reaction": "volume_up"
}
```
