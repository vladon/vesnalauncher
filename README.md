# Vesna Launcher

Vesna Launcher is a fast, free, and open-source Android home launcher built for an
international audience. It has no ads, no subscriptions, no paid features, no in-app
purchases, and no commercial analytics or tracking SDKs — and it never will.

- **Package name:** `dev.vladon.vesna.launcher`
- **License:** [MIT](LICENSE)
- **Version:** `0.4.2` ([latest published release](https://github.com/vladon/vesnalauncher/releases/latest))
- **Language:** Kotlin, English UI (Russian localization included)

## Features

### 0.4.3 (unreleased)

- Workspace pages now have stable IDs in storage. Existing layouts migrate to
  the default page without moving pins; the launcher still shows one page.
- Drag or remove a shortcut without affecting another copy of the same app.
  This covers the workspace, dock, and drops into folders.
- Drop apps from the drawer onto another app to create a folder, or onto a
  folder to add them. Existing workspace and dock shortcuts stay put.

### 0.4.2

- Disabling an app no longer deletes its workspace or dock pins or its folder
  memberships. Its icons return when the app is re-enabled.
- Moving a pin within the workspace or from the dock to the workspace no
  longer moves another copy of the same app.
- Folder creation and member removal save together with their layout changes.
  Dropping a drawer icon onto its own workspace pin no longer creates a
  one-item folder.
- Fixed bottom-row icons overlapping the dock, the drawer item menu anchor,
  and dock capacity counting folder members as separate favorites.

### 0.4.1

- Drag-and-drop fixes: drops from the drawer commit reliably, recycled drawer
  items no longer fire stale drag-release listeners, and dock reordering and workspace-to-dock drops ignore folder members. Folder cells count as
  occupied in free-cell placement, and removing a folder pre-checks that the
  grid can fit its members.

### 0.4.0

- Workspace folders: dropping an icon onto another icon creates a folder and
  dropping onto a folder adds to it; folders render as a folder icon with a
  member count, open in a dialog grid, support rename and member removal, and
  auto-dissolve when fewer than two members remain.
- Drag-and-drop from the drawer: long-press arms the drag, the drawer closes
  mid-gesture, and the app lands on a workspace cell or a dock slot.

### 0.3.0

- Drag-and-drop between containers: drag a workspace icon into the dock (drop
  onto a slot to insert it there; a full dock rejects with a message) and a dock
  icon onto an empty workspace cell; dragging sideways inside the dock reorders
  favorites.
- Drawer search: a search field above the app grid filters apps by name
  (case-insensitive substring), with an empty-state message.

### 0.2.0

- Drag-and-drop on the workspace: long-press an icon and drop it on an empty cell
  or on another icon to swap them (0.4.0 replaced the swap with folder creation).
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

Not yet implemented: widgets, icon packs, themes, and fuzzy search matching.
See [PRODUCT.md](PRODUCT.md) for the roadmap. Work-profile apps are discovered
and shown with a profile badge; uninstalling a work copy does not remove its
personal counterpart's pins.

## Build

Requirements: JDK 17+, Android SDK. The app targets `compileSdk`/`targetSdk`
37 (`minSdk` 26); CI installs `platforms;android-37.2` and
`build-tools;36.0.0`. Full setup instructions: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

```bash
./gradlew assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease        # unsigned release APK
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android Lint
```

In PowerShell, invoke `gradlew.bat` with an explicit `JAVA_HOME`, for example:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21'
.\gradlew.bat assembleDebug
```

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
