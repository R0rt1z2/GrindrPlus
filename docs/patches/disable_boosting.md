# Disable Boosting

Removes all boost-related upsells, tooltip popups, and the Store tab from Grindr's UI.

## Hooks

### DrawerProfileUiState constructor

Zeroes out all boost-related fields in the side-drawer profile state:

- `showBoostMeButton` → `false`
- `boostButtonState` / `roamButtonState` → `Unavailable` instance
- `showRNBoostCard` → `false`
- `showDayPassItem` / `unlimitedWeeklySubscriptionItem` → `null`
- `isRightNowAvailable` → `false`
- `showMegaBoost` → `false`

Search string: `'DrawerProfileUiState(showBoostMeButton='`

### RadarUiModel constructor

Nulls out radar boost/roam buttons.

Search string: `'RadarUiModel(boostButton='`

### FabUiModel / RightNowMicrosFabUiModel constructors

Hides the floating action button for boosts.

Full class names are stable (not obfuscated).

### HomeScreenBottomNavigationUiModel constructor

Filters the "Store" route out of the bottom navigation bar using
`SmallPersistentVector` (kotlinx.collections.immutable).

Search string: `'bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/ActivityHomeBinding;'`

### Tooltip anonymous functions

Suppresses the popup that shows after the user views a profile that tapped them.
These lambdas are obfuscated and **change every Grindr release**.

#### Finding the updated obfuscated names

Open the target Grindr APK in [jadx](https://github.com/skylot/jadx) and search smali for
the following string literals (they appear in stack traces or log strings inside the lambdas):

| What to search | Target |
|---|---|
| `HomeActivity$showTapsAndViewedMePopup$1$1` | First tooltip lambda |
| `HomeActivity.showTapsAndViewedMePopup.<anonymous> (HomeActivity.kt` | Second tooltip lambda |
| `HomeActivity.showTapsAndViewedMePopup.<anonymous>.<anonymous> (HomeActivity.kt` | Third tooltip lambda |
| `HomeActivity$subscribeForBoostRedeem$1` | Boost-redeem subscribe lambda |

Once the obfuscated class name is found, add it to the `listOf(...)` in `DisableBoosting.init()`.

Last known names:
- `"cd0.j2"` — pre-25.20.0 (may be stale)
- `"Il.w0"` — `subscribeForBoostRedeem` lambda

Missing class names are logged as warnings (`DisableBoosting: tooltip class '...' not found`)
rather than crashing the hook, so the rest of the functionality remains active even when
a name is outdated.
