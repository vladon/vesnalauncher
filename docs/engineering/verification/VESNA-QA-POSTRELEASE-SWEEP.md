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

## Captures

`sweep-150-home.png`, `sweep-150-homescreen.png`,
`sweep-150-loop-indicator.png`, `sweep-150-gestures.png` (EVI-VESNA-003).

## Notes

- The font scale restored to 1.0 after the sweep; the launcher relaunched
  and verified live.
- The wallpaper-scroll pixel check (#181) is font-independent (the system
  offsets) and was verified separately on the release artifact.
- The gesture dispatch (the two-finger, the pinch) was verified through
  the instrumented programmatic events; the enlarged-font sweep covers
  the assignment surface the gestures read.
