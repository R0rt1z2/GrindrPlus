<p align="center" style="border-radius: 50%;">
  <img src="gplus_icon.svg" alt="Grindr Plus Icon" width="150" height="150">
</p>

<p align="center">
  <a href="https://github.com/R0rt1z2/GrindrPlus/actions/workflows/build_apk.yml?query=branch%3Amaster+event%3Apush+is%3Acompleted"><img src="https://img.shields.io/github/actions/workflow/status/R0rt1z2/GrindrPlus/build_apk.yml?branch=master&logo=github&label=Build" alt="Build"></a>
  <img src="https://shields.io/github/downloads/R0rt1z2/GrindrPlus/total?logo=Bookmeter&label=Downloads&logoColor=Green&color=Green" alt="Total downloads">
  <a href="https://discord.gg/SPb6Kc7S4C"><img src="https://img.shields.io/discord/1161706617729974352?label=Discord&logo=discord" alt="Discord"></a>
</p>
<h1 align="center">Grindr Plus</h1>

<p align="center">
Grindr Plus is an Xposed Module that unlocks and adds unique features to the Grindr app, enhancing user experience.
</p>

## Introduction
This repository contains a completely rewritten version of ElJaviLuki's original mod, rebuilt from the ground up. It introduces new features, ensures compatibility with the latest Grindr versions, and offers improved performance and functionality.

As the title of the repo suggests, this mod is designed to enhance the user experience, but please note that it’s under active development, so stability is not always guaranteed.

Neither I ([@R0rt1z2](https://github.com/R0rt1z2)) nor any contributors listed here are affiliated with Grindr LLC. For any important inquiries related to this repository, feel free to reach out to me directly at hello@r0rt1z2.com.

Use this mod at your own discretion, and be aware that future updates may introduce changes or require further adjustments.

## Disclaimer
This mod is provided for free with no warranty of any kind. Use at your own risk! We are not responsible for lost chats, user data, unexpected bans or any other problems incurred from the use of this module.

This mod does not collect any personal data nor does it display ads of any kind. No earnings are generated or collected from the use of this software. This project is open source so you can check all these facts by your own!

## Downloads
* You can download the latest stable release by visiting the [releases page](https://github.com/R0rt1z2/GrindrPlus/releases).
* You can grab the most recent CI build from the [actions section](https://github.com/R0rt1z2/GrindrPlus/actions) or join our [Telegram CI channel](https://t.me/GrindrPlus).

## Features
<details closed>
  <summary>Chat</summary>
   
  - `Built-in command console (see /help)`
  - `Start video calls in new chats`
  - `Prevent others from seeing chat indicators`
  - `Remove any message, no matter how old it is`
</details>

<details closed>
  <summary>Media</summary>
   
  - `Unlimited expiring photos`
  - `View all albums you've received`
  - `Ability to take screenshots`
</details>

<details closed>
  <summary>Global</summary>
   
  - `Removed most analytics`
  - `Unlock developer special features`
  - `Built-in mod settings to manage hooks`
  - `Disable forced app updates (extend mod lifespan)`
</details>

<details closed>
  <summary>Profiles</summary>
   
  - `Body mass index (BMI)`
  - `Indicator for boosted users`
  - `Ability to copy profile ID`
  - `More accurate distance`
  - `Hidden (server) profile fields`
  - `More accurate online status`
  - `Customize favorites layout`
</details>

<details closed>
  <summary>Location</summary>
   
  - `Quick teleporting`
  - `Location spoofing`
  - `Save and manage locations`
</details>

<details closed>
  <summary>Premium</summary>
   
  - `Unlimited cascade view`
  - `Unlocked "Explore Mode"`
  - `Advanced search filters`
  - `ZERO third-party ads`
  - `Saved chat phrases`
  - `Disable boosting upsells`
  - `Hide your own views`
  - `Incognito mode`
</details>

## Bugs
* Incognito mode is kinda buggy (it turns off after a little while).
* GMS-related features (e.g. Google Login, "Explore" mode map) are broken with LSPatch.

## Installation
> <small>[!WARNING]
> _Each Grindr Plus release supports only a specific Grindr app version and quite possibly will not work with any other.</small>_

GrindrPlus supports both **LSPosed** and **LSPatch**, though the latter comes with additional bugs, which are documented in the bugs section.

Each installation method is completely different and comes with its own challenges, so make sure to read the guide carefully and thoroughly. Open the dropdown for the method you plan to use to continue with the installation.

<details closed>
  <summary>LSPatch</summary>
 
  > This is a simplified version of [willysmith's XDA guide](https://xdaforums.com/t/mod-xposed-grindr-plus.4461857/page-107#post-89708211). Visit the link to read the complete (and more detailed) guide!

  **Requirements:**
  - `Shizuku` [**installed**](https://shizuku.rikka.app/guide/setup) and fully functional

  **Process:**
  1. Download [JingMatrix's LSPatch fork](https://xdaforums.com/attachments/lspatch-manager-v0-6-418-release-apk.6143894) and install it (as of September 2024, official LSPatch doesn't work anymore with latest Android versions)
  2. Install the GrindrPlus module APK (check the [downloads](https://github.com/R0rt1z2/GrindrPlus?tab=readme-ov-file#downloads) section of this `README`)
  3. Download the latest Grindr app [from Play Store](https://play.google.com/store/apps/details?id=com.grindrapp.android&hl=en) or use [SAI](https://github.com/Aefyr/SAI/releases) to install [bundles from APKMirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/)
  4. Open `LSPatch` to make sure everything is set up correctly up to this point. In the **home tab**, confirm you see _Shizuku service available_ at the very top
  5. From `LSPatch`, go to "Manage > Apps" and press **+**. Create new patch, select an installed app and search for the Grindr app (`com.grindrapp.android`)
  6. Enable (select) the options **LOCAL** and **ENABLE DEBUGGING**, start the patch and wait for it to finish
  7. Once it's done, click on install. It'll ask to uninstall the original Grindr app, just do it and wait for it to install the patched APK
  8. Go back to "Manage > Apps", click on "Grindr > Module scope" and select Grindr Plus (`com.grindrplus`), confirm by pressing the checkmark (bottom right)
  9. Open Grindr and check if Mod Settings are at the top of Grindr settings
</details>

<details closed>
  <summary>LSPosed</summary>

  > If you're using the official LSPosed, the mod might not work. **It is highly recommended to switch to [JingMatrix's fork of LSPosed](https://xdaforums.com/t/mod-xposed-grindr-plus.4461857/page-105#post-89706407)!**
  
  **Requirements:**
  - Rooted using `Magisk` or `KernelSU`
  - `LSPosed` installed and fully functional

  **Process:**
  1. Install the GrindrPlus module APK (check the [downloads](https://github.com/R0rt1z2/GrindrPlus?tab=readme-ov-file#downloads) section of this `README`)
  2. Download the latest Grindr app [from Play Store](https://play.google.com/store/apps/details?id=com.grindrapp.android&hl=en) or use [SAI](https://github.com/Aefyr/SAI/releases) to install [bundles from APKMirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/)
  3. Turn on the module in `LSPosed` and make sure Grindr is in scope
  4. Open Grindr and check if Mod Settings are at the top of Grindr settings
</details>

## FAQ & Troubleshooting
<details>
  <summary>My Grindr app suddenly stops / crashes when the module is installed!</summary> 

- Make sure you're using a good LSPatch/LSPosed version (official are broken on latest Android versions). Consider switching to [JingMatrix's fork](https://github.com/JingMatrix) if you haven't already.
- Check if the module supports the app version. Grindr has lots of obfuscated symbols that change in each app update and the module couldn't work (or couldn't work properly).
</details>
<details>
  <summary>I've updated to newer Android version and LSPosed/LSPatch stopped working!</summary> 

- The development of LSPosed/LSPatch is currently frozen and that is why, no new updates have been released to support new Android versions. Make sure you're using [JingMatrix's fork](https://github.com/JingMatrix), which works with latest updates.
</details>
<details>
  <summary>I can't see profiles, whenever I open them they're blank!</summary>

- This most likely means you're using an AdBlocker (e.g. AdAway). Disable it or whitelist `cdn.cookielaw.org`. 

</details>
<details>
  <summary>I'm using LSPatch and I can't login with Google!</summary> 

- As mentioned above, when using LSPatch the original signature of the application is invalidated which causes all functions related to Google Services (GMS) to not work properly.
</details>
<details>
  <summary>Can I get banned with this?</summary>

- [Obviously](https://www.grindr.com/terms-of-service), however, the risk is very low, and there have been no reported cases of bans related to using this mod.
</details>
<details>
  <summary>Where can I download the latest stable build?</summary>

- https://github.com/R0rt1z2/GrindrPlus/releases
</details>
<details>
  <summary>Can I suggest a new feature?</summary>

- Feel free to, but keep in mind that every feature, no matter how small, has a lot of work behind it, so please be patient and understand that sometimes it is impossible to implement certain things due to the nature of how LSPosed works.
- Make sure to use our feature requests template, otherwise your inquiry will be ignored.
</details>

<details>
  <summary>I'm having issues on GrapheneOS!</summary>

- Uninstall Google Play "trio" (framework, services, store) in "apps" app and reinstall them again. They break all the time so it's a good idea to reload them. Don't worry, you won't have to login again to Google.
- Make sure to turn **ON** the exploit protection compatibility mode in "App Info" of Grindr, GrindrPlus and Google Play "trio". Just tap and hold onto the app icon to go there. When it comes to Google services you can do so from "apps" app.
- While doing the step above make sure to give Google Play services permissions to access location all the time and sensors.
- In Settings -> Apps -> Sandboxed Google Play, turn off the option "Reroute location requests to OS".

</details>

## Acknowledgments
This project relies on several third-party libraries, and we extend our gratitude to their authors for their valuable contributions. For a complete list of these dependencies, please refer to the [dependencies](https://github.com/R0rt1z2/GrindrPlus/blob/master/app/build.gradle.kts#L67-L79) section of the `build.gradle.kts` file.

I would also like to give special recognition to [@rhunk](https://github.com/rhunk) and the other developers of [SE](https://github.com/rhunk/SnapEnhance). Their work has been very useful for this mod, and some portions of their code have been used here.

## Contributing
This project is open to any kind of contribution. Feel free to [open a pull request](https://github.com/R0rt1z2/GrindrPlus/pulls) or [submit an issue](https://github.com/R0rt1z2/GrindrPlus/issues)! [Discussions section](https://github.com/R0rt1z2/GrindrPlus/discussions) also available!

## Donations
I don't usually ask for donations, but if you really want to support me, you can do so by sending me a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/r0rt1z2)

[![PayPal](https://cdn.rawgit.com/twolfson/paypal-github-button/1.0.0/dist/button.svg)](https://www.paypal.me/R0rt1z2/)

## Related resources
- [Official XDA thread](https://forum.xda-developers.com/t/mod-xposed-new-grindr-plus.4461857/#post-87076193)
- [Downloads for Grindr (APKMirror)](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date)
- [JingMatrix's LSPosed fork](https://github.com/JingMatrix/LSPosed)
- [JingMatrix's LSPatch fork](https://github.com/JingMatrix/LSPatch)

## License
This project is distributed under the GPL-3.0 License. For more information, simply refer to the [LICENSE](https://github.com/R0rt1z2/GrindrPlus/blob/master/LICENSE) file. Please note that the [`old_base`](https://github.com/R0rt1z2/GrindrPlus/tree/old_base) branch is not subjected to any license, as the original author did not assign or attribute one.

As an open source project, you're free to inspire yourself from this code. However, please **DON'T copy it and release it as your own (kanging)**. Give the proper credit and reference to the [original project](https://github.com/R0rt1z2/GrindrPlus) and its [contributors](https://github.com/R0rt1z2/GrindrPlus/graphs/contributors).
