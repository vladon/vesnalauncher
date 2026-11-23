# Android verification: app actions on Home

**Ticket:** [#114 / NT-HOME-05](https://github.com/vladon/vesnalauncher/issues/114).
**Candidate:** debug 0.4.54 (59). **Device:** Android 15 (API 35),
`Vesna_QA_Menu_API_35`, 420 dpi, gesture navigation.

The fixture publisher (`dev.vladon.qa.actionfixture`) registers two genuine
Android dynamic shortcuts through `ShortcutManager.setDynamicShortcuts`:
`new-note` (**Compose test note**) and `read-note` (**Read test note**). Both
use real intents into the fixture's non-exported activity, with distinct
visible results. Its **Retire published actions** button removes the dynamic
shortcuts through `ShortcutManager.removeAllDynamicShortcuts` without disabling
the already pinned instances. The app is signed with the QA debug key and has
no permissions. Vesna must be the default Home app to discover and pin actions.

| Scenario | Observed result |
| --- | --- |
| In-place upgrade from debug 0.4.53 (58) | Room migrated from 5 to 6. All 15 existing Home pins, four dock rows, the page, and the folder/grid tables were unchanged. The three new columns were null in all existing rows. |
| Published actions, standalone launch, and pinning | The fixture's Home long-press menu listed both labels, and **Open** reached the compose destination. **Add to Home** placed both distinct shortcuts in free cells on the current page. `dumpsys shortcut` confirmed both were pinned by Android. Re-adding the first did not create another layout row. Tapping each pinned tile reached its own destination. |
| Edit name, reset, and drawer isolation | Renaming the compose pin changed that row's visible/accessible label without changing the Android ID or launch destination. Renaming the ordinary Home pin did not change the drawer result. **Reset** restored the ordinary app name. An unsaved draft survived a compact-to-wide recreation, and **Cancel** left the saved label unchanged. |
| Locked layout and Home role | Locking Home kept the real action launch and App info available, hid edit, add and removal options, and did not alter the layout. Switching Android Home back to Pixel Launcher hid action discovery but the already pinned action still launched through Vesna's app entry. Giving Vesna the Home role again restored discovery. |
| Retired actions, restart, and removal | Retiring the two published actions removed their menu entries but preserved both pinned tiles through a launcher force-stop. Removing the renamed action removed only its row and Android pin; the second action still launched. Uninstalling the fixture removed only its own rows. |
| Material and accessibility | Checked Light English, Dark Russian on a compact 320 × 640 dp display at 200%, and wider Dark Arabic RTL with English fallback. Publisher rows expose their full names; Russian menu text wraps and the rename dialog scrolls with the keyboard open. Native menu and dialog action targets measure at least 48 dp. Captured menu text contrast was 14.7:1 in Light and 12.6:1 in Dark. D-pad focus, Menu, and D-pad center reached the published action and the real destination without touch. |

There is no Widgets row because Vesna has no widget host. This verifies Vesna's
Android action, label, and layout-lock contracts, not Nova parity. Gmail's
reference launch destination was not established without a Google sign-in;
the fixture proves action routing independently. TalkBack speech, work-profile
actions, and physical devices were not exercised in this run.

The final debug APK (`cee47d0ed958abff362f8fdfe223bd1c1564f34e12e2eb2754603be5752ba9ab`)
passed nine focused JVM tests and 31 Android instrumentation tests, including
the real Room migrations, pin-set rollback on a failed insert, per-pin
name isolation, folder rename, and accessible menu operations. Replayed
publisher pinning, launch, and uninstall cleanup on that APK after the
Room transaction fix.

Restored Pixel Launcher as Android Home and restored the display, locale, font
scale and theme. Saved native screenshots, UI hierarchies, and test logs are in
`D:/orca/scratch/vesna-workspace-shortcut-actions-qa/`.
