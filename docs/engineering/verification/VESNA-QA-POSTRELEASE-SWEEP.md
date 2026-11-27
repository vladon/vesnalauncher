# VESNA-QA-POSTRELEASE-SWEEP — enlarged-font and touch-target sweep for v0.4.102–104

The post-release acceptance sweep per the loop procedure: the system font
scale 1.5× (`settings put system font_scale 1.5`, the launcher process
restarted) across the surfaces shipped in v0.4.102–v0.4.104, with the
interactive-row touch-target heights read from the accessibility tree
(the 48 dp minimum per the Material guidance). Build: debug 0.4.106
(versionCode 111 — the code identical to the released 0.4.104 surfaces;
0.4.105's swipe-down shipped after this sweep window).

## Results (all interactive rows pass ≥48 dp at 1.5×)

| Surface | Rows checked | Heights |
| --- | --- | --- |
| Settings root + the «Лаунчер по умолчанию» subpage | The default-launcher rows | 91/182 dp — pass |
| Settings › Home screen (the top: the grid, the lock, the auto-relock, the label size) | 10 rows | 91/83/182/249/332 dp — pass |
| Settings › Home screen (the loop, the indicator, the wallpaper) | «Индикатор страниц», «Счётчик (по умолчанию)», «Точки», «Линия», «Прокрутка обоев» | 91 dp each — pass |
| Settings › Gestures & input | The swipe-down section (5 action rows), the double-tap section | 91/182 dp — pass |
| Home at 1.5× (the wallpaper, the tiles, the indicator) | The visual capture | `sweep-150-home.png` — the layout holds; the labels follow the launcher's own label-size setting (the system scale does not distort the grid) |

The text-only detail paragraphs render below 48 dp by design (they are
not touch targets); every tappable row passes.

## Core flows at 1.5×

The functional flows re-verified at the enlarged font (the captures
`150-p1/150-p2`, `150-doubletap`):

- **The loop wrap, both edges** (the loop ON via the shipped switch): the
  first-page backward wrapped onto the last page, and the last-page
  forward wrapped onto page 1 — the live content switched both times
  (the dumps).
- **The wallpaper shift**: the page-1 vs page-2 wallpaper zones differ by
  163/142 channel points (the full span) at 1.5×; the visual capture
  shows page 2 over the shifted (orange) gradient side.
- **The double-tap with no binding** (the fresh install): produces no
  action — the unassigned default verified live; the assigned
  double-tap → the drawer was verified on the same surfaces at 1.0× and
  through GestureDispatchTest.

## Captures

`sweep-150-home.png`, `sweep-150-homescreen.png`,
`sweep-150-loop-indicator.png`, `sweep-150-gestures.png`,
`sweep-150-doubletap.png` (EVI-VESNA-003).

## Notes

- The font scale restored to 1.0 after the sweep; the launcher relaunched
  and verified live.
- The wallpaper-scroll pixel check (#181) is font-independent (the system
  offsets) and was verified separately on the release artifact.
- The gesture dispatch (the two-finger, the pinch) was verified through
  the instrumented programmatic events; the enlarged-font sweep covers
  the assignment surface the gestures read.


## A/B note (the SettingsSearchLayoutTest failures are environmental)

During the same session the SettingsSearchLayoutTest instrumentation
suite failed 2-4 of its 15 tests on THIS emulator state (the
post-wedge cold boot: the short-directory search-expansion assert, the
two scroll-limit asserts, the icon-size search-count assert). The A/B
prescribed by the QA-avd procedure was run: the v0.4.104 TAG build
(the unmodified baseline, the same code the suite passed on earlier)
reproduces the SAME failures identically on this emulator state. The
failures are therefore environmental (the wedged-and-relaunched AVD
session), not caused by the shipped code. Documented here instead of
being chased; the suite re-verification belongs to the AVD baseline
restoration.
