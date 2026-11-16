# Vesna Launcher — Product Overview

## Vision

Vesna Launcher is a fast, stable, modern Android home launcher with broad
personalization options. It is fully free: no ads, no subscriptions, no paid
features, no in-app purchases, no commercial analytics or tracking SDKs. The
project is developed in the open under the MIT license for an international
audience; the primary interface language is English with Russian localization.

## Product principles

1. **Speed first.** Cold start and every interaction must feel instant. Favor
   plain Android Views on the hot path (workspace, drawer, dock) over heavier
   abstractions.
2. **Reliability.** A launcher runs all day, every day. Memory leaks, ANRs, and
   layout drift across app install/uninstall storms are release blockers.
3. **Freedom and privacy.** No monetization of any kind; no telemetry. The only
   data the launcher stores is the user's own layout, locally.
4. **Personalization without lock-in.** Users should be able to shape the
   launcher to their habits; their setup must survive restarts and updates.
5. **Independent development.** See [docs/clean-room/PROCESS.md](docs/clean-room/PROCESS.md):
   third-party launchers are used only as functional references, never as
   sources of code, assets, or decompiled logic.

## MVP scope — version 0.1.0

Delivered in 0.1.0:

- HOME launcher registration and default-launcher selection through the system UI.
- Workspace: 4×5 grid of app icons with labels, tap to launch.
- App drawer: list of installed launchable apps (public Android APIs), scrolling,
  tap to launch; live updates on install/update/uninstall.
- Dock: up to 4 favorite apps at the bottom of the screen.
- Persistence: workspace and dock layout stored locally (Room), restored after
  process death and device reboot.
- Basic settings: About screen with version info (Compose + Material 3).
- English and Russian localization.

Explicitly out of scope for 0.1.0 (and deferred, not abandoned):

- Folders on the workspace.
- Widgets.
- Drag-and-drop rearrangement of icons (0.1.0 offers add/remove via long-press menus).
- Import of layouts from other launchers.
- Complex gestures beyond swipe-up to open the drawer.
- Icon packs, icon customization, themes.
- Extended animations and transitions.
- Work profile / multi-user support.
- Search in the drawer.

## Target users

Anyone who wants a clean, fast, private home screen. No account, no network
permission, no vendor lock-in.

## Success criteria for the MVP

- The APK installs and runs on Android 8.0 (API 26) through current Android versions.
- Selecting Vesna as the default launcher works through the standard system flow.
- App layout survives launcher restart and device reboot.
- Installing or uninstalling an app updates the drawer, workspace, and dock without
  restarting the launcher.
- No crashes during a normal day of use (launching apps, rotating the device,
  switching launchers back and forth).
