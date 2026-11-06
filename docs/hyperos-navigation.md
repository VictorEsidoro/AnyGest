# HyperOS navigation

The gesture service does not depend on any of this. Everything here concerns the optional second capability: getting HyperOS to hide its three-button navigation bar.

## Current behaviour

`navigation/HyperOSNavigationIntegration.kt` fires the vendor settings intent:

```kotlin
startActivity(
    Intent("com.android.settings.NAVIGATION_MODE_SETTINGS")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
)
```

Confirmed working on a Redmi 14 running Android 16 and HyperOS OS3.0. It opens the navigation-mode screen and the user taps the option. `adb shell am start` reaches that screen by the same `ActivityManager.startActivity` path, so no privileged permission is involved.

Choosing gesture navigation there hides the bar and gains screen space. It does not give a third-party launcher working native gestures, so AnyGest's own recognizer remains the thing that performs navigation.

## TODO: apply the overlay directly

Opening a settings window is a poor substitute for doing the thing. The state to reach is this one:

```bash
cmd overlay enable com.android.internal.systemui.navbar.gestural
```

`cmd overlay list` shows the current state.

This is blocked because the call needs `android.permission.CHANGE_OVERLAY_PACKAGES`, which is signature-level, so only the platform signer can hold it. If a route ever appears that does not require a signature permission, replacing the settings intent with a direct overlay call is the change to make.

## Routes already closed

| Route | Why it fails |
| --- | --- |
| `cmd overlay enable ...` from the app | `CHANGE_OVERLAY_PACKAGES` is signature-level |
| Writing `Settings.Secure` `navigation_mode` | `WRITE_SECURE_SETTINGS` is signature or privileged. Reading it is unrestricted, so the app can report the mode but never change it |
| Shizuku | Apache-2.0 and legally forkable, yet its privileged server still has to be started by ADB or root |
| Granting the permission over adb | Works, but only with Xiaomi's "USB debugging (Security settings)" toggle on (Mi account and SIM): without it `adb shell` holds neither `WRITE_SECURE_SETTINGS` nor `GRANT_RUNTIME_PERMISSIONS`. See "ADB mode" below |

Community consensus through 2025 and 2026 is that no ADB-free way exists to force native gestures for a third-party launcher. Lawnchair closed its own issue on this as not planned.

## Also unfinished

Auto-clicking the toggle once the settings screen opens, using the app's own accessibility service (`findAccessibilityNodeInfosByText` with `ACTION_CLICK`). That needs only public APIs and no extra permission, but it is fragile:

- labels are localised
- view ids change between builds
- HyperOS versions drift

Worth doing only as best-effort, with a manual fallback.

HyperOS also resets the navigation mode across a reboot. `boot/BootCompletedReceiver.kt` observes that and reports it. A normal APK cannot restore it.

## ADB mode (3.4.0)

The route above is open once, by hand: with the Security-settings toggle on, `pm grant io.github.victoresidoro.anygest android.permission.WRITE_SECURE_SETTINGS` succeeds, and the grant outlives the debugging (to be confirmed per device by turning developer options off and rebooting; the app reports it in the ADB mode card and in Diagnostics).

What it unlocks, all behind `SecureSettingsAccess`:

- `Settings.Global force_fsg_nav_bar`: on a Poco F8 Ultra (HyperOS, Nova Launcher as default home) `navigation_mode` read 0 (buttons) with `navigation_mode_restore` 2, all three navbar overlays were inactive, and writing `force_fsg_nav_bar=1` hid the bar while AnyGest's gestures kept working. HyperOS reverts the mode on its own, so the service watches the key and rewrites it. Written on HyperOS only.
- `Settings.Secure enabled_accessibility_services` / `accessibility_enabled`: the service adds or removes its own component (merging with the other entries, short and full class spellings both matched) to switch itself on again after boot or a kill, and off for the tile switch and for exception apps.

Exception apps: window events are requested at runtime for the listed packages only (`setServiceInfo` with `packageNames`), so no other app wakes the process. On the first event the service starts `ExceptionWatchService` (a `specialUse` foreground service; if the system refuses the start, the pause is cancelled), shows the bar, and calls `disableSelf()`. The watcher polls the usage events for the foreground app and writes the service back into the enabled list when another app takes over, or at once if usage access disappears. Without a live watcher (process killed) the pause is treated as stale at the next boot, app open or tile pull-down.

Without the grant none of this runs and the app behaves as in 3.3.x.
