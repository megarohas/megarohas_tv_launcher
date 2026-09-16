# Mega TV Launcher

A minimal Apple TV-style launcher for Android TV. Pure Kotlin + RecyclerView — no Leanback library, no bloat. Around 400 lines of code.

## Features

- **tvOS-style home screen** — grid of 16:9 banner cards with smooth focus scaling, dark gradient background
- **Hide apps** — long-press a card → *Hide*; bring them back on the hidden apps screen (eye button in the corner)
- **Clock & date** in the corner
- **Settings button** — opens the standard system settings, nothing is reimplemented
- **Long-press menu** — Hide / App info / Uninstall
- Auto-refreshes when apps are installed or removed
- English + Russian UI

## Install

Grab the APK from [Releases](../../releases) and install it:

```
adb install mega-tv-launcher.apk
```

### Make it the default home screen

```
adb shell cmd package set-home-activity tv.megarohas.launcher/.MainActivity
adb shell cmd role add-role-holder --user 0 android.app.role.HOME tv.megarohas.launcher
```

On some devices (Google TV firmware) the role only kicks in after a reboot.

> **Warning:** do **not** disable the stock launcher (`pm disable-user com.google.android.apps.tv.launcherx`).
> It is the boot fallback — with it disabled, a crash or a slow start of any third-party launcher
> can leave the device on a black screen where network ADB is not up yet. Keep the stock
> launcher enabled; it does no harm in the background.

## Build

JDK 17 + Android SDK (API 34):

```
gradlew assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`.

## Requirements

Android TV 5.0+ (minSdk 21, target 34). Tested on a Xiaomi box running Android 14.
