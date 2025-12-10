# Architecture Overview

This is a high-level map of the GrindrPlus module so new contributors can orient quickly. It reflects the v4.7.0 update that targets Grindr 25.20.0 (build 147239).

## Modules
- **Xposed/LSPatch module (`app/`)** – Hooks into the Grindr app. Entry points: `XposedLoader` (for LSPosed) and `GrindrPlus` singleton.
- **Manager UI (`com.grindrplus.manager.*`)** – Jetpack Compose app that downloads, patches, signs, and installs the Grindr APK when running in LSPatch/no-root mode. Also exposes settings, logs, and telemetry toggles.
- **Bridge (`com.grindrplus.bridge.*`)** – Thin IPC layer used by the manager to communicate with the injected module for config and status reporting.
- **Persistence (`com.grindrplus.persistence.*`)** – Room database for saved phrases, albums, teleport locations, and block logs.
- **Core (`com.grindrplus.core.*`)** – Shared utilities (config, logging, scheduler, HTTP client/interceptor) plus `InstanceManager` that captures obfuscated instances from Grindr (user session, user agent, device info, location provider).
- **Hooks (`com.grindrplus.hooks.*`)** – Individual features toggled by the manager UI. Each hook focuses on a small surface (unlimited profiles, disable boosting, spoof Android ID, etc.).
- **Tasks (`com.grindrplus.tasks.*`)** – Background jobs (e.g., AlwaysOnline heartbeat).

## Hook lifecycle
1) `XposedLoader`/LSPatch entry loads `GrindrPlus`.
2) `GrindrPlus.init` checks the installed Grindr version against `BuildConfig.TARGET_GRINDR_VERSION_*`.
3) The manager copies the module DEX into app storage; `DexClassLoader` loads the module, then `InstanceManager` hooks constructors of obfuscated Grindr classes to capture live instances.
4) `HookManager` registers all hooks. Each hook uses helper methods in `com.grindrplus.utils` to attach before/after method hooks or constructor hooks.
5) HTTP calls are intercepted via `core/http/Interceptor.kt` to reuse Grindr auth tokens, spoof user agent/device info, and inject feature flags.
6) Bridge service exposes basic state (versions, Android ID spoof, forced location) and writes logs to the manager UI.

## Key targets (25.20.0)
- **User session**: `com.grindrapp.android.usersession.b` (`isLoggedIn t()`, token `x()/getValue()`, roles `F()`).
- **User agent**: `Pb.e`.
- **Device info**: `u8.u` (lazy `d -> getValue`).
- **Location provider**: `ff.e`.
- **HTTP interceptor**: updated to the new token/roles flow.
- **Paywall/restart**: `dk.c::d(...)`.
- **Interstitial ads**: `fa.A$a$a`, `fa.p0$a$a`.
- **Notification reminder**: `ue.e::a`.
- **Shuffle/Boost/Albums/Phrases**: remapped to new obfuscation classes per changelog.
- **android_id spoof**: hook `Settings.Secure.getString(..., "android_id")`.
- **WebSocket**: cache token in `WebSocketClientImpl.b(...)` and reuse on reconnect.

## Build/test
- Gradle 8.11.1, JDK 17, Compose BOM 2025.02.
- Build: `./gradlew --no-daemon assembleRelease`
- Target SDK 34; min SDK 26.
- Release output: `app/build/outputs/apk/release/GPlus_v<version>-<variant>.apk`

## Release/versioning
- `app/build.gradle.kts` encodes the supported Grindr version list (`TARGET_GRINDR_VERSION_NAMES/CODES`) and the module version (`versionCode`, `versionName`).
- `version.json` tracks the latest scraped Grindr version.
- `changelog.md` tracks hook remaps and noteworthy behavioral changes.
