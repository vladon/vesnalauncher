# Engineering Backlog

The single authoritative list of engineering work items. Ownership of content:

- Item definitions and status live **here only**.
- Sequencing and dependencies live in [ROADMAP.md](ROADMAP.md).
- Research-ID correspondence and verification state live in
  [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

Status vocabulary: **proposed** (defined, not yet approved for implementation),
**ready** (approved, may start when its dependencies are green),
**in-progress**, **done** (with a link to the verifying run or commit).
Listing an item here implies no approval and no product/privacy decision; gates
are named explicitly.

---

## VESNA-QA-002: HOME and launch-return verification (done)

**Status:** done — device run recorded 2026-11-18 on the dedicated API 35 AVD;
full reproducible record:
[verification/VESNA-QA-002.md](verification/VESNA-QA-002.md). The Android 8
(API 26) legacy follow-up — Settings chooser selection and the
Calculator → HOME return replay — is appended to the same report as
[VESNA-QA-002B](verification/VESNA-QA-002.md#api-26-follow-up-vesna-qa-002b)
(2026-11-19, [PR #37](https://github.com/vladon/vesnalauncher/pull/37)).
**Priority:** P0 (first item). **Depends on:** nothing; reuses the **existing
dedicated API 35 AVD** — no new device work is required to start.
**Research:** none — this is a Vesna platform baseline item and carries **no
approved research ID**.

Verify on the dedicated Vesna AVD (the stock launcher may stay installed;
removing it is not required):

1. Vesna registers as a HOME launcher: the system offers it in the home-app
   selection (`RoleManager` role request on API 29+, home settings on older).
2. After selection, Vesna is the default home: pressing HOME from any app
   returns to the Vesna workspace.
3. Launch an app from workspace/dock/drawer, press HOME, and the workspace is
   restored as left (drawer closed per `singleTask`/`onNewIntent` behavior).
4. **Cleanup restores the previous HOME holder** — the device is returned to the
   HOME selection it had before the run.

Record what "HOME works" means for the 0.4.1 product baseline **first**, since
no research scenario covers this: write down the acceptance assertions, then
automate or script them. On completion, add the run record link to
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md#home-role-and-launching).

<a id="vesna-qa-003-package-lifecycle-verification-partial"></a>

## VESNA-QA-003: package lifecycle verification (done)

**Status:** done — includes disabled-app pin retention verified on API 35 by
[VESNA-QA-003F](verification/VESNA-QA-003F.md). The earlier drawer-side run
(2026-11-19, dedicated API 35 AVD) is recorded as **VESNA-QA-003A**:
[verification/VESNA-QA-003.md](verification/VESNA-QA-003.md). Install →
drawer, same-package update, disable/re-enable, and uninstall (incl. launcher
process restart and uninstall-while-dead) passed. After
[PR #15](https://github.com/vladon/vesnalauncher/pull/15) restored drawer drops,
[VESNA-QA-003B](verification/VESNA-QA-003B.md) verified pinning, upgrade
retention, and folder cleanup on uninstall on API 35. Disabling an app removed
its workspace and dock placements. Re-enabling it restored drawer discovery.
[VESNA-QA-003C](verification/VESNA-QA-003C.md) (2026-11-19, API 35) verified
the managed-profile case end to end: a shell-provisioned work profile's
fixture was discovered in the drawer, a drawer-drag pin persisted with the
profile's `userSerial`, launching opened the fixture in the work profile, and
a work-only uninstall deleted exactly the work-serial placements while the
personal package and pin survived, stable across a launcher restart.
[VESNA-QA-003D](verification/VESNA-QA-003D.md) (2026-11-19, API 35) verified
uninstall with simultaneous workspace, dock, and folder placements: all three
fixture rows left within seconds, the folder dissolved, and a cold relaunch
showed no ghost entries. The dock folder-capacity defect found in this line
(VESNA-BUG-003) was fixed in [PR #26](https://github.com/vladon/vesnalauncher/pull/26)
and device-verified in [VESNA-QA-003E](verification/VESNA-QA-003E.md)
(2026-11-19, API 35). Disabled apps now keep their stored workspace and dock
positions and folder memberships. They reappear on re-enable; uninstall
still removes pins for the affected profile.
Fixture methodology is recorded at
`D:/orca/scratch/vesna-qa-003/` (hand-built debug APKs: same
package/component/signature, two versionCodes, no permissions).
**Priority:** P0. **Depends on:** the AVD inventory from
[VESNA-QA-002](#vesna-qa-002-home-and-launch-return-verification-done).

Verify the `PackageChangeMonitor` → stale-cleanup path end to end:

- Uninstall an app pinned to workspace, dock, and a folder: all entries leave the
  UI **and** the database (no ghost cells after restart).
- Update an app (same package, new version) in place: entries and order survive
  (`PackageEvent.Replaced` path).
- Disable an app with `pm disable-user`: it is hidden without changing its
  pins or folder memberships, including across a launcher restart.
  Re-enabling restores its icons.
- Repeat at least one case across a managed profile if the AVD supports one
  (also feeds the managed-profile row in
  [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids)).

## VESNA-QA-004: migration and restart verification (done)

**Status:** done. Separate API 35 runs verified historical migrations and
restart behavior. **Part A,
migrations:** `RoomMigrationQaTest` (merged as
[PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`)
seeds authentic v1 and v2 schemas into isolated on-device test databases and
migrates them through the unmodified production `LauncherDatabase.build`; the
coordinator's independent run executed both cases plus `DrawerSearchTest` on
the dedicated API 35 AVD (2026-11-19). **Part B, persistence:** process death,
full guest reboot, and no re-seed of an emptied workspace — reproducible
record: [verification/VESNA-QA-004B.md](verification/VESNA-QA-004B.md)
(2026-11-19, tested build `0c7269c`, 0.4.1/6).
**Priority:** P0. **Depends on:** AVD inventory.

- Room 1→2 and 2→3 migrations (`MIGRATION_1_2`, `MIGRATION_2_3`): run against
  representative v1 and v2 databases; layout, folders, and per-profile
  `userSerial` values survive intact (part A).
- The persisted placements and icon bounds matched after force-stop and
  relaunch (part B).
- The persisted placements and icon bounds matched after reboot. An emptied
  layout stayed empty (part B).

## VESNA-QA-005: minimum SDK (API 26) smoke verification (done)

**Status:** done — narrow smoke run recorded 2026-11-19 on the dedicated
API 26 AVD (`Vesna_QA_MinSdk_API_26`, Android 8.0, 1080×1920); reproducible
record: [verification/VESNA-QA-005-API26.md](verification/VESNA-QA-005-API26.md).
Scope: build (debug + androidTest), HOME discoverability, explicit cold start
with workspace and drawer render, one fixture drawer round-trip
(discover → launch → uninstall); no default-HOME change, no deep drawer or
workspace editing. The full existing instrumentation suite passed 3/3 on
API 26 — `RoomMigrationQaTest` (both cases) and `DrawerSearchTest`; the
per-case JUnit XML (`TEST-Vesna_QA_MinSdk_API_26-AVD-8.0.0.xml`) is listed in
the record's evidence index. The run exposed VESNA-BUG-004, where the dock
covered the fifth workspace row. [PR #23](https://github.com/vladon/vesnalauncher/pull/23) fixed the overlap and was replayed
on API 26 and API 35. The follow-up on the full eight-test suite — all eight
instrumentation tests passed on API 26 at build `4c5f0c1` — is appended to
the same report as
[VESNA-QA-005B](verification/VESNA-QA-005-API26.md#follow-up-on-the-eight-test-suite)
(2026-11-19, [PR #36](https://github.com/vladon/vesnalauncher/pull/36)); the
original smoke record above predates that suite.
**Priority:** P0. **Depends on:** the AVD inventory from
[VESNA-QA-002](#vesna-qa-002-home-and-launch-return-verification-done) (plus
the dedicated API 26 AVD).

## Engineering work items

### ENG-101: Workspace page identities and schema migration

**Status:** done — shipped in 0.4.3. Stable page identifiers and the additive
Room 3→4 migration follow [ADR-PAGE-1](../adr/0001-workspace-page-identity.md).
The migration preserved existing placements on the first page. Page controls
also shipped; page management by drag (`HOME-104`) remains a separate research
candidate. See the [migration evidence](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids).

### ENG-102: Widget host — lifecycle only

Add/update/remove widgets; host cleanup on package removal and after restore.
Stable page IDs from ENG-101 are available. The
[widget lifecycle ADR](../adr/0003-widget-host-lifecycle.md) proposes the
ownership and cleanup contract. Before enabling placement, verify provider
configuration cancellation, restart recovery, and restore behavior against
that contract. Removal with undo (`WIDGET-102`) is a separate interaction.

### ENG-103: Folder pagination and drag-into verification

Verified drag-in from Home and the drawer, restart persistence, and scrolling
through 11–12 members on Android 8 and 15. Fixed gallery items drawing over
the folder header and actions at enlarged text sizes.
See [ENG-103 device evidence](verification/VESNA-QA-ENG-103.md).

Keep the scrollable grid. The published creation, rename, and dismissal
scenarios do not establish a need for pagination; revisit only with evidence
that the current interaction is insufficient.

### ENG-104: Settings search and structured preferences

**Status:** done — Settings navigation shipped in 0.4.4 and Settings search
shipped in 0.4.5.

Settings reset is not part of this item. It belongs to the backup/reset policy
([ENG-106](#eng-106-backuprestore-policy), research `BACKUP-101`).

`NT-SETTING-01` implements clean Settings entry, named page links, and
keyboard-first Back for the current destinations. See the
[Android verification](verification/VESNA-QA-SETTINGS-NAVIGATION.md).
`SETT-110` adds pinned search and highlighted control targets. See the
[acceptance scenario and Android results](verification/VESNA-QA-SETTINGS-SEARCH.md).

### ENG-105: Cards surface

`CARDS-100` is condition-triggered content. Blocked on the **product/privacy
decision** — the location/manual-fallback handling is the open privacy question,
and notification indicators are a separate item. The `DRAWER-105`/`R-DRAWER-103.1`
ID reuse must be repaired in research first; contact opt-in (`R-DRAWER-103.1`)
stays an independent item, separate from the cards conflict. No planning beyond
the placeholder until the decision exists
([RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#known-weaknesses-in-the-research-matrix)).

### ENG-106: Backup/restore policy

Resolve the backup/privacy policy via **ADR-BACKUP-1**: the ADR may conclude to
**keep platform backup enabled and define its scope** — an explicit in-app
backup/restore feature is one option, not a requirement. Today the manifest
sets `allowBackup="true"` without a feature; that is a default, not a decision.
Settings **reset** also lands here (research `BACKUP-101`, partial coverage);
it needs its research scenario completed via
[ENG-107](#eng-107-research-matrix-fix-ups-research-branch-not-trunk). Related
research gap: `BACKUP-102` has no dedicated scenario.

### ENG-107: Research-matrix fix-ups (research branch, not trunk)

Author the missing scenarios the matrix claims as complete (`HOME-104`,
`DRAWER-104`), resolve `DRAWER-102` Back/IME ambiguity, repair the `DRAWER-105`
ID reuse of `R-DRAWER-103.1` (contact opt-in stays independent), add
`SETT-110`/`BACKUP-102` scenarios and complete `BACKUP-101` reset coverage,
resolve the `WIDGET-100` placement/provider config-cancel gap and specify
`WIDGET-102` removal/undo. Done on `vladon/vesna-research`; trunk items
un-block as each lands.

## Candidate items (unapproved)

Initial product directions kept as candidates only — no approval implied; each
needs research scenarios (via [ENG-107](#eng-107-research-matrix-fix-ups-research-branch-not-trunk))
and an explicit approval before becoming engineering work:

| ID | Candidate | Notes |
|---|---|---|
| CAND-201 | Appearance/theming | Themes, icon customization; research `appearance` spec at the pinned commit |
| CAND-202 | Essential gestures | Drawer-open settings, gesture assignment; blocked on live binding-value validation (research follow-up `ee15ed5`) |
| CAND-203 | Icon packs | Depends on appearance direction |
| CAND-204 | Extended dock/grid/folder capacity | More slots/rows/folder sizes; interacts with `DOCK-100`/`FOLDER-*` research gaps |
| CAND-205 | Accessibility | Content descriptions, focus order, TalkTeX-style navigation audit |
| CAND-206 | Foldables | Multi-window/large-screen layout behavior of the fixed 4×5 grid |
| CAND-207 | Work profiles | UX depth beyond current badged-app support; relates to the managed-profile verification gap |

---

## Product decisions for open issues

These are decisions on scope, not a request to start implementation. The
approvals apply only to the named issues, not to the rest of CAND-201,
CAND-203, or CAND-204. They do not override documented dependencies, design
checks, or ADR gates.

| Issue | Decision |
|---|---|
| [#112](https://github.com/vladon/vesnalauncher/issues/112) — Keyboard on drawer open and frequent apps | Split the scope. Approve a separate, off-by-default option that focuses search and opens the keyboard when the drawer opens. Do not collect launch history or build frequency suggestions as part of that work; defer them. |
| [#146](https://github.com/vladon/vesnalauncher/issues/146) — Home and drawer icon sizes | Approve separate size controls with **Follow Home** as the drawer default. Preserve the current default appearance and remember the independent drawer size while linked. Resize artwork only; leave cells, touch targets and placements unchanged. |
| [#116](https://github.com/vladon/vesnalauncher/issues/116) — Layout export and restore | Approve an explicit export to a user-selected file and restore from it, without launcher-managed cloud backup. This is separate from Android-managed transfer. Show what will be restored and which shortcuts are unavailable before asking for confirmation. Replace the layout atomically only after validation and explicit confirmation. Cancel, invalid input, or failure must leave the current layout unchanged. Update and review ADR-BACKUP-1 for this scope before implementation. |
| [#144](https://github.com/vladon/vesnalauncher/issues/144) — Adaptive icon shape | Approve two choices: the current default shape and a rounded square. Apply the choice only to supported adaptive icons on Home, in the dock, and in the drawer; leave legacy artwork and profile badges unchanged. |
| [#155](https://github.com/vladon/vesnalauncher/issues/155) — Launcher palette | Approve curated palettes, not arbitrary color input. Each palette needs contrast-checked Light and Dark variants; the existing Vesna palette remains the default, and the palette choice is independent of System, Light, or Dark mode. |
| [#158](https://github.com/vladon/vesnalauncher/issues/158) — Drawer background color | Defer until #155 provides color and contrast rules. Keep a separate drawer setting with **Follow theme** and a small set of contrast-checked backgrounds. Do not change Home, the dock, or the global theme. |
| [#157](https://github.com/vladon/vesnalauncher/issues/157) — Installed icon packs | Approve after #144 and #146, initially for one documented format verified with Arcticons. Use system artwork for unmatched icons or if the pack is removed, retain profile badges, and do not apply the #144 mask to pack artwork. |
| [#115](https://github.com/vladon/vesnalauncher/issues/115) — Independent dock settings and pages | Approve in stages: visibility and icon size first, then capacity and paging. On reduction, carry overflow to subsequent pages in order; reject the change if the new total capacity cannot hold every pin. Do not move or remove Home placements. |
| [#113](https://github.com/vladon/vesnalauncher/issues/113) — External search and suggestions | Split the scope. Approve optional, off-by-default actions that pass a query to the selected app only on an explicit tap. Do not add network suggestions during typing or change the offline local search. |
| [#149](https://github.com/vladon/vesnalauncher/issues/149) — Searchable widget catalog | Do not ship a browse-only entry. Revisit as part of a working add-widget flow after accepting ADR-WIDGET-1 and verifying placement, cancellation, and restart recovery under ENG-102. |
| [#133](https://github.com/vladon/vesnalauncher/issues/133) — Horizontal folder paging | Keep vertical scrolling under ENG-103. Defer horizontal paging until there is evidence that vertical navigation prevents or materially hinders access to members or folder actions. |

The first suggested implementation, once work resumes, is the keyboard-only
scope from #112. Independent icon sizes (#146) and local layout export/restore
(#116, after its ADR review) are the next priorities.

---

## ADR candidates

Architecture decisions and proposals for the work above. Each linked ADR
records whether its design is accepted or still awaiting review.

### ADR-PAGE-1: Workspace page identity and its migration

Accepted for storage and migration: [ADR 0001](../adr/0001-workspace-page-identity.md)
defines stable page IDs, page-local cells, and Room schema version 4.

### ADR-WIDGET-1: Widget host cleanup

[ADR 0003](../adr/0003-widget-host-lifecycle.md) proposes widget ID allocation,
cleanup on removal, and restart recovery. It gates ENG-102.

### ADR-BACKUP-1: Backup and privacy policy

[ADR 0002 — Backup and privacy](../adr/0002-backup-and-privacy.md) drafts scoped
Android device-to-device transfer only. No cloud backup, analytics, tracking,
or in-app export. Implementation remains gated on review and transport tests.
The [#116](https://github.com/vladon/vesnalauncher/issues/116) decision above
approves export to a user-selected file and restore from it, which this draft
excludes. Revise and review ADR 0002 for that scope before any #116 work; its
other exclusions, including cloud backup, analytics, and tracking, are
unchanged.

---

## Done

| Item | Evidence |
|---|---|
| Adaptive icon shape ([#144](https://github.com/vladon/vesnalauncher/issues/144)) | 0.4.74 — [Android 15 QA](verification/VESNA-QA-ICON-SHAPE.md) verified the unchanged default, rounded squares on Home, in the dock, the drawer and a folder, unchanged legacy icons, switching back, restart, Settings search, Dark, and Russian at 130 % text. |
| Separate Home and app drawer icon sizes ([#146](https://github.com/vladon/vesnalauncher/issues/146)) | 0.4.73 — [Android 15 QA](verification/VESNA-QA-ICON-SIZES.md) verified the unchanged default, Home scaling with the drawer linked and unlinked, a remembered drawer size, fixed list rows, restart, Settings search, Dark, and compact RTL. |
| Cross-page drops onto an occupied cell create or extend a folder ([#168](https://github.com/vladon/vesnalauncher/issues/168)) | 0.4.72 — [Android 15 QA](verification/VESNA-QA-CROSS-PAGE-FOLDER-DROPS.md) verified an app onto an app and onto a folder from another page, unchanged other rows and dock, cancellation, an invalid release, the pin's own cell, restart, and compact RTL. |
| Optional keyboard when the app drawer opens ([#112](https://github.com/vladon/vesnalauncher/issues/112), approved scope only) | 0.4.71 — [Android 15 QA](verification/VESNA-QA-DRAWER-KEYBOARD-ON-OPEN.md) verified the off-by-default switch, both entry points, Back, restart, recreation, Settings search, Dark, and compact RTL. Frequency suggestions stay deferred. |
| Slow upward swipe opens the drawer instead of the workspace menu ([#170](https://github.com/vladon/vesnalauncher/issues/170)) | 0.4.70 — [Android 15 QA](verification/VESNA-QA-SLOW-SWIPE-DRAWER.md) verified 1100 px swipes from 300 to 925 ms opening the drawer once, a slow 400 px swipe opening neither surface, the stationary-hold menu, and an unchanged tile hold. |
| Create a Home page by dropping a closed folder past the last page ([#167](https://github.com/vladon/vesnalauncher/issues/167)) | 0.4.69 — [Android 15 QA](verification/VESNA-QA-WHOLE-FOLDER-EDGE-APPEND.md) verified the blank preview, cancellation and invalid release, exact placement with unchanged name and member order, restart, the page cap, and compact RTL. |
| Edge-drag a closed Home folder to an existing neighboring page ([#163](https://github.com/vladon/vesnalauncher/issues/163)) | 0.4.68 — [Android 15 QA](verification/VESNA-QA-WHOLE-FOLDER-EDGE-NEIGHBOR.md) verified exact LTR/RTL placement, cancellation, occupied cells, stable membership, and restart without creating an extra page. |
| Create a Home page by dropping a folder member past the last page ([#160](https://github.com/vladon/vesnalauncher/issues/160)) | 0.4.67 — [Android 15 QA](verification/VESNA-QA-FOLDER-MEMBER-EDGE-APPEND.md) verified exact drops and cancellation, sparse-folder cleanup on the source page, compact RTL, and restart. |
| Unpin a Home folder with its shortcuts and Undo ([#132](https://github.com/vladon/vesnalauncher/issues/132)) | 0.4.66 — [Android 15 QA](verification/VESNA-QA-FOLDER-UNPIN-UNDO.md) verified an independent action, exact cell and order on Undo, layout lock, stable other pins, and the packed 2 × 2 case. |
| Create a Home page by dropping an app shortcut past the last page ([#151](https://github.com/vladon/vesnalauncher/issues/151)) | 0.4.65 — [Android 15 QA](verification/VESNA-QA-HOME-EDGE-APPEND.md) verified mirrored blank previews, cancellation and invalid release, exact placement and restart. |
| Remove a secondary Home page emptied by unpinning ([#153](https://github.com/vladon/vesnalauncher/issues/153)) | 0.4.64 — [Android 15 QA](verification/VESNA-QA-HOME-PAGE-UNPIN.md) verified immediate cleanup, stable remaining placements, cold restart and compact RTL keyboard removal. |
| Hide Home app labels without hiding drawer labels ([#152](https://github.com/vladon/vesnalauncher/issues/152)) | 0.4.63 — [Android 15 QA](verification/VESNA-QA-HOME-LABEL-VISIBILITY.md) verified independent Home and drawer labels, restart, and unchanged placement and accessibility. |
| Remember the vertical list with drawer auto-close enabled ([#147](https://github.com/vladon/vesnalauncher/issues/147)) | 0.4.62 — [Android 15 QA](verification/VESNA-QA-DRAWER-REMEMBER-AUTO-CLOSE.md) verified exact Clock → Android Home restoration, Back, filtered launch, both switch controls and restart. |
| Drag a Home or folder shortcut to an existing neighboring page ([#139](https://github.com/vladon/vesnalauncher/issues/139)) | 0.4.61 — [Android 15 QA](verification/VESNA-QA-HOME-EDGE-DRAG.md) verified held previews, exact cells, occupied and canceled drops, layout lock, restart and RTL. |
| Add several apps to an existing Home folder ([#134](https://github.com/vladon/vesnalauncher/issues/134)) | 0.4.60 — [Android 15 QA](verification/VESNA-QA-FOLDER-BATCH-ADD.md) verified one save, both cancel paths, layout lock, restart, single-app drag, and enlarged keyboard access. |
| Remember the vertical drawer list after Android Home ([#141](https://github.com/vladon/vesnalauncher/issues/141)) | 0.4.59 — [Android 15 QA](verification/VESNA-QA-DRAWER-REMEMBER-POSITION.md) verified both switch states, exact row bounds, navigation modes and preference persistence. |
| One-action alphabetical Home folder sorting ([#135](https://github.com/vladon/vesnalauncher/issues/135)) | 0.4.58 — [Android 15 QA](verification/VESNA-QA-FOLDER-SORT.md) verified the explicit command, localized and custom labels, unchanged membership and cell, lock, and restart. |
| Reorder shortcuts inside an open Home folder ([#131](https://github.com/vladon/vesnalauncher/issues/131)) | 0.4.57 — [Android 15 QA](verification/VESNA-QA-FOLDER-REORDER.md) verified insertion, edge scroll, cancellation, lock and restart without regressing drag-out. |
| Optional app drawer auto-close ([#137](https://github.com/vladon/vesnalauncher/issues/137)) | 0.4.56 — [Android 15 QA](verification/VESNA-QA-DRAWER-AUTO-CLOSE.md) verified Back to the same Home page when on and the unchanged scrolled list when off. |
| Drag a member out of a Home folder ([#130](https://github.com/vladon/vesnalauncher/issues/130)) | 0.4.55 — [Android 15 QA](verification/VESNA-QA-FOLDER-MEMBER-DRAG.md) verified chosen cells, cancellation, layout lock, dissolution, and restart. Dock folders are out of scope. |
| NT-HOME-05: App actions and shortcut names on Home ([#114](https://github.com/vladon/vesnalauncher/issues/114)) | 0.4.54 — [Android 15 QA](verification/VESNA-QA-WORKSPACE-APP-ACTIONS.md) verified real published actions, pin/rename, layout lock, and Room v5 → v6. Widgets remain gated on ENG-102. |
| NT-HOME-03: Resize the workspace grid without losing shortcuts ([#106](https://github.com/vladon/vesnalauncher/issues/106)) | 0.4.53 — verified the Room v4 → v5 upgrade, edge-cell reflow and overflow, restart, and unchanged dock in [Android 15 QA](verification/VESNA-QA-WORKSPACE-GRID.md). |
| NT-HOME-04: Opt-in placement of newly installed apps ([#107](https://github.com/vladon/vesnalauncher/issues/107)) | 0.4.52 — verified first install with the setting off and on, updates, locked layout, and no changes while Vesna is stopped in [Android 15 QA](verification/VESNA-QA-NEW-APP-PLACEMENT.md). |
| NT-DRAWER-05: Selectable drawer layouts ([#108](https://github.com/vladon/vesnalauncher/issues/108)) | 0.4.51 — verified vertical grid, list, horizontal paging, global search, and persisted layout selection in [Android 15 QA](verification/VESNA-QA-DRAWER-LAYOUTS.md). |
| NT-DRAWER-09: Hide individual apps from drawer browsing ([#119](https://github.com/vladon/vesnalauncher/issues/119)) | 0.4.50 — verified browsing, exact search, unchanged pins, restart and unhide in [Android 15 QA](verification/VESNA-QA-DRAWER-HIDDEN-APPS.md). |
| NT-DRAWER-06: Alphabetical drawer fast scroller ([#109](https://github.com/vladon/vesnalauncher/issues/109)) | 0.4.49 — verified real thumb drags, visible letters, search and launch in [Android 15 QA](verification/VESNA-QA-DRAWER-FAST-SCROLL.md). |
| NT-HOME-01: Open the workspace from the Vesna app icon ([#101](https://github.com/vladon/vesnalauncher/issues/101)) | Shipped in [0.4.46](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.46) via [PR #104](https://github.com/vladon/vesnalauncher/pull/104). On an Android 15 emulator running the signed APK, real icon taps from Pixel Launcher opened Vesna's workspace on cold launch and relaunch. Vesna's drawer launched Clock, Android Home returned to Pixel Launcher without changing the Home role, and Settings opened from Home options and all four deep links. |
| Drawer search QA baseline (`R-DRAWER-100.4` order-preservation subset, `R-DRAWER-101.1` local-filtering subset) | `DrawerSearchTest` added in PR #9 and strengthened by [VESNA-QA-008 (PR #34)](https://github.com/vladon/vesnalauncher/pull/34). The updated suite passed 8/8 on the dedicated API 35 AVD. |
| VESNA-QA-004 migration and restart verification | Part A: `RoomMigrationQaTest` ([PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`), both migration cases plus `DrawerSearchTest` on the dedicated API 35 AVD; part B: [VESNA-QA-004B](verification/VESNA-QA-004B.md) (force-stop, full guest reboot, no re-seed) — see [PROJECT_STATUS.md](PROJECT_STATUS.md#instrumentation-verification-record) |
| VESNA-QA-005 minimum SDK (API 26) smoke verification | [VESNA-QA-005-API26](verification/VESNA-QA-005-API26.md) (2026-11-19, dedicated API 26 AVD): build, HOME discoverability, cold start, workspace/drawer render, fixture round-trip, instrumentation suite 3/3; VESNA-BUG-004 fixed in [PR #23](https://github.com/vladon/vesnalauncher/pull/23) |
| VESNA-QA-006 Android 16 (API 36.1) smoke verification | [VESNA-QA-006-API36-1](verification/VESNA-QA-006-API36-1.md) (2026-11-19, dedicated API 36.1 AVD on Windows): instrumentation 5/5 at `332fb03` and 7/7 with the integrated BUG-005 candidate `d8d6a748` (later merged as [PR #27](https://github.com/vladon/vesnalauncher/pull/27)); HOME/launch-return smoke; scope limited to that AVD configuration |
| VESNA-QA-007 debug cold-launch timing baseline | [VESNA-QA-007-COLD-START](verification/VESNA-QA-007-COLD-START.md) (2026-11-19, PR #33): ten process-cold `HomeActivity` launches, debug 0.4.1 at `81cea18`, Android 15/API 35 shared-host emulator; see the run record for ActivityManager `TotalTime` observations |
| VESNA-QA-009 launch-state timing verification | [VESNA-QA-009-LAUNCH-STATES](verification/VESNA-QA-009-LAUNCH-STATES.md) (2026-11-19, [PR #40](https://github.com/vladon/vesnalauncher/pull/40)): paired cold and warm ActivityManager timings for one Android 15 debug-emulator configuration |
| VESNA-QA-010 Android 12 (API 31) smoke verification | [VESNA-QA-010-API31](verification/VESNA-QA-010-API31.md) (2026-11-19, [PR #43](https://github.com/vladon/vesnalauncher/pull/43)): 12/12 instrumentation tests on the dedicated API 31 AVD at the debug `75ca3ed` candidate from [PR #39](https://github.com/vladon/vesnalauncher/pull/39); workspace/drawer render; HOME-registration check kept the stock HOME; boot fallback and teardown in the record. The follow-up passed 16/16 on the same API 31 AVD at `1b5dc2e`. |
