# iOS Port Roadmap

This repository can be moved toward iOS, but full parity requires more than enabling a new target.

## Current Shared Layer Status

These modules are now Kotlin Multiplatform-ready and can be consumed by a future iOS app:

- `lrclib`
- `simpmusic`
- `kugou`
- `lastfm`
- `canvas`
- `betterlyrics`
- `innertube` shared parser/auth layer plus a portable executor for Home, Search, albums, artists, and playlists
- `shazamkit`
- `shared` iOS umbrella framework and root UI shell

These are good candidates for the next multiplatform passes, but still contain platform-specific code today:

- `innertube`

## Remaining Shared-Layer Blockers

### `innertube`

This module is now partially split for multiplatform use.

- Shared today: serialized models, response bodies, page parsers, playback auth state, cookie parsing, SHA-1 fingerprinting, po-token generation, request metadata/header generation, request specs, both read-side and write-side request body builders, and a portable Ktor-backed executor for Home, Search, albums, artists, playlists, and new releases
- Still Android-only: `InnerTube` request execution, `YouTube`, proxy handling, concrete transport wiring, and `NewPipe`
- Remaining work: replace the Android request executor/proxy layer, expand the portable executor into more account/library mutations and playback-oriented flows, and split parsing/orchestration from `YouTube` so iOS can call the shared parser/auth/request stack directly

### `shazamkit`

This is now Kotlin Multiplatform-ready.

- Shared today: recognition API client, request/response models, signature generation, FFT/binary encoding helpers
- Remaining future work is product-level integration, not core portability

## What "Exact UI + All Features" Actually Means

To match the Android app on iPhone, we still need all of the following:

1. Shared domain/data layer
- Finish converting reusable networking, parsing, and repository code to multiplatform.
- Remove JVM-only dependencies from modules that must also run on iOS.

2. iOS app shell
- Create an iOS app target and project structure.
- Add dependency wiring for the shared Kotlin modules.
- Set up signing, bundle identifiers, app assets, and build configs.

3. Playback parity
- Replace Android `Media3/ExoPlayer` behavior with `AVPlayer`.
- Rebuild queue handling, seek behavior, repeat/shuffle, and playback state syncing.
- Add lock screen controls, Now Playing metadata, and remote command handling.

4. Storage and offline behavior
- Replace Room/DataStore patterns with iOS-friendly persistence.
- Rebuild downloads, cache policy, and media file management for iOS sandbox rules.

5. UI parity
- Rebuild every screen and component for iOS.
- Match player layout, library views, menus, dialogs, settings, and navigation behavior.
- Recreate motion, artwork handling, gestures, and screen transitions.

6. Platform integrations
- Notifications and background audio behavior
- Share sheet and external links
- Deep links
- Audio session handling
- Permissions and background task constraints

## Recommended Build Order

1. Convert more service modules to Kotlin Multiplatform.
2. Extract a shared repository/domain layer above those modules.
3. Create the iOS app shell.
4. Implement playback and queue management on iOS.
5. Rebuild the player UI first, then library/home/search/settings.
6. Finish downloads, background behavior, and platform polish.

## Practical Next Targets

The next highest-value work is now:

1. Finish `innertube` executor and `YouTube` portability
2. Wire the native iOS target/project to the new `shared` framework shell
3. Implement native playback and downloads
4. Replace the placeholder shared shell with real screen-by-screen UI parity

## iOS App Shell Progress

The repository now contains a real iOS-facing shell:

- `shared` exports the portable modules as a static iOS framework
- `shared` includes a root Compose UI shell with home, search, library, and player surfaces plus `TamedIosBridge`
- the shell now drives live Home and Search requests through the new portable `innertube` path while keeping fixture fallbacks
- Home chips and Search suggestions/sections now come from the shared `innertube` layer, so the shell is starting to behave like a real app instead of a static mock
- Home now exposes live new releases and a real album detail flow backed by the shared `innertube` executor
- live rows can now open shared artist and playlist detail surfaces from the same portable `innertube` stack
- `iosApp/` now includes a real `Tamed.xcodeproj`, native audio-session bootstrap, and lock-screen/remote-command playback scaffolding around the shared root controller

This is not full feature parity yet, but it is the first actual app shell rather than only shared service modules.

## Latest Verification

The current shared-layer status is verified by:

- `./gradlew --no-daemon --console=plain :innertube:compileKotlinIosSimulatorArm64`
- `./gradlew --no-daemon --console=plain :shared:compileKotlinIosSimulatorArm64`
- `./gradlew --no-daemon --console=plain :app:compileUniversalDebugKotlin`

At this point, the work has largely shifted from shared-core migration to the actual iOS product:
