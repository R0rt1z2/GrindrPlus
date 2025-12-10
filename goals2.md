High-priority remaps

AntiDetection: find the emulator/root checks (strings like “sdk_gphone”, “emulator”, “google_sdk”, Firebase CommonUtils). Hook to return false/no-op.
DisableUpdates: locate Play Core update call (requestUpdateInfo(%s)) and no-op it.
WebSocketAlive: find the WebSocket heartbeat client and force keep-alive.
Feature gaps to remap

LocalSavedPhrases: phrases service (v3/me/prefs) and the Success(successValue=...) wrapper.
UnlimitedAlbums: album/red-dot endpoints (v1/albums/red-dot, related album content classes).
AntiBlock / QuickBlock / ProfileDetails: chat delete plugin, inbox delete, unblock ViewModel, blockedProfiles observer; search markers like chat_read_receipt, ProfileViewHolder$onBind.
ReverseRadarTabs: current Radar tab adapter to swap indices.
Validation/cleanup

Confirm online indicator fallback is acceptable (forces onlineUntil far future on cascade profiles).
Confirm device ID spoof works (global Settings.Secure.getString(..., "android_id") hook).
Build/sign/install, then watch logcat for ClassNotFoundException/NoSuchMethodException to identify any remaining obfuscation misses.
