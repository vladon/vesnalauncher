# Android verification: separate Home and app drawer icon sizes

- **Ticket:** [#146](https://github.com/vladon/vesnalauncher/issues/146), approved scope
- **Candidate:** debug 0.4.73 (78), SHA-256
  `d98f215f2647fed263cac367ed66e7ed16388df07e27cbbff39b8602e5266756`. The matrix
  below was measured on `b2dfa051e813694d7d628ecbcd3d417ca0dc01b2b7c5ac5c14bb65470932fdd0`,
  which differs only by the Russian format string for the percentage; the
  default and 130 % states were measured again on the final build.
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

Artwork sizes below come from the view hierarchy of the top activity: the
`ImageView` inside each tile, in pixels. 48 dp is 126 px here. Sliders were set
by tapping their tracks in Settings.

| Scenario | Observed result |
| --- | --- |
| Default | No size is stored. Home tiles 239 × 339 px with 126 px artwork, dock 126 px, drawer grid tiles 238 × 214 px with 126 px artwork: the appearance of earlier versions. |
| Home at 130 % | Home artwork 164 px in the same 239 × 339 px cells; dock artwork stayed 126 px; the saved layout was unchanged. With the drawer matching Home, its grid showed 164 px artwork in every tile, also after scrolling. |
| Drawer unmatched at 80 % | **Match Home icon size** off stores its own size. The drawer grid showed 101 px artwork in the default 238 × 214 px cells while Home stayed at 164 px. |
| Home changed to 110 % while unmatched | Home artwork 139 px; the drawer kept its 101 px. |
| Matching again | With the switch on, the disabled drawer slider still showed its stored 80 %, and the drawer followed Home at 139 px. Turning the switch off brought the 101 px artwork back. |
| Vertical list | Rows stayed 168 px (64 dp) high at 80 % and 130 %; the artwork was 101 px and 138 px square. |
| Horizontal pages | Tiles kept their 270 × 211 px cells; the artwork stayed within the space above the label, as before. |
| Restart | After force-stop (PID 12709 → 15436), Home 110 % and the drawer's own 130 % were still applied, and the saved layout was unchanged. |
| Settings search | `icon` offered **Home icon size**, **Drawer icon size** and **Match Home icon size**. Opening **Drawer icon size** showed the App drawer page scrolled to its slider. |
| Dark and compact RTL | In the Dark theme, Home at 110 % and the drawer at 130 % kept their cells. At 320 × 640 dp with 200 % text, Arabic (Egypt) and three-button navigation, the artwork shrank above the scaled labels as it does at the default size. |

All 63 `ui`, 86 `workspace` and 13 `settings` instrumentation tests, 48 JVM
tests and lint passed. The new `IconScaleLayoutTest` covers the unchanged
tile and label sizes at 130 %, the clamp in narrow cells, fixed list rows with
square artwork, a drawer cell that does not shrink below its default below
100 %, and a recycled drawer tile taking the current size when rebound.

QA found and fixed two defects on this candidate before the run above: a
recycled drawer tile kept the size of its earlier bind, and artwork above 108 %
stretched sideways in vertical list rows.

QA restored the stored defaults, Light, normal text and display size, gesture
navigation and the default app locale. The single page and all 19 saved Home
and dock rows equal the baseline. Pixel remained the Home role holder.
Evidence is in `D:/orca/scratch/vesna-icon-sizes-qa/`. A Home label that
already ellipsized at the default size, **Voice Search**, stays ellipsized;
that predates this change. Not exercised: TalkBack speech, hardware
keyboards, landscape, and dock sizing (#115).
