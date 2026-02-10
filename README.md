# babylon-wallet-android

An Android wallet for interacting with the Radix DLT ledger.

## App requirements

- [Android 8.1](https://developer.android.com/about/versions/oreo/android-8.1) (API 27) minimum
- Handsets only (tablets are not supported)

## Getting started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Environment variables `GPR_USER` and `GPR_TOKEN` for GitHub Packages access (required for the [Sargon](https://github.com/radixdlt/sargon) dependency)

### Clone and build

```bash
git clone https://github.com/radixdlt/babylon-wallet-android.git
cd babylon-wallet-android

# Light flavor (no private submodule access required)
./gradlew assembleLightDebug

# Full flavor (requires the Arculus CSDK submodule)
git submodule update --init --recursive
./gradlew assembleFullDebug
```

## Build variants

The project uses a **`version`** flavor dimension with two product flavors:

| Flavor  | Description |
|---------|-------------|
| `full`  | Complete app with all features and 3rd-party integrations: AppsFlyer analytics, Firebase Crashlytics, and Arculus NFC card support. Requires access to the private `arculus-android-csdk` submodule. |
| `light` | Stripped-down app without AppsFlyer, Firebase Crashlytics, or Arculus. Can be built without access to any private dependencies or submodules. |

Combined with the existing build types, the available variants are:

| | `debug` | `debugAlpha` | `release` | `releasePreview` |
|---|---|---|---|---|
| **full** | `fullDebug` | `fullDebugAlpha` | `fullRelease` | `fullReleasePreview` |
| **light** | `lightDebug` | `lightDebugAlpha` | `lightRelease` | `lightReleasePreview` |

### Build type differences

| Build type | Minified | Debuggable | Crash reporting | App name suffix |
|------------|----------|------------|-----------------|-----------------|
| `debug` | No | Yes | No | Dev |
| `debugAlpha` | Yes | No | Yes (full only) | Alpha |
| `release` | Yes | No | Yes (full only) | — |
| `releasePreview` | Yes | No | Yes (full only) | Preview |

> **Note:** `CRASH_REPORTING_AVAILABLE` is always `false` for all `light` variants, regardless of the build type.

## Project structure

| Module | Description |
|--------|-------------|
| `app` | Main application module containing UI, navigation, ViewModels, and use cases. |
| `core` | Shared library module with Sargon drivers, extensions, DI providers, and data stores. |
| `profile` | Profile management, Google Drive backup, and cloud sync via WorkManager. |
| `designsystem` | Reusable Compose UI components and theming. |
| `peerdroid` | WebRTC peer-to-peer communication for dApp connector links. |
| `webrtc-library` | Bundled WebRTC AAR. |
| `arculus-android-csdk:csdknative` | Arculus NFC card native JNA bindings (private submodule, optional for `light` builds). |

## Architecture

The architecture is based on the [Android app architecture](https://developer.android.com/topic/architecture) guide, incorporating principles from [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html).

Key principles:

- **Data** and **presentation** are the primary layers.
- Each layer owns its data models; mapping occurs at layer boundaries.
- Business logic lives in the model (e.g. `FungibleResource`) only when directly relevant to that model.
- The data layer follows the [repository pattern](https://developer.android.com/topic/architecture/data-layer).
- The **domain layer** is optional and used when it provides clear value — such as combining data from multiple repositories or encapsulating reusable business logic. See the [Android docs on the domain layer](https://developer.android.com/topic/architecture/domain-layer) for guidance.
- **MVVM** pattern for the presentation layer using Jetpack Compose.

## Tech stack

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — declarative UI
- [Material 3](https://m3.material.io/) — design system
- [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) — REST networking
- [Ktor](https://ktor.io/) — WebSocket connections
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) — dependency injection
- [Coil](https://coil-kt.github.io/coil/) — image loading
- [Room](https://developer.android.com/training/data-storage/room) — local database
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) — preferences and encrypted storage
- [Detekt](https://detekt.dev/) + plugins (formatting, [compose rules](https://twitter.github.io/compose-rules/)) — static code analysis
- [JaCoCo](https://www.eclemma.org/jacoco/) — code coverage
- [Sargon](https://github.com/radixdlt/sargon) — Radix core logic and cryptography

We favour native/first-party libraries and keep third-party dependencies to a minimum.

## Dependencies

- Version catalog: `gradle/libs.versions.toml`
- Update all dependencies: `./gradlew versionCatalogUpdateLibraries`
- Interactive update: `./gradlew versionCatalogUpdateLibraries --interactive` — generates a diff file. Review and remove any exclusions, then apply with `./gradlew versionCatalogApplyUpdatesLibraries`.
- Dependency lock file: `dependencies.lock` — run `./gradlew compareDependencies` to verify, or `./gradlew generateDependenciesLockFile` to regenerate.

## Best practices

- [Keep It Simple](https://imageio.forbes.com/specials-images/imageserve/6141f431cb79cea26593300b/Shortcut-From-Point-A-to-Point-B-Concept/960x0.jpg?format=jpg&width=960) — favour clarity over cleverness.
- Write your code, leave it for a week, come back and read it. If you can't understand it in under a minute, you're probably overengineering.
- Comments are helpful.
- Watch this [talk on composition vs. inheritance](https://www.youtube.com/watch?v=OMPfEXIlTVE) — it helps you decide when to (not) use abstraction.
- Read the [conventions](https://github.com/radixdlt/babylon-wallet-android/blob/main/docs/Conventions.md) doc.

### Useful Kotlin resources

- [Value classes](https://quickbirdstudios.com/blog/kotlin-value-classes/)
- [Sealed interfaces](https://quickbirdstudios.com/blog/sealed-interfaces-kotlin/)

## License

The Android Radix Wallet binaries are licensed under the [Radix Wallet Software EULA](https://www.radixdlt.com/terms/walletEULA).

The Android Radix Wallet code is released under the [Apache 2.0 license](./LICENSE).

```
Copyright 2023 Radix Publishing Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.

You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.
```
