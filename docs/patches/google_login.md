# Google Login (LSPatch)

Google Sign-In is a known-broken flow under LSPatch (non-root). This document explains
why, what partial mitigations exist, and the recommended workarounds.

## Why it breaks

When Grindr authenticates via Google, it delegates to Google Mobile Services (GMS), which
runs in a **separate process** (`com.google.android.gms`). GMS verifies the calling app's
authenticity by asking PackageManagerService (PMS) for the calling package's signing
certificate, then comparing its SHA-256 fingerprint against the fingerprint registered in
the Google Cloud Console for Grindr's OAuth client ID.

Under LSPatch (non-root), the APK is re-signed with a new key. PMS always returns the
**re-signed** certificate. GMS receives the wrong fingerprint and rejects the login with
`10 DEVELOPER_ERROR`.

**Why Xposed hooks cannot fix this directly:**
- GMS calls PMS via Android Binder IPC **from its own process**
- LSPatch/Xposed hooks only apply within a single app process
- Hooking `PackageManager.getPackageInfo()` in Grindr's process does not intercept GMS's
  cross-process call to PMS

**What LSPatch does help with:**
- LSPatch's native `liblspatch.so` hooks `PackageManager.getPackageInfo()` inside Grindr's
  own process, so in-app signature checks (Firebase, Facebook, etc.) work fine
- Cross-process GMS auth is not covered

## Current state of mitigation

`SignatureSpoofer.kt` already handles:
- Firebase fingerprint (`getFingerprintHashForPackage`) — ✅ works
- Facebook login → browser redirect — ✅ works
- GCM/FCM package name spoofing — ✅ works

Google Sign-In via GMS — ❌ **cannot be fixed with user-space hooks**

## Recommended workarounds

### Option 1 — Use LSPosed (root/Magisk) instead of LSPatch

If the device is rooted, install the module via LSPosed rather than LSPatch. LSPosed uses
Magisk to patch PMS system-wide, so GMS receives the original certificate fingerprint for
all packages. Google Login works normally.

### Option 2 — Log in first on an unpatched Grindr, then switch to LSPatch

1. Uninstall the LSPatch-patched Grindr
2. Install the official Grindr APK and complete Google Sign-In once
3. Google stores the auth token on the device under the original package name
4. Reinstall via LSPatch — the stored token may allow the session to resume without re-authentication

Note: This may stop working when the token expires or Grindr forces re-auth.

### Option 3 — Use email/password login

The email + password flow does not involve GMS signature verification. It uses Grindr's own
backend auth endpoint, which the LSPatch-patched app can reach without issues.

### Option 4 — Shizuku-based patching (advanced)

Shizuku allows privileged system operations without full root. A Shizuku-aware install flow
could:
1. Use Shizuku to install the patched APK while retaining the original signing certificate
   through the system installer path (requires `ShizukuProvider` which is already declared
   in the `AndroidManifest.xml`)
2. GMS would then receive the original certificate from PMS

`ShizukuProvider` is already declared in `AndroidManifest.xml`. The installation pipeline
would need to be updated to use `IPackageInstaller` via Shizuku instead of the standard
session installer in `InstallApkStep.kt`. This is the most complete fix but requires
significant implementation work.

## Fingerprint reference

Grindr's official SHA-1 signing fingerprint (from known release builds):

```
82:3F:5A:17:C3:3B:16:B4:77:54:80:B3:16:07:E7:DF:35:D6:7A:F8
```

(This is already stored as `packageSignature` in `SignatureSpoofer.kt`.)

The SHA-256 fingerprint registered in Google's OAuth console for Grindr is derived from the
same release certificate. It cannot be changed without Grindr reconfiguring their Google
Cloud project.
