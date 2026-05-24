  <img src="TamedMask.png" width="120" alt="Tamed" />
  <h1>Tamed</h1>
  <p>A YouTube Music client for Android</p>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat&logo=kotlin&logoColor=white" />
  <img alt="License" src="https://img.shields.io/github/license/calmerism/Tamed?style=flat" />

## Requirements

- Android **8.0 (API 26)** or higher
- A YouTube Music account *(optional, but recommended for full features)*


## Building from Source

**Prerequisites:** Android Studio Ladybug (2024.2.1+) · JDK 17 · Android SDK API 36

```bash
git clone https://github.com/calmerism/Tamed.git
cd Tamed
./gradlew assembleDebug
```

The APK will be output to `app/build/outputs/apk/debug/`.


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
