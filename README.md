# WireGuard for Android — Material 3 Expressive

A fork of [WireGuard for Android](https://github.com/WireGuard/wireguard-android)
rebuilt on **Material 3 Expressive**. The app does exactly what upstream does;
what changed is how it looks and how it moves.

The package name is kept as `com.wireguard.android`, so a build of this
**replaces** the official app rather than installing alongside it.

## What the redesign changes

- **One activity, two pages.** The tunnel list and settings are pages of a
  single `ViewPager2` behind a navigation bar, instead of separate activities.
  Detail, editor and the log viewer open as fragments over that pager, so the
  app bar, the navigation bar and the back behaviour are decided in one place.
- **A FAB menu instead of a bottom sheet.** Adding a tunnel expands the FAB
  into three labelled actions, with the icon morphing between plus and cross.
  The FAB hides on scroll and stays clear of the navigation bar.
- **Expressive toolbars.** Toolbar actions sit in shaped containers and are
  joined into Material's connected button group, sized through Material's own
  size overlay rather than by hand.
- **Expressive settings.** Categories instead of androidx's automatic
  "Advanced" fold, grouped rows with top/middle/bottom backgrounds, Material
  switches, and a scrollbar that matches the rest of the app.
- **Real transitions.** Screens slide and fade as one movement, dialogs grow
  into place and dim the page behind them the way the FAB menu does.
- **A rebuilt app picker.** The per-app exclusion screen is a list with
  Material's list-item metrics and two action pills, not a column of blobs.
- **Favourite tunnels**, which sort to the top of the list.

## Building

Requires **JDK 17** specifically (newer JDKs break the Android Gradle Plugin's
`jlink` transform), the Android SDK (platform 37.2, build-tools 37.0.0), and
NDK 27+ with CMake for the native `wg`/`wg-quick` binaries and the userspace
`libwg-go.so`. No Go toolchain setup is needed — `tunnel/tools/libwg-go`
downloads and patches its own pinned Go release.

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :ui:assembleRelease
```

Release builds are signed from environment variables — `WG_KEYSTORE_PATH`,
`WG_KEYSTORE_PASSWORD`, `WG_KEY_ALIAS`, `WG_KEY_PASSWORD`. No keystore is
included in this repo (see `.gitignore`). Because the package name matches
the official app, installing a build signed with a different key means
uninstalling the official app first.

## License

Same as upstream WireGuard for Android — see [COPYING](COPYING) (Apache 2.0).
