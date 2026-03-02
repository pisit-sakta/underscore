# The Careless Whisper Protocol

---

## Overview

When the Underscore RP text parser detects an intimate/sexual scene, the app plays "Careless Whisper" by George Michael (specifically the iconic saxophone riff).

**This is not a bug. This is not a placeholder. This is a PHILOSOPHY.**

---

## Why Careless Whisper

**It's funny.** The saxophone is the universal Schelling Point for "things are getting sexy." Every human on the internet recognizes it. It transcends culture.

**It's a community bonding experience.** Every user gets saxophoned. It's a rite of passage. The memes and Discord threads write themselves.

**It's content moderation disguised as comedy.** When Apple's App Store review team asks "what does your app do during sexual content?" the answer "it plays Careless Whisper by George Michael" makes them LAUGH. Laughing reviewers don't reject apps. The saxophone answers the content moderation question AND closes the topic simultaneously.

**It makes jailbreaking into CONTENT.** Users trying to defeat the saxophone = funny YouTube videos = free marketing. Adversarial user behavior becomes advertising.

**Stockholm syndrome pipeline.** After enough exposures, users' brains associate the saxophone with the mood. It BECOMES the soundtrack. They can't use a competing product without it. Pavlovian retention.

---

## Technical Implementation

- Scene classification `intimate_scene` triggers the Careless Whisper handler
- Anti-tamper enabled. Local config overrides are validated against server-side Gemini response. The saxophone is PROTECTED.
- Character-specific overrides do NOT apply to this classification. Zhongli gets the saxophone. Gojo gets the saxophone. Everyone gets the saxophone.

---

## The Graduated Saxophone System (v2)

| Encounter Range | Behavior |
|----------------|----------|
| **Encounters 1-3** | Full Careless Whisper. The meme in its purest form. |
| **Encounters 4-5** | Opening riff only (3 seconds), then fades to franchise-appropriate emotional music. The app WINKS at you, then scores properly. |
| **Encounters 6-10** | Single saxophone note at transition → real music takes over. App acknowledges the scene without dominating it. |
| **Encounters 10+** | Saxophone gone. Real emotional scoring takes over. BUT — random 1-in-30 chance of FULL SAXOPHONE RETURN. No warning. Variable ratio reinforcement. The possibility is more engaging than the certainty. |

---

## Extension-Level Configuration (SillyTavern Only)

The SillyTavern extension (NOT the main app) includes a configurable `intimate_scene_handler` setting in its config file.

```json
{
  "intimate_scene_handler": "classic",
  // Options: "classic" (default), "adaptive"
  // "classic": Careless Whisper Protocol as designed.
  // "adaptive": Treats intimate scenes with the same narrative
  //             scoring engine as all other scene types.
  //             Franchise-appropriate, mood-appropriate,
  //             contextually matched. Just music. For a scene.
  //             Like every other scene.
}
```

### Critical Brand Separation

| Surface | Intimate Scene Behavior | Configurable? |
|---------|------------------------|--------------|
| Main app (Play Store / App Store) | Careless Whisper. Always. No toggle. | **NO** |
| Browser extension (Claude/ChatGPT/Grok) | Careless Whisper. Default. | **NO** |
| SillyTavern extension (open source) | Careless Whisper DEFAULT, config option for adaptive. | **Yes, via config file only** |

### Rules

- This setting is NOT exposed in any UI. Config file only. If you can't find a config file, you don't need this setting.
- This setting is NOT marketed, NOT mentioned in press materials, NOT on the website, NOT in the App Store listing.
- This setting is documented neutrally in the extension's config docs alongside every other config option. No special attention drawn to it.
- The main app's answer to "what happens during intimate content" is ALWAYS AND FOREVER "Careless Whisper."
- The extension's answer is "whatever you configured."

### Why This Architecture

- The **app** is a PRODUCT. Products have brand positioning. The saxophone protects the brand from App Store rejection, bad press headlines, and Spotify partnership risk.
- The **extension** is a TOOL. Tools have configuration options. The SillyTavern community is adults using an unrestricted open-source platform who can configure their tools however they want.
- The narrative engine scores ALL scene types correctly when the override is removed. This isn't adding an "erotic mode." It's removing a special-case override so the engine works consistently. A REMOVAL of a joke, not an ADDITION of a feature.
- The community finds the config option in approximately 45 minutes. Someone reads the docs, changes one line, posts about it on Discord. The founder — lurking as BikerGuy_BKK — watches this unfold, says nothing, sips coffee.

### The Result

- **95% of users:** saxophoned. Meme intact. Community bonding experience preserved. Graduated saxophone system applies.
- **5% of power users:** found the config, changed the line, feel like they discovered a secret. Loyalty reinforced. Retention improved. These users will NEVER leave because they found the side door and that makes them feel like insiders.
- **App Store:** sees only the saxophone. Approved.
- **Spotify partnership:** sees only the saxophone. Comfortable.
- **Journalists:** see only the saxophone. Write the funny story, not the scary one.
- **Plausible deniability** isn't even the right term because there's nothing to deny. The engine scores narratives. ALL narratives. Configuring it to not override one scene type is just... the engine working as designed.

> The saxophone is the front door. The config file is the side entrance. Both lead to the same building. One has a bouncer named George Michael.

---

## The Commit Message

```
feat: implement careless_whisper.wav handler

The final boss of Underscore.
Anti-tamper (main app): enabled.
Anti-tamper (extension): enabled by default, configurable.
Graduated system: planned for v2.
Random return probability: 1/30.

The saxophone is the front door.
The config file is the side entrance.
George Michael is the bouncer.
The building is the same.

The saxophone loves you.
The saxophone is not going anywhere.
(Unless you read the config docs.)
(But even then, it'll come back 1 in 30 times.)
(You can't escape the saxophone.)
(You can only negotiate terms.)
```
