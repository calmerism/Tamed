<div align="center">
  <img src="Tamed (Stable)/TamedMask.png" width="120" alt="Tamed" />
  <h1>Tamed</h1>
  <p>A beautiful YouTube Music client for Android with two flavors: <b>Stable</b> and <b>Lossless (Experimental)</b></p>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat&logo=kotlin&logoColor=white" />
  <img alt="License" src="https://img.shields.io/github/license/calmerism/Tamed?style=flat" />
</div>

## Two Applications

This repository contains two parallel applications that serve different needs:

1. **Tamed (Stable)** - The core, stable application focused on reliability, streaming, and beautiful UI.
2. **Tamed (Lossless)** - An experimental branch exploring lossless audio capabilities and unique download structures.

## Requirements

- Android **8.0 (API 26)** or higher
- A YouTube Music account *(optional, but recommended for full features)*

## Building from Source

**Prerequisites:** Android Studio Ladybug (2024.2.1+) · JDK 17 · Android SDK API 36

```bash
git clone https://github.com/calmerism/Tamed.git
cd Tamed

# Build the Stable version:
cd "Tamed (Stable)"
./gradlew assembleUniversalRelease

# Or build the Lossless version:
cd "../Tamed (Experimental)"
./gradlew assembleUniversalRelease
```

The APK will be output to `app/build/outputs/apk/universal/release/app-universal-release.apk` inside the respective folder.

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
