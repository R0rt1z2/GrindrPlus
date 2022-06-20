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
- Extra profile fields in the so-called CruiseViewHolder ('Profile ID', and exact time of 'Last seen')
- Make videocalls even if you didn't start a chat with the recipient.
- Access to some user-hidden features (developer features)
- More accurate online status from other users. (The green dot from other profiles goes off after 3 minutes of inactivity.)
- Allow unlimited taps (no 24h restriction)

## Bugs
* Incognito mode is kinda buggy (it turns off after a little while)

## How to install

1. Install the Xposed Framework and its app (Xposed, EDXposed, etc.)
3. Download the Grindr Plus module.
4. Activate the module in the Xposed Installer.
5. Reboot your device.
6. Done!

## Troubleshooting
### My Grindr app suddenly stops when the module is installed.
Check if the module supports the app version. Grindr has lots of obfuscated symbols that change in each app update and the module couldn't work (or couldn't work properly)

## Contributing
This project is open to any kind of contribution. Feel free to open an issue or a pull request or submit an issue or suggestion!

## Credits
As an open source project, you're free to inspire yourself from this code. However, please **DON'T copy it and release it as your own (kanging)**. Give the proper credit and reference to the original project (https://github.com/ElJaviLuki/GrindrPlus) and its contributors.

Main contributor: [@ElJaviLuki](https://github.com/ElJaviLuki)

Additional credits to:
- [@veeti](https://github.com/veeti) for [DisableFlagSecure](https://github.com/veeti/DisableFlagSecure)
