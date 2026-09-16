# Mega TV Launcher

A minimal Apple TV-style launcher for Android TV. Pure Kotlin + RecyclerView — no Leanback library, no bloat. Around 450 lines of code.

## Features

- **tvOS-style home screen** — grid of 16:9 banner cards with smooth focus scaling, dark gradient background
- **Hide apps** — long-press a card → *Hide*; bring them back on the hidden apps screen (eye button in the corner)
- **Home button override** — Projectivy-style accessibility service that puts this launcher on top whenever the stock Google TV home appears (HOME press, boot). No system packages get disabled
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

### Make it your home screen

Google TV firmware hardcodes the stock launcher (`launcherx` declares its HOME intent-filter
with `priority=2`), so neither `set-home-activity` nor the HOME role can beat it while it is
enabled. Two ways around it:

**Option A — built-in Home button override (recommended).** Enable the launcher's
accessibility service: *Settings → Accessibility → Home button override*, or via adb:

```
adb shell settings get secure enabled_accessibility_services
```

then append `tv.megarohas.launcher/.HomeWatchService` to whatever that returned
(entries are `:`-separated):

```
adb shell settings put secure enabled_accessibility_services "<previous value>:tv.megarohas.launcher/.HomeWatchService"
adb shell settings put secure accessibility_enabled 1
```

The stock launcher stays enabled and flashes for a split second before this launcher takes
over — that is the price of keeping the system fallback intact. Fully reversible: turn the
accessibility service off and the stock home is back.

**Option B — plain default-home commands.** On non-Google-TV firmware (plain Android TV,
Fire TV, AOSP boxes) the normal way may just work:

```
adb shell cmd package set-home-activity tv.megarohas.launcher/.MainActivity
adb shell cmd role add-role-holder --user 0 android.app.role.HOME tv.megarohas.launcher
```

> **Warning:** do **not** disable the stock launcher (`pm disable-user com.google.android.apps.tv.launcherx`),
> even though most guides suggest it. It is the boot fallback — with it disabled, a crash or
> a slow start of any third-party launcher can leave the device on a black screen where
> network ADB is not up yet. The override above exists so you never have to.

## Build

JDK 17 + Android SDK (API 34):

```
gradlew assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`.

## Requirements

Android TV 5.0+ (minSdk 21, target 34). Tested on a Xiaomi box running Android 14 (Google TV).
