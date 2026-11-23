# Android verification: dock icon size (stage 1 of #115)

- **Ticket:** [#115](https://github.com/vladon/vesnalauncher/issues/115), approved
  stage 1 (icon size); the visibility, capacity and paging stages stay open.
- **Candidate:** debug 0.4.75 (80), SHA-256
  `53b53324c3e99ae70cc09104ca3fdb23d3ff8b97e3293f62e31b840d347cf226`.
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

Dock artwork sizes come from the `app_icon` views inside the dock in the view
hierarchy of the top activity, in pixels; the dock bar position is the dock
view's top edge. The Home screen page was scrolled to the new slider and set
by dragging its thumb.

| Scenario | Observed result |
| --- | --- |
| Default | Nothing is stored. The four dock artworks stayed 126 px, the dock bar top at 2103 px, and the workspace tiles 239 × 339 px. |
| 130 % | **Dock icon size** stores 130 %. Dock artworks 164 px, the bar grew to 2065 px, and the 15 workspace tiles kept their order and cells while shrinking to 332 px high. |
| 80 % | Dock artworks 101 px, the bar shrank to 2128 px, workspace tiles grew to 344 px. |
| Restart | After force-stop at 130 %, a new process still showed 164 px dock artwork, the bar at 2065 px, and the same four pins in order. |
| Back to 100 % | Dock artworks 126 px, the bar at 2103 px, workspace tiles 339 px again. |
| Settings search | `icon size` offered all four icon size settings, including **Dock icon size**; opening it showed the Home screen page at its slider. In Russian the page showed **Размер значков в доке** with its description next to the Home slider. |
| Dark | With night mode on, Home at 130 % kept the enlarged dock and the drawer rendered on its dark surface. |

All 66 `ui`, 86 `workspace` and 14 `settings` instrumentation tests, 48 JVM
tests and lint passed on this build. The new `dockScaleGrowsOnlyTheArtworkWithinItsBar`
case covers slots keeping their width, 130 % and 80 % artwork sizes, and the
bar growing and shrinking with the artwork.

QA restored 100 %, the default app locale and day mode; the single page and
all 19 Home and dock pins equal the baseline. Pixel remained the Home role
holder. Evidence is in `D:/orca/scratch/vesna-dock-icon-size-qa/`. Not
exercised: landscape, TalkBack speech, and dock visibility, capacity and
paging (remaining #115 stages).
