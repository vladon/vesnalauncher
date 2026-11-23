# Android verification: close the drawer after launching an app

**Ticket:** [#137](https://github.com/vladon/vesnalauncher/issues/137).
**Candidate:** debug 0.4.56 (61), Android 15 (API 35),
`Vesna_QA_Menu_API_35`, 420 dpi, gesture navigation. Vesna held the Android
Home role during the replay. Clock was launched from a temporary second
Home page, not from its dock shortcut.

| Scenario | Observed result |
| --- | --- |
| Default off | Upgrading from 0.4.55 left the new setting unchecked without a stored key. After scrolling the vertical app list and launching Clock, one Android Back returned to the same 13 visible rows at identical pixel bounds. |
| On and persistent | Searching Settings for `automatically close` opened the App drawer page at the highlighted switch. After enabling it, Back from Clock returned to the same second Home page with the drawer closed. A force-stop and cold launch with a new PID retained the setting and the active page. |
| Other launch inputs | In the enabled mode, Enter on the sole Clock search result after the cold launch returned to Home, as did tapping Clock in both the vertical grid and horizontal pager. The same Home and dock placements remained intact. |
| Toggle off again | A keyboard toggle persisted through force-stop. Back from another launch retained the scrolled vertical list again. |
| Settings accessibility | The English Light and Russian Dark controls each exposed one clickable, checkable row with the title, description and state. At 200% text on a compact 320 × 640 dp Dark display, the whole localized row was reachable by scrolling. Tab reached the control and D-pad center toggled it. Sampled supporting-text contrast was 8.9:1 in Light and 10.9:1 in Dark. |

All 26 tests in `DrawerSearchTest` and `SettingsSearchLayoutTest` passed.
The focused launch regression exercised a failed launch response with
auto-close on: the drawer stayed open with its query, then closed after a
successful response.

The final debug APK SHA-256 was
`a591b8a5bde5b32645650a914be3d4b5fdf0b5bf08b487c519e6bc12e8dffbaa`.

QA removed the empty test page and verified all original saved Home and dock
rows. Native captures, accessibility trees and test logs are in
`D:/orca/scratch/vesna-drawer-auto-close-qa/`. TalkBack speech, a physical device,
and three-button navigation were not exercised.
