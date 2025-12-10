<p align="center" style="border-radius: 50%;">
  <img src="gplus_icon.svg" alt="Grindr Plus Icon" width="150" height="150">
</p>

<p align="center">
  <a href="https://github.com/R0rt1z2/GrindrPlus/actions/workflows/build_apk.yml?query=branch%3Amaster+event%3Apush+is%3Acompleted"><img src="https://img.shields.io/github/actions/workflow/status/R0rt1z2/GrindrPlus/build_apk.yml?branch=master&logo=github&label=Build" alt="Build"></a>
  <img src="https://shields.io/github/downloads/R0rt1z2/GrindrPlus/total?logo=Bookmeter&label=Downloads&logoColor=Green&color=Green" alt="Total downloads">
  <a href="https://discord.gg/5ZxHJVGR"><img src="https://img.shields.io/discord/1161706617729974352?label=Discord&logo=discord" alt="Discord"></a>
  <a href="https://t.me/GrindrPlus"><img src="https://img.shields.io/badge/Telegram-2CA5E0?style=flat&logo=telegram&logoColor=white" alt="Telegram"></a>
</p>
<h1 align="center">Grindr Plus</h1>


<p align="center">
Grindr Plus is an Xposed Module that unlocks and adds unique features to the Grindr app, enhancing user experience.
</p>

## Introduction
This repository contains a completely rewritten version of [ElJaviLuki's original mod](https://github.com/ElJaviLuki/GrindrPlus), rebuilt from the ground up. It introduces new features, ensures compatibility with the latest Grindr versions, and offers improved performance and functionality.

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
   
  - `Ability to see ban details`
  - `Ability to spoof Android ID`
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

> [!WARNING]
> Please read this section carefully before reporting bugs. Many issues listed here are known limitations.

- **Google Login (LSPatch only)**: Requires a workaround to function properly. See the [FAQ section](#faq--troubleshooting) for detailed instructions.
- **Incognito Mode**: Does not work reliably and turns off automatically after a short period.
- **"Viewed Me" List**: Will not work as this is a server-side feature that cannot be modified.
- **Boosting & Roaming**: Disabled by default. To enable, turn off the "Disable Boosting" hook in settings and restart Grindr.
- **Random Crashes/Album Editing Issues**: If you experience crashes or cannot edit your own albums, disable the "Unlimited Albums" hook.
- **In-app Maps (LSPatch only)**: Maps functionality (explore, shared locations in chats) requires a custom Google Maps API key. See the [FAQ section](#faq--troubleshooting) for setup instructions.
- **Saved Locations Button**: Does not appear on cloned/dual apps.
- **LSPatch Limitations**: Users may experience additional stability issues compared to LSPosed installations.

> [!TIP]
> Before reporting a new bug, please check our [Discord](https://discord.gg/5ZxHJVGR) or [Telegram](https://t.me/GrindrPlus) channels to see if it's already known.

## Installation
> <small>[!WARNING]
> _Each Grindr Plus release supports only a specific Grindr app version and quite possibly will not work with any other.</small>_

GrindrPlus supports both **LSPosed** and **LSPatch**, though the latter comes with additional bugs, which are documented in the bugs section.

Each installation method is completely different and comes with its own challenges, so make sure to read the guide carefully and thoroughly. Open the dropdown for the method you plan to use to continue with the installation.

<details closed>
  <summary>No root</summary>

**Prerequisites:**
- No Grindr installed on device

**Process:**
1. Download & Install the GrindrPlus module APK (check the [downloads](https://github.com/R0rt1z2/GrindrPlus?tab=readme-ov-file#downloads) section of this `README`).
2. If the Grindr app is installed, uninstall it. **Make sure it's also gone from Secure Folder, Second Space or Private Space**.
3. Open the new "Grindr Plus" app and click on the "Install" button (bottom left).
4. Wait for the versions to load (if loading seems stuck, force close app & retry).
5. Select your preferred version (we recommend using latest).
6. Click on the "Install" button.
7. Wait for the installation to complete. Duration will depend on connection speed and phone's specs.
8. When prompted, install the newly generated Grindr app.
9. The app might crash multiple times during the first launches. This is normal, just keep relaunching it.
10. If the installation fails, <b>retry it</b> multiple times before asking for support.

**Verification:**
- Long press the "Browse" tab (first tab) in the bottom navigation bar
- A popup should appear showing GrindrPlus status and information
- You should see unlimited profiles and no third-party ads
- If these features don't work, try restarting the app or reinstalling

  </details>

<details closed>
  <summary>Root (LSPosed)</summary>

> **Make sure you're using [JingMatrix's fork of LSPosed](https://github.com/R0rt1z2/LSMirror/raw/refs/heads/main/LSPosed-v1.10.1-7167-zygisk-release.zip)!**

**Requirements:**
- Rooted using `Magisk` or `KernelSU`
- `LSPosed` installed and fully functional

**Process:**
1. Install the GrindrPlus module APK (check the [downloads](https://github.com/R0rt1z2/GrindrPlus?tab=readme-ov-file#downloads) section of this `README`)
2. Download the latest Grindr app [from Play Store](https://play.google.com/store/apps/details?id=com.grindrapp.android&hl=en) or use [SAI](https://github.com/Aefyr/SAI/releases) to install [bundles from APKMirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/)
3. Turn on the module in `LSPosed` and make sure Grindr is in scope
4. Open Grindr and verify the installation is working

**Verification:**
- Long press the "Browse" tab (first tab) in the bottom navigation bar
- A popup should appear showing GrindrPlus status and information
- You should see unlimited profiles and no third-party ads
- If these features don't work, check that the module is properly enabled in LSPosed
</details>

## FAQ & Troubleshooting
<details>
  <summary>How do I login with Google?</summary>

- If you're not using LSPosed you might have noticed that the Google Login button doesn't work. This is because the original signature of the application is invalidated when using LSPatch, which causes all functions related to Google Services (GMS) to not work properly.
- In order to fix that, you have to:
    1. Uninstall the patched Grindr app.
    2. Reinstall the original Grindr app (either from the Play Store or the official APK).
    3. Reboot your device (this is optional, but **HIGHLY RECOMMENDED**).
    4. Log in using your Google account.
    5. Uninstall the original Grindr app.
    6. Install the app again with GrindrPlus.
    7. Open the patched app and log in with Google **within 10 minutes**. If you wait too long, **the login will fail**.
    8. You should now be able to log in successfully using Google.
- NOTE: **You'll need to repeat this process every time you want to log in with Google**.
</details>
<details>
  <summary>Maps not loading on LSPatch version?</summary>

- For LSPatch users, the Maps functionality in Grindr won't work properly due to signature validation issues. To fix this, you'll need to set up a custom Google Maps API key.
- Here's how to set up a Google Maps API Key:
    1. Go to the Google Cloud Console at https://console.cloud.google.com/. You may need to log in with your Google account if you're not already.
    2. Select or create a new Google Cloud project to associate your API key with. If creating a new project, give it a name and ID. What you call the project is not important. Wait a few seconds for the project to be created.
    3. Make sure your new project is selected in the top dropdown menu, then open the navigation sidebar and go to "APIs & Services" > "Credentials".
    4. On the Credentials page, click "+ Create Credentials" and choose "API key" from the dropdown.
    5. Your new API key will be displayed. Click "Close" to return to the Credentials list. You should see your key listed under "API Keys".
    6. Click "Edit API key" to set up restrictions. You can give it a name, choose which websites or IP addresses can use it, and set an expiration date. For use with GrindrPlus, you should not set restrictions.
    7. Copy your API key and add it into the Grindr Plus settings for the Maps API Key.
    8. Use the install button in Grindr Plus to setup Grindr and the Maps API key.
- NOTE: **You may be prompted for credit card details by Google, even though use of the Maps API is part of their 'free tier'.**
</details>
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

- Turn **ON** the **"Exploit Protection Compatibility"** mode on Grindr. To do so - tap and hold on the app icon, click "App info" and scroll down a little. In general - Grindr should work without a problem with that option turned off but if it gives you any issues then you can try to play around with those settings.
- Do the same for Google services. In contrary to other apps - you have to access those options through "App store" app provided by GOS team.
- Make sure to give Google Play services permissions to **All time location** and to **Sensors**.
- In Settings -> Apps -> Sandboxed Google Play, turn **OFF** the option **"Reroute location requests to OS"**. This option sometimes breaks the location features on Grindr.

</details>

## Acknowledgments
Big part of the credit goes to [ElJaviLuki](https://github.com/ElJaviLuki/GrindrPlus), the creator of the original idea and mod — this project wouldn’t exist without his initial work.

This project relies on several third-party libraries, and we extend our gratitude to their authors for their valuable contributions. For a complete list of these dependencies, please refer to the [dependencies](https://github.com/R0rt1z2/GrindrPlus/blob/master/app/build.gradle.kts#L67-L79) section of the `build.gradle.kts` file.

Parts of the manager were coded with the help of [Vendetta's Manager](https://github.com/vendetta-mod/VendettaManager).

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
