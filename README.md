<p align="center">
  <img src="./docs/readme/cover.png" alt="Raydroid cover" width="100%" />
</p>

<p align="center">
  <strong>Raydroid</strong><br />
  A minimal, plugin-first command surface built with Kotlin Multiplatform.
</p>

<p align="center">
  Android · iOS · Desktop
</p>

## Overview

Raydroid is a cross-platform command launcher and runtime with a shared search core, shared plugin system, and native shells per platform. It is built around one idea: type once, surface actions fast, and let plugins own the experience.

## Highlights

- Search-first UI with inline results, fullscreen plugin views, context actions, and overlays
- Shared plugin runtime powered by Zipline with permission-gated host bridges
- Built-in commands for calculator, files, contacts, notes, and weather
- iOS integration for Shortcuts and Spotlight indexing
- Shared architecture across Android, iOS, and desktop modules

## Stack

- Kotlin 2.3.10
- Compose Multiplatform + Material 3
- SwiftUI shell for iOS
- Koin for dependency injection
- Zipline for plugin execution

## Quick Start

```bash
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
```

For iOS, open [`iosApp`](./iosApp) in Xcode and run the app from there.

## Project Layout

- [`androidApp`](./androidApp) Android entry point and packaging
- [`sharedUi`](./sharedUi/src) shared Compose UI for Android and desktop
- [`iosApp`](./iosApp) native iOS shell and integrations
- [`desktopApp`](./desktopApp) desktop target
- [`feature/search`](./feature/search) command search feature and view model flow
- [`plugin`](./plugin) plugin API, host bridges, and built-in plugins
- [`core`](./core) shared data, domain, model, and design system modules
- [`sharedLogic`](./sharedLogic/src) common cross-platform non-UI utilities

## Android Release Signing

Local release builds default to the committed test keystore at [`androidApp/signing/test-release.keystore`](./androidApp/signing/test-release.keystore). Real signing credentials can be supplied through environment variables, Gradle properties, or `local.properties`.

See [`androidApp/signing/README.md`](./androidApp/signing/README.md) for exact keys and setup.
