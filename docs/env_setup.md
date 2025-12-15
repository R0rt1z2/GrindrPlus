# Dev environment setup

What you (might) need:
 - Android Studio [link](https://developer.android.com/studio)
 - Grindr apk
 - JADX [link](https://github.com/skylot/jadx/releases)
 - HTTP interception
 - an Android phone with LSPosed


## Android Studio
Download the newest version, open the repository and install necessaty plugins/tools.
Try to build the app and install on your phone (via ADB). If you use LSPosed, make sure 
to check `Always install with package manager`. If not, LSPosed will not notice you installed
an update and will not reload the module. You can find this option in the 
[run configurations](img/run_configs.png).


## Grindr apk
You will need the apk to a) inspect the code and b) test your patches in the app.  

Download the latest supported version (found in [version.json](../version.json))
from [apkmirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/)
(or whatever source you like). If you download from apkmirror, you will get .apkm file,
which is just a zip of all the partial .apk files.

For the decompilation, you will need only the base apk from it.

If you have a phone with LSPosed, install the app on it. Apkm files can be installed using
[apkmirror installer](https://play.google.com/store/apps/details/APKMirror_Installer_Official?id=com.apkmirror.helper.prod).


## JADX
JADX is used to decompile and inspect the code of the original Grindr app.
We recommend to enable `Show inconsistent code` and `Enable deobfuscation` (see below).
Some methods are not able to be cleanly decompiled and JADX fails on them.
Enabling inconsistent code, you get at least some of the code, so you can try to guess 
what the method does, instead of having just the method name (obfuscated) and arguments.

#### Export to Android Studio (optional)
Optionally you can export the code to Android Studio, if you are more familiar with the UI and tools.
Be sure to export as Gradle project with `Android app` template.

### Deobfuscation
Deobfuscation is helpful, because obfuscated method and field names in the classes are mostly just
single letters, making it difficult to use search. With deobf. enabled, all field and method names 
get renamed to an unique name. This might also be useful on Windows because of it's 
case insensitive file system - you can not have two classes/files differing only in one being
upper-case and the other lower-case (A.java and a.java).

## HTTP interception
HTTP interception is used to inspect what the app is sending to the servers and getting back from them.

### Mitmproxy
[Mitmproxy](https://www.mitmproxy.org/downloads/)'s main advantage is that it is simple to set up and enable/disable afterwards
and does not require root and/or patching the target app. One disadvantage is
that it intercepts traffic from the whole OS/phone.

Download it and run `mitmweb -m wireguard`, a web browser will open.
Install wireguard app on your phone, add tunnel config and scan 
the QR code on mitmproxy webpage in th `Capture` tab.
Paste `~d grindr.mobi` in the `Flow List` tab into filter/search field.

Optionally install the CA certificate in your phone for other apps to work while connected,
but this is not necessary for the patched Grindr app.

### HTTP Toolkit
[HTTP toolkit](https://httptoolkit.com/) has several modes.
The one we're most interested in is Android Device via ADB. In combination with Grindr plus 
this will disable certificate pinning and allow for interception of https traffic between 
Grindr/the manager, and internet servers they may connect to. 
Note that this requires either a rooted phone or for Grindr/Grindr plus to be running 
in a supported emulator (the one bundled with Android studio has been the most 
extensively tested)

Alternatively, it can hook the app at runtime via frida (requires root), though this 
has not been extensively tested and may cause issues with lspatch. To try that, download, 
install and open it, select Android App via Frida and follow the manual.


## Android phone
There are two options for efficient development: LSPatch (non-root) and LSPosed (for root).

### LSPatch
Download and install [LSPatch](https://github.com/JingMatrix/LSPatch/releases),
[Shizuku](https://github.com/RikkaApps/Shizuku/releases)
and Grindr app (see above).
Set up Shizuku and open LSPatch and patch Grindr app in local mode.
Then tap on Grindr in LSPatch in installed apps, open Module scope and enable Grindr Plus.

### LSPosed
You will need to look up a method/manual to install LSPosed specifically for your brand/model.
Generally, you want to install [Magisk](https://github.com/topjohnwu/Magisk), enable Zygisk,
install [LSPosed](https://github.com/JingMatrix/LSPosed) as Magisk module.
Rooting your phone carries some risks, please study them carefully before deciding.

### Plain Grindr plus manager
You can also use just the GrindrPlus app to patch the Grindr app,
but every change to the patch code will require you build a patched Grindr app and install it,
instead of just installing the GrindrPlus app and force-stopping the Grindr app.

### Android emulator / VM
Alternatively, you can use Android emulator on your PC, with any of the above methods,
but this is out of scope of this guide.
