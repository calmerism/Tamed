<div align="center">

<img src="TamedMask.png" width="128" alt="Tamed Logo" /><br/>

# Tamed

**An elegant, modern Android music application featuring Apple Music-inspired ambient backdrops, time-synced lyrics, and high-fidelity lossless audio playback.**

[![Version](https://img.shields.io/badge/version-0.1.0-blue.svg?style=for-the-badge)](https://github.com/calmerism/Tamed/releases/tag/0.1.0)
[![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg?style=for-the-badge)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-GPL--3.0-orange.svg?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0-purple.svg?style=for-the-badge)](https://kotlinlang.org/)

---

</div>

## Key Features

### Apple Music Ambient Backdrop & Dynamic Glow
- **Intelligent Palette Extraction**: Automatically extracts primary, secondary, and tertiary vibrant color accents directly from album artwork and artist media.
- **Fluid Ambient Glow**: Seamless animated radial gradients that adapt smoothly behind player controls, album pages, and artist screens.

### Synchronized & Plain Lyrics
- **Karaoke Time-Synced Lyrics**: Line-by-line and word-by-word synchronized scrolling powered by LRC and TTML formats with active phrase highlighting.
- **Enhanced Plain Text Lyrics**: High-readability typography and layout matching the Karaoke design for unsynced tracks.

### Lossless & Multi-Format Local Playback
- **High-Resolution Lossless Support**: Native bit-perfect playback for **FLAC**, **AIFF**, **WAV**, **ALAC**, **APE**, **WavPack**, and **DSD**.
- **Universal Local Format Compatibility**: Full support for **M4A**, **AAC**, **MP3**, **OGG**, **OPUS**, **WMA**, **MKV**, and **AMR**.
- **Embedded Tag Parsing**: Reads ID3, Vorbis, and MP4 metadata directly from local media files.

### Modern Jetpack Compose UI
- Built with **Jetpack Compose** and **Material 3 Expressive Design System**.
- Premium glassmorphism UI components, fluid player expand/collapse transitions, and custom animated shimmer placeholders.
- Built-in equalizer band controls and audio normalization.

---

## Tech Stack & Dependencies

- **UI Framework**: Android Jetpack Compose, Material 3 Expressive
- **Media Engine**: AndroidX Media3 (ExoPlayer) with MediaSession
- **Image & Color Processing**: Coil 3 & Android Palette
- **Architecture**: Clean Architecture, Android Architecture Components (ViewModel, StateFlow, Coroutines)
- **Dependency Injection**: Hilt / Dagger

---

## Download & Installation

Download the latest ARM64 APK from the [Releases](https://github.com/calmerism/Tamed/releases/tag/0.1.0) section:

- **Filename**: `Tamed.apk`
- **Architecture**: `arm64-v8a`
- **Requires**: Android 8.0 (API Level 26) or higher

---

## Building From Source

To build **Tamed** locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/calmerism/Tamed.git
   cd Tamed
   ```

2. Build the ARM64 debug/release APK using Gradle:
   ```bash
   ./gradlew :app:assembleArm64Release
   ```

3. The compiled APK will be output to:
   `app/build/outputs/apk/arm64/release/Tamed.apk`

---

## License

Distributed under the GPL-3.0 License. See `LICENSE` for details.
