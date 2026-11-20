# Development

Everything a contributor needs. For what the app is and how to use it, see the [README](../README.md).

## Stack

- **Language.** Kotlin, JDK 21
- **Build.** Gradle with the Android Gradle Plugin, both provided by the build image and pinned there to one version
- **Gestures.** `AccessibilityService` plus `TYPE_ACCESSIBILITY_OVERLAY` edge strips, which need no "draw over other apps" permission
- **UI.** Jetpack Compose, Material 3
- **Persistence.** DataStore Preferences, no database
- **Quality.** JUnit over device-free logic, Android lint, shellcheck and CodeQL

Versions are pinned in `gradle/libs.versions.toml`, and Renovate raises the bumps in grouped pull requests. `minSdk` is 30 because the edge overlays need `WindowMetrics` insets and `setSystemGestureExclusionRects`.

## Repository layout

```text
app/
  src/main/kotlin/io/github/victoresidoro/anygest/
    gesture/      Pure, Android-free recognition (unit tested)
    service/      AccessibilityService, edge overlays, global actions
    navigation/   SystemNavigation: the mode, the Xiaomi check, the mode screen
    settings/     DataStore-backed configuration
    diagnostics/  Local-only device/state snapshot
    boot/         BOOT_COMPLETED observer
    ui/           MainActivity, the single Compose screen, theme
  src/test/kotlin/            Plain-JVM tests, no device
docker/           The build image (docker/build.Dockerfile)
scripts/          build-apk.sh and version.sh
docs/             This file and the platform investigations
.github/          CI workflow, the toolchain action and CodeQL
```

## Build image

Everything that builds the app runs in one image, `docker/build.Dockerfile`, which starts from a pinned plain Debian and adds a JDK 21, the Android SDK and Gradle (checked by sha256). It is the only place the toolchain is installed, so nothing is needed on the host except Docker. No image, action or package mirror of another project is used: the base is the official Debian image and the SDK and Gradle come from Google's and Gradle's own servers.

The file has three stages:

- `toolchain`: the tools above. CI builds it to run the unit tests and lint, and to verify the APK signature.
- `builder`: copies the repository in and runs `assembleRelease`.
- `final`: `FROM scratch`, holding only `anygest.apk`, so neither the key nor its password is in a layer that could be published.

CI caches the toolchain layers per version of the Dockerfile (`.github/actions/toolchain`), so the first build after a change to the file takes a few minutes and the rest reuse the cache. The Gradle downloads are cached separately.

## Commands

Gradle is the only task runner. There is no Node toolchain. With Docker alone:

```sh
docker buildx build -f docker/build.Dockerfile --target toolchain --load -t anygest-toolchain .
docker run --rm -v "$PWD:/workspace" -w /workspace anygest-toolchain gradle testDebugUnitTest lintDebug
./scripts/build-apk.sh 1.0.0   # signed release APK, left in the repository root
```

With a local JDK and Android SDK the usual tasks work too: `gradle assembleDebug`, `gradle test`, `gradle lint`.

## Testing

The tests are plain JVM unit tests (`gradle test`): the decision logic is kept free of `android.*` imports, so it runs without a device or Robolectric. Timers are driven by calling the handlers directly (`onHoldElapsed()`, `onLongPressElapsed()`), so no clock is involved. Behaviour that only a phone can show is covered by the diagnostics the app writes, which the maintainer reads from real devices.

## Releasing

Signing material lives outside git:

- `android.keystore` at the repository root
- `KEYSTORE_PASSWORD` in `.env`, gitignored and sourced by `scripts/build-apk.sh`

```sh
./scripts/build-apk.sh 1.0.0   # produces ./anygest.apk
```

The password reaches the build as a Docker build arg, and the final image stage is `FROM scratch` carrying only the APK, so a publishable layer contains neither the key nor the password. With either piece missing, the release build degrades to unsigned rather than failing, so CI can still verify that `assembleRelease` compiles.

**Back up the keystore somewhere off this machine.** Android identifies an app by its signing certificate, so an update signed with a different key is refused by every device that already has AnyGest on it, and the only recovery is a new package name. `android.keystore` and `.env` are gitignored and must never be committed, which also means git is not a backup: keep the key and its password in a password manager, `base64` the keystore if the manager only takes text.

Pushing a `v*.*.*` tag runs the same build in CI and attaches the APK to a GitHub release. That needs two repository secrets, `ANDROID_KEYSTORE_B64` (`base64 -w0 android.keystore`) and `KEYSTORE_PASSWORD`. Unlike a local build, CI stops with an error when either is missing rather than publishing an unsigned APK, and it runs `apksigner verify --print-certs` on the result so the certificate is visible in the job log. After a release, CI bumps `versionCode` on `main`, because Android refuses an install whose `versionCode` went backwards.
