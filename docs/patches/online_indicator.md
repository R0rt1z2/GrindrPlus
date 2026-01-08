# Online indicator

Used to override the duration of the green dot (online indicator) on other users' profiles.
This lets you see more precisely how long ago were they on the app, in contrast to the default 
timeout of 10 or 15 minutes.


## Notes on 25.20.0
this version uses `onlineUntil` field in the profile (computed on the server)
instead of `lastSeen` and computing it in the app. This behaviour is behind a feature flag,
but the old behaviour will most likely be removed from the code entirely.

Old implementation relies on this method: 
`boolean Vm.m0::a(long lastSeen) { return currentTimeMillis - lastSeen < 600000L }`
and the new implementation on this method:
`boolean Ql.t::q(long onlineUntil) { return onlineUntil > currentTimeMillis }`.