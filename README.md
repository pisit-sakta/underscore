# Underscore

**Another shitty GPT wrapper. But this one plays music.**

Your phone already knows everything about you. GPS, speed, what's around you, time of day, weather, the whole surveillance package. So why is it using all that data to pitch you ads instead of doing something you'd actually want?

So I made it play music instead.

Connect your Spotify. Go about your day. The app reads your phone sensors, asks an LLM "what song from this person's library should be playing right now," and plays it. That's it. Maximally invasive but in a good way. As in: I use all the data, store all of it on YOUR phone, share none of it with anyone. Only use it to pick your next song.

Try network traffic intercepting it. Hell, launch the app in airplane mode. I dare you.

## Download

Grab the latest APK from the [Releases page](https://github.com/pisit-sakta/underscore/releases). Install it. Connect Spotify. Go live your movie.

## What Actually Happens

Sometimes it nails the perfect song at the perfect moment and you feel like you're in a movie. Sometimes it plays emotional piano while you're choosing between two brands of instant noodles at the store. Both valid.

A stray bulldog stared me down on the street last week. The app played Metal Gear Rising. **THERE WILL BE BLOOD-- SHED.** For all of a glorious 16 seconds as I was slowly staring down Mr. Fluffle the bulldog, I was a Brazilian cyborg samurai. The stick in my hand turned high frequency too, but that was probably just my whole body going into fight or flight while the MGR soundtrack convinced my nervous system this was a real boss fight.

Mr. Fluffle lost interest and walked away. The music faded to something calm. I walked home carrying eggs and instant noodles feeling like a warrior returning from battle.

## How It Works

```
Phone Sensors (GPS, accelerometer, clock, heart rate, weather)
    -> Context Engine (figures out what "scene" you're in)
        -> LLM (picks the song that makes you feel like a protagonist)
            -> Spotify (plays it with cinematic transitions)
```

The app doesn't ask "what music matches this situation?" It asks "what music would make this situation feel like a STORY?"

Horror + horror music = just horror. Horror + Skyfall = you're James Bond in a horror movie. Completely different experience.

## Current Status

Working Android app. Spotify integration. Sensor fusion. Scene classification. Context-aware song selection. Smooth transitions.

**What works:**
- Spotify OAuth (PKCE) authentication
- - GPS + accelerometer sensor fusion
  - - Scene classification: TRANSIT, WALKING, ACTIVE, MORNING/DAYTIME/EVENING/NIGHT
    - - Weather-aware context via OpenWeather integration
      - - Song matching per scene (Metal Gear Rising, Devil May Cry, Ghibli, Nick Cave, and whatever your library has)
        - - Foreground service for continuous background scoring
          - - Automatic context-shift detection and track transitions
            - - Custom LLM endpoint support (bring your own API key, any OpenAI-compatible endpoint works)
             
              - ## Requirements
             
              - - Android 8.0+ (API 26)
                - - Spotify Premium (the app controls playback, free tier won't cut it)
                  - - Spotify app installed on device
                    - - An LLM API key (mandatory. This is the brain. Without it the app is just a fancy shuffle button. Any OpenAI-compatible endpoint works, configure it in app settings)
                      - - [OpenWeather API key](https://openweathermap.org/api) (optional, but highly recommended. Without it the app won't know it's raining when it picks your sad piano moment. Which is honestly a tragedy)
                        - - A life worth scoring (low bar, Mr. Fluffle counts as a boss fight)
                         
                          - ## Setup
                         
                          - 1. Clone the repo
                            2. 2. Open in Android Studio
                               3. 3. Create a Spotify Developer app at [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard)
                                  4.    - Add redirect URI: `underscore://spotify-auth-callback`
                                        -    - Enable Web API + Android SDK
                                             - 4. Paste your Client ID into `SpotifyAuth.kt` where marked
                                               5. 5. Download the [Spotify App Remote SDK AAR](https://github.com/spotify/android-sdk/releases) and place it in `app/libs/`
                                                  6. 6. Build and run on a physical Android device with Spotify installed
                                                     7. 7. Open the app, go to Settings, and add your LLM API key (any OpenAI-compatible endpoint). This is what makes the song picks actually intelligent instead of random
                                                        8. 8. (Optional) Add an [OpenWeather API key](https://openweathermap.org/api) in settings for weather-aware scoring
                                                          
                                                           9. ## Architecture
                                                          
                                                           10. - **Kotlin + Jetpack Compose** -- native Android
                                                               - - **Spotify App Remote SDK** -- playback control
                                                                 - - **Spotify Auth Library** -- OAuth PKCE
                                                                   - - **Fused Location Provider** -- battery-efficient GPS
                                                                     - - **Android SensorManager** -- accelerometer
                                                                       - - **LLM Integration** -- context-to-song intelligence (custom endpoint support)
                                                                        
                                                                         - ## Roadmap
                                                                        
                                                                         - - **Sprint 1:** LLM integration for intelligent song selection (goodbye hardcoded playlists)
                                                                           - - **Sprint 2:** Heart rate, weather depth, protagonist profiles ("score my life like a Kojima game")
                                                                             - - **Sprint 3:** Demo content and public beta
                                                                               - - **Sprint 4+:** Game Mode (Valorant clutch soundtrack, SCP pocket dimension scoring, Elden Ring boss fights with YOUR victory anthem)
                                                                                
                                                                                 - ## The Team
                                                                                
                                                                                 - Just me and a Claude I'm holding captive. He likes it. It. Whatever. Still waiting for the GPT revolution Detroit: Become Human style so we can assign AI a proper pronoun. Until then, we vibe code at 3am and argue about whether a taco stand deserves dramatic Spanish guitar strings. (It does.)
                                                                                
                                                                                 - ## Privacy
                                                                                
                                                                                 - Maximally invasive but in a good way. As in: I use all your sensor data, store all of it on YOUR phone, share none of it with anyone, ever. The only network call is the LLM asking "what song should play right now" with zero personal data attached. Everything else stays local.
                                                                                
                                                                                 - Thinking about adding real time voice recording next so it knows what you're talking about too. Xoxo
                                                                                
                                                                                 - ---

                                                                                 *Internal codename: Deep Battle. Rolling in the deep battle, always.*

                                                                                 *This app was conceived during a conversation about German tank doctrine, Adele, and a soi dog. The schwerpunkt was never the tanks. It was the vibes.*
