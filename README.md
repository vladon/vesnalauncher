# Vesna Launcher

Vesna Launcher is a fast, free, and open-source Android home launcher built for an
international audience. It has no ads, no subscriptions, no paid features, no in-app
purchases, and no commercial analytics or tracking SDKs — and it never will.

- **Package name:** `dev.vladon.vesna.launcher`
- **License:** [MIT](LICENSE)
- **Status:** `0.1.0` — first working MVP
- **Language:** Kotlin, English UI (Russian localization included)

## Features (0.1.0)

- Registers as an Android HOME launcher; can be set as the default launcher through
  the system dialog (Role Manager on Android 10+, system home settings on older versions).
- Workspace with a 4×5 grid, app icons and labels, tap to launch.
- App drawer with all installed launchable apps, smooth scrolling, swipe-up gesture
  and a grab handle to open it.
- Dock with up to 4 favorite apps.
- Icon layout persisted in a local Room database; restored after restarts and reboots.
- Install/update/uninstall of apps is reflected live in the drawer, workspace, and dock.
- Settings screen (Jetpack Compose + Material 3) with app and version info.
- English and Russian localization.

Not yet implemented: folders, widgets, drag-and-drop rearrangement, icon packs,
themes, gestures beyond drawer open/close. See [PRODUCT.md](PRODUCT.md) for the roadmap.

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

Every push and pull request is built and tested by GitHub Actions
([.github/workflows/android.yml](.github/workflows/android.yml)). Pushing a `v*`
tag runs the release job, which attaches the APK to a GitHub Release (until 1.0
this is the debug build; release signing arrives with 1.0). No build artifacts
are committed to the repository.

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
