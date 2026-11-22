<h1 align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="./design/readme/hero-dark.svg">
    <img src="./design/readme/hero-light.svg" alt="Vesna Launcher" width="960">
  </picture>
</h1>

<p align="center"><strong>A fast, free, open-source Android home launcher</strong></p>

<p align="center">
  <a href="https://github.com/vladon/vesnalauncher/releases/latest"><strong>Get the signed APK</strong></a><br>
  <a href="#features">Features</a>
  · <a href="#design">Design</a>
  · <a href="#build">Build</a>
</p>

<p align="center"><sub>Android 8.0+ · English and Russian · MIT license</sub></p>

## Install

Download the signed APK from [GitHub Releases](https://github.com/vladon/vesnalauncher/releases/latest),
install it, and choose **Vesna Launcher** as your default Home app in Android
Settings. See the [latest release](https://github.com/vladon/vesnalauncher/releases/latest)
for download details and release notes.
The Android package name is `dev.vladon.vesna.launcher`.

## Features

- **Home and dock.** Add, switch, and reorder Home pages. Move shortcuts and
  folders between pages and keep up to four favorites in the dock. Vesna saves
  your layout locally and restores it after a restart.
- **App drawer and search.** Swipe up or tap the app-grid button. Search by name
  with or without accents, and press Enter to open a single result.
- **Drag and drop.** Drag apps from the drawer to the workspace or dock. Move
  pins between the workspace and dock. Drop an app on another in the workspace
  to create a folder, then rename it or remove members as needed.
- **Work profiles.** Launch work apps from badged icons. Removing a work copy
  leaves its personal counterpart's pins in place.
- **Appearance and accessibility.** Choose System, Light, or Dark and decide
  whether to show app names. Keyboard shortcuts open the same menus as touch.
- **Free and private.** Vesna is free of ads, subscriptions, paid features,
  in-app purchases, commercial analytics and tracking SDKs.

<details>
<summary>Feature history</summary>

### 0.4.32

- Keyboard focus stays on the selected app in the open app drawer when
  the system theme changes, including while search is active.

### 0.4.31

- Keyboard focus stays on the same Home or dock shortcut when the system
  theme changes.

### 0.4.30

- Keyboard focus stays on the same app in an open Home folder when the
  system theme changes.

### 0.4.29

- An open Home folder stays open when the system theme changes. Back
  and Home still close it.

### 0.4.28

- The folder rename dialog keeps the “Enter a folder name” message
  after a system theme change. Entering a valid name clears it.

### 0.4.27

- Drawer search shows “No matching apps” above the keyboard even when
  landscape and enlarged text leave little vertical space.

### 0.4.26

- In landscape, drawer search stays in the app instead of opening a
  full-screen keyboard editor that hides the results.

### 0.4.25

- A folder rename in progress survives a system theme change. Save or
  Cancel still returns to the folder if you started there.

### 0.4.24

- An open app drawer keeps its search when Android recreates the launcher,
  for example after a system theme change. Closing the drawer still clears the search.

### 0.4.23

- Home shortcuts and the dock stay clear of side navigation bars in
  landscape. Wide, short Home tiles show the icon alongside the app or folder name.

### 0.4.22

- Rotating the device keeps your Settings search query or the setting you
  opened from a search result. Opening Settings again starts clean.

### 0.4.21

- Touching the screen with a second finger during a drag cancels the move
  instead of adding or moving a shortcut by accident.

### 0.4.20

- The first Tab in Settings reaches Search. Activating Clear with the keyboard
  returns focus to Search so you can type a new query immediately.

### 0.4.19

- Settings search stays visible above the keyboard in short windows at large
  text sizes. The query stays in place when the keyboard hides, and matching
  settings still open at the highlighted control.

### 0.4.18

- The app drawer keeps search, Clear, and Close visible above the keyboard
  in short windows, including at 200% text. Hiding the keyboard keeps the
  current search and results.

### 0.4.17

- Long context menu labels wrap instead of being cut off in narrow windows
  or at larger text sizes.

### 0.4.16

- The folder name stays visible while typing in short windows, even with
  enlarged text and the keyboard open. The dialog scrolls to make its
  title, validation message and actions reachable.

### 0.4.15

- Folder apps remain visible and scrollable in short, narrow windows with
  large text. When the action labels do not fit, Rename and Remove stay
  available as compact, accessible icon buttons.

### 0.4.14

- Replaced the placeholder app icon with the approved Vesna mark. The
  Android themed icon uses its own monochrome artwork, and About shows the
  same Vesna branding.

### 0.4.13

- Home and dock icons fit narrow windows without being cropped or moving
  shortcuts to different slots.

### 0.4.12

- Page numbers stay readable with large text in very narrow windows. The
  Home controls give up outer spacing before shrinking their touch targets.

### 0.4.11

- Home options, the page picker, and the app drawer stay fully tappable in
  narrow windows. If the page arrows don’t fit, use the picker or swipe
  between pages.

### 0.4.10

- Touching the Home workspace with a second finger no longer switches
  pages by mistake. Single-finger swipes and the page controls still work.

### 0.4.9

- A long press still opens a dock shortcut’s menu if your finger moves
  slightly. A deliberate drag still moves the shortcut.

### 0.4.8

- Fixed a cancelled long press in the app drawer taking over your next swipe.
  The drawer scrolls normally after the cancellation.

### 0.4.7

- Fixed a quick scroll back to the top closing the app drawer. Swipe down
  again from the top to close it.

### 0.4.6

- Fixed icons drawing over the header and buttons when scrolling long folders
  on narrow screens with large text.

### 0.4.5

- Search by setting name or description. Results show the parent section and
  scroll to the highlighted setting without changing its value.
- The search field stays visible while the results scroll.

### 0.4.4

- Search Vesna settings and open separate pages for default launcher setup,
  Appearance, and About. Settings opens without a leftover search or keyboard.
- Android Back closes the keyboard before clearing search. Each settings page
  has a [direct Android link](docs/engineering/verification/VESNA-QA-SETTINGS-NAVIGATION.md).

### 0.4.3

- Manage multiple Home pages, including a page picker and moves between pages.
  Existing shortcuts and folders migrate to the first page without moving.
- Use the Home options and drawer buttons, keyboard menus, more forgiving
  search, and layouts that adapt to narrow screens and larger text.
- Keep the drawer at the same scroll position when apps change. Folder
  operations preserve exact shortcuts and explain when there is not enough room.
- Choose fixed Vesna light and dark themes and optionally hide app names.

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

</details>

See the [product overview](PRODUCT.md) and [engineering roadmap](docs/engineering/ROADMAP.md)
for project scope and future work. Widgets, icon packs, and fuzzy search are
planned or deferred.

## Design

The Vesna identity pairs a sprouting V with Spring Green, Fresh Teal and Sky
Blue. The launcher design specification builds on Material Design 3 with
light and dark palettes, Nunito headlines and Inter interface text.

[Design specification](design/system/README.md) ·
[Design tokens](design/system/tokens.json) ·
[Logo assets](design/system/brand/) ·
[Adaptive icon](design/system/adaptive-icon/)

Open [`design/system/preview.html`](design/system/preview.html) in a browser
from your checkout to try the proposed components in both themes.

## Build

Vesna is written in Kotlin. Use JDK 17+ and Android SDK platform 37. CI installs
`platforms;android-37.2` and `build-tools;36.0.0`. The minimum API level is 26.
See the [development guide](docs/DEVELOPMENT.md) for setup.

```bash
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

On Windows, use the PowerShell equivalent with the path to your installed JDK.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21'
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. The
[Android CI workflow](.github/workflows/android.yml) builds the app, compiles
instrumentation test sources, runs JVM tests and runs Android Lint on pull
requests and pushes to `main`.

<details>
<summary>Release builds and signing</summary>

Run `./gradlew assembleRelease` to build a release APK. A local release is
unsigned unless you export the required signing variables.

```bash
export VESNA_RELEASE_KEYSTORE=/path/to/vesna-release.keystore
export VESNA_RELEASE_STORE_PASSWORD=...
export VESNA_RELEASE_KEY_ALIAS=vesna
export VESNA_RELEASE_KEY_PASSWORD=...
./gradlew assembleRelease
```

A `v*` tag triggers the release job. CI restores the keystore from the
`RELEASE_KEYSTORE_B64` secret and uses `RELEASE_STORE_PASSWORD`,
`RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` to sign the APK. The job checks
the signature with `apksigner` and attaches the APK to the GitHub Release.
The maintainers keep an offline copy of the signing material. Keep signing
material and local build outputs outside the repository.

</details>

## Documentation

- [Product overview](PRODUCT.md)
- [Development workflow](docs/DEVELOPMENT.md)
- [Architecture overview](docs/architecture/OVERVIEW.md)
- [Independent development policy](docs/clean-room/PROCESS.md)

## Contributing

Issues and pull requests are welcome. Use short-lived branches, Conventional
Commits and English documentation, and make sure CI passes before merging.

## License

Vesna Launcher is distributed under the [MIT license](LICENSE).
