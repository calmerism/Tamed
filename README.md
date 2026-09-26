<div align="center">
  <img src="TamedMask.png" width="128" alt="Tamed Logo" />
  <h1>Tamed</h1>
  <p>Android music player with synchronized listening rooms, video canvases, and song recognition.</p>
</div>

Tamed brings online streaming and offline libraries into a single player with shared listening, video canvases, and on-device audio recognition.

## Highlights

* **Together Rooms**: Create or join listening rooms with friends. Playback stays synchronized in real time over WebSockets.
* **Song Recognition**: Identify tracks playing around you right from search using built-in audio fingerprinting.
* **Video Canvases**: Plays looping artist video canvases behind track playback with ambient color backdrops.
* **Synchronized Lyrics**: Word-by-word and line-by-line scrolling lyrics sourced across LRCLIB, KuGou, and TTML, with offline caching.
* **Lossless Audio & Native DSP**: Plays local FLAC, ALAC, WAV, and Opus alongside streams, with a C++ DSP engine for real-time tempo and spectrogram analysis.
* **Discord RPC & Scrobbling**: Broadcast active listening status on Discord and scrobble plays to Last.fm and stats.fm.

## Download

Get the public beta APK from the [Releases](https://github.com/calmerism/Tamed/releases/tag/2026.9) page:

* **[Tamed.apk](https://github.com/calmerism/Tamed/releases/download/2026.9/Tamed.apk)** (ARM64, Android 8.0+)

## Build from Source

Requires JDK 21 and the Android SDK (API 35+).

```bash
git clone https://github.com/calmerism/Tamed.git
cd Tamed
./gradlew assembleArm64Release
```

The APK will be generated at `app/build/outputs/apk/arm64/release/app-arm64-release.apk`.

## License

[GPL-3.0](LICENSE)
