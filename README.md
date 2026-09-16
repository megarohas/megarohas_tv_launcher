# Mega TV Launcher

**English** | [Русский](README.ru.md)

A minimal Apple TV-style launcher for Android TV. Pure Kotlin + RecyclerView — no Leanback library, no bloat.

## Features

- **tvOS-style home screen** — grid of 16:9 banner cards with smooth focus scaling, dark gradient background
- **Focus glow** — a soft halo in the dominant color of the app artwork under the focused card
- **Manual reordering** — long-press a card → *Move*, reposition with the arrow keys, OK to save
- **Hidden apps folder** — long-press → *Hide*; inside the folder a press launches the app, holding OK brings it back
- **Per-item edge fade** — rows dissolve into the top edge and float in from the bottom while scrolling
- **Home button override** — built-in accessibility service that puts this launcher on top whenever the stock Google TV home appears; no system packages get disabled
- **Clock & date**, one-click system **Settings** button, long-press menu (Hide / App info / Uninstall)
- Auto-refreshes when apps are installed or removed; English + Russian UI

## Installation (no computer needed)

1. Download `mega-tv-launcher.apk` from [Releases](../../releases) onto the TV box — e.g. with the
   **Downloader** app, **LocalSend**, or any file manager. Allow installing from unknown sources
   when the system asks.
2. Install the APK and open *Mega TV Launcher* from the app list — the launcher works right away.

### Make it your home screen

**Regular Android TV (AOSP boxes, Fire TV, older firmware):** press HOME — the system will offer
a launcher picker, or set it via *Settings → Apps → Default apps → Home app*.

**Google TV (Chromecast with Google TV, Xiaomi boxes on Google TV, etc.):** the firmware hardwires
the stock launcher, so use the built-in Home button override instead:

1. Open *Settings → System → Accessibility → Mega TV Launcher* and turn the service **on**.
2. On Android 13+ the toggle may be blocked with a *“Restricted setting”* warning for sideloaded
   apps. In that case first open *Settings → Apps → Mega TV Launcher* and choose
   **Allow restricted settings** (behind the ⋮ menu), then enable the service again.
3. Press HOME — the stock screen flashes for a split second and Mega TV Launcher takes over.
   Fully reversible: turn the accessibility service off and everything is back to stock.

### With adb (optional, for advanced users)

```
adb install mega-tv-launcher.apk
adb shell cmd package set-home-activity tv.megarohas.launcher/.MainActivity
adb shell cmd role add-role-holder --user 0 android.app.role.HOME tv.megarohas.launcher
```

On Google TV the two commands above are not enough (the stock launcher declares its HOME filter
with `priority=2` and always wins) — enable the accessibility override instead:

```
adb shell settings get secure enabled_accessibility_services
adb shell settings put secure enabled_accessibility_services "<previous value>:tv.megarohas.launcher/.HomeWatchService"
adb shell settings put secure accessibility_enabled 1
```

> **Warning:** do **not** disable the stock launcher
> (`pm disable-user com.google.android.apps.tv.launcherx`), even though most guides suggest it.
> It is the boot fallback — with it disabled, a crash or a slow start of any third-party launcher
> can leave the device on a black screen where network ADB is not up yet. The override above
> exists so you never have to.

## Build

JDK 17 + Android SDK (API 34):

```
gradlew assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`.

## Requirements

Android TV 5.0+ (minSdk 21, target 34). Tested on a Xiaomi box running Android 14 (Google TV).
