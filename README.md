# Vesna Launcher

Vesna Launcher is a fast, free, and open-source Android home launcher built for an
international audience. It has no ads, no subscriptions, no paid features, no in-app
purchases, and no commercial analytics or tracking SDKs — and it never will.

- **Package name:** `dev.vladon.vesna.launcher`
- **License:** [MIT](LICENSE)
- **Status:** `0.2.0`
- **Language:** Kotlin, English UI (Russian localization included)

## Features

### 0.2.0

- Drag-and-drop on the workspace: long-press an icon and drop it on an empty cell
  or on another icon to swap them.
- Work-profile apps are listed and launchable (discovery and launching via
  `LauncherApps`, with profile badges on the icons).
- Signed release builds: GitHub Releases carry APKs signed with the project key.

### 0.1.0

- Registers as an Android HOME launcher; can be set as the default launcher through
  the system dialog (Role Manager on Android 10+, system home settings on older versions).
- Workspace: 4×5 grid, app icons with labels, tap to launch; long-press menus for
  add/remove (workspace, dock, drawer).
- App drawer: smooth scrolling grid of installed launchable apps, swipe-up gesture
  or grab handle to open.
- Dock with up to 4 favorite apps.
- Icon layout persisted in a local Room database; restored after restarts and reboots.
- Install/update/uninstall of apps is reflected live in the drawer, workspace, and dock.
- Settings screen (Jetpack Compose + Material 3) with app and version info.
- English and Russian localization.

Not yet implemented: folders, widgets, drag between dock and drawer, icon packs,
themes, search in the drawer. See [PRODUCT.md](PRODUCT.md) for the roadmap.

## Build

Requirements: JDK 17+, Android SDK with platform 37.

```bash
./gradlew assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease        # unsigned release APK
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android Lint
```

On Windows shells (Git Bash/PowerShell) invoke `gradlew.bat` with an explicit
`JAVA_HOME`, for example:
`JAVA_HOME='C:\Program Files\Microsoft\jdk-21' ./gradlew.bat assembleDebug`.

Release builds are signed only when the signing environment is present, so plain
`./gradlew assembleRelease` locally produces an unsigned APK. To sign locally, set:

```bash
VESNA_RELEASE_KEYSTORE=/path/to/vesna-release.keystore
VESNA_RELEASE_STORE_PASSWORD=...
VESNA_RELEASE_KEY_ALIAS=vesna
VESNA_RELEASE_KEY_PASSWORD=...
```

The keystore itself never lives in the repository: CI restores it from the
`RELEASE_KEYSTORE_B64` secret (alongside `RELEASE_STORE_PASSWORD`,
`RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`). The maintainers keep an offline
copy of the same material.

Every push and pull request is built and tested by GitHub Actions
([.github/workflows/android.yml](.github/workflows/android.yml)). Pushing a `v*`
tag runs the release job, which builds the **signed** release APK, verifies the
signature with `apksigner`, and attaches it to the GitHub Release. No build
artifacts or signing material are committed to the repository.

## Documentation

- [PRODUCT.md](PRODUCT.md) — product concept and MVP boundaries.
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) — trunk-based development workflow,
  commit conventions, CI gates, release process.
- [docs/architecture/OVERVIEW.md](docs/architecture/OVERVIEW.md) — architecture.
- [docs/clean-room/PROCESS.md](docs/clean-room/PROCESS.md) — independent development
  policy (how third-party launchers may and may not be used as references).

## Contributing

Issues and pull requests are welcome. Please follow the conventions in
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md): short-lived branches, Conventional
Commits, English documentation, and green CI before merge.

## License

Vesna Launcher is distributed under the MIT license. See [LICENSE](LICENSE).
