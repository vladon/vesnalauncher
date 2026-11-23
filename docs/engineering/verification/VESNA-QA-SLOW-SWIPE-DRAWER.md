# Android verification: a slow upward swipe opens the drawer

- **Ticket:** [#170](https://github.com/vladon/vesnalauncher/issues/170)
- **Candidate:** debug 0.4.70 (75), SHA-256
  `e60f36cac4343ab0b897532058df6ed76689166e2aa1f7ef0a5d94e07b39b641`
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

Each trajectory below is one continuous `input swipe` on an empty bottom-row
cell with the drawer closed, repeated from two start cells. The saved layout
was identical before and after the run. On the installed debug 0.4.68 (73) the
same 1100 px swipe at 665 ms and the 400 px swipe at 800 ms opened the
workspace menu, and the fast controls opened the drawer.

| Trajectory | 0.4.70, both start cells |
| --- | --- |
| 1100 px in 300, 500, 665, 800 or 925 ms | drawer, once; the workspace menu never appeared |
| 400 px in 800 ms | neither surface |
| 400 px in 150 ms | drawer |
| Stationary hold | workspace menu |

The slow swipe passes the touch slop in its first frames, which drops the
empty grid's pending long press. At release, 1100 px (419 dp) exceeds the
360 dp long-swipe distance and 400 px (152 dp) does not.

| Other scenario | Observed result |
| --- | --- |
| Tile hold with a wandering finger | Holding Contacts and moving 39 px (15 dp) to the right inside the tile before the timeout still lifted the tile, shown dimmed while held. Releasing without further movement opened its shortcut options, as a stationary hold does, and no saved row changed. |
| Short workspace | At 320 × 640 dp, 200% text, Arabic (Egypt) and three-button navigation, a slow 550 px drag from a free cell opened nothing and a slow 900 px drag opened the drawer; a stationary hold on the free cell opened the workspace menu. The same 900 px drag starting on a tile opened the drawer once the pointer had left the tile, while a stationary hold on that tile opened its own options. |
| Red and green tests | `WorkspaceSwipeUpTest` failed 4 of 8 cases on 0.4.68: the two slow long swipes and the short-workspace drag never opened the drawer, and the slow short swipe opened the menu. On the candidate all 8 passed three times, together with the 62 focused folder, page, drop-target, shortcut and page-swipe tests. |

QA restored Light, normal text and display size, gesture navigation, the
default app locale and the unlocked layout; the single page and all 19 saved
Home and dock rows equal the baseline. Pixel remained the Home role holder.
Evidence is in `D:/orca/scratch/vesna-slow-swipe-drawer-qa/`. Physical devices,
TalkBack speech, landscape and Android's own gesture-navigation zone were not
exercised, and Nova's thresholds were not measured.
