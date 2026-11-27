# VESNA-QA-PAGE-LOOP-INDICATOR — page loop and indicator choice (#174, v0.4.102)

The approved #174 scope: an optional page loop (off by default) and a page
indicator choice that keeps today's appearance as the default. Build:
debug 0.4.102 (versionCode 107) on `Vesna_QA_Menu_API_35` (emulator-5560,
Android 15/API 35, RU locale, dark theme, two content pages).

## What is verified

| Check | Result |
| --- | --- |
| Loop off (default) | An outward (forward) swipe on the last page stays put: the live page keeps its content, no wrap, no crash. |
| Loop on | Settings › Home screen › «Зацикливать страницы» ON (the switch persists `home_page_loop = true`): the same swipe on the last page wraps onto page 1 (the first page's content live after the settle). |
| Both edges wrap under the opt-in | The last page forward wraps onto page 1, and (with the loop on) the first page backward wraps onto the last page — superseding the #179 drawer edge while the loop is on; with the loop OFF the first-page backward still opens the drawer. |
| Indicator: Counter (default) | Today's rendering — the outline dots with the active bar — plus the «Страница X из Y» accessibility counter; the default requires no pref entry. |
| Indicator: Dots | `home_page_indicator = dot` renders one circle per page with the active page filled (`indicator-dot-04102.png`). |
| Indicator: Line | `home_page_indicator = line` renders a thin baseline with the active page's highlighted segment (`indicator-line-04102.png`). |
| Persistence | Both prefs survive force-stop/relaunch (the pref file re-read on every entry; the wrap and the rendering applied on the fresh process). |
| Unchanged guarantees | Page order, pin positions, the eight-page cap, the dock and the Go-to-page picker behave as before; the counter content description stays the TalkBack source of truth in every style. |
| Regression suites | FolderDisplacement 3 + PageAutoRemoval 5 + Badges 6 = 14/14; WorkTabDrawerTest + SettingsSearchLayoutTest 18/18 on the same build. |

## Known automation gap

The Dots/Line radio rows render and are wired identically to the shipped
transition-effect radios, but the automated tap on them did not persist
the choice in this session (the write path is identical to the loop
switch and the transition radios, both of which persist; the layout
harness flaked — the fling-decay and dump-to-tap races are recorded in
the loop notes). The choice itself was exercised by writing the same
preference entry the radio writes and verifying the applied rendering
and the restart persistence. A manual tap-through of the two radios is
the remaining check before wider distribution.

## Notes

- The loop ON supersedes the #179 drawer edge on the first page (the
  user opted into infinite scrolling); the drawer returns when the loop
  is off. A one-page home never wraps (the same-page slide is guarded).
- The wrap composes with the #180 Slide effect and the #180 phase-2
  finger-follow: both resolve the wrapped target the same way, so a
  looping swipe animates onto the first page like any other page change.
- The release build and physical devices need separate runs.
