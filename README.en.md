# HyperGesture

🇧🇷 [Português](README.md) · 🇺🇸 English

A **gesture navigation app for HyperOS (Xiaomi, Redmi and POCO)** that lets you use **the
launcher you actually like** (Nova, Niagara, Lawnchair...) without losing your gestures — and it
also opens **Circle to Search** with a long press on the navigation handle, just like the system
does. It works **without root, Shizuku, ADB or a computer**: it's a single app.

**[⬇️ Download the latest APK](https://github.com/VictorEsidoro/HyperGesture/releases/latest/download/hypergesture.apk)**
· [all versions](https://github.com/VictorEsidoro/HyperGesture/releases) ·
[changelog](CHANGELOG.md)

> This is a **fork** of [HyperGesture](https://github.com/h3nc4/HyperGesture), created by
> **Henrique Almeida ([h3nc4](https://github.com/h3nc4))**. See [Credits](#credits) for who did
> what.

## The problem it solves

HyperOS ties gesture navigation to Xiaomi's own launcher. Switch to Nova, Niagara or Lawnchair
and full-screen gestures stop working: the phone drops back to the **three-button bar** with no
way to turn them on again. HyperGesture gives you the gestures back.

## What the app does

| Gesture | Does |
|---|---|
| Swipe in from the **left or right edge** | Back |
| Swipe **up from the bottom** | Home |
| Swipe up from the bottom **and hold** | Recent apps |
| Swipe **sideways along the bottom edge** *(optional)* | Previous / next app |
| **Touch and hold the middle of the bottom edge** *(optional)* | **Circle to Search** |

Also:

- **Everything is adjustable**: edge width, swipe distance, hold times and vibration.
- **In English and Portuguese**, with an in-app language picker.
- **Lightweight**: about 2.5 MB, no internet, no ads, no data collection.

## What this fork adds

- **Circle to Search on a long press of the navigation handle**, with no root and no other app
  (before, you had to pair MiCTS with a gesture app).
- **Behaves exactly like the system's**: the long press uses the same values Android uses on the
  stock handle — the **Touch and hold delay** you chose in Accessibility, the same movement
  tolerance and the same area (only over the handle, in the middle). Long presses elsewhere on
  the edge still reach the app underneath.
- **Full Brazilian Portuguese translation**, using HyperOS 3's **exact** screen names.
- **Language picker**: Automatic (Portuguese when the phone is in Portuguese, English otherwise),
  Português (Brasil) or English.
- **First steps inside the app**: an ordered list with colours (red to do, green done, amber to
  check) that opens each system screen and unlocks the next step only once the previous one is
  done.
- **Control Center shortcut**: shows whether gestures are running and, if HyperOS stops the
  service while the button bar is hidden, takes you straight back to Accessibility — no restart.
- **Credits** inside the app, and releases published here with the `.apk` attached directly, no
  zip.

The full list, version by version, is in [CHANGELOG.md](CHANGELOG.md).

---

## Getting it running (step by step, no tech skills needed)

The app isn't on the Play Store, so you install the `.apk` file yourself. It only takes a few
steps, once.

### Step 1 — Download and install

1. On your phone, tap **[Download the latest APK](https://github.com/VictorEsidoro/HyperGesture/releases/latest/download/hypergesture.apk)**.
2. Open the downloaded `hypergesture.apk` and install it.

> **Play Protect complained?** That's normal for apps installed outside the store. Choose to
> install anyway, or pause Play Protect just for the install.
>
> **Sent the file through WhatsApp?** It sometimes arrives altered and the install fails with
> "App not installed". Download it from the link above or move it through Google Drive.

### Step 2 — Follow the app's First steps

Open HyperGesture. The **First steps** card at the top shows what is left, in order, and every
step has a button that opens the right system screen:

| Colour | Means |
|---|---|
| 🔴 Red | Required and not done yet |
| 🟢 Green | Done |
| 🟡 Amber | The app cannot check it on its own: check it and tap **I did it** |
| ⚪ Grey | Locked until the step before it |

The essential steps, in order:

1. **Allow restricted settings** — Settings → Apps → HyperGesture → **App info** → **Advanced
   settings** section → turn on **Allow restricted settings**. Without it Android will not let
   you turn Accessibility on. **Skipping this is the most common reason the app seems to do
   nothing.** The option only appears until it has been allowed.
2. **Turn HyperGesture on** — **Accessibility** → **HyperGesture** → turn on **Use
   HyperGesture**.
3. **Battery: no restrictions** — on the app's battery page, choose no restrictions.
4. **Autostart** — turn it on for HyperGesture, so gestures come back by themselves after a
   restart.

HyperOS likes to stop background apps to save battery — and when that happens to an
Accessibility app, its permission is switched off too. Steps 3 and 4 prevent it. *(Optional)*
In the recent apps screen, hold HyperGesture's card and tap the **lock**, so "Clear all" won't
close it.

The exact names of these HyperOS screens vary by language; the Portuguese README lists the
pt-BR ones exactly as HyperOS 3 shows them.

Once everything is green, the card folds to **All set**. If HyperOS stops the service later, it
opens again by itself, showing what to redo. Finished steps keep their button to the system
screen, and a step marked with **I did it** can be unmarked.

### Step 3 — Add the Control Center shortcut *(recommended)*

On the **Control Center shortcut** card, tap **Add to Control Center**.

Why it matters: if HyperOS stops the gesture service while the button bar is hidden, you are
left with no Back and no Home, and the only way out would be restarting the phone. The Control
Center still opens with a swipe from the top of the screen, and the shortcut shows whether
gestures are running. When they are stopped, one tap on it opens HyperGesture's Accessibility
page so you can turn it back on.

If the button is missing or does nothing, edit the Control Center and add the HyperGesture
shortcut by hand.

### Step 4 — Turn on Circle to Search *(optional)*

1. In the app's **Circle to Search** card, turn on **Bottom edge long press = Circle to
   Search**.
2. Google must be the **digital assistant app**. The card's button opens the right screen.
3. Done: touch and hold the middle of the bottom edge. The phone vibrates and Circle to Search
   opens.

### Step 5 — Hide the button bar *(optional)*

The **Button bar** card shows whether the bar is hidden and takes you to the system screen
where you can pick gesture navigation and hide the three-button bar to reclaim the space. It unlocks only once
HyperGesture is on: a hidden bar with no gestures leaves you with no way to navigate. With a
third-party launcher, the gestures that work are still HyperGesture's — it's only about screen
space.

---

## Getting to know the app

| Card | What you'll find |
|---|---|
| **First steps** | What is left for gestures to work, in order and colour-coded, with a button for each system screen. Folds to "All set" when done. |
| **Control Center shortcut** | Why the shortcut matters, whether it is added, and the button to add it. |
| **Button bar** | Whether the bar is hidden, and a shortcut to Navigation mode. Unlocks only with HyperGesture on. |
| **App switching** | Turns on the sideways swipe along the bottom edge. Needs **usage access**. |
| **Circle to Search** | Turns on the handle long press, with an optional manual hold time instead of the system's. |
| **Your gestures** | A reminder of what each gesture does. |
| **App language** | Automatic, Português (Brasil) or English (Android 13 or newer). |
| **Gesture settings** | Widths, distances, times and vibration. Collapsed by default so a stray tap changes nothing. |
| **Diagnostics** | Device details and the last error. Useful for reporting problems — nothing leaves the phone. |
| **Credits** | Who made the app and where each part comes from. |
| **The shortcut itself** (in the Control Center) | Outside the app: shows whether gestures are running and opens Accessibility if not. |

---

## Privacy

- **No internet**: the app has no network permission. Nothing leaves your phone.
- **Accessibility used only to navigate**: the service watches for touches in thin strips along
  the screen edges. It **does not read** what's on screen, your text or your keystrokes.
- **Usage access** is only requested if you turn on app switching, to know which app came
  before.
- **Circle to Search**: once it opens, the screen is read by the **Google app** — exactly as when
  you use the handle gesture on Xiaomi's launcher. HyperGesture only asks it to open.

---

## What it can't do

- **It can't turn on HyperOS's own gestures.** Those depend on permissions only the system has.
  HyperGesture does the gestures itself.
- **The app open/close animation** (the app shrinking back into its icon) belongs to the system
  and only works fully with Xiaomi's launcher. With another launcher you get Android's default
  transition.
- **Circle to Search depends on Google**: it needs the Google app as the digital assistant and
  the feature enabled for your device.

---

## Common problems

| Symptom | What to do |
|---|---|
| "App not installed" | If it came through WhatsApp, download it again from the link or through Drive. If a **test (debug)** build is installed, uninstall it first — the signature is different. |
| The Accessibility switch won't turn on | You missed **Allow restricted settings** (the first item in First steps). |
| Gestures stop on their own / Accessibility turns off | Check the battery and autostart items in First steps. If you see "This service is malfunctioning", turn HyperGesture off and on again in Accessibility. |
| Gestures stopped and the button bar is hidden | Swipe down from the top to open the Control Center and tap the HyperGesture shortcut: it opens Accessibility so you can turn it back on. |
| Circle to Search doesn't open | Check that Google is the **digital assistant app**. The reason appears under **Diagnostics → Last failure**. |
| A long press on the edge opens Circle to Search by accident | It only counts in the middle, over the handle. If it still gets in the way, turn off **Use the system touch and hold delay** and raise the manual time. |
| Taps near the bottom edge feel slightly slow | Expected: the app has to briefly hold each edge touch to see whether it becomes a gesture. Lowering the **Edge activation width** shrinks the affected area. |
| A gesture fires by accident or won't fire | Adjust that edge's swipe distance in **Gesture settings**. |

---

## Credits

- **[HyperGesture](https://github.com/h3nc4/HyperGesture)** by **Henrique Almeida
  ([h3nc4](https://github.com/h3nc4))** — created the app: the gestures, the edge touch capture,
  app switching, the screens and the whole foundation this fork is built on.
- **[MiCTS](https://github.com/parallelcc/MiCTS)** by **parallelcc** and
  **[CTSLauncher](https://github.com/chenxiaolong/CTSLauncher)** by **chenxiaolong** — showed how
  to open Circle to Search **without root** by talking directly to Android's assistant service.
  The technique used here is theirs.
- **Victor Eduardo ([@victoresidoro](https://github.com/VictorEsidoro))** — conceived and led this
  fork: Circle to Search built into the gesture app, tuning it to the system's own values for
  navigation as fluid as the stock one, the Portuguese translation with HyperOS's real screen
  names, and the language picker.
- Built with help from **Claude** (Anthropic).

All three projects are free software under the **GPL-3.0**, and so is this one.

---

## For developers

An Android project in **Kotlin + Jetpack Compose**, with gesture recognition kept in plain
Kotlin (no Android types) so it can be tested on the JVM. Building, testing and on-device
debugging are covered in [`docs/development.md`](docs/development.md); what HyperOS does and
doesn't allow about the navigation bar is in
[`docs/hyperos-navigation.md`](docs/hyperos-navigation.md).

In this fork, the [`fork-apk.yaml`](.github/workflows/fork-apk.yaml) workflow runs the tests and
lint, builds the signed release APK and publishes a Release with the `.apk` whenever
`versionName` changes. `dev/...` branches go through the same tests and build an APK to download from Actions, without publishing a release. Signing uses the `ANDROID_KEYSTORE_B64` and `KEYSTORE_PASSWORD` secrets,
the same names as upstream.

---

## License

<!-- vale off -->

HyperGesture is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

HyperGesture is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with HyperGesture. If
not, see https://www.gnu.org/licenses/.

Copyright (C) 2026  Henrique Almeida &lt;me@h3nc4.com&gt; — changes in this fork (C) 2026 Victor Eduardo
(@victoresidoro).
