# Nova parity audit — full inventory of known divergences (2026-11-26)

Scope directive (product owner, #228): Vesna must be a **full, indistinguishable copy of
Nova 9.0.1 Prime** in behavior, features and visible surfaces. Every divergence below is
a work item regardless of earlier "documented decision" framing; earlier decisions
(dot-only badges, pills, switch-gated work tab) are superseded.

Method: full pass over `COVERAGE_MATRIX.md` (46 areas), OBS-VESNA-001..174, and the open
ticket set (#195/#197/#199/#200/#203/#228). Items are classified:

- **[V]** verified divergence — measured on device, evidence linked;
- **[D]** documented divergence — recorded in earlier passes, needs a fresh A/B on the
  current release before implementation;
- **[=]** verified identical — no action.

## 1. Verified behavioral divergences (device-measured, dev queue)

| # | Area | Nova | Vesna today | Evidence / ticket |
|---|------|------|-------------|-------------------|
| 1 | Drawer work-set enumeration | Full launchable work set inline | **RESOLVED on v0.4.107** — the drawer is always tabbed (no merged-inline mode left to omit entries): Apps tab lists all 23 personal apps incl. YouTube/YT Music, Work tab the full 17 | OBS-176; #199 |
| 2 | Emptied-page auto-removal | Page disappears the moment a move empties it, any move path | v0.4.92 trigger did not fire for edge-drag moves; **v0.4.107 re-check pending** | OBS-173; #176 |
| 3 | Work-tab slice rendering | Stable per-tab slices | Intermittent broken render captured once on v0.4.95; **v0.4.107 re-check pending** (always-on landed in v0.4.100) | OBS-170 addendum; #197 follow-up |
| 4 | First-launch speed | Desktop loads inside the boot sequence; sub-second to a settled desktop | Post-reboot TotalTime 809/1071 ms, settle ~2.5-3 s; snapshot-restore first load 2288 ms / settle ~7.8 s. Root cause: `AppDiscovery.getBadgedIcon` × ~190 gates the first state emission; no persisted catalogue/icon cache | OBS-172; #195 |
| 5 | Persistent bottom pills | None (gesture-only desktop) | `Home options` (⋯) + `Open app drawer` (grid) always render | OBS-174; #228 — decision recorded: remove. **Implementation map (v0.4.95 tree, HomeActivity.kt):** the pills are load-bearing — `VesnaPopupMenu(homeOptionsButton)` anchors both popups (1935/1972); the swipe-up drawer gesture gates on `!homeOptionsButton.isShown` → return (1970); focus fallbacks target `openDrawerButton` (868/953); `homeControls.visibility` hides pills under the drawer (949) and carries inset positioning (1819-1822); views wired at 305-314. Removal = refactor unit: re-anchor popups (dock or launcher_root), drop the isShown gate term, reroute focus fallbacks to the workspace, simplify `HomeControls.onMeasure` (no otherControlsWidth math), delete `home_options`/`home_open_drawer` from activity_home.xml, keep the page-controls pager (Nova-like dots), re-keep empty-hint text or reword, and migrate 5 instrumentation tests off `home_open_drawer.performClick()`/`home_options` (DrawerRecreationTest ×3, ShortcutActionsTest ×4, HomeControlsLayoutTest) to gesture/long-press openers — MotionEvents helper pattern already exists in DockDropTargetTest | OBS-174; #228 — decision recorded: remove |
| 6 | Settings search coverage | Settings rows findable by search | The dots rows (`Show notification dots`, `Notification access`) return *No settings found* for `notification`/`dot` — **re-verified on v0.4.92 (97): persists** | OBS-171; #200 comment |

## 2. Feature/config gaps (Nova has the surface, Vesna lacks or partially)

| # | Area | Nova surface | Vesna today | Class |
|---|------|--------------|-------------|-------|
| 7 | Badge configuration | Master switch, style (dynamic / notification dots / numeric dots), Large/Medium/Small size, popup-menu content toggle, test button; **default on** | Dot-only, **default off**, no style/size/counts/popup toggle. Full-copy supersedes the dot-only scope decision: implement the full config with Nova's defaults | [V] structurally documented (#200/OBS-171) |
| 8 | Work tab gating | Tab bar always present when a work profile exists (Apps | Work) | Behind `Work apps in their own tab` switch, **default off** — indistinguishability requires the always-on Nova presentation (or default-on switch) | [V] OBS-170/#197 |
| 9 | Work-profile dots | Nova lights work copies per its badge pipeline | Per-profile dots need a work-profile notification-access grant inside the profile (recorded limitation) | [D] QA-BADGE-DOTS Limitations |
| 10 | Per-surface appearance | Per-surface text tones with minimum-contrast slider; Settings/drawer recolor independently | Not started | [D] APPPEAR-004/006/012 |
| 11 | Page behavior extras | Infinite page looping, page-indicator style options, page-change animations + nine transition effects, wallpaper scroll On/Off/Reverse | None of these | [D] HOME-001..015 (#174 adjacent) |
| 12 | Gesture configuration | Gesture settings surface with configurable actions (swipe/pinch/double-tap bindings), outward swipe on first page | Fixed gestures only (swipe-up = drawer); no config surface | [D] GESTURE-001..006/112 |
| 13 | Drawer native context actions | Clock's native actions open from drawer search/menus | Only on Home tiles; **re-verified on v0.4.92: drawer long-press menu = Add to dock / Add to workspace / App info only** | [V] DRAWER-010 |
| 14 | Fresh-app suggestion row | Recently launched apps enter the drawer suggestion row | Alphabetical grid, no suggestion row | [D] DRAWER-100 (OBS-050) |
| 15 | Hidden-app prefix search | Hidden app reachable via prefix search | Not exposed on prefix search | [D] DRAWER-008 (#119) |
| 16 | Offline place handoff | Offline place query hands off to Maps with an offline notice | Local no-match for place queries | [D] DRAWER-005 |
| 17 | Vertical list position on search clear | Retains position | Resets to top | [D] DRAWER-003 (#108 — verify on current release) |
| 18 | Dock landscape paging | Dock pages vertically in landscape | Keeps a single bottom row | [D] DOCK (OBS-040/047) |
| 19 | Dock overlap | Creates a dock folder on overlap | Swaps pins | [D] DOCK (OBS-041) |
| 20 | Label edit + Back | Back keeps the Home label edit | Discards it | [D] HOME-102 (OBS-042/053; informal probe on v0.4.92 inconclusive — dump-layer unreliability, controlled rerun pending) |
| 21 | Lock sheet affordances | Padlock-to-unlock + automatic relock switch in the sheet | Message without action | [D] HOME-013/107 |
| 22 | Widget picker | Widget catalog with provider filter, sizes, empty-state return | No picker entry found (v0.4.60/61 era; #149) | [D] WIDGET-001..008 |
| 23 | Nova Now cards ecosystem | Day/time/weather/Bluetooth/map cards, custom card builder, overlay affordances (deep links, gears, overflow) | Partial (CARDS-100/103/104/105 present; custom builder + overlays + calendar connect not started) | [D] CARDS-101/102/103 |
| 24 | Backup edge behavior | Placeholder retained for absent app; renamed native action restores launchable | Manual route absent in the v0.4.54-era pass; current state needs a re-run after the backup ADR work | [D] BACKUP-100 |
| 25 | Prime gating | Prime-gated features behave as Nova's | Probe covered activation surface; item-level gating parity unverified | [D] SYS-111/26 |

## 3. Verified identical (no action)

Folders core (rank insert/sort/batch add/drag-out/whole-folder moves, v0.4.69+), grid
resize persistence, drawer auto-close variants, icon-pack fallback across surfaces,
per-surface label hiding + icon sizes (+restart persistence), theme switch, work-tab
presence (tab bar + per-tab slices in the clean state), work-badge co-render, paused-work
banner semantics (#198), unpin-pruned secondary pages, sole-page protection.

## 4. Open questions for implementation

- "Indistinguishable" includes Nova's **Prime-gated** surfaces: decide whether Vesna
  mirrors gate interactions item-by-item or exposes everything ungated.
- Nova's default states are part of indistinguishability: badges default ON (dynamic),
  work tab default present, drawer groups master ON. Vesna's defaults must move to
  Nova's.
- Every item in §2 needs a fresh A/B on the current release before implementation —
  several were recorded against v0.4.53–v0.4.75 eras and may have drifted.
