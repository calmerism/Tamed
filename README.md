<div align="center">
  <img src="TamedMask.png" width="128" alt="Tamed Logo" /><br/>
  <b><font size="6">Tamed</font></b>
  <p>An elegant, modern Android music application featuring Apple Music-inspired ambient backdrops, time-synced lyrics, and high-fidelity lossless audio playback.</p>

  <p>
    <a href="https://github.com/calmerism/Tamed/releases/tag/0.1.0"><img src="https://img.shields.io/badge/version-0.1.0-blue.svg?style=for-the-badge" alt="Version" /></a>
    <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/platform-Android-brightgreen.svg?style=for-the-badge" alt="Platform" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-orange.svg?style=for-the-badge" alt="License" /></a>
    <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/kotlin-2.0-purple.svg?style=for-the-badge" alt="Kotlin" /></a>
  </p>
</div>

### Overview

Tamed is a modern music player for Android designed with a focus on visual aesthetics and audio fidelity. It offers dynamic user interface color extraction, synchronized lyric display, and native playback of lossless audio formats.

### Key Features

#### Ambient Visuals
* **Palette Extraction**: Automatically extracts color profiles from album art to theme the interface dynamically.
* **Animated Backdrops**: Renders smooth, fluid radial gradient animations behind player controls and content views.

#### Synchronized Lyrics
* **Time-Synced Display**: Support for line-by-line and word-by-word scrolling utilizing LRC and TTML formats.
* **Plain Lyrics**: Clear, highly readable text presentation for tracks without timing metadata.

#### High-Fidelity Audio Playback
* **Lossless Formats**: Native support for FLAC, AIFF, WAV, ALAC, APE, WavPack, and DSD.
* **Standard Formats**: Compatibility with M4A, AAC, MP3, OGG, OPUS, WMA, MKV, and AMR.
* **Tag Metadata**: Automatic parsing of embedded ID3, Vorbis, and MP4 tags from local files.

#### Interface
* **Jetpack Compose**: Modern declarative UI built using Material 3 guidelines.
* **Responsive Layouts**: Designed with smooth transitions, custom loading shimmers, and interactive elements.

### Tech Stack

* **UI Framework**: Android Jetpack Compose, Material 3
* **Audio Engine**: AndroidX Media3 (ExoPlayer) with MediaSession
* **Image Loading**: Coil 3
* **Color Processing**: Android Palette library
* **Dependency Injection**: Hilt / Dagger

### Installation

The latest pre-compiled ARM64 binary can be downloaded from the Releases page:

* **Package**: [Tamed.apk](https://github.com/calmerism/Tamed/releases/download/0.1.0/Tamed.apk)
* **Architecture**: arm64-v8a
* **Requirements**: Android 8.0 (API Level 26) or higher

### Build Instructions

To build the project from source:

1. Clone the repository:
   ```bash
   git clone https://github.com/calmerism/Tamed.git
   cd Tamed
   ```

2. Build the release binary:
   ```bash
   ./gradlew :app:assembleArm64Release
   ```

3. The generated APK will be located at:
   `app/build/outputs/apk/arm64/release/Tamed.apk`

### License

This project is licensed under the GPL-3.0 License. See the `LICENSE` file for details.
