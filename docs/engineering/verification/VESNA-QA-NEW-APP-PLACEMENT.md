# Android verification: opt-in placement of newly installed apps

**Ticket:** [#107 / NT-HOME-04](https://github.com/vladon/vesnalauncher/issues/107).
**Candidate:** debug 0.4.52 (57), APK SHA-256
`67ee25cfbeb0adfbfffb37b73221ff5e06fdf82f6c13a3e16be1ac2ee6b02777`.
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.
Pixel Launcher remained the default Home app. Used two versions (1 and 2) of
`dev.vladon.qa.lifecyclefixture` (QA Fixture), signed with the same test key.

Vesna handles a fresh Android `LauncherApps.Callback.onPackageAdded` while its
process is running, picks one launchable activity for that package and profile,
and uses the first free cell on the selected page when enabled and unlocked.
Updating, starting Vesna, and availability changes do not trigger placement.
A full page or an existing pin in the same profile leaves the layout alone.
This verifies Vesna's sideload path, not equivalence to Nova's installer flow.

| Scenario | Observed result |
| --- | --- |
| First install, default off | QA Fixture was discoverable by name in the drawer. All existing workspace, dock, page, and folder records were unchanged. |
| First install, opted in | The setting survived a process restart. Installing v1 with Vesna already running added exactly one shortcut at the first free cell (index 9) without moving any saved item. The drawer found it and tapping the new Home shortcut opened the fixture activity. |
| Replacement and uninstall | Updating v1 → v2 retained the exact pinned row, including its ID and cell. Uninstall removed only the fixture pin and restored the original layout rows. |
| Locked install and unpinned update | With opt-in on and the layout locked, a fresh install appeared in the drawer only. Unlocking and updating this unpinned app also left the saved layout unchanged. |
| Install while stopped | With opt-in on but Vesna stopped, a fresh install showed in the drawer on the next start without adding a Home shortcut. |
| Settings and accessibility | Search for “new apps” opened the highlighted, labeled match. The native tree exposed one checkable target for the setting; Tab reached it and Enter changed it. Verified English Light, Russian Dark at 200% on a compact display (the full text is reachable by scrolling), and mirrored RTL using the Arabic app locale with English fallback. Checked a wider Light layout with three-button navigation and compact layouts with gestures. Measured text contrast against the Settings surface: Light 16.2:1 (title) and 8.9:1 (detail); Dark 14.35:1 and 10.9:1. |

All 10 cases in `NewAppPlacementTest` and `SettingsSearchLayoutTest` passed.
The Room tests additionally cover a selected secondary page, folder-reserved
cells, a full page with a free page elsewhere, duplicate suppression across
containers and different Android profile identifiers.

Restored layout, opt-in off, unlocked state, font scale, display size, app
locale, system theme and navigation mode. Local captures and DB snapshots:
`D:/orca/scratch/vesna-new-app-shortcut-qa/`. No physical device, live TalkBack
speech or managed-profile device was used; the profile check uses real Room
rows with separate profile identifiers.
