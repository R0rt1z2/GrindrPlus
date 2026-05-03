# GrindrPlus Kotlin Code Quality Audit

## Metadata

| Field | Value |
|-------|-------|
| Date | 2026-05-03 |
| Audited revision | `ab57f40` (EOL commit) |
| Branch | `audit/kotlin-quality-remediation` |
| Kotlin | 2.0.0 |
| AGP | 8.10.1 |
| compileSdk | 35 / minSdk 26 |

---

## 1. Executive Summary

GrindrPlus is a Kotlin Android Xposed module that hooks into the Grindr dating application to
add location teleporting, premium feature unlocking, expiring media persistence, and other
capabilities. The project reached end-of-life in April 2026 after Grindr adopted PairIP
tamper-detection technology.

This audit identified **10 Kotlin code-quality issues** across 16 source files — crash-inducing
null assertions, swallowed `CancellationException`s, `runBlocking` calls on the main thread,
class-loading crashes at object-init time, missing input validation, and unsafe concurrent
data structures. All 10 issues have been remediated in this branch without altering the
module's intended functionality.

A separate category of **intentional security/architectural features** was identified and
documented. These are not bugs — they are the core purpose of the module — and no code was
changed for them. See Section 2.

---

## 2. Intentional Security Features (WONTFIX — By Design)

These patterns look like security issues from the outside but are the module's core features.
They are documented here for completeness and to inform anyone forking this project of the
risks they accept.

### 2.1 SSL Certificate Unpinning
- **File:** `app/src/main/java/com/grindrplus/hooks/SSLUnpinning.kt`
- **What it does:** Replaces Grindr's `X509TrustManager` with one that accepts all certificates; hooks `OkHttpClient.Builder.certificatePinner()` to no-op. Enables HTTP traffic inspection.
- **Existing mitigation:** Only activated when `BuildConfig.DEBUG` is true (gated in `XposedLoader.kt`).
- **Residual risk:** Production release builds should never distribute with DEBUG=true.

### 2.2 Location / GPS Spoofing
- **File:** `app/src/main/java/com/grindrplus/hooks/LocationSpoofer.kt`
- **What it does:** Hooks `Location.getLatitude()`, `getLongitude()`, `isMock()`/`isFromMockProvider()` to return forced coordinates from config.
- **Intentional:** Core teleport feature.

### 2.3 Premium Feature Unlocking
- **Files:** `hooks/EnableUnlimited.kt`, `hooks/FeatureGranting.kt`, `hooks/UnlockExplorer.kt`
- **What it does:** Hooks role-list update callbacks and `Feature.isGranted()` to report full subscription entitlements.
- **Intentional:** Core module purpose.

### 2.4 Unencrypted Room Database
- **File:** `app/src/main/java/com/grindrplus/persistence/GPDatabase.kt`
- **What it does:** Standard Room SQLite database at `grindrplus.db`; no SQLCipher encryption.
- **Acceptable:** Data stored (teleport locations, saved phrases, album metadata) is non-credential. On a rooted device, ADB or root shell can read it regardless.
- **Future recommendation:** SQLCipher if handling more sensitive data.

### 2.5 Unencrypted Config JSON
- **File:** `Config.kt` (persisted via `BridgeService.kt`)
- **Same threat model as 2.4.**

### 2.6 Exported AIDL Bridge Service
- **File:** `app/src/main/java/com/grindrplus/bridge/BridgeService.kt`
- **What it does:** `IBridgeService.Stub` is exported and exposes `getConfig()`, `setConfig()`, `logBlockEvent()`, `getForcedLocation()`, `sendNotification()`.
- **Acceptable:** Required for cross-process IPC between the Xposed module (running in Grindr's process) and the manager app.
- **Future recommendation:** Add a caller UID check — verify the caller is either the GrindrPlus package or the targeted Grindr package before serving requests.

### 2.7 Plaintext Log Files with Sensitive Data
- **File:** `app/src/main/java/com/grindrplus/core/Logger.kt`
- **What it does:** `writeRaw()` sends stack traces and debug info to the bridge log file. Logs may include profile IDs and geohash strings.
- **Future recommendation:** Strip or hash numeric IDs and coordinate values in release-mode log entries.

---

## 3. Code Quality Issues Fixed

### Issue 1 — Non-Null Assertions (`!!`)

Bare `!!` operators produce `KotlinNullPointerException` with no context on failure.
Replaced with `requireNotNull(...) { descriptive message }` or safe early-return patterns.

| File | Before | After |
|------|--------|-------|
| `hooks/ExpiringMedia.kt` | `classMap["key"]!!` (×4) | `requireNotNull(classMap["key"]) { "ExpiringMedia: missing key..." }` |
| `hooks/BanManagement.kt` | `args[1]!!::class.java.name` | `val arg1 = args.getOrNull(1); arg1 != null && arg1::class...` |
| `hooks/OnlineIndicator.kt` | `a!!.javaClass.getMethod(...)` | `val a = param.args()[0] ?: return@hook` |

**Lines changed:** ~18

---

### Issue 2 — `CancellationException` Swallowing

Generic `catch (e: Exception)` blocks swallow coroutine cancellation signals, preventing
structured concurrency from working correctly.

| File | Fix |
|------|-----|
| `core/TaskScheduler.kt` | Added `catch (e: CancellationException) { throw e }` before generic catch in `periodic()`, `once()`, `withRetry()` |
| `bridge/BridgeClient.kt` | Split anonymous `catch (_: Exception)` into explicit `TimeoutCancellationException` (→ false) and `CancellationException` rethrow |
| `hooks/LocalSavedPhrases.kt` | Added explicit `Dispatchers.IO` to `runBlocking()` calls; fixed unsafe `as String` cast to `as? String` with null guard |

**Lines changed:** ~22

---

### Issue 3 — `runBlocking` on Main Thread (ANR Risk)

`HookManager.registerHooks()` wrapped all hook registration in `runBlocking(Dispatchers.IO)`.
Xposed hook registration via `XposedHelpers.findAndHookMethod` is synchronous reflection that
**must run on the calling thread** — dispatching it to an IO thread via `runBlocking` is both
incorrect and ANR-risky if called during app startup.

| File | Before | After |
|------|--------|-------|
| `utils/HookManager.kt` | `runBlocking(Dispatchers.IO) { hookList.forEach { hook.init() } }` | Direct synchronous iteration with per-hook `try { hook.init() } catch (e: Throwable)` |
| `utils/HookManager.kt` | `fun reloadHooks()` with `runBlocking(Dispatchers.IO)` | Removed `runBlocking`; synchronous cleanup + re-registration |

**Lines changed:** ~30

---

### Issue 4 — `XposedHelpers.findClass()` at Object-Init Time

`XposedHelpers.findClass()` throws `ClassNotFoundError` (an `Error`, not `Exception`) when
the target class is absent. Calling it in object initializers produces
`ExceptionInInitializerError` — a non-recoverable JVM state that crashes the module.

| File | Before | After |
|------|--------|-------|
| `core/CoroutineHelper.kt` | Three `val X = XposedHelpers.findClass(...)` at object level | Three `val X: Class<*> by lazy { XposedHelpers.findClassIfExists(...) ?: error("...") }` |
| `utils/Hook.kt` | Only `findClass(name)` (throws) | Added `findClassOrNull(name): Class<*>?` using `runCatching` |

All hooks that call `findClass()` with obfuscated class names (e.g. `"gc0.b"`, `"vn.a"`) are
now protected by the per-hook `catch (e: Throwable)` wrapper added to `HookManager` in Issue 3.

**Lines changed:** ~25

---

### Issue 5 — Missing Input Validation

| File | Validation Added |
|------|-----------------|
| `hooks/LocationSpoofer.kt` | Coordinate bounds (lat ∈ [-90,90], lon ∈ [-180,180]), name length ≤ 100, blank name rejection |
| `hooks/LocalSavedPhrases.kt` | Blank phrase rejection, phrase length capped at 1000 chars |
| `hooks/StatusDialog.kt` | Canonical path check in `clearDirectoryContents()` to prevent symlink-based path traversal on rooted devices |

A testable pure function `validateCoordinates(lat, lon, name)` was extracted to
`core/CoordinateValidator.kt` and wired into `LocationSpoofer.saveLocation()`.

**Lines changed:** ~35

---

### Issue 6 — TaskScheduler Thread Safety + `isActive` Guard

| File | Before | After |
|------|--------|-------|
| `core/TaskScheduler.kt` | `private val runningJobs = mutableMapOf<String, Job>()` | `ConcurrentHashMap<String, Job>()` |
| `core/TaskScheduler.kt` | `while (true)` in `periodic()` | `while (isActive)` — exits cleanly on scope cancellation |

**Lines changed:** 2

---

### Issue 7 — Static Context References (Pre-Existing Correct Implementation)

`GrindrPlus.currentActivity` was already backed by `WeakReference<Activity>` in the original
code. The `context` field holds the `Application` context (process-scoped, no leak risk). A
comment was added to document this invariant and suppress the lint warning intentionally.

**Lines changed:** 2 (comment only)

---

### Issue 8 — Continuation Hook: `CancellationException` Propagation

`SuspendResultUtils.withSuspendResult` hooks `invokeSuspend` to intercept async results, but
did not check whether the incoming result represents a cancelled coroutine. If the coroutine
was cancelled, calling `onResult` on a failure result could corrupt state.

| File | Fix |
|------|-----|
| `utils/SuspendResultUtils.kt` | Added `extractCancellationException()` helper; early-return from hook if incoming result is a `kotlin.Result$Failure` wrapping `CancellationException`. Wrapped `onResult` in try-catch so unhook still runs on failure. |

**Lines changed:** ~35

---

### Issue 9 — Silent Hook Registration Failure in `XposedLoader`

`spoofSignatures()` and `sslUnpinning()` were bare calls with no error handling. If the
target Firebase/Facebook classes are absent (different Grindr build variant), a
`ClassNotFoundError` aborts the entire module before `GrindrPlus.init()` runs.

| File | Fix |
|------|-----|
| `XposedLoader.kt` | Wrapped both calls in `try { ... } catch (e: Throwable) { Log.e(...) }` |

**Lines changed:** ~10

---

### Issue 10 — Test Coverage

No test source set existed. Added:

| File | Tests |
|------|-------|
| `test/.../CoordinateValidatorTest.kt` | 12 tests: boundary values, null inputs, blank/long names |
| `test/.../TaskSchedulerBehaviourTest.kt` | 4 tests: `isActive` guard, `CancellationException` propagation, retry logic, `ConcurrentHashMap` thread safety |

`build.gradle.kts` additions:
```
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
```

---

## 4. Build Verification

```bash
# Kotlin compilation
./gradlew compileDebugKotlin

# Full APK build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Confirm !! operators removed from changed files
grep -n "!!" app/src/main/java/com/grindrplus/hooks/ExpiringMedia.kt   # expect: none
grep -n "!!" app/src/main/java/com/grindrplus/hooks/BanManagement.kt   # expect: none (inside comments/strings only)
grep -n "!!" app/src/main/java/com/grindrplus/hooks/OnlineIndicator.kt # expect: none

# Confirm runBlocking removed from HookManager
grep -n "runBlocking" app/src/main/java/com/grindrplus/utils/HookManager.kt  # expect: none
```

---

## 5. Remaining Technical Debt (Out of Scope for this Audit)

| Item | Risk | Notes |
|------|------|-------|
| Obfuscated class names (`gc0.b`, `vn.a`, etc.) | High | Break with every Grindr update; no automated compat test |
| `Thread.sleep()` in `Client.kt` `blockUser`/`unblockUser` | Medium | Blocking sleep on an IO coroutine thread |
| No Grindr version compatibility matrix | Medium | No CI gate to detect Grindr obfuscation changes |
| No in-place database migration strategy | Low | Room schema changes require destructive migration |
| `BridgeService` caller UID verification | Low | See Section 2.6 |
| PII redaction in log files | Low | See Section 2.7 |

---

## 6. Files Modified Summary

| File | Change |
|------|--------|
| `hooks/ExpiringMedia.kt` | Replace `!!` with `requireNotNull` |
| `hooks/BanManagement.kt` | Replace `args[1]!!` with safe access |
| `hooks/OnlineIndicator.kt` | Replace `a!!` with null guard |
| `core/TaskScheduler.kt` | `CancellationException` rethrow, `isActive`, `ConcurrentHashMap` |
| `bridge/BridgeClient.kt` | Split catch blocks for `TimeoutCancellationException` vs `CancellationException` |
| `hooks/LocalSavedPhrases.kt` | Safe cast, blank check, `Dispatchers.IO` in `runBlocking` |
| `utils/HookManager.kt` | Remove `runBlocking`, add per-hook `Throwable` catch |
| `core/CoroutineHelper.kt` | Move class lookups to `by lazy` with `findClassIfExists` |
| `utils/Hook.kt` | Add `findClassOrNull()` |
| `hooks/LocationSpoofer.kt` | Wire `validateCoordinates()` into `saveLocation()` |
| `hooks/StatusDialog.kt` | Add canonical path guard in `clearDirectoryContents` |
| `utils/SuspendResultUtils.kt` | `CancellationException` propagation, `onResult` try-catch |
| `XposedLoader.kt` | `Throwable` catch around `spoofSignatures` and `sslUnpinning` |
| `GrindrPlus.kt` | Document `applicationContext` invariant |
| `app/build.gradle.kts` | Add test dependencies |
| `core/CoordinateValidator.kt` | New: pure coordinate validation function |
| `test/.../CoordinateValidatorTest.kt` | New: 12 unit tests |
| `test/.../TaskSchedulerBehaviourTest.kt` | New: 4 coroutine behaviour tests |
