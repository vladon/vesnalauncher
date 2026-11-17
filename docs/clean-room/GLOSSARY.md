# Vesna Launcher — Glossary

Shared terminology for observations, specifications, and acceptance tests.
Use these terms consistently; define new terms here before first use.

| Term | Meaning in this project |
|---|---|
| Workspace (home screen) | The paged, widget-and-icon surface shown when no app is open. |
| Page | One scrollable screen of the workspace. |
| Cell | One grid slot of a page that can hold an icon, folder, or widget. |
| Dock | A persistent favorites area at the bottom of the workspace. |
| Drawer | The scrollable surface listing installed apps, typically opened by a swipe. |
| Folder | A container holding multiple app icons, displayed as one icon. |
| Widget | An interactive app-provided component placed on a page. |
| Icon label | Text shown under an app/folder/widget icon. |
| Adaptive icon | Android icon format that can be masked into different shapes. |
| Icon pack | A user-installable theme replacing default app icons. |
| App shortcut | Launcher-provided quick action exposed by an app (Android shortcuts API). |
| Notification badge | Count/dot on an icon reflecting notifications. |
| Work profile | Android secondary profile (badged apps) with its own app list. |
| Default launcher | The app holding the Android HOME role; started on Home. |
| Backup | A user-exported file capturing launcher layout/settings. |
| Restore | Importing a backup into the launcher. |
| Gesture action | A behavior bound to an input gesture (e.g. swipe down → notifications). |
| Evidence ID | `EVI-<AREA>-<NNN>` pointer into the restricted local capture store. |
| Environment ID | `ENV-<N>` row in TEST_ENVIRONMENTS.md. |
| Observed | Behavior directly triggered and seen during this research. |
| Partially Observed | Behavior seen but not exhaustively exercised. |
| Inferred | Behavior concluded from labels/other evidence without direct observation; never used in approved specs without labeling. |
| Not Tested | Discovered but not yet exercised. |
