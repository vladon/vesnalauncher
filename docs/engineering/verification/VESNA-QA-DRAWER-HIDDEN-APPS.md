# Android verification: hide apps from browsing

**Ticket:** [#119 / NT-DRAWER-09](https://github.com/vladon/vesnalauncher/issues/119).
**Device:** Android 15 (API 35), dedicated `Vesna_QA_Menu_API_35` AVD, 420 dpi.
**Build:** debug 0.4.50 (55), APK SHA-256
`6afe9df2789eafa00b5162da4436cc1020762b196602ad26e5836989b4970ded`.

The AVD had stock apps and 120 disposable drawer aliases. Pixel Launcher
remained the default Home app. The starting Vesna layout had Clock in the
dock and Contacts on Home.

| Scenario | Observed result |
| --- | --- |
| Find and change the setting | In Light at 100% with gesture navigation, Settings search `hide apps` opened the highlighted section. The Clock and Contacts rows were each one accessible, checkable target. Tapping them hid those entries in the already-running Home process. |
| Browse and search | Both entries disappeared from browsing; the fast-scroll rail remained available. `Clo` showed no result. Completing the query to `Clock` returned Clock alone, and tapping it launched the app. |
| Restart and pins | Force-stopped Vesna and verified a new PID. Both entries remained hidden from browsing. The Contacts Home shortcut and Clock dock favorite stayed visible, Clock still launched from the dock, and Android reported it enabled. The stored shortcut, folder and page rows were unchanged. |
| Undo | Reopened the Settings page after restart, confirmed both switches were on, and turned them off. Both entries returned to browsing. |
| Compact and wide Settings | Dark with three-button navigation at 200% text: Russian and Arabic RTL at 320 × 640 dp, plus English at approximately 549 × 686 dp. Verified the localized heading, text wrapping, mirrored switch rows, a 48 dp or larger Chrome target, keyboard traversal to a switch, and hide/unhide in both compact layouts. |

The `AppSearchTest` suite passed all nine tests, including a personal/work pair
with the same activity and display name. Hiding one profile's entry left the
other visible during browsing and partial search; a normalized full-name search
returned both entries in their original order.
All 22 cases in `SettingsSearchLayoutTest`, `DrawerSearchTest`, and
`DrawerRecreationTest` passed on the same AVD.

Restored the original switches, locale, display size, font scale, night mode,
Home role and navigation mode. Screenshots and UI dumps are in
`D:/orca/scratch/vesna-hidden-apps-qa/`. No physical device was used. Live
TalkBack speech and a live managed-profile UI run were not checked.
