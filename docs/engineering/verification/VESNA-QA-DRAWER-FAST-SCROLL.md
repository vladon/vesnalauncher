# Android verification: alphabetical drawer fast scroll

**Ticket:** [#109 / NT-DRAWER-06](https://github.com/vladon/vesnalauncher/issues/109).
**Device:** Android 15 (API 35), dedicated `Vesna_QA_Menu_API_35` AVD, 420 dpi.
**Build:** debug 0.4.49 (54), APK SHA-256
`89a2081cda7fb3fb4aa04852727596ea0f1c15a3d3d952e33a9c8bde70b29856`.

Installed the [drawer frame fixture](VESNA-QA-DRAWER-FRAMES.md#workload-and-procedure)
with 120 launchable aliases alongside the stock apps. Opened the real Home drawer
and sent DOWN, MOVE and UP to the trailing thumb. Captured the results while the
finger was down, not after the chip disappeared.

| Configuration | Observed result |
| --- | --- |
| Light, English, gesture navigation, 411 × 914 dp, 100% text | Drag from the top brought YouTube and YT Music into view, with a **Y** chip while held. The chip disappeared on release. Typing `Clock` hid the fast scroller, returned only Clock, and the result launched the Android Clock app. |
| Dark, Russian, three-button navigation, 320 × 640 dp, 200% text | Rail stayed on the right. Holding the thumb near the bottom showed **Ч**, with **Часы** in view. The localized slider label was present in the Android accessibility tree. |
| Dark, Arabic RTL, three-button navigation, 320 × 640 dp, 200% text | Rail and chip mirrored to the left. The drag reached **ص** with Arabic-labeled apps in view. |
| Dark, three-button navigation, 549 × 686 dp, 200% text | Drag reached **Y**, and the final row and the indicator stayed above the navigation bar. |

Each rail was 48 dp wide and outside the app tile hitboxes. The Android tree
exposed a focusable, named slider. All 15 `DrawerSearchTest` and
`DrawerRecreationTest` instrumentation cases passed. They verify that
accessibility progress and D-pad Down advance through distinct sections sharing
the last row, including a Cyrillic label, and that clearing search returns to the
top and restores the rail. The refresh-offset test previously requested an
unreachable offset near the end on this tall display; the same failure reproduced
on the pre-feature v0.4.48 code. The fixture now leaves spare rows below the
test anchor without changing the offset checks.

The HOME role remained with Pixel Launcher and workspace item, folder and page
rows were unchanged. Restored display size, font scale, night mode, locale and
navigation mode. Local screenshots and Android UI dumps are under
`D:/orca/scratch/vesna-fast-scroll-qa/`. Live TalkBack speech and physical-device
behavior were not verified.
