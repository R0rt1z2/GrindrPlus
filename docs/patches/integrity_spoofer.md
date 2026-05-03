# Integrity Spoofer

Hooks the Google Play Integrity API client-side model classes and the native
library loader to interfere with Grindr's PairIP tamper-detection mechanism.

## What PairIP does

PairIP is Grindr's runtime integrity check (adopted ~April 2026). It runs in two layers:

1. **Native layer** — `libpairip.so` / `libpairipcore.so` load at startup, perform
   environment checks (root, frida, debugger, LSPatch presence), and compute an attestation
   payload before the JVM initializes. Xposed/LSPatch hooks cannot intercept this layer.

2. **Java layer** — After native evaluation, results are surfaced via the Google Play
   Integrity API. The client calls `IntegrityManager.requestIntegrityToken()`, and Google
   returns a signed JWT. Grindr reads the decoded verdict fields to decide whether to allow
   the session.

## Current approach

### NativeLibraryGuard (primary)

Hooks `Runtime.loadLibrary0`, `System.loadLibrary`, and `System.load` to block the native
library from loading at all. If PairIP's `.so` never loads, it cannot perform its checks.

Blocked names (case-insensitive substring match): `pairip`, `pairipcore`, `integrity_checker`

The hook throws `SecurityException` rather than calling `setResult(null)` (which is a no-op
on void methods). The `SecurityException` surfaces as `UnsatisfiedLinkError` to the caller;
Grindr typically catches and logs it.

### IntegritySpoofer (secondary)

If native blocking fails and Grindr falls back to the Java Integrity API, `IntegritySpoofer`
patches the decoded verdict model objects to return favourable values:

- `IntegrityTokenResponse.token()` → structurally valid `alg:none` JWT
- `AppIntegrity.getAppRecognitionVerdict()` → `"PLAY_RECOGNIZED"`
- `DeviceIntegrity.getDeviceRecognitionVerdict()` → `["MEETS_DEVICE_INTEGRITY", "MEETS_STRONG_INTEGRITY"]`
- `AccountDetails.getAppLicensingVerdict()` → `"LICENSED"`

> **Limitation:** The raw JWT token is validated server-side by Google before Grindr receives
> it. Only client-side parsing of the decoded fields benefits from this hook. If Grindr sends
> the raw token to its own backend for secondary validation, this hook provides no benefit.

## Diagnosing effectiveness

Enable `TimberLogging` hook, install via LSPatch, then watch logcat for:

```
GrindrPlus: Blocked anti-tampering library (loadLibrary0): pairip
GrindrPlus: Blocked anti-tampering library (System.load): /data/.../libpairipcore.so
```

If those messages appear and Grindr remains functional for >60 s, the native block is working.

If Grindr force-logs-out immediately after the native block, PairIP has a secondary Java-layer
check — verify that `IntegritySpoofer` hook hits are appearing for `IntegrityTokenResponse`.

## Updating after a Grindr version bump

PairIP's library names are stable. The Java-layer class names may change if Grindr updates
its Play Core dependency. Search the new APK's smali for:

- `com/google/android/play/core/integrity/IntegrityTokenResponse`
- `com/google/android/play/core/integrity/model/AppIntegrity`
- `com/google/android/play/core/integrity/model/DeviceIntegrity`
- `com/google/android/play/core/integrity/model/AccountDetails`

If those classes are obfuscated, search for the string `"requestIntegrityToken"` in smali to
find the call site, then trace the return type.
