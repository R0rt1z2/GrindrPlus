# Enable Unlimited

Unlocks Grindr Unlimited features by faking the user's subscription roles and hiding
paywall-related UI elements.

## Hooks

### UserSession.rolesUpdated (obfuscated: `W`)

Replaces the incoming roles list with the full set of all subscription tiers before
the session stores them. This is the core unlock — it makes Grindr think the user
has every subscription.

Search string: `'Intrinsics.checkNotNullParameter(roles, "roles");'` inside the UserSession class.

Injected roles: `Plus`, `Xtra`, `Unlimited`, `Premium`, `Free_Plus`, `Free_Unlimited`, `Free_Premium`

### UserSession.getRolesAsString (obfuscated: `D`)

Returns `"[]"` to prevent the fake roles from leaking into HTTP request headers, which would
cause server-side rejection.

### Chat interstitial ads

Hooks the `emit()` method of the chat subscription observer to suppress interstitial ad events.
Allows `NoInterstitialCreated` and `OnInterstitialDismissed` through; blocks everything else.

Current obfuscated class: `"mo.b1$a"` — search for
`'ChatActivityV2$subscribeToInterstitialAds$1$1$1'` in smali to find the updated name.

### TabLayout.addTab — Store tab removal

Removes the Store tab (position 4) from the bottom navigation `TabLayout` by removing its
view from the parent immediately after it is added.

### View visibility — hiding paywall UI

Hides paywall-related views in multiple fragments by resolving resource IDs and setting
`visibility = GONE` / `height = 0`. Views hidden:

| Fragment | View IDs |
|---|---|
| `ProfileTagCascadeFragment` | `upsell_bottom_bar` |
| `CascadeFragment` | `upsell_bottom_bar`, `shuffle_top_bar`, `floating_rating_banner`, `micros_fab`, `right_now_progress_compose_view` |
| `HomeActivity` binding | `persistentAdBannerContainer` |
| `DrawerProfileFragment` | `plans_title`, `store_in_profile_drawer_card`, `sideDrawerBoostContainer`, `drawer_profile_offer_card` |
| `RadarFragment` | `micros_fab`, `right_now_fabs_container` |

### Persistent banner ad container (obfuscated: `nb.d`)

Hooks the inflate method of the persistent ad banner binding class to hide
`persistent_banner_ad_compose_view`.

#### Finding the updated obfuscated class name

Search smali for:

```
bind(Landroid/view/View;)Lcom/grindrapp/android/databinding/...
```

Near the binding class that references `ComposeView` and `findChildViewById`. The matching
class in the smali `implements ViewBinding` with a single `bind(View)` method returning the
banner binding type is the target.

The view ID changed from `persistent_banner_ad_container` to `persistent_banner_ad_compose_view`
in Grindr 25.x — the current code already uses the new name.

### Paywall utils (obfuscated: `x90.e`)

When Grindr tries to show a "feature is server-enforced" paywall dialog, this hook intercepts
it and shows a developer-friendly alert with a stack trace copy button instead.

Search string: `'app_restart_required'` in smali to find the updated obfuscated class name.

### ProfileViewState.isChatPaywalled

Forces `isChatPaywalled()` to always return `false` so chat is never gated.

### Profile.isBlockable / component60

Forces both to return `true` so blocking always works regardless of account tier.
