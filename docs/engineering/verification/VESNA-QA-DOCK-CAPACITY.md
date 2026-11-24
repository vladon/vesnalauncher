# VESNA-QA-DOCK-CAPACITY — dock capacity and paging (#115 stage 2, v0.4.81)

Device: Android 15 emulator (API 35, emulator-5560), debug build 0.4.81 (86), installed over the 0.4.68 baseline data (15 workspace tiles, four dock pins Calendar · Camera · Chrome · Clock, English, Light).
Fixture: the fifth pin is Maps, added from the drawer's search result so the options menu provably belongs to Maps.

| Check | Evidence | Result |
|---|---|---|
| Setting found: Home screen → **Dock capacity** ("Favorite slots per dock page. Extra pins flow onto a second page in order; a reduction is rejected when it cannot hold every pin."), slider 2–8 with the default showing **4 slots** | Capture `qa-c1-capacity-slider` | PASS |
| Reduction to 3 reflows in order: the dock shows Calendar · Camera · Chrome; the workspace keeps all 15 tiles and their cells | Node dump before/after + `qa-c2-home-capacity3` | PASS |
| Overflow reaches the second page: a horizontal swipe across the dock flips to **Clock** (saved order), swiping back returns to the first page; the workspace page is untouched by the dock gesture | Node dump (dock band `Clock` after the swipe) + `qa-c3-dock-page2` | PASS |
| A fifth pin flows onto the second page: **Add to dock** on Maps appends it behind Clock — page 2 shows Clock · Maps while page 1 keeps its three pins; the dock stays on page two across the drawer round-trip (session state, like the recorded Nova behavior) | Node dumps + `qa-c4-five-pins-page2` | PASS |
| Rejection: with five pins, requesting 2 slots (total 4 < 5) shows the toast **"Fewer slots cannot hold every pin"** and the committed value stays **3 slots** | Capture `qa-c5-reject-toast` | PASS |
| Expansion packs onto one page: capacity 5 shows all five pins in order (Calendar · Camera · Chrome · Clock · Maps) on a single page | `qa-c6-capacity5-one-page` | PASS |
| Restoring 4 and restarting: after a force-stop and relaunch the pins keep their saved order and the dock opens on page one (page selection is session state, matching the recorded Nova restart behavior) | `qa-c7-restart-page1` | PASS |
| Russian: **«Вместимость дока»**, value **«Ячеек: 4»**, the translated detail and rejection toast | `qa-c8-russian-capacity` | PASS |
| Dark: dock paging and the page-two band render with the Vesna dark surface (capacity 4 → page 2 correctly shows only Maps) | `qa-c9-dark-dock-page2` | PASS |
| Tests: `DockPagesTest` (7 JVM cases over the page arithmetic), `DockPagingTest` (overflow slicing, capacity reflow, sweep flips, vertical-sweep no-op, drag-keeps-stream, RTL), `WorkspaceRepositoryDockCapacityTest` (two-page insert bounds, default capacity, front-insert shifts across the boundary, workspace→dock FULL), resolver `drop columns respect the two page capacity`, updated full-dock fixtures | 62 JVM tests, instrumentation 178/178 (workspace 90, ui 73, settings 15), lintDebug on this candidate | PASS |

Notes and limits:

- Capacity lives per page; the dock never splits into more than two pages (`DockPages.MAX_PAGES = 2`), which is what makes a reduction rejectable at all. The two-page shape matches the recorded Nova observations (overflow went to a second dock page; swipes move between two pages).
- Reordering across the page boundary shifts the overflow pins in saved order (covered by `insertIntoDockShiftsOverflowOntoTheSecondPage`); drags only target the visible page's slots, matching the dock's one-bar layout.
- Drop highlights and drag sessions are page-aware: a dock drag suppresses paging (`dockDragKeepsTheStreamFromPaging`), so holding a pin to drag it never flips the page under the finger.
- Hidden-dock and dock-icon-size behavior from stages 1/1b is unchanged; the dock capacity preference is a separate key.
- Screenshots and raw node dumps: `D:/orca/scratch/arcticons/` (`qa-c0` … `qa-c9`). The QA device was restored to the 0.4.68 baseline data afterwards (19 tiles, four dock pins, no capacity preference).
