<div align="center">
  <img src="Tamed (Stable)/TamedMask.png" width="120" alt="Tamed" />
  <h1>Tamed</h1>
  <p>A beautiful YouTube Music client for Android with two flavors: <b>Stable</b> and <b>Lossless (Experimental)</b></p>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat&logo=kotlin&logoColor=white" />
  <img alt="Version" src="https://img.shields.io/badge/Version-0.0.6-FF9900?style=flat" />
  <img alt="License" src="https://img.shields.io/github/license/calmerism/Tamed?style=flat" />
</div>

## Two Applications

This repository contains two parallel applications that serve different needs, both hosted side-by-side in a unified monorepo structure:

1. **Tamed (Stable) `v0.0.6`** - The core application focused on reliability, streaming, and beautiful UI.
2. **Tamed (Lossless) `v0.0.6.1`** - An experimental branch exploring lossless audio capabilities and unique download structures. Please note that this version is highly experimental and not fully stable; for example, lossless songs can load slowly because the app performs complex real-time search, matching, and high-fidelity audio resolution under the hood.

---

## 🚀 Recent Updates (v0.0.6 & v0.0.6.1)

- **High-Res Artwork Fixes**: Rewrote the YouTube thumbnail fetching logic to ensure the highest quality artwork is always displayed. Fixed pixelated album art in the media player background, Now Playing screen, and Android system notifications by strictly preventing Coil downsampling.
- **Canvas Squeeze Fix**: Completely resolved the video squeeze issue for Apple Music Canvas backgrounds. The player now dynamically responds to video size changes, correctly calculates aspect ratios on the fly, and uses `SCALE_TO_FIT_WITH_CROPPING` for a flawless full-screen experience.
- **Download Indicators**: Added a robust fallback mechanism for download badges in lists and grids so you can actually see what songs are saved locally without relying on live `DownloadManager` states.
- **Streamlined Build System**: Removed redundant ABI splits. The project now generates a single, clean **Universal APK** that works on all Android architectures out of the box.

---

## Requirements

- Android **8.0 (API 26)** or higher
- A YouTube Music account *(optional, but recommended for full features)*

## Building from Source

**Prerequisites:** Android Studio Ladybug (2024.2.1+) · JDK 17 · Android SDK API 36

```bash
git clone https://github.com/calmerism/Tamed.git
cd Tamed
```

### Build the Stable Version:
```bash
cd "Tamed (Stable)"
./gradlew buildReleaseApk
```
*Output:* `Tamed (Stable)/release/Tamed.apk`

### Build the Lossless Version:
```bash
cd "Tamed (Experimental)"
./gradlew buildReleaseApk
```
*Output:* `Tamed (Experimental)/release/Tamed (Lossless).apk`

---

## Tech Stack

| | |
|---|---|
| Language | Kotlin 2.3 |
| UI | Jetpack Compose · Material 3 |
| Playback | Media3 / ExoPlayer |
| Architecture | MVVM · Clean Architecture |
| Database | Room |
| DI | Hilt |
| Networking | Retrofit · InnerTube API |
