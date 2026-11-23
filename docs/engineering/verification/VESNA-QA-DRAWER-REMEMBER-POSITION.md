# Android verification: remember the vertical drawer list position

**Ticket:** [#141](https://github.com/vladon/vesnalauncher/issues/141).
**Candidate:** debug 0.4.59 (64), Android 15 (API 35),
`Vesna_QA_Menu_API_35`, 420 dpi. Vesna held the Android Home role.
The following replays used the real Clock activity and kept drawer auto-close off.

| Scenario | Observed result |
| --- | --- |
| Default off | No preference key was present after upgrading from 0.4.58. After a scroll and a launch of Clock from the vertical list, Android Home closed the drawer. Reopening showed the same rows at the top as a new drawer. |
| Opt-in and exact offset | Searching Settings for `remember position` opened the highlighted switch. With it on, returning from Clock via Android Home and reopening restored all 13 recorded row bounds, including the clipped first row. The same check passed with the actual Home button in three-button navigation, preserving all 12 visible row bounds there. |
| Scope and restart | A manual close and a launch from filtered search did not retain a scroll offset or query. `Remember position` survived force-stop with a new launcher PID; the scroll offset itself did not survive process restart. |
| Settings accessibility | One checkable row exposed the title, supporting text and state in both themes. At 320 × 640 dp and 200% font scale in Dark Russian, the full label and description were reachable. Tab focused the row and D-pad Center toggled it in both directions. In the sampled Light and focused Dark captures, supporting text contrast exceeded 8.5:1. |

All 27 focused tests in `DrawerSearchTest` and `SettingsSearchLayoutTest`
passed, including the exact negative offset, default reset, and scoped
handling of filtered results.

The tested debug APK SHA-256 was
`61ccc35aa058ae65430e277d8136c51b43a4e5b46ccff226e00ce05e16f253f2`.
The original workspace and dock rows were unchanged. Screenshots, accessibility
trees, row bounds, and test logs are in `D:/orca/scratch/vesna-drawer-remember-position-qa/`.
TalkBack speech and physical devices were not exercised.
