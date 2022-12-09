# Grindr Plus
Grindr Plus is a Xposed Module that allows you to unlock some features in Grindr.

## Extra features
(Depending on the version of the client and module you're using, some features may not be available or may work buggy.)
- Allow mocking locations.
- Grant most of the Xtra and Unlimited features.
  -   Unlimited cascade view.
  -   Unlocked 'Explore mode'.
  -   Advanced search filters.
  -   See 'Read' and 'Typing...' message states.
- Allow taking screenshots in any part of the app (including albums and expiring photos).
- Unlimited expiring photos.
- Remove expiration on incoming expiring photos, allowing to see them any number of times you want.
- Extra profile fields in the so-called CruiseViewHolder
    -   Profile ID.
    -   Exact time of 'Last seen'.
    -   Body mass index (BMI) and its description (underweight, normal, overweight, obesity I, obesity II or obesity III), if both weight and height are available.
- Make videocalls even if you didn't start a chat with the recipient.
- Access to some user-hidden features (developer features)
- More accurate online status from other users. (The green dot from other profiles goes off after 3 minutes of inactivity.)
- Allow unlimited taps (no 24h restriction)
- Ability to see who viewed your profile even if you have "Show Me In Viewed Me List" disabled.
- Ability to remove messages from anyone, any time.

## Bugs
* Incognito mode is kinda buggy (it turns off after a little while)

## How to install
Each Grindr Plus release supports only a specific Grindr app version and quite possibly will not work with any other. Before installing Grindr Plus, some prep work is needed to make sure that you are currenly running and will remain on a supported Grindr version.

### Prep work
1. Install an Xposed framework and its management app. (See: Xposed, EdXposed, LSPosed, etc.)
2. If you use Play Store auto update (most likely), disable auto update for the Grindr app:
   - Go to the Play Store and open the Grindr app page.
   - Click on the 3 dots (overflow menu) and untick "enable auto update".
3. Go to Grindr Plus [releases](https://github.com/ElJaviLuki/GrindrPlus/releases) and pick the latest. Each Grindr Plus release specifies its compatible Grindr app version on its filename. Take note of the required Grindr app version.
4. Check the Grindr version currently installed on your device.
   - If it matches the required version, you are ready to install Plus.
   - If it is older than the required version, go to [APKMirror](https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/) and download and install the required version. Check that Grindr works before proceeding.
   - If your version is newer than the required version, you have 2 options:
     - Wait for the Grindr Plus dev to maybe release a newer Plus. In the meantime, your Grindr will continue to work but will not auto update.
     - Downgrade your Grindr. You are likely rooted, so maybe you have a recent local app backup to restore. Or you can take a backup, install the older version, and restore only the app data afterwards (which might or might not explode in your face). Or you can simply do a clean install of the older version. Consider backing up messages using the Grindr backup service before uninstalling. And you might want to take a local app backup too, just in case. **However you downgrade Grindr, you probably need to immediately disable Play Store auto update again afterwards.** And double-check that Grindr works before continuing.

### Plus installation
1. Download your chosen Grindr Plus release and install it.
2. Activate the module in your Xposed manager.
3. Reboot your device.

## Troubleshooting
### My Grindr app suddenly stops when the module is installed.
Check if the module supports the app version. Grindr has lots of obfuscated symbols that change in each app update and the module couldn't work (or couldn't work properly)

## Contributing
This project is open to any kind of contribution. Feel free to [open a pull request](https://github.com/ElJaviLuki/GrindrPlus/pulls) or [submit an issue](https://github.com/ElJaviLuki/GrindrPlus/issues)! [Discussions section](https://github.com/ElJaviLuki/GrindrPlus/discussions) also available!

## Interesting links
- [XDA Official Thread](https://forum.xda-developers.com/t/mod-xposed-new-grindr-plus.4461857/#post-87076193)

## Disclaimer
This module is neither affiliated with nor endorsed by the official Grindr app. It is provided for free with no warranty of any kind. Use at your own risk! I am not responsible for lost chats, user data, unexpected bans or any other problems incurred from the use of this module. This mod does not collect any personal data nor does it display ads of any kind. No earnings are generated or collected from the use of this software. This project is open source so you can check all these facts by your own!

## Credits
As an open source project, you're free to inspire yourself from this code. However, please **DON'T copy it and release it as your own (kanging)**. Give the proper credit and reference to the original project (https://github.com/ElJaviLuki/GrindrPlus) and its contributors.

Main contributor: [@ElJaviLuki](https://github.com/ElJaviLuki)

Additional credits to:
- [@veeti](https://github.com/veeti) for [DisableFlagSecure](https://github.com/veeti/DisableFlagSecure)
