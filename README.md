<p align="center" style="border-radius: 50%;">
  <img src="gplus_icon.svg" alt="Grindr Plus Icon" width="150" height="150">
</p>

<h1 align="center">Grindr Plus</h1>
<p align="center">
  <a href="https://discord.gg/SPb6Kc7S4C">
    <img src="https://img.shields.io/discord/1161706617729974352?label=Discord&logo=discord&style=for-the-badge" alt="Discord">
  </a>
</p>
Grindr Plus is an Xposed Module that unlocks and adds unique features to the Grindr app, enhancing user experience.

## Extra features
> <small>[!IMPORTANT]
> Depending on the version of the client and module you're using, some features may not be available or may work poorly.
- Allow to teleport to any location.
- Allow to save and manage teleport locations.
- Grant most of the Xtra and Unlimited features.
    -   Unlimited cascade view.
    -   Unlocked 'Explore mode'.
    -   Advanced search filters.
    -   Get rid of in-app ads/annoyances.
    -   Ability to save favorite phrases.
- Allow taking screenshots in any part of the app (including albums and expiring photos).
- Unlimited expiring photos and unlimited albums.
- Remove expiration on incoming expiring photos, allowing to see them any number of times you want.
- Extra profile fields and details
    -   Body mass index (BMI) and its description, if both weight and height are available.
    -   Indicator to know whether a user is boosting or not.
    -   Ability to copy profile ID when long pressing on the profile name.
    -   Ability to display hidden (server) profile fields when clicking on the profile name.
- Start video-calls even if you didn't start a chat with the recipient.
- Access to some user-hidden features (developer features).
- More accurate online status from other users. (The green dot from other profiles goes off after 3 minutes of inactivity.)
- Allow unlimited taps (no 24h restriction).
- Ability to prevent people from seeing you in their profile view list.
- Ability to customize the layout for the favorites tab.
- Ability to remove messages from anyone, any time.
- Block app (forced) updates. (The latest version of Grindr will be always fetched and spoofed internally).
- Execute commands in any chat to perform various actions:
    - Use `/teleport` to (virtually) travel to any location by specifying an alias, its name, or coordinates.
    - Retrieve the profile ID of the person you're chatting (+ yours) by using `/id`.
    - Access any profile directly by its ID using `/open`.
    - Discover more commands with `/help`, which provides a list of available commands.

## Bugs
* Incognito mode is kinda buggy (it turns off after a little while).
* Chat markers (read, typing) are not working properly since they're server-side.
* Show blocks in chat is not working properly since it's server-side.
* If using LSPatch, GMS (Google) related features won't work properly (e.g. login with Google or 'Explore' mode).

## How to install
Each Grindr Plus release supports only a specific Grindr app version and quite possibly will not work with any other. Before installing Grindr Plus, some prep work is needed to make sure that you are currenly running and will remain on a supported Grindr version.

### Prep work
<details>
  <summary>See more..</summary>

1. If you use Play Store auto update (most likely), disable auto update for the Grindr app:
    - Go to the Play Store and open the Grindr app page.
    - Click on the 3 dots (overflow menu) and untick "enable auto update".
2. Check the Grindr version currently installed on your device.
    - If it matches the required version, you are ready to install the mod.
    - If it is older than the required version, go to [APKMirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/) and download and install the required version. Check that Grindr works before proceeding.
    - If your version is newer than the required version, you have 2 options:
        - Wait for the Grindr Plus dev to maybe release a newer Plus. In the meantime, your Grindr will continue to work but will not auto update.
        - Downgrade your Grindr. You are likely rooted, so maybe you have a recent local app backup to restore. Or you can take a backup, install the older version, and restore only the app data afterwards (which might or might not explode in your face). Or you can simply do a clean install of the older version. Consider backing up messages using the Grindr backup service before uninstalling. And you might want to take a local app backup too, just in case. **However you downgrade Grindr, you probably need to immediately disable Play Store auto update again afterwards.** And double-check that Grindr works before continuing.
</details>

### Installation
> [!WARNING]
> Although LSPatch should work fine with this mod, its use is not recommended.

> [!WARNING]
> If you're using GrapheneOS, make sure to check out the FAQ if you encounter any issues.
<details>
  <summary>LSPosed instructions</summary>

1. **Root your device**:
    - There is no specific guide for this, you will have to look it up on your own. LSPosed only supports Magisk and KernelSU
    - If you can't root your phone either because you can't unlock the bootloader or another reason, you will have to use LSPatch

2. **Install LSPosed**:
    - (For Riru flavor) Install [Riru](https://github.com/RikkaApps/Riru/releases/latest) v26.1.7+.
    - [Download](https://github.com/LSPosed/LSPosed/blob/master/README.md#download) and install LSPosed in Magisk / KernelSU app.
    - Reboot your device.
    - Open LSPosed manager from notification and make sure to configure it.

3. **Enable GrindrPlus**:
    - Install both the latest release of GrindrPlus and the corresponding Grindr version.
    - Open LSPosed (you can use the 'Android System' notification), go to Modules and select GrindrPlus.
    - Enable the module and make sure the Grindr app is selected within the module scope list.

4. **Complete the setup**:
    - Open Grindr. You should now be able to use Grindr with GrindrPlus enhancements.
</details>
<details>
  <summary>LSPatch instructions</summary>

1. **Download necessary tools**:
    - [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api)
    - [LSpatch Manager](https://github.com/LSPosed/LSPatch/releases)
    - [GrindrPlus](https://github.com/R0rt1z2/GrindrPlus/releases)
    - [Grindr APK from APKMirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date)

2. **Set up Shizuku**:
    - Enable ADB debugging, connect your phone to a computer, and open Shizuku.
    - Follow on-screen commands to start Shizuku.

3. **Patch Grindr with LSpatch**:
    - Make sure to have previously installed the required Grindr version.
    - Open LSpatch and connect it to Shizuku.
    - Go to the manage view, add Grindr, and set "Signature bypass" to "lv2".

5. **Choose Patch Mode**:
    - For Local Mode: Start Patch, install the patched app, and select Grindr Plus in Module scope.
    - For Embedded Mode: Embed modules, select Grindr Plus, start patch, and install the patched app.

6. **Complete the setup**:
    - Open Grindr. You should now be able to use Grindr with GrindrPlus enhancements.
</details>

## FAQ & Troubleshooting
<details>
  <summary>My Grindr app suddenly stops / crashes when the module is installed!</summary> 

- Check if the module supports the app version. Grindr has lots of obfuscated symbols that change in each app update and the module couldn't work (or couldn't work properly).
</details>
<details>
  <summary>I've updated to Android 14 (QPR2) / Android 15 and LSPosed stopped working!</summary> 

- The development of LSPosed is currently frozen and that is why, no new updates have been released to support new Android versions. As an alternative, you can use [this **unofficial** fork](https://github.com/mywalkb/LSPosed_mod).
</details>
<details>
  <summary>I'm using LSPatch and I can't login with Google!</summary> 

- As mentioned above, when using LSPatch the original signature of the application is invalidated which causes all functions related to Google Services (GMS) to not work properly.
</details>
<details>
  <summary>Can I get banned with this?</summary>

- Obviously, however, the risk is very low, and there have been no reported cases of bans related to using this mod.
</details>
<details>
  <summary>Where can I download the latest stable build?</summary>

- https://github.com/R0rt1z2/GrindrPlus/releases
</details>
<details>
  <summary>Can I suggest a new feature?</summary>

- Feel free to, but keep in mind that every feature, no matter how small, has a lot of work behind it, so please be patient and understand that sometimes it is impossible to implement certain things due to the nature of how LSPosed works.
</details>

<details>
  <summary>I'm having issues on GrapheneOS!</summary>

- Uninstall Google Play "trio" (framework, services, store) in "apps" app and reinstall them again. They break all the time so it's a good idea to reload them. Don't worry, you won't have to login again to Google.
- Make sure to turn **ON** the exploit protection compatibility mode in "App Info" of Grindr, GrindrPlus and Google Play "trio". Just tap and hold onto the app icon to go there. When it comes to Google services you can do so from "apps" app.
- While doing the step above make sure to give Google Play services permissions to access location all the time and sensors.
- In Settings -> Apps -> Sandboxed Google Play, turn off the option "Reroute location requests to OS".
- Feel free to, but keep in mind that every feature, no matter how small, has a lot of work behind it, so please be patient and understand that sometimes it is impossible to implement certain things due to the >

</details>

## Contributing
This project is open to any kind of contribution. Feel free to [open a pull request](https://github.com/R0rt1z2/GrindrPlus/pulls) or [submit an issue](https://github.com/R0rt1z2/GrindrPlus/issues)! [Discussions section](https://github.com/R0rt1z2/GrindrPlus/discussions) also available!

## Interesting links
- [XDA Official Thread](https://forum.xda-developers.com/t/mod-xposed-new-grindr-plus.4461857/#post-87076193)

## Disclaimer
This module is neither affiliated with nor endorsed by the official Grindr app. It is provided for free with no warranty of any kind. Use at your own risk! We are not responsible for lost chats, user data, unexpected bans or any other problems incurred from the use of this module. This mod does not collect any personal data nor does it display ads of any kind. No earnings are generated or collected from the use of this software. This project is open source so you can check all these facts by your own!

## Credits
As an open source project, you're free to inspire yourself from this code. However, please **DON'T copy it and release it as your own (kanging)**. Give the proper credit and reference to the [original project](https://github.com/ElJaviLuki/GrindrPlus) and its contributors.

- Original developer / creator: [@ElJaviLuki](https://github.com/ElJaviLuki)
- Main contributor(s): [@ElJaviLuki](https://github.com/ElJaviLuki), [@R0rt1z2](https://github.com/R0rt1z2), [@TebbeUbben](https://github.com/TebbeUbben), [@poundross](https://github.com/poundross)

Additional credits to:
- [@veeti](https://github.com/veeti) for [DisableFlagSecure](https://github.com/veeti/DisableFlagSecure)
