# Android verification: dock visibility (stage 1 of #115)

- **Ticket:** [#115](https://github.com/vladon/vesnalauncher/issues/115), approved
  stage 1 (visibility and icon size); capacity and paging stay open.
- **Candidate:** debug 0.4.76 (81), SHA-256
  `81638e0d8ea54d18ee4748b52cceb027f1e81942b6da068c9974c6a392dc9d7d`.
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

The QA device started from the saved baseline: fifteen workspace pins and the
dock pins Calendar, Camera, Chrome and Clock, Light theme. Dock visibility was
read from the toggle in Settings and from which tiles Android exposes on Home.

| Scenario | Observed result |
| --- | --- |
| Default | Nothing is stored and the dock is visible: four 126 px artworks, bar top at 2103 px, workspace tiles 339 px high. |
| Hiding | Turning **Show dock** off stores `dock_visible=false`. Home shows no dock pins; the controls sit at the very bottom (2274 px) and the workspace tiles grew to 399 px. |
| Menus while hidden | A workspace tile's hold menu offered **Remove from workspace**, **Edit name**, **App info** — no **Add to dock**. A drawer row's hold menu offered **Add to workspace** and **App info** — no **Add to dock**. |
| Restart while hidden | After force-stop and a new process, the dock stayed hidden. |
| Showing again | Turning **Show dock** on stores `dock_visible=true`; the dock returned with Calendar, Camera, Chrome and Clock in the same slots and the workspace tiles back to 339 px. |
| Dragging toward the hidden dock | A drawer drag attempt toward the hidden dock's old area dropped nothing and moved no pin; the layout stayed at 19 pins. |
| Settings search | `dock` surfaced **Show dock** with its description (next to **Dock icon size**). |
| Dark and Russian | With the Dark theme the hidden-dock Home and the drawer rendered as usual. In Russian the switch is **Показывать док** with its description. |

All 66 `ui`, 86 `workspace` and 14 `settings` instrumentation tests, 48 JVM
tests and lint passed on this build; the updated Settings search test now
covers all four icon size settings, and the dock scale case covers the bar
growing and shrinking.

QA restored the visible dock (the pins stayed intact the whole time), Light,
the default app locale and day mode; the single page and all 19 Home and dock
pins equal the baseline. Pixel remained the Home role holder. Evidence is in
`D:/orca/scratch/vesna-dock-icon-size-qa/`. Not exercised: TalkBack speech,
hardware keyboards, landscape, and dock capacity and paging (remaining #115
stages).
