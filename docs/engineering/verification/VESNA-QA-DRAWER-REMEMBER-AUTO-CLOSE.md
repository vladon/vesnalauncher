# Android verification: remember the drawer position after auto-close

**Ticket:** [#147](https://github.com/vladon/vesnalauncher/issues/147).  
**Candidate:** debug 0.4.62 (67), SHA-256
`9e2aaf0f172add5a5720e819fd1175d0d9fa531adc18e4640e387d52f189caa1`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi. The actual
Clock app launched, and Vesna temporarily held the Android Home role.

| Scenario | Observed result |
| --- | --- |
| Both switches on, Android Home | In the unfiltered vertical list with no keyboard, the user scrolled and opened Clock. Android Home returned to the launcher with its drawer closed. Opening the drawer restored all 13 visible row rectangles in Light gesture navigation, including the partially clipped Camera row. |
| Both switches on, Back or search | Back from Clock returned to Home with the drawer closed. The next explicit open started at the top. Launching Clock from a filtered query and returning via Android Home also reopened at the top with no query. |
| Individual switches | With only auto-close on, Android Home and reopen started at the top. With only Remember position on, the same Home route restored the exact scrolled list bounds as before. |
| Compact Dark and restart | At 320 × 640 dp and 200% text, tapping the actual Android three-button navigation Home button from Clock restored all eight visible row rectangles with no keyboard. A separate force-stop/new process retained both settings but discarded the old viewport. The Russian drawer controls were inspected at the same compact size. |
| Existing visuals and access | The drawer controls and app artwork were unchanged. The close control measured 48 dp at both tested sizes; screenshot samples of the enabled label and surface roles gave 16.20:1 in Light and 14.35:1 in Dark. |

All 28 focused Android instrumentation tests passed. The new regression
asserts an exact negative offset after Home, a fresh top after Back, and a
fresh top with Remember position disabled. Existing tests exercise search,
layout modes, failed launches and Settings accessibility.

All 19 original Home and dock rows were unchanged. The emulator was returned
to its original Light theme, normal size and text, gesture navigation, no
app locale override, grid layout, both switches off and the Pixel Home role.
Evidence is in `D:/orca/scratch/vesna-drawer-remember-after-launch-qa/`.
Physical devices and TalkBack speech were not exercised.
