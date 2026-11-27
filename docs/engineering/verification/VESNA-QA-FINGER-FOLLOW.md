# VESNA-QA-FINGER-FOLLOW — pages follow the finger (#180 phase 2, v0.4.101)

Phase 2 of the approved #180 scope: with the Slide effect selected, a page
swipe shows BOTH pages moving with the finger — the outgoing page snapshot
follows the drag while the adjacent page's bitmap slides in from the
screen edge — and everything settles on release (commit at the swipe
threshold, spring back below it). The Instant effect keeps the legacy
threshold switch with no follow layers. The animator duration scale and
the Remove animations setting scale the settle animations. Build: debug
0.4.101 (versionCode 106) on `Vesna_QA_Menu_API_35` (emulator-5560, API
35, RU locale, dark theme, two content pages via the DB probe).

## What is verified

| Check | Result |
| --- | --- |
| Follow engaged | A slow leftward drag: the mid-swipe frame shows the outgoing page mid-translation with the incoming page revealed behind it (`follow-midswipe.png`); the release past the threshold commits. |
| Both pages between | The follow layers carry the outgoing and the incoming page bitmaps (the acceptance's «the outgoing and the incoming page in between»); the incoming layer is captured by flipping the live binding to the adjacent page under the outgoing snapshot, then flipping back. |
| Commit settles | After the release the live page is the target (page 2: the Frame App tile at the cell-3 bounds) and the page controls show both arrows. |
| Spring back | A swipe shorter than the 56dp threshold leaves the live page unchanged (no commit; the layers return to rest). |
| Backward follow | A slow rightward drag from page 2 commits back to page 1; the mid-swipe frame captured (`follow-back-midswipe.png`). |
| The #179 drawer edge | A rightward swipe on page 1 still opens the drawer (no adjacent page → no follow → today's stepPage path) with Slide selected. |
| Instant effect | With `home_page_transition = instant` the same rightward swipe takes today's path (the drawer edge opened; no follow session starts). Restored to Slide afterwards. |
| Regression suites | FolderDisplacement 3 + PageAutoRemoval 5 + Badges 6 = 14/14; WorkTabDrawerTest + SettingsSearchLayoutTest 18/18 on the same build. |

## Design notes

- The commit decision is single-sourced: while a follow session exists,
  `stepPage` no-ops and the drag-end handler commits through
  `selectWorkspacePage` (the release fires both the legacy swipe callback
  and the drag-end callback; only the drag-end path acts).
- A release during the capture window (a fast fling) falls back to the
  plain switch; the capture job checks `committed` before flipping the
  selection back, so a mid-flip release cannot revert the page.
- The capture waits for each page rebind with a 600 ms timeout and aborts
  the session (falling back to no-follow) instead of showing a wrong
  incoming layer.
- The page indicator is hidden during the capture rebinds and restored
  on every exit path.

## Limitations

- The incoming layer is a static bitmap of the adjacent page captured at
  drag start; live content changes on that page during the drag are not
  reflected until the commit rebind. A full two-page live pager remains
  future work beyond the approved wording.
- The settle duration scales with the system animator scale; the follow
  itself is direct finger manipulation and runs unscaled.
- Release builds and physical devices need separate runs.
